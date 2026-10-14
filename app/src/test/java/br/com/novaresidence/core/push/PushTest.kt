package br.com.novaresidence.core.push

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.network.ApiTransport
import br.com.novaresidence.core.network.NovaJson
import br.com.novaresidence.core.session.SessionCoordinator
import br.com.novaresidence.testSession
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PushTest {
    private val server = MockWebServer()
    private val store = MemoryPushStore()
    private lateinit var session: SessionCoordinator
    private lateinit var registration: PushRegistration
    private var fcmToken = "fcm-token-1"
    private var device = PushDevice(true, true, "1.0", "Android 9", "samsung SM-J400M")
    private var clock = 1_000_000L

    @Before fun setup() = runBlocking {
        server.start()
        session = SessionCoordinator(TestSessionStore()) { error("Refresh não esperado") }
        session.replace(testSession(), session.generation())
        registration = PushRegistration(ApiClient(ApiTransport(server.url("/").toString()), session), store, { fcmToken }, { device }, { clock })
    }

    @After fun cleanup() { server.shutdown() }

    private fun enqueueRegistered() = server.enqueue(MockResponse().setBody(
        """{"success":true,"data":{"platform":"android","installationId":"${store.installationId}","notificationsEnabled":true,"alarmChannelEnabled":true,"registeredAt":"2026-10-13T22:00:00.000Z"},"meta":{}}""",
    ))

    @Test fun `registro envia o aparelho por PUT e fica pronto`() = runBlocking {
        enqueueRegistered()
        registration.register()
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/v3/api/notifications/devices/android", request.path)
        assertEquals("Bearer access", request.getHeader("Authorization"))
        val body = NovaJson.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(store.installationId, body["installationId"]!!.jsonPrimitive.content)
        assertEquals("fcm-token-1", body["token"]!!.jsonPrimitive.content)
        assertTrue(body["notificationsEnabled"]!!.jsonPrimitive.boolean)
        assertEquals(PushStatus.Ready, registration.status.value)
        // O token FCM não fica salvo em claro no aparelho.
        assertFalse(store.registration!!.contains("fcm-token-1"))
    }

    @Test fun `sem mudanca nao chama a API de novo, mas renova depois de 24 h ou com token novo`() = runBlocking {
        enqueueRegistered()
        registration.register()
        registration.register()
        assertEquals(1, server.requestCount)

        clock += 25 * 60 * 60 * 1000L
        enqueueRegistered()
        registration.register()
        assertEquals(2, server.requestCount)

        fcmToken = "fcm-token-2"
        enqueueRegistered()
        registration.register()
        assertEquals(3, server.requestCount)
    }

    @Test fun `permissao ou canal do alarme mudam o estado e registram de novo`() = runBlocking {
        enqueueRegistered()
        registration.register()
        device = device.copy(alarmChannelEnabled = false)
        registration.refreshStatus()
        assertEquals(PushStatus.ChannelBlocked, registration.status.value)
        enqueueRegistered()
        registration.register()
        assertEquals(2, server.requestCount)
        device = device.copy(notificationsEnabled = false)
        registration.refreshStatus()
        assertEquals(PushStatus.PermissionNeeded, registration.status.value)
    }

    @Test fun `sessao com troca de senha ou aceite pendente nao registra`() = runBlocking {
        session.replace(testSession(mandatory = true), session.generation())
        registration.register()
        assertEquals(0, server.requestCount)
        assertEquals(PushStatus.NotRegistered, registration.status.value)
    }

    @Test fun `falha da API nao marca como registrado`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setBody(
            """{"success":false,"error":{"type":"urn:nova-api:upstream-error","title":"x","status":503,"detail":"x","instance":"/"},"meta":{}}""",
        ))
        val error = runCatching { registration.register() }.exceptionOrNull()
        assertEquals(ApiErrorKind.Unavailable, (error as ApiException).kind)
        assertNull(store.registration)
        assertEquals(PushStatus.NotRegistered, registration.status.value)
    }

    @Test fun `remocao usa o access token recebido e esquece o registro mesmo com falha`() = runBlocking {
        enqueueRegistered()
        registration.register()
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setBody(
            """{"success":false,"error":{"type":"urn:nova-api:internal-error","title":"x","status":500,"detail":"x","instance":"/"},"meta":{}}""",
        ))
        runCatching { registration.unregister("token-capturado") }
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v3/api/notifications/devices/android/${store.installationId}", request.path)
        assertEquals("Bearer token-capturado", request.getHeader("Authorization"))
        assertNull(store.registration)
    }

    @Test fun `aviso do FCM vira mensagem com canal, destino e chave de substituicao`() {
        val alarm = PushMessage.from(mapOf("type" to "cie.alarm", "title" to "Alarme de incêndio", "body" to "Torre B", "channel" to "general", "target" to "fire"))!!
        assertEquals(PushChannels.FireAlarm, alarm.channel)
        assertEquals(PushTarget.Fire, alarm.target)
        assertEquals("cie-alarm", alarm.tag)
        assertTrue(alarm.isFireAlarm)
        val cleared = PushMessage.from(mapOf("type" to "cie.alarm.cleared", "title" to "Encerrado", "channel" to "cie_status", "target" to "fire"))!!
        assertEquals(alarm.tag, cleared.tag)

        val exhaust = PushMessage.from(mapOf("type" to "exhaust.expiring", "title" to "Exaustor", "channel" to "exhaust", "target" to "exhaust", "exhaustId" to "A4"))!!
        assertEquals("exhaust:A4", exhaust.tag)
        val message = PushMessage.from(mapOf("type" to "message.published", "title" to "Aviso", "channel" to "messages", "target" to "message", "messageId" to "12"))!!
        assertEquals(PushOpen(PushTarget.Message, 12), message.open())

        val unknownChannel = PushMessage.from(mapOf("type" to "novo.tipo", "title" to "Novo", "channel" to "canal-novo", "target" to "outro"))!!
        assertEquals(PushChannels.General, unknownChannel.channel)
        assertEquals(PushTarget.Home, unknownChannel.target)
        assertNull(PushMessage.from(mapOf("title" to "Sem tipo")))

        // Notificação só fora da tela do aviso; o alarme aparece sempre.
        assertFalse(exhaust.shouldNotify(PushTarget.Exhaust))
        assertTrue(exhaust.shouldNotify(PushTarget.Fire))
        assertTrue(exhaust.shouldNotify(null))
        assertFalse(message.shouldNotify(PushTarget.Message))
        assertFalse(cleared.shouldNotify(PushTarget.Fire))
        assertTrue(alarm.shouldNotify(PushTarget.Fire))
        assertNull(PushMessage.from(mapOf("type" to "cie.alarm")))
    }
}

private class MemoryPushStore : PushStore {
    override val installationId = "8d0f2a52-3c1f-4c8b-9a51-6f0c2d3e4b5a"
    override var registration: String? = null
    override var registeredAt = 0L
    override var permissionAsked = false
    override var promptDismissed = false
}

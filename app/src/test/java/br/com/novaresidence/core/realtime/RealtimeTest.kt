package br.com.novaresidence.core.realtime

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiTransport
import br.com.novaresidence.core.session.SessionCoordinator
import br.com.novaresidence.feature.fire.cieSignature
import br.com.novaresidence.testSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RealtimeTest {
    private val server = MockWebServer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var client: RealtimeClient
    private val received = Collections.synchronizedList(mutableListOf<String>())

    private fun ticket(value: String) = MockResponse().setBody("""{"success":true,"data":{"ticket":"$value","expiresIn":30},"meta":{}}""")

    /** Socket do servidor que já cumprimenta, manda [events] e opcionalmente fecha com [closeCode]. */
    private fun socket(events: List<String> = emptyList(), closeCode: Int? = null, closed: CountDownLatch? = null) =
        MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""{"event":"connection.established","timestamp":"x","data":{"ok":true},"meta":{"version":"v3"}}""")
                events.forEach { webSocket.send("""{"event":"$it","timestamp":"x","data":{"service":"cie"},"meta":{"version":"v3"}}""") }
                closeCode?.let { webSocket.close(it, "fim") }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Responde o fechamento, como o servidor real; sem isso o MockWebServer não encerra.
                webSocket.close(1000, null)
                closed?.countDown()
            }
        })

    @Before fun setup() = runBlocking {
        server.start()
        val session = SessionCoordinator(TestSessionStore()) { error("Refresh não esperado") }
        session.replace(testSession(), session.generation())
        val api = ApiClient(ApiTransport(server.url("/").toString()), session)
        client = RealtimeClient(api, "ws://${server.hostName}:${server.port}", scope, backoff = { 50L })
        scope.launch { client.events.collect { received += it.name } }
        delay(50)
    }

    @After fun cleanup() {
        client.stop()
        Thread.sleep(200)
        scope.cancel()
        server.shutdown()
    }

    private fun waitUntil(condition: () -> Boolean) = runBlocking {
        withTimeout(5_000) { while (!condition()) delay(20) }
    }

    @Test fun `pede ticket, conecta e repassa os eventos com o aviso de reconexao`() = runBlocking {
        server.enqueue(ticket("t1"))
        server.enqueue(socket(listOf("cie.active.updated")))
        client.start()
        withTimeout(5_000) { client.connected.first { it } }
        waitUntil { "cie.active.updated" in received }

        val ticketRequest = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("POST", ticketRequest.method)
        assertEquals("/v3/api/ws/ticket", ticketRequest.path)
        assertEquals("Bearer access", ticketRequest.getHeader("Authorization"))
        assertEquals("/v3/ws?ticket=t1", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
        assertEquals(listOf(RealtimeEvent.Connected, "connection.established", "cie.active.updated"), received.toList())
    }

    @Test fun `token expirado (4001) pede ticket novo e reconecta`() = runBlocking {
        server.enqueue(ticket("t1"))
        server.enqueue(socket(closeCode = 4001))
        server.enqueue(ticket("t2"))
        server.enqueue(socket())
        client.start()
        waitUntil { received.count { it == RealtimeEvent.Connected } == 2 }
        val paths = (1..4).map { server.takeRequest(5, TimeUnit.SECONDS)!!.path }
        assertEquals(listOf("/v3/api/ws/ticket", "/v3/ws?ticket=t1", "/v3/api/ws/ticket", "/v3/ws?ticket=t2"), paths)
    }

    @Test fun `conta sem acesso (403 no handshake) para de tentar`() = runBlocking {
        server.enqueue(ticket("t1"))
        server.enqueue(MockResponse().setResponseCode(403))
        client.start()
        waitUntil { server.requestCount == 2 }
        delay(400)
        assertEquals(2, server.requestCount)
        assertFalse(client.connected.value)
    }

    @Test fun `servidor cheio (503) tenta de novo com espera`() = runBlocking {
        server.enqueue(ticket("t1"))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(ticket("t2"))
        server.enqueue(socket())
        client.start()
        withTimeout(5_000) { client.connected.first { it } }
        assertEquals(4, server.requestCount)
    }

    @Test fun `stop fecha o socket e nao reconecta`() = runBlocking {
        val closed = CountDownLatch(1)
        server.enqueue(ticket("t1"))
        server.enqueue(socket(closed = closed))
        client.start()
        withTimeout(5_000) { client.connected.first { it } }
        client.stop()
        assertTrue(closed.await(5, TimeUnit.SECONDS))
        assertFalse(client.connected.value)
        delay(300)
        assertEquals(2, server.requestCount)
    }

    @Test fun `assinatura da central ignora relogio e muda com contadores, conexao e reinicio`() {
        fun status(alarme: Int = 0, falha: Int = 0, connected: Boolean = true, restartingUntil: Long? = null, clock: String = "10:00:00") =
            Json.parseToJsonElement("""{"connected":$connected,"restartingUntil":$restartingUntil,"dataHora":"$clock","lastUpdated":${clock.length},
                "status":{"status":{"alarme":$alarme,"falha":$falha,"supervisao":0,"bloqueio":0},"leds":{}}}""")
        val now = 1_000_000L
        val base = cieSignature(status(), now)
        assertNotNull(base)
        assertEquals(base, cieSignature(status(clock = "10:00:01"), now))
        assertNotEquals(base, cieSignature(status(alarme = 1), now))
        assertNotEquals(base, cieSignature(status(falha = 2), now))
        assertNotEquals(base, cieSignature(status(connected = false), now))
        assertNotEquals(base, cieSignature(status(restartingUntil = now + 60_000), now))
        assertEquals(base, cieSignature(status(restartingUntil = now - 1), now))
        assertNull(cieSignature(Json.parseToJsonElement("""{"ok":true}"""), now))
    }
}

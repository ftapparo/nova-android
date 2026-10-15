package br.com.novaresidence.feature.notification

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiTransport
import br.com.novaresidence.core.network.NovaJson
import br.com.novaresidence.core.session.SessionCoordinator
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.asUiText
import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.testSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class NotificationTest {
    private val main = newSingleThreadContext("notification-test")
    private val server = MockWebServer()
    private lateinit var repository: NotificationRepository
    private fun preferences(messages: Boolean = true) =
        """{"success":true,"data":{"exhaustExpiring":true,"exhaustExpiringMinutes":15,"exhaustOff":true,"messages":$messages,"cieStaffAlerts":true},"meta":{}}"""
    private fun failure(type: String, status: Int, detail: String) = MockResponse().setResponseCode(status)
        .setBody("""{"success":false,"error":{"type":"urn:nova-api:$type","title":"x","status":$status,"detail":"$detail","instance":"/"},"meta":{}}""")

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(main)
        server.start()
        val session = SessionCoordinator(TestSessionStore()) { error("Refresh não esperado") }
        session.replace(testSession(), session.generation())
        repository = NotificationRepository(ApiClient(ApiTransport(server.url("/").toString()), session))
    }

    @After fun cleanup() {
        Dispatchers.resetMain()
        main.close()
        server.shutdown()
    }

    @Test fun `preferencia alterada envia so o campo e desfaz quando a API recusa`() = runBlocking {
        server.enqueue(MockResponse().setBody(preferences()))
        val model = NotificationViewModel(repository)
        withContext(main) { model.load() }
        withTimeout(5000) { model.state.first { it.preferences != null } }
        assertEquals("/v3/api/notifications/preferences", server.takeRequest().path)

        server.enqueue(MockResponse().setBody(preferences(messages = false)))
        withContext(main) { model.set(NotificationOption.Messages, false) }
        val saved = withTimeout(5000) { model.state.first { it.saving == null && it.preferences?.messages == false } }
        assertNull(saved.error)
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals(setOf("messages"), NovaJson.parseToJsonElement(put.body.readUtf8()).jsonObject.keys)

        server.enqueue(failure("internal-error", 500, "x"))
        withContext(main) { model.set(NotificationOption.Messages, true) }
        val reverted = withTimeout(5000) { model.state.first { it.saving == null && it.error != null } }
        assertFalse(reverted.preferences!!.messages)
    }

    @Test fun `teste mostra o motivo da API quando o envio esta desligado`() = runBlocking {
        server.enqueue(failure("upstream-error", 503, "O envio de notificações está desativado no servidor."))
        val model = NotificationViewModel(repository)
        withContext(main) { model.sendTest() }
        val state = withTimeout(5000) { model.state.first { it.testMessage != null } }
        assertEquals("O envio de notificações está desativado no servidor.".asUiText(), state.testMessage)
        assertEquals("POST", server.takeRequest().method)

        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"targets":0,"sent":0,"failed":0},"meta":{}}"""))
        withContext(main) { model.sendTest() }
        val none = withTimeout(5000) { model.state.first { !it.testing && it.testMessage == uiText(R.string.notification_test_no_device) } }
        assertNotNull(none.testMessage)
    }
}

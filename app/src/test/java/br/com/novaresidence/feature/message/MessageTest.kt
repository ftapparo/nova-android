package br.com.novaresidence.feature.message

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiTransport
import br.com.novaresidence.core.session.SessionCoordinator
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.asUiText
import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.testSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class MessageTest {
    private val main = newSingleThreadContext("message-test")
    private val server = MockWebServer()
    private lateinit var repository: MessageRepository
    private fun response(data: String) = MockResponse().setBody("""{"success":true,"data":$data,"meta":{"requestId":"test"}}""")
    private fun message(id: Int, read: Boolean = false, category: String = "manutencao", label: String? = null) =
        """{"id":$id,"category":"$category","label":${label?.let { "\"$it\"" } ?: "null"},"title":"Comunicado $id","body":"Texto","publishedAt":"2026-10-13T13:30:00.000Z","expiresAt":null,"read":$read}"""

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(main)
        server.start()
        val session = SessionCoordinator(TestSessionStore()) { error("Refresh não esperado") }
        session.replace(testSession(), session.generation())
        repository = MessageRepository(ApiClient(ApiTransport(server.url("/").toString()), session))
    }

    @After fun cleanup() {
        Dispatchers.resetMain()
        main.close()
        server.shutdown()
    }

    @Test fun `rotulos usam a etiqueta da administracao ou o nome da categoria`() {
        val base = CondoMessage(1, "manutencao", null, "T", "B", "2026-10-13T13:30:00Z")
        assertEquals(uiText(R.string.message_category_maintenance), base.badgeLabel())
        assertEquals("Manutenção programada".asUiText(), base.copy(label = "Manutenção programada").badgeLabel())
        assertEquals(uiText(R.string.message_category_maintenance), base.copy(label = "Manutenção programada").categoryLabel())
        assertEquals(uiText(R.string.message_category_urgent), base.copy(category = "urgente").categoryLabel())
        assertNotNull(base.publishedInstant)
    }

    @Test fun `home consulta o carrossel da API e o contador de nao lidos`() = runBlocking {
        server.enqueue(response("""{"items":[${message(2)},${message(1, read = true, label = "Manutenção programada")}],"unreadCount":1}"""))
        val model = MessageViewModel(repository)
        withContext(main) { model.start(true) }
        val state = withTimeout(5000) { model.state.first { it.latest.isNotEmpty() } }
        assertEquals(1, state.unreadCount)
        assertEquals("Manutenção programada".asUiText(), state.latest[1].badgeLabel())
        assertEquals("/v3/api/messages/carousel", server.takeRequest().path)
    }

    @Test fun `conta sem acesso aos servicos nao consulta comunicados`() = runBlocking {
        val model = MessageViewModel(repository)
        withContext(main) { model.start(false); model.loadList(false) }
        assertNull(server.takeRequest(300, TimeUnit.MILLISECONDS))
        assertEquals(MessageState(), model.state.value)
    }

    @Test fun `abrir o detalhe marca como lido e atualiza lista e contador`() = runBlocking {
        server.enqueue(response("""{"items":[${message(2)},${message(1, read = true)}],"unreadCount":1}"""))
        server.enqueue(response("""{"items":[${message(2)}],"unreadCount":1}"""))
        server.enqueue(response(message(2, read = true)))
        server.enqueue(response("""{"items":[${message(2, read = true)},${message(1, read = true)}],"unreadCount":0}"""))
        val model = MessageViewModel(repository)
        withContext(main) { model.start(true) }
        withTimeout(5000) { model.state.first { it.latest.isNotEmpty() } }
        withContext(main) { model.loadList(unreadOnly = true) }
        val list = withTimeout(5000) { model.state.first { it.list != null } }
        withContext(main) { model.markRead(list.list!!.single()) }
        val state = withTimeout(5000) { model.state.first { it.unreadCount == 0 } }
        assertTrue("lido sai do filtro de não lidas", state.list!!.isEmpty())
        assertTrue(state.latest.first { it.id == 2L }.read)
        server.takeRequest()
        assertEquals("/v3/api/messages?filter=unread&limit=50", server.takeRequest().path)
        val read = server.takeRequest()
        assertEquals("POST", read.method)
        assertEquals("/v3/api/messages/2/read", read.path)
        assertEquals("/v3/api/messages/carousel", server.takeRequest(5, TimeUnit.SECONDS)!!.path)
    }

    @Test fun `comunicado lido sai do carrossel quando a API o retira`() = runBlocking {
        server.enqueue(response("""{"items":[${message(3, category = "urgente")},${message(2)}],"unreadCount":2}"""))
        server.enqueue(response(message(3, read = true, category = "urgente")))
        // TTL 0: depois da leitura a API não devolve mais o 3 no carrossel desta conta.
        server.enqueue(response("""{"items":[${message(2)}],"unreadCount":1}"""))
        val model = MessageViewModel(repository)
        withContext(main) { model.start(true) }
        val first = withTimeout(5000) { model.state.first { it.latest.size == 2 } }
        assertEquals(listOf(3L, 2L), first.latest.map { it.id })
        withContext(main) { model.markRead(first.latest.first()) }
        val state = withTimeout(5000) { model.state.first { it.latest.size == 1 } }
        assertEquals(listOf(2L), state.latest.map { it.id })
        assertEquals(1, state.unreadCount)
    }
}

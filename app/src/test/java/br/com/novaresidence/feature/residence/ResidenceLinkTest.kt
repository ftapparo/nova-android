package br.com.novaresidence.feature.residence

import br.com.novaresidence.R
import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiTransport
import br.com.novaresidence.core.network.NovaJson
import br.com.novaresidence.core.session.SessionCoordinator
import br.com.novaresidence.core.ui.asUiText
import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.testSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class ResidenceLinkTest {
    private val main = newSingleThreadContext("link-test")
    private val server = MockWebServer()
    private lateinit var model: ResidenceLinkViewModel
    private val brazil = PhoneCountries.first { it.iso == "BR" }
    private val portugal = PhoneCountries.first { it.iso == "PT" }
    // CPF fictício com dígitos válidos (não é de nenhuma pessoa do cadastro).
    private val cpf = "52998224725"
    private val unit = """{"sequencia":97,"quadra":"A","lote":"124","label":"A-124","morador":true,"proprietario":true,"locatario":false,"blocked":false,"blockedReason":null}"""
    private val checked = """{"success":true,"data":{"confirmation":"token-de-teste-123","person":{"nomeMasked":"Mo***** de Te***","cpfMasked":"•••.982.247-••"},"units":[$unit]},"meta":{}}"""

    private fun failure(type: String, status: Int, detail: String = "x") = MockResponse().setResponseCode(status)
        .setBody("""{"success":false,"error":{"type":"urn:nova-api:$type","title":"x","status":$status,"detail":"$detail","instance":"/"},"meta":{"requestId":"req-1"}}""")

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(main)
        server.start()
        val session = SessionCoordinator(TestSessionStore()) { error("Refresh não esperado") }
        session.replace(testSession(), session.generation())
        model = ResidenceLinkViewModel(ResidenceRepository(ApiClient(ApiTransport(server.url("/").toString()), session)))
    }

    @After fun cleanup() {
        Dispatchers.resetMain()
        main.close()
        server.shutdown()
    }

    @Test fun `cpf e celular conferidos levam a conferencia e o confirmar vincula`() = runBlocking {
        server.enqueue(MockResponse().setBody(checked))
        withContext(main) { model.check(cpf, brazil, "17991234567") }
        val state = withTimeout(5000) { model.state.first { it.check != null } }
        assertEquals("Mo***** de Te***", state.check!!.person.nomeMasked)
        assertEquals("A-124", state.check!!.units.single().label)
        val request = server.takeRequest()
        assertEquals("/v3/api/residence/link/self/check", request.path)
        val body = NovaJson.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(cpf, body["cpf"]!!.jsonPrimitive.content)
        assertEquals("55", body["phone"]!!.jsonObject["ddi"]!!.jsonPrimitive.content)
        assertEquals("17991234567", body["phone"]!!.jsonObject["number"]!!.jsonPrimitive.content)

        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"status":"ativo","blocked":false,"person":{"sequencia":91,"nome":"MORADOR DE TESTE"},"units":[$unit]},"meta":{}}"""))
        var linked: Residence? = null
        withContext(main) { model.confirm { linked = it } }
        withTimeout(5000) { while (linked == null) delay(20) }
        assertEquals("ativo", linked!!.status)
        val confirm = server.takeRequest()
        assertEquals("/v3/api/residence/link/self/confirm", confirm.path)
        assertTrue(confirm.body.readUtf8().contains("token-de-teste-123"))
        assertNull(model.state.value.check)
    }

    @Test fun `dados invalidos no app nao chamam a API`() = runBlocking {
        withContext(main) { model.check("12345678900", brazil, "1799123") }
        val state = model.state.value
        assertEquals(uiText(R.string.link_invalid_cpf), state.cpfError)
        assertEquals(uiText(R.string.link_invalid_phone_br), state.phoneError)
        assertEquals(0, server.requestCount)
    }

    @Test fun `celular estrangeiro vai com o DDI do pais escolhido`() = runBlocking {
        server.enqueue(failure("not-found", 404))
        withContext(main) { model.check(cpf, portugal, "912345678") }
        val state = withTimeout(5000) { model.state.first { !it.busy && it.error != null } }
        // A API responde igual para CPF inexistente, celular diferente ou pessoa sem unidade.
        assertEquals(uiText(R.string.link_not_matched), state.error)
        val body = NovaJson.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("351", body["phone"]!!.jsonObject["ddi"]!!.jsonPrimitive.content)
    }

    @Test fun `pessoa vinculada a outra conta mostra a orientacao da API`() = runBlocking {
        server.enqueue(failure("conflict", 409, "Esta pessoa já está vinculada a outra conta. Procure a administração."))
        withContext(main) { model.check(cpf, brazil, "17991234567") }
        val state = withTimeout(5000) { model.state.first { !it.busy && it.error != null } }
        assertEquals("Esta pessoa já está vinculada a outra conta. Procure a administração.".asUiText(), state.error)
        assertEquals("req-1", state.requestId)
    }

    @Test fun `muitas tentativas bloqueiam o envio pelo tempo da API`() = runBlocking {
        server.enqueue(failure("rate-limited", 429).setHeader("Retry-After", "120"))
        withContext(main) { model.check(cpf, brazil, "17991234567") }
        withTimeout(5000) { model.state.first { !it.busy && it.retrySeconds > 0 } }
        withContext(main) { model.check(cpf, brazil, "17991234567") }
        assertEquals(1, server.requestCount)
    }

    @Test fun `conferencia vencida volta ao formulario`() = runBlocking {
        server.enqueue(MockResponse().setBody(checked))
        withContext(main) { model.check(cpf, brazil, "17991234567") }
        withTimeout(5000) { model.state.first { it.check != null } }
        server.enqueue(failure("not-found", 404))
        withContext(main) { model.confirm { fail("não deveria vincular") } }
        val state = withTimeout(5000) { model.state.first { !it.busy && it.error != null } }
        assertNull(state.check)
        assertEquals(uiText(R.string.link_expired), state.error)
    }

    @Test fun `validacoes locais de cpf celular e bandeira`() {
        assertTrue(isValidCpf("52998224725"))
        assertFalse(isValidCpf("52998224724"))
        assertFalse(isValidCpf("11111111111"))
        assertFalse(isValidCpf("5299822472"))
        assertTrue(isPlausiblePhone(brazil, "1791234567"))
        assertTrue(isPlausiblePhone(brazil, "17991234567"))
        assertFalse(isPlausiblePhone(brazil, "991234567"))
        assertTrue(isPlausiblePhone(portugal, "912345678"))
        assertFalse(isPlausiblePhone(portugal, "12345"))
        assertEquals("🇧🇷", brazil.flag)
        assertEquals("BR", PhoneCountries.first().iso)
    }
}

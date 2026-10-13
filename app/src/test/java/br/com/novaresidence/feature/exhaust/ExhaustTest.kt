package br.com.novaresidence.feature.exhaust

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.testSession
import br.com.novaresidence.core.network.*
import br.com.novaresidence.core.session.SessionCoordinator
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
class ExhaustTest {
    private val main = newSingleThreadContext("exhaust-test")
    private val server = MockWebServer()
    private lateinit var repository: ExhaustRepository
    private var model: ExhaustViewModel? = null
    private val off = Exhaust("A4", "A", 4, false, null, "executado", true)
    private val jsonOff = """{"id":"A4","tower":"A","final":4,"on":false,"expiresAt":null,"processStatus":"executado","moduleOnline":true}"""
    private fun response(data: String) = MockResponse().setBody("""{"success":true,"data":$data,"meta":{"requestId":"test"}}""")

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(main)
        server.start()
        val transport = ApiTransport(server.url("/").toString())
        val session = SessionCoordinator(TestSessionStore()) { error("Refresh não esperado") }
        session.replace(testSession(), session.generation())
        repository = ExhaustRepository(ApiClient(transport, session))
    }
    @After fun cleanup() {
        runBlocking { withContext(main) { model?.stop() } }
        Dispatchers.resetMain()
        main.close()
        server.shutdown()
    }
    private suspend fun start(): ExhaustViewModel {
        server.enqueue(response("[$jsonOff]"))
        val value = ExhaustViewModel(repository)
        model = value
        withContext(main) { value.start("A", "124", true) }
        withTimeout(5000) { value.state.first { it.canActivate } }
        assertEquals("/v3/api/exhausts", server.takeRequest().path)
        return value
    }

    @Test fun `seis estados seguem dados reais e prazo nao confirma desligamento`() {
        assertEquals(ExhaustPhase.Off, ExhaustState(off, loading = false).phase)
        assertEquals(ExhaustPhase.Starting, ExhaustState(off.copy(processStatus = "iniciando")).phase)
        assertEquals(ExhaustPhase.Running, ExhaustState(off.copy(on = true, expiresAt = 9000), nowMillis = 1000).phase)
        assertEquals(8, ExhaustState(off.copy(on = true, expiresAt = 9000), nowMillis = 1000).remainingSeconds()!!.toInt())
        assertEquals("03:00", ExhaustState(off.copy(on = true, expiresAt = 10_800_000), nowMillis = 0).remainingHoursMinutes())
        assertEquals("02:59:00", ExhaustState(off.copy(on = true, expiresAt = 10_740_000), nowMillis = 0).remainingClock())
        val expired = ExhaustState(off.copy(on = true, expiresAt = 9000), nowMillis = 10000)
        assertEquals(ExhaustPhase.Finishing, expired.phase)
        assertFalse(expired.canActivate)
        assertEquals(ExhaustPhase.Offline, ExhaustState(off.copy(moduleOnline = false)).phase)
        assertEquals(ExhaustPhase.Error, ExhaustState(off.copy(processStatus = "erro")).phase)
        assertNull(ExhaustState(off.copy(on = true)).remainingSeconds())
        assertFalse(ExhaustState(off, loading = false, activationUncertain = true).canActivate)
    }

    @Test fun `direcao do comando e manutencao definem desligando e indisponivel`() {
        val stopping = ExhaustState(off.copy(processStatus = "iniciando", command = "desligar"), loading = false)
        assertEquals(ExhaustPhase.Finishing, stopping.phase)
        assertEquals(ExhaustPhase.Starting, ExhaustState(off.copy(processStatus = "iniciando", command = "ligar")).phase)
        val maintenance = ExhaustState(off.copy(maintenance = true), loading = false)
        assertEquals(ExhaustPhase.Maintenance, maintenance.phase)
        assertFalse(maintenance.canActivate)
        assertEquals("Em manutenção.", maintenance.homeSummary())
    }

    @Test fun `reinicio so vale com exaustor ligado e confirmado`() {
        val running = ExhaustState(off.copy(on = true, expiresAt = 10_800_000, command = "ligar"), loading = false, nowMillis = 0)
        assertTrue(running.canRestart)
        assertEquals("Tempo restante 3:00h", running.homeSummary())
        assertFalse(ExhaustState(off, loading = false).canRestart)
        assertFalse(running.copy(sending = true).canRestart)
        assertFalse(running.copy(retrySeconds = 5).canRestart)
        assertFalse(ExhaustState(off.copy(on = true, processStatus = "iniciando"), loading = false).canRestart)
    }

    @Test fun `unidade seleciona somente equipamento retornado pelo servidor`() {
        assertEquals(off, listOf(off).forUnit("A", "124"))
        assertNull(listOf(off).forUnit("B", "124"))
        assertNull(emptyList<Exhaust>().forUnit("A", "124"))
        assertNull(listOf(off, off).forUnit("A", "124"))
    }

    @Test fun `ativacao envia 180 minutos e bloqueia toque duplicado`() = runBlocking {
        val value = start()
        server.enqueue(response(jsonOff.replace("\"on\":false", "\"on\":true")).setBodyDelay(200, TimeUnit.MILLISECONDS))
        withContext(main) { value.activate(); value.activate() }
        assertTrue(value.state.value.sending)
        withTimeout(5000) { value.state.first { !it.sending && it.exhaust?.on == true } }
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v3/api/exhausts/A4/on", request.path)
        assertEquals("{\"minutes\":180}", request.body.readUtf8())
        assertNotNull(request.getHeader("Authorization"))
        assertEquals(2, server.requestCount)
    }

    @Test fun `reiniciar tempo reenvia 180 minutos uma unica vez`() = runBlocking {
        val jsonOn = jsonOff.replace("\"on\":false", "\"on\":true").replace("\"expiresAt\":null", "\"expiresAt\":${System.currentTimeMillis() + 3_600_000}")
        server.enqueue(response("[$jsonOn]"))
        val value = ExhaustViewModel(repository)
        model = value
        withContext(main) { value.start("A", "124", true) }
        withTimeout(5000) { value.state.first { it.canRestart } }
        server.takeRequest()
        server.enqueue(response(jsonOn).setBodyDelay(200, TimeUnit.MILLISECONDS))
        withContext(main) { value.restart(); value.restart(); value.activate() }
        assertTrue(value.state.value.restarting)
        assertEquals(ExhaustPhase.Running, value.state.value.phase)
        withTimeout(5000) { value.state.first { !it.sending } }
        val request = server.takeRequest()
        assertEquals("/v3/api/exhausts/A4/on", request.path)
        assertEquals("{\"minutes\":180}", request.body.readUtf8())
        assertEquals(2, server.requestCount)
    }

    @Test fun `manutencao recusada com 409 nao repete o comando`() = runBlocking {
        val value = start()
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"success":false,"error":{"type":"urn:nova-api:conflict","detail":"Exaustor em manutenção."},"meta":{"requestId":"maintenance"}}"""))
        withContext(main) { value.activate() }
        withTimeout(5000) { value.state.first { !it.sending && it.error != null } }
        withContext(main) { value.activate() }
        assertEquals(2, server.requestCount)
        assertFalse(value.state.value.canActivate)
    }

    @Test fun `erro inconclusivo exige consulta antes de novo comando`() = runBlocking {
        val value = start()
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"success":false,"error":{"type":"urn:nova-api:upstream-error"},"meta":{"requestId":"failure"}}"""))
        withContext(main) { value.activate() }
        withTimeout(5000) { value.state.first { !it.sending && it.activationUncertain } }
        withContext(main) { value.activate() }
        assertEquals(2, server.requestCount)
        assertEquals("failure", value.state.value.requestId)
        server.enqueue(response("[$jsonOff]"))
        withContext(main) { value.refresh() }
        withTimeout(5000) { value.state.first { it.canActivate } }
        assertEquals(3, server.requestCount)
    }

    @Test fun `limite 429 bloqueia comandos e consultas durante retry after`() = runBlocking {
        val value = start()
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "60")
            .setBody("""{"success":false,"error":{"type":"urn:nova-api:rate-limited"},"meta":{}}"""))
        withContext(main) { value.activate() }
        withTimeout(5000) { value.state.first { !it.sending && it.retrySeconds > 0 } }
        withContext(main) { value.activate(); value.refresh() }
        assertFalse(value.state.value.canActivate)
        assertEquals(2, server.requestCount)
    }

    @Test fun `resposta pendente nao restaura tela encerrada`() = runBlocking {
        val value = start()
        server.enqueue(response(jsonOff.replace("\"on\":false", "\"on\":true")).setBodyDelay(300, TimeUnit.MILLISECONDS))
        withContext(main) { value.activate() }
        assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
        withContext(main) { value.stop() }
        delay(400)
        assertNull(value.state.value.exhaust)
        assertFalse(value.state.value.sending)
    }

    @Test fun `unidade bloqueada nao consulta equipamento`() = runBlocking {
        val value = ExhaustViewModel(repository)
        model = value
        withContext(main) { value.start("A", "124", false); value.activate() }
        assertEquals(ExhaustPhase.Unavailable, value.state.value.phase)
        assertEquals(0, server.requestCount)
    }
}

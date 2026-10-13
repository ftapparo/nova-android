package br.com.novaresidence.feature.fire

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiTransport
import br.com.novaresidence.core.session.SessionCoordinator
import br.com.novaresidence.core.ui.clockTime
import br.com.novaresidence.core.ui.dayLabel
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class FireTest {
    private val main = newSingleThreadContext("fire-test")
    private val server = MockWebServer()
    private lateinit var repository: FireRepository
    private var model: FireViewModel? = null
    private val panel = CiePanel(online = true, connected = true, lastUpdated = 0)
    private fun log(type: String, at: String = "2026-09-21T18:48:10.000Z", device: String? = "8 ANDAR B T-B", zone: String? = "TORRE B\u0000\u0000") =
        CieLog("$type:$at", type, 1, deviceName = device, zoneName = zone, deviceTypeLabel = "Acionador Manual", occurredAt = at)
    private fun response(data: String) = MockResponse().setBody("""{"success":true,"data":$data,"meta":{"requestId":"test"}}""")
    private fun item(at: String) = """{"key":"alarme:$at","type":"alarme","id":1,"zoneName":"TORRE A","deviceName":"TERREO T-A","occurredAt":"$at"}"""

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(main)
        server.start()
        val session = SessionCoordinator(TestSessionStore()) { error("Refresh não esperado") }
        session.replace(testSession(), session.generation())
        repository = FireRepository(ApiClient(ApiTransport(server.url("/").toString()), session))
    }

    @After fun cleanup() {
        runBlocking { withContext(main) { model?.stop() } }
        Dispatchers.resetMain()
        main.close()
        server.shutdown()
    }

    @Test fun `estado segue alarme, comunicacao e contadores da central`() {
        assertEquals(FireStatus.Normal, FireState(panel, CieAlarms(false), loading = false).status)
        assertEquals(FireStatus.Attention, FireState(panel, CieAlarms(false, CieCounters(falha = 1)), loading = false).status)
        assertEquals(FireStatus.Attention, FireState(panel, CieAlarms(false, CieCounters(supervisao = 2)), loading = false).status)
        // Dispositivos bloqueados sozinhos não indicam ocorrência para o morador.
        assertEquals(FireStatus.Normal, FireState(panel, CieAlarms(false, CieCounters(bloqueio = 2)), loading = false).status)
        assertEquals(FireStatus.Offline, FireState(panel.copy(connected = false), CieAlarms(false), loading = false).status)
        val alarm = FireState(panel.copy(connected = false), CieAlarms(true, CieCounters(alarme = 1), listOf(log("alarme"))), loading = false)
        assertEquals("o alarme prevalece sobre a falta de comunicação", FireStatus.Alarm, alarm.status)
        assertEquals("Torre B - Andar 8", alarm.homeAlarmPlace())
        assertNull(FireState(panel, CieAlarms(false), loading = false).homeAlarmPlace())
        val active = log("alarme", device = "16 ANDAR B T-B")
        assertEquals("o alarme ativo agora prevalece sobre o histórico", "16º andar",
            FireState(panel, CieAlarms(true, latestAlarmLogs = listOf(log("alarme")), activeAlarms = listOf(active)), loading = false).activeAlarm?.floorLabel())
        assertEquals(FireStatus.Loading, FireState().status)
        assertEquals(FireStatus.Unavailable, FireState(loading = false).status)
    }

    @Test fun `rotulos do painel ficam legiveis sem inventar informacao`() {
        assertEquals("Torre B", log("alarme").towerLabel())
        assertEquals("8º andar", log("alarme").floorLabel())
        assertEquals("térreo", log("alarme", device = "TERREO T-A").floorLabel())
        assertEquals("Torre B · 8º andar", log("alarme").placeLine())
        assertEquals("Torre B - 8º andar", log("alarme").placeLine(" - "))
        val operation = CieLog("op", "operacao", 1, deviceTypeLabel = "Ausente", occurredAt = "2026-09-21T18:48:10.000Z")
        assertEquals("Evento operacional", operation.historyTitle())
        assertNull(operation.deviceType())
        assertNull(log("alarme", device = "\u0000\u0000").floorLabel())
    }

    @Test fun `campos interpretados pela API prevalecem sobre o nome do dispositivo`() {
        val interpreted = log("falha", device = "12 ANDAR B T-A", zone = "TORRE A").copy(
            loop = 1, address = 181, eventType = 7, blocked = false,
            event = CieEventInfo(category = "falha", label = "Disp. em Falha", kind = "ativacao", severity = "warning"),
            display = CieDisplay(device = "Acionador Manual", deviceName = "12 ANDAR B T-A",
                place = CiePlace(towerLabel = "Torre A", floor = 0, floorLabel = "Térreo"), loop = 1, address = 181, zoneName = "TORRE A"),
        )
        assertEquals("Térreo", interpreted.floorLabel())
        assertEquals("Torre A · Térreo", interpreted.historyTitle())
        assertEquals("L1D181", interpreted.loopAddress())
        assertEquals("Dispositivo em falha", interpreted.detailTitle())
        assertEquals("Falha", interpreted.categoryLabel())
        assertEquals("TORRE A", interpreted.zoneNameRaw())
        val rule = CieLog("op:2", "operacao", 1744, occurredAt = "2026-10-04T21:56:19Z", display = CieDisplay(rule = CieRule(3, "TORRE B\u0000")))
        assertEquals("o nome da regra identifica operações sem local", "TORRE B", rule.historyTitle())
        val ruleWithZone = rule.copy(zoneName = "TORRE B", deviceName = "TORRE B", display = CieDisplay(deviceName = "TORRE B", rule = CieRule(3, "TORRE B")))
        assertNull("sem place da API, o nome da zona não vira local", ruleWithZone.placeLine())
        assertEquals("TORRE B", ruleWithZone.historyTitle())
        val system = CieLog("op:3", "operacao", 1743, loop = 0, address = 0, occurredAt = "2026-10-04T21:57:37Z",
            event = CieEventInfo(category = "operacao", label = "Sistema Iniciado"), display = CieDisplay(loop = 0, address = 0))
        assertEquals("Sistema Iniciado", system.historyTitle())
        val area = CieLog("f:3", "falha", 3, occurredAt = "2026-10-04T21:56:19Z", display = CieDisplay(place = CiePlace(area = "Elevador", areaDetail = "1")))
        assertEquals("Elevador 1", area.placeLine())
    }

    @Test fun `historico agrupa por hoje, ontem e data curta no fuso do aparelho`() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val today = LocalDate.of(2026, 9, 21)
        assertEquals("Hoje", Instant.parse("2026-09-21T18:48:10Z").dayLabel(zone, today))
        assertEquals("Ontem", Instant.parse("2026-09-21T00:18:00Z").dayLabel(zone, today))
        assertEquals("18 set", Instant.parse("2026-09-18T05:14:00Z").dayLabel(zone, today))
        assertEquals("21/09/2026 · 15:48:10", Instant.parse("2026-09-21T18:48:10Z").fullTime(zone))
        assertEquals("15:48", Instant.parse("2026-09-21T18:48:10Z").clockTime(zone))
    }

    @Test fun `historico segue o cursor e para no limite de 30 dias`() = runBlocking {
        val now = Instant.parse("2026-10-01T03:00:00Z")
        server.enqueue(response("""{"items":[${item("2026-09-30T10:00:00Z")},${item("2026-09-21T18:00:00Z")}],"nextCursor":"2"}"""))
        server.enqueue(response("""{"items":[${item("2026-09-05T10:00:00Z")},${item("2026-08-01T10:00:00Z")}],"nextCursor":"4"}"""))
        val value = FireViewModel(repository) { now }
        model = value
        withContext(main) { value.loadHistory() }
        val state = withTimeout(5000) { value.state.first { it.history != null } }
        assertEquals(3, state.history!!.size)
        assertEquals("o evento mais recente vem primeiro", "alarme:2026-09-30T10:00:00Z", state.history!!.first().key)
        assertTrue("dados técnicos guardam o registro recebido", state.history!!.first().raw!!.contains("\"deviceName\": \"TERREO T-A\""))
        assertEquals("/v3/api/cie/logs?limit=50", server.takeRequest().path)
        assertEquals("/v3/api/cie/logs?limit=50&cursor=2", server.takeRequest().path)
        assertEquals("evento antigo encerra a paginação", 2, server.requestCount)
    }

    @Test fun `consulta da central so faz leituras`() = runBlocking {
        // Painel e alarmes são pedidos em paralelo: a resposta depende do caminho, não da ordem de chegada.
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = when (request.path) {
                "/v3/api/cie/panel" -> response("""{"online":true,"connected":true,"lastUpdated":1}""")
                else -> response("""{"isTriggered":false,"counters":{"alarme":0,"falha":0,"supervisao":0,"bloqueio":2},"latestAlarmLogs":[]}""")
            }
        }
        val value = FireViewModel(repository)
        model = value
        withContext(main) { value.start(true) }
        val state = withTimeout(5000) { value.state.first { !it.loading && it.alarms != null } }
        assertEquals(FireStatus.Normal, state.status)
        val requests = listOf(server.takeRequest(), server.takeRequest())
        assertTrue(requests.all { it.method == "GET" })
        assertEquals(setOf("/v3/api/cie/panel", "/v3/api/cie/alarms/active"), requests.map { it.path }.toSet())
        assertEquals("sem falha ativa, os eventos ativos não são consultados", 2, server.requestCount)
    }

    @Test fun `atencao consulta o local das falhas ativas`() = runBlocking {
        val failure = """{"key":"falha:1","type":"falha","id":1099,"occurredAt":"2026-09-21T18:36:43Z","display":{"place":{"towerLabel":"Torre A","floorLabel":"12º andar"}}}"""
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = when (request.path) {
                "/v3/api/cie/panel" -> response("""{"online":true,"connected":true,"lastUpdated":1}""")
                "/v3/api/cie/events/active" -> response("""{"events":{"alarme":[],"falha":[$failure],"supervisao":[]}}""")
                else -> response("""{"isTriggered":false,"counters":{"alarme":0,"falha":1,"supervisao":0,"bloqueio":0},"latestAlarmLogs":[]}""")
            }
        }
        val value = FireViewModel(repository)
        model = value
        withContext(main) { value.start(true) }
        val state = withTimeout(5000) { value.state.first { !it.loading && it.alarms != null } }
        assertEquals(FireStatus.Attention, state.status)
        assertEquals("Torre A · 12º andar", state.activeEvents.single().placeLine())
        assertEquals(3, server.requestCount)
    }
}

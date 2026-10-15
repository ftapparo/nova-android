package br.com.novaresidence.feature.fire

import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.core.ui.asUiText
import br.com.novaresidence.core.ui.UiText
import br.com.novaresidence.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class CieCounters(val alarme: Int = 0, val falha: Int = 0, val supervisao: Int = 0, val bloqueio: Int = 0)

@Serializable
data class CiePanel(
    val online: Boolean, val connected: Boolean = false, val restarting: Boolean = false,
    val lastUpdated: Long? = null, val counters: CieCounters? = null,
)

/** Evento já interpretado pela API (`event`): o app não traduz códigos da central. */
@Serializable
data class CieEventInfo(val category: String? = null, val label: String? = null, val kind: String? = null, val severity: String? = null)

/** Local interpretado pela API a partir do cadastro da central. */
@Serializable
data class CiePlace(
    val towerLabel: String? = null, val floor: Int? = null, val floorLabel: String? = null,
    val area: String? = null, val areaDetail: String? = null,
)

@Serializable
data class CieRule(val id: Int, val name: String? = null)

/** Cada dado do evento em um campo próprio (`display`); o app só escolhe o layout. */
@Serializable
data class CieDisplay(
    val device: String? = null, val deviceName: String? = null, val place: CiePlace? = null, val rule: CieRule? = null,
    val loop: Int? = null, val address: Int? = null, val zoneName: String? = null,
)

@Serializable
data class CieLog(
    val key: String, val type: String, val id: Int,
    val zone: Int? = null, val address: Int? = null, val loop: Int? = null,
    val deviceName: String? = null, val zoneName: String? = null, val deviceTypeLabel: String? = null,
    val eventType: Int? = null, val blocked: Boolean? = null, val occurredAt: String,
    // Campos interpretados da v3; servidores anteriores não os enviam e o app usa os campos brutos.
    val event: CieEventInfo? = null, val display: CieDisplay? = null,
    /** Registro exatamente como veio da API, para "Dados técnicos" (Figma 30). */
    @Transient val raw: String? = null,
) {
    // Parsed once: sorting, filtering and grouping up to 200 events used to parse each date hundreds of times.
    internal val occurredInstant: Instant? by lazy { runCatching { Instant.parse(occurredAt) }.getOrNull() }
}

@Serializable
data class CieAlarms(
    val isTriggered: Boolean, val counters: CieCounters = CieCounters(),
    val latestAlarmLogs: List<CieLog> = emptyList(), val activeAlarms: List<CieLog> = emptyList(),
)

@Serializable
data class CieActiveEvents(val events: Map<String, List<CieLog>> = emptyMap())

@Serializable
data class CieLogPage(val items: List<CieLog> = emptyList(), val nextCursor: String? = null)

enum class FireStatus { Loading, Normal, Attention, Offline, Alarm, Unavailable }

data class FireState(
    val panel: CiePanel? = null,
    val alarms: CieAlarms? = null,
    /** Falhas e supervisões ativas agora (consultadas só quando os contadores indicam atenção). */
    val activeEvents: List<CieLog> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val requestId: String? = null,
    val retrySeconds: Long = 0,
    val history: List<CieLog>? = null,
    val historyLoading: Boolean = false,
    val historyError: UiText? = null,
) {
    val status: FireStatus get() {
        val panel = panel
        val alarms = alarms
        return when {
            alarms?.isTriggered == true -> FireStatus.Alarm
            panel == null || alarms == null -> if (loading) FireStatus.Loading else FireStatus.Unavailable
            !panel.online || !panel.connected -> FireStatus.Offline
            alarms.counters.falha > 0 || alarms.counters.supervisao > 0 -> FireStatus.Attention
            else -> FireStatus.Normal
        }
    }

    /** Alarme em andamento: o ativo agora, ou o último do histórico em servidores sem `activeAlarms`. */
    val activeAlarm: CieLog? get() = if (alarms?.isTriggered == true) alarms.activeAlarms.firstOrNull() ?: alarms.latestAlarmLogs.firstOrNull() else null

    /** Texto da linha da central na home (Figma 08a/08c). */
    fun homeSummary(): UiText = uiText(when (status) {
        FireStatus.Loading -> R.string.fire_home_loading
        FireStatus.Normal -> R.string.fire_home_normal
        FireStatus.Attention -> R.string.fire_home_attention
        FireStatus.Offline -> R.string.fire_home_offline
        FireStatus.Alarm -> R.string.fire_home_alarm
        FireStatus.Unavailable -> R.string.fire_home_unavailable
    })

    /** Destaque da home durante um alarme (Figma 08c): "Torre B - Andar 8". */
    fun homeAlarmPlace(): UiText? = if (status != FireStatus.Alarm) null
        else activeAlarm?.let { log ->
            val tower = log.towerLabel()
            val floor = log.homeFloor()
            when {
                tower != null && floor != null -> uiText(R.string.fire_home_place, tower, floor)
                else -> tower?.asUiText() ?: floor
            }
        } ?: uiText(R.string.fire_alarm_triggered)
}

private val PtBr = Locale.forLanguageTag("pt-BR")

private fun String.panelText(): String? = replace("\u0000", "").trim().ifEmpty { null }

/** "TORRE A" → "Torre A". Nomes vêm do painel com caracteres nulos e caixa alta. */
fun String.cieName(): String = replace("\u0000", "").trim().lowercase(PtBr).split(Regex("\\s+")).filter { it.isNotEmpty() }
    .joinToString(" ") { word -> if (word.length == 1) word.uppercase(PtBr) else word.replaceFirstChar { it.titlecase(PtBr) } }

fun CieLog.category(): String = event?.category ?: type

fun CieLog.categoryLabel(): UiText = uiText(when (category()) {
    "alarme" -> R.string.fire_category_alarm
    "falha" -> R.string.fire_category_failure
    "supervisao" -> R.string.fire_category_supervision
    "operacao" -> R.string.fire_category_operation
    "bloqueio" -> R.string.fire_category_blocked
    else -> R.string.fire_category_event
})

// Com `display` presente, o local vem só da API: `place` nulo significa que o nome não segue a convenção
// do condomínio (ex.: regras e eventos do sistema), e o app não deve adivinhar a partir do nome.

/** "Torre B": do local interpretado pela API ou, em servidores anteriores, do nome da zona. */
fun CieLog.towerLabel(): String? =
    if (display != null) display.place?.towerLabel else zoneName?.cieName()?.takeIf { it.isNotEmpty() }

/** "16º andar", "Térreo" ou a área fora dos andares ("Elevador 1"). */
fun CieLog.floorLabel(): String? {
    if (display != null) {
        val place = display.place ?: return null
        return place.floorLabel ?: listOfNotNull(place.area, place.areaDetail).joinToString(" ").ifEmpty { null }
    }
    // Servidores anteriores à v3 interpretada: "12 ANDAR B T-A" → "12º andar".
    val name = deviceName?.panelText() ?: return null
    Regex("(\\d+)\\s*ANDAR", RegexOption.IGNORE_CASE).find(name)?.let { return "${it.groupValues[1]}º andar" }
    if (name.contains("TERREO", ignoreCase = true) || name.contains("TÉRREO", ignoreCase = true)) return "térreo"
    if (name.contains("SUBSOLO", ignoreCase = true)) return "subsolo"
    return name.cieName()
}

/** Andar no formato da home (Figma 08c): "Andar 8". */
private fun CieLog.homeFloor(): UiText? {
    display?.place?.floor?.takeIf { it > 0 }?.let { return uiText(R.string.fire_home_floor, it) }
    val label = floorLabel() ?: return null
    return Regex("^(\\d+)º andar$").find(label)?.let { uiText(R.string.fire_home_floor, it.groupValues[1].toInt()) }
        ?: label.replaceFirstChar { it.uppercase() }.asUiText()
}

/** "Torre B · 8º andar" (separador configurável). */
fun CieLog.placeLine(separator: String = " · "): String? =
    listOfNotNull(towerLabel(), floorLabel()?.replaceFirstChar { it.uppercase() }).joinToString(separator).ifEmpty { null }

/** Título da linha do histórico (Figma 29): o local, o nome registrado na central ou o rótulo do evento. */
fun CieLog.historyTitle(): UiText = (placeLine()
    ?: display?.rule?.name?.panelText() ?: deviceNameRaw() ?: zoneNameRaw() ?: event?.label?.takeIf { it.isNotBlank() })?.asUiText()
    ?: uiText(R.string.fire_history_operational)

fun CieLog.deviceNameRaw(): String? = (display?.deviceName ?: deviceName)?.panelText()

fun CieLog.zoneNameRaw(): String? = (display?.zoneName ?: zoneName)?.panelText()

fun CieLog.deviceType(): String? = (display?.device ?: deviceTypeLabel)?.takeIf { it.isNotBlank() && it != "Ausente" }

/** Laço e endereço no formato do painel: "L1D100". */
fun CieLog.loopAddress(): String? {
    val loop = display?.loop ?: loop ?: return null
    val address = display?.address ?: address ?: return null
    return "L${loop}D$address"
}

/** Título do detalhe (Figma 30a–30c). */
fun CieLog.detailTitle(): UiText = uiText(when (category()) {
    "alarme" -> R.string.fire_detail_alarm
    "falha" -> R.string.fire_detail_failure
    "supervisao" -> R.string.fire_detail_supervision
    "bloqueio" -> if (blocked == false) R.string.fire_detail_unblocked else R.string.fire_detail_blocked
    "operacao" -> R.string.fire_detail_operation
    else -> R.string.fire_detail_event
})

fun CieLog.instant(): Instant? = occurredInstant

/** "04/10/2026 · 18:56:19" no fuso do aparelho; em inglês, mês antes do dia. */
fun Instant.fullTime(zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
    atZone(zone).format(DateTimeFormatter.ofPattern(if (locale.language == "en") "MM/dd/yyyy · HH:mm:ss" else "dd/MM/yyyy · HH:mm:ss", locale))

/**
 * Assinatura de um `cie.status.updated` do WebSocket, que chega a cada ~1 s com o snapshot bruto do nova-cie.
 * Junta só o que muda o estado da tela (conexão, reinício e contadores), para a tela reconsultar o REST apenas
 * quando algo mudou; `dataHora` e `lastUpdated` mudam a cada envio e ficam de fora. Nulo = payload sem estado.
 */
fun cieSignature(data: JsonElement, nowMillis: Long = System.currentTimeMillis()): String? {
    val snapshot = data as? JsonObject ?: return null
    val counters = (snapshot["status"] as? JsonObject)?.get("status") as? JsonObject
    val connected = (snapshot["connected"] as? JsonPrimitive)?.booleanOrNull
    if (counters == null && connected == null) return null
    val restarting = ((snapshot["restartingUntil"] as? JsonPrimitive)?.longOrNull ?: 0L) > nowMillis
    val values = listOf("alarme", "falha", "supervisao", "bloqueio")
        .joinToString(",") { (counters?.get(it) as? JsonPrimitive)?.intOrNull?.toString() ?: "-" }
    return "$connected|$restarting|$values"
}

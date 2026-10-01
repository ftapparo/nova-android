package br.com.novaresidence.feature.fire

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
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

@Serializable
data class CieLog(
    val key: String, val type: String, val id: Int,
    val zone: Int? = null, val address: Int? = null, val loop: Int? = null,
    val deviceName: String? = null, val zoneName: String? = null, val deviceTypeLabel: String? = null,
    val eventType: Int? = null, val blocked: Boolean? = null, val occurredAt: String,
)

@Serializable
data class CieAlarms(val isTriggered: Boolean, val counters: CieCounters = CieCounters(), val latestAlarmLogs: List<CieLog> = emptyList())

@Serializable
data class CieLogPage(val items: List<CieLog> = emptyList(), val nextCursor: String? = null)

enum class FireStatus { Loading, Normal, Attention, Offline, Alarm, Unavailable }

data class FireState(
    val panel: CiePanel? = null,
    val alarms: CieAlarms? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val requestId: String? = null,
    val retrySeconds: Long = 0,
    val history: List<CieLog>? = null,
    val historyLoading: Boolean = false,
    val historyError: String? = null,
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

    /** Registro mais recente do alarme em andamento, se houver. */
    val activeAlarm: CieLog? get() = if (alarms?.isTriggered == true) alarms.latestAlarmLogs.firstOrNull() else null

    /** Texto da linha da central na home (Figma 08a/08c). */
    fun homeSummary(): String = when (status) {
        FireStatus.Loading -> "Consultando a central…"
        FireStatus.Normal -> "Nenhum alarme de incêndio ativo."
        FireStatus.Attention -> "Existe uma ocorrência que requer verificação."
        FireStatus.Offline -> "Central sem comunicação no momento."
        FireStatus.Alarm -> "Siga as orientações de segurança"
        FireStatus.Unavailable -> "Não foi possível consultar a central."
    }

    /** Título em destaque na home durante um alarme: "Alarme disparado - Torre B - Andar 8". */
    fun homeAlarmTitle(): String? = activeAlarm?.let { log ->
        // Formato do Figma 08c: "Andar 8" em vez de "8º andar".
        val floor = log.floorLabel()?.let { label -> Regex("^(\\d+)º andar$").find(label)?.let { "Andar ${it.groupValues[1]}" } ?: label.replaceFirstChar { it.uppercase() } }
        listOfNotNull("Alarme disparado", log.towerLabel(), floor).joinToString(" - ")
    } ?: if (status == FireStatus.Alarm) "Alarme disparado" else null
}

private val PtBr = Locale.forLanguageTag("pt-BR")

/** "TORRE A" → "Torre A". Nomes vêm do painel com caracteres nulos e caixa alta. */
fun String.cieName(): String = replace("\u0000", "").trim().lowercase(PtBr).split(Regex("\\s+")).filter { it.isNotEmpty() }
    .joinToString(" ") { word -> if (word.length == 1) word.uppercase(PtBr) else word.replaceFirstChar { it.titlecase(PtBr) } }

fun CieLog.towerLabel(): String? = zoneName?.cieName()?.takeIf { it.isNotEmpty() }

/** "12 ANDAR B T-A" → "12º andar"; "TERREO T-A" → "térreo"; senão o nome do dispositivo. */
fun CieLog.floorLabel(): String? {
    val name = deviceName?.replace("\u0000", "")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    Regex("(\\d+)\\s*ANDAR", RegexOption.IGNORE_CASE).find(name)?.let { return "${it.groupValues[1]}º andar" }
    if (name.contains("TERREO", ignoreCase = true) || name.contains("TÉRREO", ignoreCase = true)) return "térreo"
    if (name.contains("SUBSOLO", ignoreCase = true)) return "subsolo"
    return name.cieName()
}

fun CieLog.deviceLabel(): String? = deviceTypeLabel?.takeIf { it.isNotBlank() && it != "Ausente" }

fun CieLog.instant(): Instant? = runCatching { Instant.parse(occurredAt) }.getOrNull()

fun CieLog.title(): String = when (type) {
    "alarme" -> "Alarme de incêndio"
    "falha" -> "Falha em dispositivo"
    "supervisao" -> "Supervisão de dispositivo"
    "bloqueio" -> if (blocked == false) "Dispositivo desbloqueado" else "Dispositivo bloqueado"
    "operacao" -> "Operação na central"
    else -> "Evento da central"
}

/** Linha de local: "Torre B · 8º andar · Acionador Manual" ou "Central de monitoramento". */
fun CieLog.locationLine(): String =
    listOfNotNull(towerLabel(), floorLabel(), deviceLabel()).joinToString(" · ").ifEmpty { "Central de monitoramento" }

fun CieLog.statusLabel(): String = when (type) {
    "alarme" -> "Alarme registrado"
    "falha", "supervisao" -> "Verificação pela equipe"
    "bloqueio" -> "Bloqueio registrado"
    "operacao" -> "Registrada"
    else -> "Registrado"
}

/** "Hoje, 13:42", "Ontem, 21:18" ou "24 set, 10:00" no fuso do aparelho. */
fun Instant.eventTime(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
    val local = atZone(zone)
    val time = local.format(DateTimeFormatter.ofPattern("HH:mm", PtBr))
    return when (local.toLocalDate()) {
        today -> "Hoje, $time"
        today.minusDays(1) -> "Ontem, $time"
        else -> local.format(DateTimeFormatter.ofPattern("d MMM", PtBr)).replace(".", "") + ", $time"
    }
}

fun Long.clockTime(zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", PtBr))

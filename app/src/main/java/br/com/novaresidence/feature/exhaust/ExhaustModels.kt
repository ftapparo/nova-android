package br.com.novaresidence.feature.exhaust

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Exhaust(
    val id: String, val tower: String, val final: Int, val on: Boolean,
    val expiresAt: Long?, val processStatus: String?, val moduleOnline: Boolean,
    // Campos novos da v3: os padrões mantêm a leitura de servidores anteriores.
    val command: String? = null,
    val maintenance: Boolean = false,
)

enum class ExhaustPhase { Loading, Off, Starting, Running, Finishing, Offline, Maintenance, Error, Unavailable }

data class ExhaustState(
    val exhaust: Exhaust? = null,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val restarting: Boolean = false,
    val error: String? = null,
    val requestId: String? = null,
    val retrySeconds: Long = 0,
    val nowMillis: Long = System.currentTimeMillis(),
    val activationUncertain: Boolean = false,
) {
    val phase: ExhaustPhase get() {
        val item = exhaust
        return when {
            sending && !restarting -> ExhaustPhase.Starting
            error != null -> ExhaustPhase.Error
            item == null -> if (loading) ExhaustPhase.Loading else ExhaustPhase.Unavailable
            !item.moduleOnline -> ExhaustPhase.Offline
            item.maintenance -> ExhaustPhase.Maintenance
            item.processStatus == "erro" -> ExhaustPhase.Error
            // Durante o desligamento a API já informa on=false; só a direção do comando distingue de "ligando".
            item.processStatus == "iniciando" && item.command == "desligar" -> ExhaustPhase.Finishing
            item.on && item.expiresAt != null && item.expiresAt <= nowMillis -> ExhaustPhase.Finishing
            item.processStatus == "iniciando" -> ExhaustPhase.Starting
            item.processStatus !in setOf(null, "executado") -> ExhaustPhase.Unavailable
            item.on -> ExhaustPhase.Running
            else -> ExhaustPhase.Off
        }
    }
    val canActivate: Boolean get() = !loading && !sending && retrySeconds == 0L && error == null &&
        !activationUncertain && exhaust?.moduleOnline == true && exhaust.on == false && !exhaust.maintenance &&
        exhaust.processStatus in setOf(null, "executado", "erro")

    val canRestart: Boolean get() = !loading && !sending && retrySeconds == 0L && error == null &&
        phase == ExhaustPhase.Running && exhaust?.processStatus == "executado"

    fun remainingSeconds(): Long? = exhaust?.expiresAt?.let { ((it - nowMillis + 999) / 1000).coerceAtLeast(0) }

    fun remainingHoursMinutes(): String? = remainingSeconds()?.let { seconds ->
        "%02d:%02d".format(Locale.ROOT, seconds / 3600, (seconds % 3600) / 60)
    }

    /** Resumo exibido na linha do exaustor na home (telas 08a/08b). */
    fun homeSummary(): String = when (phase) {
        ExhaustPhase.Loading -> "Consultando equipamento…"
        ExhaustPhase.Off -> "Disponível para utilização."
        ExhaustPhase.Starting -> "Ligando…"
        ExhaustPhase.Running -> remainingSeconds()?.let { "Tempo restante %d:%02dh".format(Locale.ROOT, it / 3600, (it % 3600) / 60) } ?: "Sem desligamento programado."
        ExhaustPhase.Finishing -> "Desligando…"
        ExhaustPhase.Offline, ExhaustPhase.Unavailable -> "Indisponível no momento."
        ExhaustPhase.Maintenance -> "Em manutenção."
        ExhaustPhase.Error -> "Verifique o estado do equipamento."
    }
}

fun List<Exhaust>.forUnit(tower: String, apartment: String): Exhaust? {
    val final = apartment.lastOrNull()?.digitToIntOrNull() ?: return null
    // A lista autorizada pelo servidor é a fonte de acesso; a unidade apenas seleciona nela.
    return singleOrNull { it.tower.equals(tower, ignoreCase = true) && it.final == final }
}

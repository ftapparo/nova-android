package br.com.novaresidence.feature.exhaust

import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.core.ui.UiText
import br.com.novaresidence.R
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

/**
 * Por que o comando não saiu por causa da localização (Figma 25c–25i). Os quatro primeiros são do aparelho
 * e nada foi enviado; os demais vêm do 403 da API, recusado antes do relé. Em nenhum caso o comando fica incerto.
 */
enum class LocationIssue { Permission, Approximate, Disabled, Unavailable, Outside, Inaccurate, Mock }

/** Títulos estáveis do 403 de `POST /exhausts/{id}/on` (APP-GUIDE §2); "Localização necessária" só ocorre sem leitura. */
internal fun locationIssueFor(title: String?): LocationIssue? = when (title) {
    "Fora da área do condomínio" -> LocationIssue.Outside
    "Localização imprecisa" -> LocationIssue.Inaccurate
    "Localização simulada" -> LocationIssue.Mock
    "Localização necessária" -> LocationIssue.Unavailable
    else -> null
}

data class ExhaustState(
    val exhaust: Exhaust? = null,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val restarting: Boolean = false,
    val error: UiText? = null,
    val requestId: String? = null,
    val retrySeconds: Long = 0,
    val nowMillis: Long = System.currentTimeMillis(),
    val activationUncertain: Boolean = false,
    /** Lendo a localização antes de enviar o comando ("Verificando localização..."). */
    val locating: Boolean = false,
    val location: LocationIssue? = null,
) {
    val phase: ExhaustPhase get() {
        val item = exhaust
        return when {
            sending && !restarting && !locating -> ExhaustPhase.Starting
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

    /** Contador da tela do exaustor (Figma 21): "02:59:00". */
    fun remainingClock(): String? = remainingSeconds()?.let { seconds ->
        "%02d:%02d:%02d".format(Locale.ROOT, seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    }

    /** Resumo exibido na linha do exaustor na home (telas 08a/08b). */
    fun homeSummary(): UiText = when (phase) {
        ExhaustPhase.Loading -> uiText(R.string.exhaust_home_loading)
        ExhaustPhase.Off -> uiText(R.string.exhaust_home_off)
        ExhaustPhase.Starting -> uiText(R.string.exhaust_home_starting)
        ExhaustPhase.Running -> remainingSeconds()?.let { uiText(R.string.exhaust_home_remaining, it / 3600, (it % 3600) / 60) }
            ?: uiText(R.string.exhaust_no_auto_off)
        ExhaustPhase.Finishing -> uiText(R.string.exhaust_home_finishing)
        ExhaustPhase.Offline, ExhaustPhase.Unavailable -> uiText(R.string.exhaust_home_unavailable)
        ExhaustPhase.Maintenance -> uiText(R.string.exhaust_home_maintenance)
        ExhaustPhase.Error -> uiText(R.string.exhaust_home_error)
    }
}

fun List<Exhaust>.forUnit(tower: String, apartment: String): Exhaust? {
    val final = apartment.lastOrNull()?.digitToIntOrNull() ?: return null
    // A lista autorizada pelo servidor é a fonte de acesso; a unidade apenas seleciona nela.
    return singleOrNull { it.tower.equals(tower, ignoreCase = true) && it.final == final }
}

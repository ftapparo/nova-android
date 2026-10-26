package br.com.novaresidence.feature.exhaust

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*

private data class StateCopy(val image: Int, val title: UiText, val description: UiText, val info: UiText?)

private val AutoOff = uiText(R.string.exhaust_auto_off)
private val LocationPrivacy = uiText(R.string.exhaust_location_privacy)

/** Aviso de localização (Figma 25c–25i); o botão resolve no aparelho ([action]) ou repete o toque ([retry]). */
private data class LocationCopy(val title: UiText, val description: UiText, val info: UiText, val action: UiText? = null, val retry: Boolean = false)

private fun locationCopy(issue: LocationIssue, permissionBlocked: Boolean): LocationCopy = when (issue) {
    LocationIssue.Permission -> if (permissionBlocked) LocationCopy(uiText(R.string.exhaust_location_denied_title),
        uiText(R.string.exhaust_location_denied), LocationPrivacy, uiText(R.string.exhaust_open_settings))
        else LocationCopy(uiText(R.string.exhaust_location_needed_title), uiText(R.string.exhaust_location_needed), LocationPrivacy,
            uiText(R.string.exhaust_allow_location))
    LocationIssue.Approximate -> LocationCopy(uiText(R.string.exhaust_location_precise_title),
        uiText(R.string.exhaust_location_precise), LocationPrivacy,
        uiText(if (permissionBlocked) R.string.exhaust_open_settings else R.string.exhaust_allow_precise_location))
    LocationIssue.Disabled -> LocationCopy(uiText(R.string.exhaust_location_off_title), uiText(R.string.exhaust_location_off), LocationPrivacy,
        uiText(R.string.exhaust_enable_location))
    LocationIssue.Outside -> LocationCopy(uiText(R.string.exhaust_outside_title), uiText(R.string.exhaust_outside),
        uiText(R.string.exhaust_outside_info))
    LocationIssue.Inaccurate, LocationIssue.Unavailable -> LocationCopy(uiText(R.string.exhaust_inaccurate_title),
        uiText(R.string.exhaust_inaccurate), uiText(R.string.exhaust_inaccurate_info), retry = true)
    LocationIssue.Mock -> LocationCopy(uiText(R.string.exhaust_mock_title), uiText(R.string.exhaust_mock),
        uiText(R.string.exhaust_mock_info), retry = true)
}

/** Exaustor da unidade ativa (Figma 19–24). [unitLabel] segue o formato "Apto. 124 · Torre A". */
@Composable
fun ExhaustScreen(
    state: ExhaustState, unitLabel: UiText?, blockedMessage: UiText?,
    activate: () -> Unit, restart: () -> Unit, refresh: () -> Unit, selectUnit: () -> Unit,
    permissionBlocked: Boolean = false, locationAction: (LocationIssue) -> Unit = {},
) {
    val phase = state.phase
    // O aviso de localização só substitui os estados em que o botão envia comando.
    val location = state.location?.takeIf { phase == ExhaustPhase.Off || phase == ExhaustPhase.Running }
        ?.let { it to locationCopy(it, permissionBlocked) }
    val phaseCopy = when (phase) {
        ExhaustPhase.Off -> StateCopy(R.drawable.nova_exhaust_off, uiText(R.string.exhaust_off_title), uiText(R.string.exhaust_off), AutoOff)
        ExhaustPhase.Starting -> StateCopy(R.drawable.nova_exhaust_spinning, uiText(R.string.exhaust_starting_title), uiText(R.string.exhaust_starting), AutoOff)
        ExhaustPhase.Running -> StateCopy(R.drawable.nova_exhaust_on, uiText(R.string.exhaust_running_title),
            state.remainingClock()?.let { uiText(R.string.exhaust_remaining_clock, it) } ?: uiText(R.string.exhaust_no_auto_off), AutoOff)
        ExhaustPhase.Finishing -> StateCopy(R.drawable.nova_exhaust_spinning, uiText(R.string.exhaust_finishing_title), uiText(R.string.exhaust_finishing), AutoOff)
        ExhaustPhase.Offline -> StateCopy(R.drawable.nova_exhaust_offline, uiText(R.string.exhaust_offline_title), uiText(R.string.exhaust_offline),
            uiText(R.string.exhaust_offline_info))
        ExhaustPhase.Maintenance -> StateCopy(R.drawable.nova_exhaust_maintenance, uiText(R.string.exhaust_maintenance_title), uiText(R.string.exhaust_maintenance),
            uiText(R.string.exhaust_maintenance_info))
        // Resultado inconclusivo (falha de rede ou resposta de erro) mantém os textos aprovados: não afirmamos que falhou.
        ExhaustPhase.Error -> if (state.error != null) StateCopy(R.drawable.nova_exhaust_inconclusive, uiText(R.string.exhaust_uncertain_title),
            uiText(R.string.exhaust_uncertain), uiText(R.string.exhaust_error_info))
            else StateCopy(R.drawable.nova_exhaust_inconclusive, uiText(R.string.exhaust_failed_title), uiText(R.string.exhaust_failed),
            uiText(R.string.exhaust_error_info))
        ExhaustPhase.Loading -> StateCopy(R.drawable.nova_exhaust_off, uiText(R.string.exhaust_loading_title), uiText(R.string.exhaust_loading), null)
        // Sem equipamento para a unidade (Figma 23h, fundo rosado): o selo de "sem Wi-Fi" diria que há um exaustor fora do ar.
        ExhaustPhase.Unavailable -> StateCopy(R.drawable.nova_exhaust_unavailable, uiText(R.string.exhaust_unavailable_title),
            blockedMessage ?: uiText(if (unitLabel == null) R.string.exhaust_select_unit else R.string.exhaust_none_for_unit),
            uiText(R.string.exhaust_unavailable_info))
    }
    val copy = location?.second?.let { StateCopy(R.drawable.nova_exhaust_location, it.title, it.description, it.info) } ?: phaseCopy
    NovaPage(stringResource(R.string.exhaust_title), background = NovaServiceBackground) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // Em telas mais baixas que o frame (ex.: J4 com botões), a ilustração encolhe antes de esconder ação e aviso (DESIGN_GUIDE §6).
            val illustration = (maxHeight - 454.dp).coerceIn(150.dp, 250.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = NovaSpacing.Margin, end = NovaSpacing.Margin, top = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    ContextCard(unitLabel, state, selectUnit)
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NovaIllustration(copy.image, Modifier.size(illustration), contentScale = ContentScale.Crop)
                        NovaStateHeadline(copy.title.text(), copy.description.text(), announce = true)
                        if (phase == ExhaustPhase.Error) state.error?.let { Text(it.text(), Modifier.fillMaxWidth(), style = NovaType.Body, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center) }
                        if (state.retrySeconds > 0) Text(stringResource(R.string.auth_retry_in, state.retrySeconds), style = NovaType.Body, color = NovaOnSurfaceVariant)
                        Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Action(phase, state, blockedMessage, activate, restart, refresh, location, locationAction)
                            copy.info?.let { NovaInfoStrip(it.text()) }
                            state.requestId?.let { Text(stringResource(R.string.common_support_reference, it), Modifier.fillMaxWidth(), style = NovaType.Caption, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Action(
    phase: ExhaustPhase, state: ExhaustState, blockedMessage: UiText?, activate: () -> Unit, restart: () -> Unit, refresh: () -> Unit,
    location: Pair<LocationIssue, LocationCopy>?, locationAction: (LocationIssue) -> Unit,
) {
    val running = phase == ExhaustPhase.Running
    val action = location?.second?.action
    when {
        state.locating -> NovaButton(stringResource(R.string.exhaust_checking_location), {}, enabled = false, icon = if (running) null else R.drawable.nova_power,
            style = if (running) NovaButtonStyle.Outlined else NovaButtonStyle.Primary)
        location != null && action != null -> NovaButton(action.text(), { locationAction(location.first) }, enabled = blockedMessage == null)
        location != null && location.second.retry -> NovaButton(stringResource(R.string.exhaust_try_again), if (running) restart else activate,
            enabled = (if (running) state.canRestart else state.canActivate) && blockedMessage == null)
        else -> PhaseAction(phase, state, blockedMessage, activate, restart, refresh)
    }
}

@Composable
private fun PhaseAction(
    phase: ExhaustPhase, state: ExhaustState, blockedMessage: UiText?, activate: () -> Unit, restart: () -> Unit, refresh: () -> Unit,
) {
    when (phase) {
        ExhaustPhase.Off -> NovaButton(stringResource(R.string.exhaust_turn_on), activate, enabled = state.canActivate && blockedMessage == null,
            icon = R.drawable.nova_power)
        ExhaustPhase.Starting -> NovaButton(stringResource(R.string.exhaust_turning_on), {}, enabled = false, icon = R.drawable.nova_power)
        ExhaustPhase.Finishing -> NovaButton(stringResource(R.string.exhaust_turning_off), {}, enabled = false, icon = R.drawable.nova_power)
        ExhaustPhase.Running -> NovaButton(stringResource(if (state.restarting) R.string.exhaust_restarting else R.string.exhaust_restart), restart,
            enabled = state.canRestart && blockedMessage == null, style = NovaButtonStyle.Outlined)
        ExhaustPhase.Error -> NovaButton(stringResource(R.string.exhaust_try_again), if (state.canActivate) activate else refresh,
            enabled = !state.sending && !state.loading && state.retrySeconds == 0L)
        ExhaustPhase.Unavailable -> NovaButton(stringResource(R.string.exhaust_refresh), refresh, enabled = !state.loading && state.retrySeconds == 0L,
            style = NovaButtonStyle.Secondary)
        else -> Unit
    }
}

/** Cartão "Unidade ativa · Equipamento" (Figma 23a); o toque abre a seleção de unidade. */
@Composable
private fun ContextCard(unitLabel: UiText?, state: ExhaustState, selectUnit: () -> Unit) {
    val exhaust = state.exhaust
    val (label, color, container) = when {
        exhaust == null -> if (state.loading) Triple(R.string.exhaust_badge_loading, NovaPrimary, NovaPrimarySoft)
            else Triple(R.string.exhaust_badge_unavailable, NovaOffline, NovaOfflineContainer)
        !exhaust.moduleOnline -> Triple(R.string.exhaust_badge_offline, NovaOffline, NovaOfflineContainer)
        exhaust.maintenance -> Triple(R.string.exhaust_badge_maintenance, NovaWarning, NovaWarningContainer)
        else -> Triple(R.string.exhaust_badge_available, NovaSuccess, NovaSuccessContainer)
    }
    NovaCard(Modifier.fillMaxWidth(), NovaShapes.ContextCard) {
        Row(Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.home_select_unit), onClick = selectUnit).heightIn(min = 116.dp).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(NovaPrimaryTint, NovaShapes.Strip), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.nova_units), null, Modifier.size(28.dp), tint = NovaPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.exhaust_active_unit), style = NovaType.Supporting, color = NovaOnSurfaceVariant)
                Text(unitLabel?.text() ?: stringResource(R.string.home_no_unit), style = NovaType.UnitLabel, color = NovaInk)
            }
            VerticalDivider(Modifier.height(60.dp), color = NovaOutline)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.exhaust_equipment), style = NovaType.Supporting, color = NovaOnSurfaceVariant)
                NovaStatusBadge(stringResource(label), color, container, Modifier.widthIn(min = 76.dp), height = 22.dp)
            }
        }
    }
}

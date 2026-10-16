package br.com.novaresidence.feature.notification

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.com.novaresidence.R
import br.com.novaresidence.core.push.PushStatus
import br.com.novaresidence.core.push.openAlarmChannelSettings
import br.com.novaresidence.core.ui.*

/**
 * Notificações (aberta pelas Configurações): situação do push neste aparelho, alarme de incêndio sempre
 * ativo e as preferências da conta, que a API aplica a todos os aparelhos dela.
 */
@Composable
fun NotificationScreen(
    state: NotificationState, status: PushStatus, staff: Boolean,
    load: () -> Unit, set: (NotificationOption, Boolean) -> Unit, sendTest: () -> Unit,
    enable: () -> Unit, retryRegistration: () -> Unit, back: () -> Unit,
) {
    LaunchedEffect(Unit) { load() }
    val context = LocalContext.current
    NovaPage(stringResource(R.string.notification_title), back = back) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = NovaSpacing.Margin, end = NovaSpacing.Margin, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            DeviceStatus(status, enable, retryRegistration)

            Column {
                NovaSectionTitle(stringResource(R.string.notification_fire_section))
                NovaRowText(stringResource(R.string.notification_always_on), stringResource(R.string.notification_fire_detail), Modifier.padding(vertical = 12.dp))
                NovaTextLink(stringResource(R.string.notification_alarm_channel_settings), { openAlarmChannelSettings(context) },
                    alignment = Alignment.CenterStart, style = NovaType.Body)
            }

            Column {
                NovaSectionTitle(stringResource(R.string.notification_alerts_section))
                val preferences = state.preferences
                if (preferences == null && state.loading) NovaLoading(stringResource(R.string.notification_loading))
                val enabled = preferences != null
                val minutes = preferences?.exhaustExpiringMinutes ?: 15
                Toggle(stringResource(R.string.notification_exhaust_expiring),
                    pluralStringResource(R.plurals.notification_exhaust_expiring_detail, minutes, minutes),
                    preferences?.exhaustExpiring ?: false, enabled && state.saving == null) { set(NotificationOption.ExhaustExpiring, it) }
                NovaDivider()
                Toggle(stringResource(R.string.notification_exhaust_off), stringResource(R.string.notification_exhaust_off_detail),
                    preferences?.exhaustOff ?: false, enabled && state.saving == null) { set(NotificationOption.ExhaustOff, it) }
                NovaDivider()
                Toggle(stringResource(R.string.notification_messages), stringResource(R.string.notification_messages_detail),
                    preferences?.messages ?: false, enabled && state.saving == null) { set(NotificationOption.Messages, it) }
                if (staff) {
                    NovaDivider()
                    Toggle(stringResource(R.string.notification_cie_alerts), stringResource(R.string.notification_cie_alerts_detail),
                        preferences?.cieStaffAlerts ?: false, enabled && state.saving == null) { set(NotificationOption.CieStaffAlerts, it) }
                }
                state.error?.let { Box(Modifier.padding(top = 12.dp)) { NovaNotice(it.text()) } }
                if (state.error != null && preferences == null) NovaButton(stringResource(R.string.notification_try_again), load, Modifier.padding(top = 12.dp),
                    style = NovaButtonStyle.Secondary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NovaButton(stringResource(if (state.testing) R.string.notification_sending else R.string.notification_send_test), sendTest,
                    enabled = !state.testing && status != PushStatus.PermissionNeeded, style = NovaButtonStyle.Outlined)
                state.testMessage?.let { Text(it.text(), style = NovaType.Supporting, color = NovaOnSurfaceVariant) }
                Text(stringResource(R.string.notification_delivery_note),
                    style = NovaType.Supporting, color = NovaOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeviceStatus(status: PushStatus, enable: () -> Unit, retryRegistration: () -> Unit) {
    when (status) {
        PushStatus.Ready -> NovaInfoStrip(stringResource(R.string.notification_ready))
        PushStatus.PermissionNeeded -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaNotice(stringResource(R.string.notification_denied), title = stringResource(R.string.notification_denied_title))
            NovaButton(stringResource(R.string.push_enable), enable)
        }
        PushStatus.ChannelBlocked -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaNotice(stringResource(R.string.notification_channel_blocked), title = stringResource(R.string.notification_channel_blocked_title))
            NovaButton(stringResource(R.string.notification_open_settings), enable)
        }
        PushStatus.NotRegistered -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaNotice(stringResource(R.string.notification_not_registered))
            NovaButton(stringResource(R.string.notification_try_again), retryRegistration, style = NovaButtonStyle.Secondary)
        }
    }
}

/** Linha com interruptor: a linha inteira alterna, com alvo bem maior que 48 dp. */
@Composable
private fun Toggle(title: String, detail: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = change)
        .heightIn(min = 72.dp).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        NovaRowText(title, detail, Modifier.weight(1f), enabled)
        // onCheckedChange nulo: o toque é tratado pela linha, e o leitor de tela anuncia um único controle.
        Switch(checked, null, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = NovaPrimary, checkedThumbColor = NovaSurface))
    }
}

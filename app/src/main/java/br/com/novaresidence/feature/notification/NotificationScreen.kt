package br.com.novaresidence.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.novaresidence.core.push.PushStatus
import br.com.novaresidence.core.push.openAlarmChannelSettings
import br.com.novaresidence.core.ui.*

private val Margin = 28.dp

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
    Column(Modifier.fillMaxSize().background(Color.White)) {
        NovaTopBar("Notificações", back)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            DeviceStatus(status, enable, retryRegistration)

            Column {
                SectionTitle("Alarme de incêndio")
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sempre ativo", style = NovaType.ItemTitle, color = NovaOnSurface)
                    Text("Enviado quando a central aciona o alarme, enquanto as notificações estiverem permitidas neste aparelho.",
                        style = NovaType.Body, color = NovaOnSurfaceVariant)
                }
                NovaTextLink("Configurar som e vibração do alarme", { openAlarmChannelSettings(context) },
                    alignment = Alignment.CenterStart, style = NovaType.Body)
            }

            Column {
                SectionTitle("Avisos")
                val preferences = state.preferences
                if (preferences == null && state.loading) NovaLoading("Carregando preferências…")
                val enabled = preferences != null
                Toggle("Antes de desligar o exaustor",
                    "Faltam ${preferences?.exhaustExpiringMinutes ?: 15} minutos para desligar o exaustor que você ligou ou reiniciou por último.",
                    preferences?.exhaustExpiring ?: false, enabled && state.saving == null) { set(NotificationOption.ExhaustExpiring, it) }
                HorizontalDivider(color = NovaOutline)
                Toggle("Exaustor desligado", "Quando o exaustor que você ligou ou reiniciou por último desligar.",
                    preferences?.exhaustOff ?: false, enabled && state.saving == null) { set(NotificationOption.ExhaustOff, it) }
                HorizontalDivider(color = NovaOutline)
                Toggle("Comunicados", "Novos comunicados da administração do condomínio.",
                    preferences?.messages ?: false, enabled && state.saving == null) { set(NotificationOption.Messages, it) }
                if (staff) {
                    HorizontalDivider(color = NovaOutline)
                    Toggle("Alertas da central", "Falha, supervisão, reinício, bloqueio e falta de comunicação da central de incêndio.",
                        preferences?.cieStaffAlerts ?: false, enabled && state.saving == null) { set(NotificationOption.CieStaffAlerts, it) }
                }
                state.error?.let { Box(Modifier.padding(top = 12.dp)) { NovaNotice(it) } }
                if (state.error != null && preferences == null) NovaButton("Tentar novamente", load, Modifier.padding(top = 12.dp),
                    style = NovaButtonStyle.Secondary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NovaButton(if (state.testing) "Enviando…" else "Enviar notificação de teste", sendTest,
                    enabled = !state.testing && status != PushStatus.PermissionNeeded, style = NovaButtonStyle.Outlined)
                state.testMessage?.let { Text(it, style = NovaType.Supporting, color = NovaOnSurfaceVariant) }
                Text("A entrega depende do Android e da conexão do aparelho e pode atrasar com a economia de bateria.",
                    style = NovaType.Supporting, color = NovaOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeviceStatus(status: PushStatus, enable: () -> Unit, retryRegistration: () -> Unit) {
    when (status) {
        PushStatus.Ready -> NovaInfoStrip("Este aparelho recebe as notificações do condomínio.", centered = false)
        PushStatus.PermissionNeeded -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaNotice("Sem permissão, o alarme de incêndio e os demais avisos não aparecem neste aparelho.", title = "Notificações desativadas")
            NovaButton("Ativar notificações", enable)
        }
        PushStatus.ChannelBlocked -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaNotice("O alarme de incêndio está bloqueado nas configurações do Android.", title = "Alarme de incêndio bloqueado")
            NovaButton("Abrir configurações", enable)
        }
        PushStatus.NotRegistered -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaNotice("Este aparelho ainda não foi registrado para receber notificações. Verifique a conexão.")
            NovaButton("Tentar novamente", retryRegistration, style = NovaButtonStyle.Secondary)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, Modifier.semantics { heading() }, style = NovaType.Section, color = NovaOnSurface)
}

/** Linha com interruptor: a linha inteira alterna, com alvo bem maior que 48 dp. */
@Composable
private fun Toggle(title: String, detail: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = change)
        .heightIn(min = 72.dp).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = NovaType.ItemTitle, color = if (enabled) NovaOnSurface else NovaDisabledContent)
            Text(detail, style = NovaType.Body, color = NovaOnSurfaceVariant)
        }
        // onCheckedChange nulo: o toque é tratado pela linha, e o leitor de tela anuncia um único controle.
        Switch(checked, null, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = NovaPrimary, checkedThumbColor = Color.White))
    }
}

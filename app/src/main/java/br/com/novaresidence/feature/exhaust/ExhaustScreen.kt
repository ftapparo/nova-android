package br.com.novaresidence.feature.exhaust

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*

private val ExhaustOffline = Color(0xFF667085)
private val ExhaustOfflineContainer = Color(0xFFEEF0F3)
private val ExhaustSuccessContainer = Color(0x1A189D1F)
private val ExhaustWarningContainer = Color(0xFFFFF4E5)
private val ExhaustIconContainer = Color(0xFFEEE4FA)
private val ExhaustRunning = Color(0xFF2E7D32)

private data class StateCopy(val title: String, val description: String, val color: Color, val note: String)

@Composable
fun ExhaustScreen(
    state: ExhaustState, unitLabel: String?, blockedMessage: String?,
    activate: () -> Unit, restart: () -> Unit, refresh: () -> Unit,
    back: () -> Unit, selectUnit: () -> Unit,
) {
    val phase = state.phase
    val autoOff = "O exaustor será desligado automaticamente ao final do tempo programado."
    val copy = when (phase) {
        ExhaustPhase.Off -> StateCopy("Desligado", "O exaustor está disponível\npara utilização.", NovaPrimary, autoOff)
        ExhaustPhase.Starting -> StateCopy("Ligando", "Aguarde enquanto o exaustor\né acionado.", NovaPrimary, "O acionamento pode levar alguns segundos.")
        ExhaustPhase.Running -> StateCopy("Ligado", "O exaustor está em funcionamento.", NovaSuccess, autoOff)
        ExhaustPhase.Finishing -> StateCopy("Desligando", "O ciclo está sendo encerrado.\nAguarde alguns instantes.", NovaPrimary, "O exaustor ficará disponível novamente após a finalização.")
        ExhaustPhase.Offline -> StateCopy("Indisponível", "Sem comunicação com o exaustor.\nTente novamente mais tarde.", ExhaustOffline, "O equipamento não pode ser utilizado enquanto estiver offline.")
        ExhaustPhase.Maintenance -> StateCopy("Indisponível", "O exaustor está temporariamente\nindisponível para utilização.", NovaWarning, "O uso será liberado após a conclusão da manutenção.")
        // Resultado inconclusivo (falha de rede ou resposta de erro) mantém os textos aprovados: não afirmamos que falhou.
        ExhaustPhase.Error -> if (state.error != null) StateCopy("Não foi possível confirmar a ativação", "Atualize o estado antes de tentar novamente.", NovaDanger, "Se o problema continuar, informe a administração do condomínio.")
            else StateCopy("Falha na ativação", "Não foi possível ligar o exaustor.\nTente novamente.", NovaDanger, "Se o problema continuar, informe a administração do condomínio.")
        ExhaustPhase.Loading -> StateCopy("Consultando", "Aguarde enquanto consultamos o estado do equipamento.", NovaPrimary, autoOff)
        ExhaustPhase.Unavailable -> StateCopy("Indisponível", blockedMessage ?: if (unitLabel == null) "Selecione uma unidade vinculada para consultar seu exaustor." else "Nenhum exaustor disponível para esta unidade.", ExhaustOffline, "O estado exibido é exclusivo do equipamento da sua unidade.")
    }
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar("Exaustor", back)
        NovaUnitContext(unitLabel, NovaContextSubtle, selectUnit)
        BoxWithConstraints(Modifier.weight(1f)) {
            val horizontal = 24.dp
            val available = maxHeight
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = available),
                verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    state.exhaust?.let { EquipmentIdentity(it, horizontal) }
                    Column(Modifier.fillMaxWidth().padding(start = horizontal, end = horizontal, top = 36.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Estado de operação", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(copy.title, Modifier.semantics { heading() }, fontSize = 26.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = copy.color)
                        if (phase in setOf(ExhaustPhase.Starting, ExhaustPhase.Finishing, ExhaustPhase.Loading)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().height(8.dp), color = NovaPrimary, trackColor = MaterialTheme.colorScheme.primaryContainer)
                        } else Box(Modifier.size(41.dp, 4.dp).background(copy.color, RoundedCornerShape(2.dp)))
                        Text(copy.description, style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.5.sp), color = NovaOnSurface)
                        if (phase == ExhaustPhase.Running) state.remainingHoursMinutes()?.let { RemainingTime(it) }
                        if (phase == ExhaustPhase.Error) state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        state.requestId?.let { Text("Referência para suporte: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(start = horizontal, end = horizontal, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (state.retrySeconds > 0) Text("Tente novamente em ${state.retrySeconds}s", style = MaterialTheme.typography.bodyMedium)
                    when (phase) {
                        ExhaustPhase.Off -> NovaButton("Ligar exaustor", activate, enabled = state.canActivate && blockedMessage == null)
                        ExhaustPhase.Starting -> NovaButton("Ligando ...", {}, enabled = false)
                        ExhaustPhase.Running -> NovaButton(if (state.restarting) "Reiniciando ..." else "Reiniciar tempo", restart, enabled = state.canRestart && blockedMessage == null, outlined = true)
                        ExhaustPhase.Error -> NovaButton("Tentar novamente", if (state.canActivate) activate else refresh, enabled = !state.sending && !state.loading && state.retrySeconds == 0L)
                        ExhaustPhase.Unavailable -> NovaButton("Atualizar", refresh, enabled = !state.loading && state.retrySeconds == 0L, secondary = true)
                        else -> Unit
                    }
                    HorizontalDivider(color = NovaOutline)
                    NovaFootnote(copy.note)
                }
            }
        }
    }
}

@Composable
private fun EquipmentIdentity(exhaust: Exhaust, horizontal: androidx.compose.ui.unit.Dp) {
    Row(Modifier.fillMaxWidth().background(Color.White).heightIn(min = 79.dp).padding(horizontal = horizontal, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(ExhaustIconContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.nova_exhaust_nav), null, Modifier.size(24.dp), tint = NovaPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Exaustor da unidade", style = MaterialTheme.typography.labelLarge)
            Text("Torre ${exhaust.tower} · Prumada ${exhaust.final}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val (label, color, container) = when {
            !exhaust.moduleOnline -> Triple("Offline", ExhaustOffline, ExhaustOfflineContainer)
            exhaust.maintenance -> Triple("Manutenção", NovaWarning, ExhaustWarningContainer)
            else -> Triple("Disponível", NovaSuccess, ExhaustSuccessContainer)
        }
        Row(Modifier.background(container, RoundedCornerShape(16.dp)).heightIn(min = 32.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
        }
    }
}

@Composable
private fun RemainingTime(time: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(ExhaustSuccessContainer, RoundedCornerShape(8.dp)).height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(ExhaustRunning, RoundedCornerShape(2.dp)))
        Row(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tempo restante", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.sp), color = ExhaustRunning)
            Text(time, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = ExhaustRunning, textAlign = TextAlign.End)
        }
    }
}

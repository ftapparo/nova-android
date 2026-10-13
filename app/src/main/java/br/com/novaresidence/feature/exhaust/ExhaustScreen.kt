package br.com.novaresidence.feature.exhaust

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*

private data class StateCopy(val image: Int, val title: String, val description: String, val info: String?)

private const val AutoOff = "Desligamento automático ao final do tempo programado."

/** Exaustor da unidade ativa (Figma 19–24). [unitLabel] segue o formato "Apto. 124 · Torre A". */
@Composable
fun ExhaustScreen(
    state: ExhaustState, unitLabel: String?, blockedMessage: String?,
    activate: () -> Unit, restart: () -> Unit, refresh: () -> Unit, selectUnit: () -> Unit,
) {
    val phase = state.phase
    val copy = when (phase) {
        ExhaustPhase.Off -> StateCopy(R.drawable.nova_exhaust_off, "Desligado", "Pronto para utilização.", AutoOff)
        ExhaustPhase.Starting -> StateCopy(R.drawable.nova_exhaust_off, "Ligando", "Aguarde o exaustor ser acionado.", AutoOff)
        ExhaustPhase.Running -> StateCopy(R.drawable.nova_exhaust_on, "Ligado",
            state.remainingClock()?.let { "Tempo restante · $it" } ?: "Sem desligamento programado.", AutoOff)
        ExhaustPhase.Finishing -> StateCopy(R.drawable.nova_exhaust_off, "Desligando", "Aguarde alguns instantes.", AutoOff)
        ExhaustPhase.Offline -> StateCopy(R.drawable.nova_exhaust_offline, "Offline", "Não foi possível se comunicar com o exaustor.",
            "O equipamento não pode ser utilizado enquanto estiver offline.")
        ExhaustPhase.Maintenance -> StateCopy(R.drawable.nova_exhaust_maintenance, "Em manutenção", "O exaustor está temporariamente indisponível para utilização.",
            "O uso será liberado após a conclusão da manutenção.")
        // Resultado inconclusivo (falha de rede ou resposta de erro) mantém os textos aprovados: não afirmamos que falhou.
        ExhaustPhase.Error -> if (state.error != null) StateCopy(R.drawable.nova_exhaust_off, "Não foi possível confirmar a ativação",
            "Atualize o estado antes de tentar novamente.", "Se o problema continuar, informe a administração do condomínio.")
            else StateCopy(R.drawable.nova_exhaust_off, "Falha na ativação", "Não foi possível ligar o exaustor. Tente novamente.",
            "Se o problema continuar, informe a administração do condomínio.")
        ExhaustPhase.Loading -> StateCopy(R.drawable.nova_exhaust_off, "Consultando", "Aguarde enquanto consultamos o estado do equipamento.", null)
        ExhaustPhase.Unavailable -> StateCopy(R.drawable.nova_exhaust_offline, "Indisponível",
            blockedMessage ?: if (unitLabel == null) "Selecione uma unidade vinculada para consultar seu exaustor." else "Nenhum exaustor disponível para esta unidade.",
            "O estado exibido é exclusivo do equipamento da sua unidade.")
    }
    Column(Modifier.fillMaxSize().background(NovaServiceBackground)) {
        NovaTopBar("Exaustor")
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // Em telas mais baixas que o frame (ex.: J4 com botões), a ilustração encolhe antes de esconder ação e aviso (DESIGN_GUIDE §6).
            val illustration = (maxHeight - 452.dp).coerceIn(150.dp, 247.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    ContextCard(unitLabel, state, selectUnit)
                    Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        NovaIllustration(copy.image, Modifier.height(illustration).aspectRatio(329f / 247f, matchHeightConstraintsFirst = true))
                        Text(copy.title, Modifier.fillMaxWidth().semantics { heading(); liveRegion = LiveRegionMode.Polite }, style = NovaType.Hero, color = NovaInk, textAlign = TextAlign.Center)
                        Text(copy.description, Modifier.fillMaxWidth(), color = NovaOnSurfaceVariant, textAlign = TextAlign.Center,
                            style = if (phase == ExhaustPhase.Running) NovaType.HeroSupporting.copy(fontWeight = FontWeight.Medium) else NovaType.HeroSupporting)
                        if (phase == ExhaustPhase.Error) state.error?.let { Text(it, Modifier.fillMaxWidth(), style = NovaType.Body, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center) }
                        if (state.retrySeconds > 0) Text("Tente novamente em ${state.retrySeconds}s", style = NovaType.Body, color = NovaOnSurfaceVariant)
                        Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Action(phase, state, blockedMessage, activate, restart, refresh)
                            copy.info?.let { NovaInfoStrip(it) }
                            state.requestId?.let { Text("Referência para suporte: $it", Modifier.fillMaxWidth(), style = NovaType.Caption, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Action(phase: ExhaustPhase, state: ExhaustState, blockedMessage: String?, activate: () -> Unit, restart: () -> Unit, refresh: () -> Unit) {
    val power = NovaType.Button.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    when (phase) {
        ExhaustPhase.Off -> NovaButton("Ligar exaustor", activate, enabled = state.canActivate && blockedMessage == null,
            icon = R.drawable.nova_power, height = 52.dp, textStyle = power)
        ExhaustPhase.Starting -> NovaButton("Ligando...", {}, enabled = false, icon = R.drawable.nova_power, height = 52.dp, textStyle = power)
        ExhaustPhase.Finishing -> NovaButton("Desligando...", {}, enabled = false, icon = R.drawable.nova_power, height = 52.dp, textStyle = power)
        ExhaustPhase.Running -> NovaButton(if (state.restarting) "Reiniciando..." else "Reiniciar tempo", restart,
            enabled = state.canRestart && blockedMessage == null, style = NovaButtonStyle.Outlined, height = 52.dp)
        ExhaustPhase.Error -> NovaButton("Tentar novamente", if (state.canActivate) activate else refresh,
            enabled = !state.sending && !state.loading && state.retrySeconds == 0L, height = 52.dp)
        ExhaustPhase.Unavailable -> NovaButton("Atualizar", refresh, enabled = !state.loading && state.retrySeconds == 0L,
            style = NovaButtonStyle.Secondary, height = 52.dp)
        else -> Unit
    }
}

/** Cartão "Unidade ativa · Equipamento" (Figma 19); o toque abre a seleção de unidade. */
@Composable
private fun ContextCard(unitLabel: String?, state: ExhaustState, selectUnit: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val exhaust = state.exhaust
    val (label, color, container) = when {
        exhaust == null -> if (state.loading) Triple("Consultando", NovaPrimary, NovaPrimarySoft) else Triple("Indisponível", NovaOffline, NovaOfflineContainer)
        !exhaust.moduleOnline -> Triple("Offline", NovaOffline, NovaOfflineContainer)
        exhaust.maintenance -> Triple("Manutenção", NovaWarning, NovaWarningContainer)
        else -> Triple("Disponível", NovaSuccess, NovaSuccessContainer)
    }
    Row(Modifier.fillMaxWidth().clip(shape).background(Color.White).border(1.dp, NovaOutline, shape)
        .clickable(role = Role.Button, onClickLabel = "Selecionar unidade", onClick = selectUnit).heightIn(min = 116.dp).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(NovaPrimaryTint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.nova_units), null, Modifier.size(28.dp), tint = NovaPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Unidade ativa", style = NovaType.Supporting, color = NovaOnSurfaceVariant)
            Text(unitLabel ?: "Nenhuma unidade", style = NovaType.HeroSupporting.copy(fontWeight = FontWeight.Bold), color = NovaInk)
        }
        VerticalDivider(Modifier.height(60.dp), color = NovaOutline)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Equipamento", style = NovaType.Supporting, color = NovaOnSurfaceVariant)
            NovaStatusBadge(label, color, container, height = 22.dp)
        }
    }
}

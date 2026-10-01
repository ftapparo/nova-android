package br.com.novaresidence.feature.fire

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*

private enum class FirePage { Status, History, Event }

/** Central de incêndio (Figma 09–13), somente leitura. */
@Composable
fun FireFeature(
    state: FireState, unitLabel: String?, back: () -> Unit, selectUnit: () -> Unit, refresh: () -> Unit, loadHistory: () -> Unit,
    fullScreen: (Boolean) -> Unit = {},
) {
    var page by rememberSaveable { mutableStateOf(FirePage.Status) }
    LaunchedEffect(page) { fullScreen(page != FirePage.Status) }
    var eventKey by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(page != FirePage.Status) { page = if (page == FirePage.Event) FirePage.History else FirePage.Status }
    val event = state.history?.firstOrNull { it.key == eventKey }
    when {
        page == FirePage.Event && event != null -> FireEventScreen(event) { page = FirePage.History }
        page != FirePage.Status -> FireHistoryScreen(state, { page = FirePage.Status }, loadHistory) { eventKey = it.key; page = FirePage.Event }
        else -> FireStatusScreen(state, unitLabel, back, selectUnit, refresh) { loadHistory(); page = FirePage.History }
    }
}

@Composable
private fun FireStatusScreen(state: FireState, unitLabel: String?, back: () -> Unit, selectUnit: () -> Unit, refresh: () -> Unit, openHistory: () -> Unit) {
    val status = state.status
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar("Central de Incêndio", back)
        NovaUnitContext(unitLabel, if (status == FireStatus.Normal) NovaContextSubtle else NovaSecondaryContainer, selectUnit)
        if (status == FireStatus.Alarm) AlarmContent(state) else BoxWithConstraints(Modifier.weight(1f)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                StatusSummary(state)
                Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(color = NovaOutline)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.nova_radio_checked), null, Modifier.size(24.dp), tint = Color(0xFF1D1B20))
                        // Sem push nativo, o app só acompanha a central enquanto estiver aberto; não prometemos aviso fora dele.
                        Text("Monitoramento automático ativo.\nO estado é atualizado enquanto o aplicativo estiver aberto.",
                            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.5.sp), color = NovaOnSurfaceVariant)
                    }
                    NovaButton("Ver histórico de eventos", openHistory, secondary = true, icon = R.drawable.nova_history)
                }
            }
        }
    }
}

@Composable
private fun StatusSummary(state: FireState) {
    val status = state.status
    data class Copy(val chip: String, val mark: String, val title: String, val text: String, val color: Color, val container: Color)
    val copy = when (status) {
        FireStatus.Normal -> Copy("Normal", "✓", "Sistema normal", "Nenhum alarme de incêndio ativo.", NovaSuccess, NovaSuccessContainer)
        FireStatus.Attention -> Copy("Verificação necessária", "!", "Atenção", "Existe uma ocorrência que requer verificação.", NovaWarning, NovaWarningContainer)
        FireStatus.Offline -> Copy("Sem comunicação", "!", "Central sem comunicação", "Não foi possível obter o estado atual da central. A equipe do condomínio é avisada pelo painel.", NovaOffline, Color(0xFFEEF0F3))
        FireStatus.Loading -> Copy("Consultando", "…", "Consultando a central", "Aguarde enquanto obtemos o estado atual.", NovaPrimary, NovaSecondaryContainer)
        else -> Copy("Indisponível", "!", "Estado indisponível", state.error ?: "Não foi possível consultar a central.", NovaOffline, Color(0xFFEEF0F3))
    }
    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NovaStatusChip(copy.chip, copy.color, copy.container) {
            Text(copy.mark, Modifier.width(16.dp), fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, color = copy.color)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(copy.title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, color = copy.color)
            Text(copy.text, style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.5.sp), color = NovaOnSurface)
            state.panel?.lastUpdated?.let { Text("Última atualização: ${it.clockTime()}", fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = NovaOnSurfaceVariant) }
            if (state.error != null && status != FireStatus.Unavailable) Text(state.error, style = MaterialTheme.typography.bodySmall, color = NovaOnSurfaceVariant)
            if (state.retrySeconds > 0) Text("Nova consulta em ${state.retrySeconds}s", style = MaterialTheme.typography.bodySmall, color = NovaOnSurfaceVariant)
        }
    }
}

@Composable
private fun ColumnScope.AlarmContent(state: FireState) {
    val log = state.activeAlarm
    Column(Modifier.fillMaxWidth().background(NovaError).semantics { liveRegion = LiveRegionMode.Assertive }.padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.nova_siren), null, Modifier.size(30.dp), tint = Color.White)
            Text("ALARME DE INCÊNDIO", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
        val place = listOfNotNull(log?.towerLabel(), log?.floorLabel()?.replaceFirstChar { it.uppercase() })
        if (place.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            place.forEach { Text(it, style = MaterialTheme.typography.headlineMedium, color = Color.White) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(log?.deviceLabel() ?: "Central de incêndio", fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = Color.White)
            log?.instant()?.let { Text(it.toEpochMilli().clockTime(), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = Color.White) }
        }
    }
    BoxWithConstraints(Modifier.weight(1f)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Mantenha a calma e siga as orientações locais", style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp), color = NovaOnSurface)
                Guidance(R.drawable.nova_circle_x, "Use as escadas. Não utilize elevadores.")
                Guidance(R.drawable.nova_route, "Siga a sinalização e as instruções da equipe do condomínio.")
            }
            Text(listOfNotNull(state.panel?.lastUpdated?.let { "Atualizado às ${it.clockTime()}" }, "O aviso permanecerá visível enquanto o alarme estiver ativo.").joinToString(" · "),
                Modifier.fillMaxWidth().padding(top = 24.dp), style = MaterialTheme.typography.bodyMedium, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Guidance(icon: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = NovaError)
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.5.sp), color = NovaOnSurface)
    }
}

private data class EventLook(val color: Color, val container: Color)

private fun CieLog.look(): EventLook = when (type) {
    "alarme" -> EventLook(NovaError, NovaErrorContainer)
    "falha", "supervisao" -> EventLook(NovaWarning, NovaWarningContainer)
    "operacao" -> EventLook(NovaSuccess, NovaSuccessContainer)
    else -> EventLook(NovaOffline, Color(0xFFEEF0F3))
}

@Composable
private fun EventIcon(log: CieLog, size: Int) {
    val look = log.look()
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        when (log.type) {
            "alarme" -> Icon(painterResource(R.drawable.nova_siren), null, Modifier.size(size.dp), tint = look.color)
            "bloqueio" -> Icon(painterResource(R.drawable.nova_mic_off), null, Modifier.size(size.dp), tint = look.color)
            "operacao" -> Text("✓", fontSize = (size - 2).sp, lineHeight = (size + 4).sp, fontWeight = FontWeight.Medium, color = look.color)
            else -> Text("!", fontSize = (size - 2).sp, lineHeight = (size + 4).sp, fontWeight = FontWeight.Medium, color = look.color)
        }
    }
}

@Composable
private fun FireHistoryScreen(state: FireState, back: () -> Unit, reload: () -> Unit, open: (CieLog) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        NovaTopBar("Histórico de eventos", back)
        val events = state.history.orEmpty()
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Últimos 30 dias", fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = NovaOnSurfaceVariant)
                    if (state.history != null) Text(if (events.size == 1) "1 evento" else "${events.size} eventos", style = MaterialTheme.typography.labelMedium, color = NovaPrimary)
                }
                HorizontalDivider(color = NovaOutline)
            }
            when {
                state.historyLoading && state.history == null -> item { NovaLoading("Carregando eventos…") }
                state.historyError != null && state.history == null -> item {
                    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NovaNotice(state.historyError)
                        NovaButton("Tentar novamente", reload, secondary = true)
                    }
                }
                events.isEmpty() -> item {
                    Text("Nenhum evento registrado nos últimos 30 dias.", Modifier.padding(28.dp), style = MaterialTheme.typography.bodyMedium, color = NovaOnSurfaceVariant)
                }
                else -> items(events, key = { it.key }) { log ->
                    Row(Modifier.fillMaxWidth().clickable { open(log) }.heightIn(min = 88.dp).padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EventIcon(log, 22)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(log.title(), Modifier.weight(1f), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = NovaOnSurface)
                                log.instant()?.let { Text(it.eventTime(), style = MaterialTheme.typography.bodySmall, color = NovaOnSurfaceVariant) }
                            }
                            Text(log.locationLine(), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = NovaOnSurfaceVariant)
                            Text(log.statusLabel(), style = MaterialTheme.typography.labelMedium, color = log.look().color)
                        }
                        Text("›", Modifier.width(18.dp), fontSize = 12.sp, lineHeight = 16.sp, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    HorizontalDivider(color = NovaOutline)
                }
            }
        }
    }
}

@Composable
private fun FireEventScreen(log: CieLog, back: () -> Unit) {
    val look = log.look()
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar("Detalhe do evento", back)
        BoxWithConstraints(Modifier.weight(1f).background(Color.White)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    NovaStatusChip(log.statusLabel(), look.color, look.container) { EventIcon(log, 16) }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(log.title(), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                        log.instant()?.let { Text(it.eventTime(), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = NovaOnSurfaceVariant) }
                    }
                    HorizontalDivider(color = NovaOutline)
                    Column {
                        val rows = listOf(
                            "Local" to (listOfNotNull(log.towerLabel(), log.floorLabel()).joinToString(" · ").ifEmpty { "Central de monitoramento" }),
                            "Origem" to (log.deviceLabel() ?: "Central de incêndio"),
                            "Início" to (log.instant()?.toEpochMilli()?.clockTime() ?: "—"),
                            "Situação" to log.statusLabel(),
                        )
                        rows.forEachIndexed { index, (label, value) ->
                            if (index > 0) HorizontalDivider(color = NovaOutline)
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(label, Modifier.width(132.dp), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = NovaOnSurfaceVariant)
                                Text(value, Modifier.weight(1f), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, color = NovaOnSurface)
                            }
                        }
                    }
                }
                NovaButton("Fechar", back, Modifier.padding(top = 24.dp), secondary = true)
            }
        }
    }
}

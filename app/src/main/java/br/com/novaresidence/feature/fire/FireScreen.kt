package br.com.novaresidence.feature.fire

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*
import kotlinx.coroutines.delay

private enum class FirePage { Status, History, Event }

/**
 * Central de incêndio (Figma 26–30c), somente leitura. [fullScreen] oculta o menu inferior no histórico e no detalhe.
 * [notificationsReady]: aparelho registrado e notificações do alarme permitidas; senão a tela 26 oferece [enableNotifications].
 */
@Composable
fun FireFeature(
    state: FireState, refresh: () -> Unit, loadHistory: () -> Unit, fullScreen: (Boolean) -> Unit = {},
    notificationsReady: Boolean = false, enableNotifications: (() -> Unit)? = null,
) {
    var page by rememberSaveable { mutableStateOf(FirePage.Status) }
    var eventKey by rememberSaveable { mutableStateOf<String?>(null) }
    var eventOrigin by rememberSaveable { mutableStateOf(FirePage.History) }
    LaunchedEffect(page) { fullScreen(page != FirePage.Status) }
    BackHandler(page != FirePage.Status) { page = if (page == FirePage.Event) eventOrigin else FirePage.Status }
    val event = (state.history.orEmpty() + listOfNotNull(state.activeAlarm)).firstOrNull { it.key == eventKey }
    val open: (CieLog, FirePage) -> Unit = { log, origin -> eventKey = log.key; eventOrigin = origin; page = FirePage.Event }
    when {
        page == FirePage.Event && event != null -> FireEventScreen(event) { page = eventOrigin }
        page != FirePage.Status -> FireHistoryScreen(state, { page = FirePage.Status }, loadHistory) { open(it, FirePage.History) }
        else -> FireStatusScreen(state, refresh, { loadHistory(); page = FirePage.History }, notificationsReady, enableNotifications) { open(it, FirePage.Status) }
    }
}

private data class StatusLook(
    val image: Int, val grayscale: Boolean, val badge: String, val color: Color, val container: Color,
    val title: String, val text: String, val info: String?,
)

@Composable
private fun FireStatusScreen(
    state: FireState, refresh: () -> Unit, openHistory: () -> Unit, notificationsReady: Boolean, enableNotifications: (() -> Unit)?,
    openEvent: (CieLog) -> Unit,
) {
    val status = state.status
    Column(Modifier.fillMaxSize().background(NovaServiceBackground)) {
        NovaTopBar("Central de Incêndio", background = NovaServiceBackground, color = NovaInk)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // Em telas mais baixas que o frame, a ilustração encolhe antes de empurrar orientações e ações para fora da tela.
            val available = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (status == FireStatus.Alarm) AlarmContent(state, (available - 560.dp).coerceIn(96.dp, 159.dp), openEvent)
                    else StatusContent(state, (available - 420.dp).coerceIn(120.dp, 200.dp), refresh, openHistory, notificationsReady, enableNotifications)
                }
            }
        }
    }
}

@Composable
private fun StatusContent(
    state: FireState, illustration: Dp, refresh: () -> Unit, openHistory: () -> Unit,
    notificationsReady: Boolean, enableNotifications: (() -> Unit)?,
) {
    val status = state.status
    val look = when (status) {
        FireStatus.Normal -> StatusLook(R.drawable.nova_fire_normal, false, "Central online", NovaSuccess, NovaSuccessContainer,
            // Só promete aviso com o aparelho registrado e o alarme permitido (AGENTS.md, tela 26); a pessoa pode
            // desligar as notificações a qualquer momento, por isso o estado é reconferido a cada volta ao app.
            "Sistema normal", "Nenhum alarme de incêndio ativo.",
            if (notificationsReady) "Você será avisado se o estado da central mudar." else "O estado é atualizado enquanto o aplicativo estiver aberto.")
        FireStatus.Attention -> StatusLook(R.drawable.nova_fire_attention, false, "Em verificação", NovaWarning, NovaWarningContainer,
            "Atenção", "Existe um aviso que requer verificação.", "A ocorrência foi registrada pela central e deve ser verificada pela equipe responsável.")
        FireStatus.Offline -> StatusLook(R.drawable.nova_fire_normal, true, "Sem comunicação", NovaOffline, NovaOfflineContainer,
            "Central sem comunicação", "Não foi possível obter o estado atual da central.", "A equipe do condomínio é avisada pelo painel.")
        FireStatus.Loading -> StatusLook(R.drawable.nova_fire_normal, true, "Consultando", NovaPrimary, NovaPrimarySoft,
            "Consultando a central", "Aguarde enquanto obtemos o estado atual.", null)
        else -> StatusLook(R.drawable.nova_fire_normal, true, "Indisponível", NovaOffline, NovaOfflineContainer,
            "Estado indisponível", state.error ?: "Não foi possível consultar a central.", null)
    }
    Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NovaIllustration(look.image, Modifier.size(illustration), contentScale = ContentScale.Crop,
            colorFilter = if (look.grayscale) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
        NovaStatusBadge(look.badge, look.color, look.container)
        Text(look.title, Modifier.fillMaxWidth().semantics { heading() }, style = NovaType.Hero, color = NovaInk, textAlign = TextAlign.Center)
        Text(look.text, Modifier.fillMaxWidth(), style = NovaType.Field.copy(lineHeight = 22.sp), color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
        if (status == FireStatus.Attention) state.activeEvents.firstNotNullOfOrNull { it.placeLine() }?.let { place ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Local do aviso", style = NovaType.Caption, color = NovaOnSurfaceVariant)
                Text(place, style = NovaType.ItemTitle, color = NovaInk, textAlign = TextAlign.Center)
            }
        }
        look.info?.let { NovaInfoStrip(it, accent = look.color, container = look.container, centered = false) }
        if (status == FireStatus.Normal && !notificationsReady && enableNotifications != null) NovaTextLink("Ativar notificações", enableNotifications)
        if (status == FireStatus.Unavailable) NovaButton("Tentar novamente", refresh, enabled = state.retrySeconds == 0L, height = 52.dp)
        NovaButton("Ver histórico de eventos", openHistory, style = NovaButtonStyle.Outlined, height = 52.dp)
        Footnotes(state)
    }
}

@Composable
private fun Footnotes(state: FireState) {
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(15_000); value = System.currentTimeMillis() } }
    state.panel?.lastUpdated?.let { updated ->
        Text(if (now - updated < 60_000) "Atualizado agora" else "Atualizado às ${updated.clockTime()}",
            style = NovaType.Caption, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
    }
    if (state.error != null && state.status != FireStatus.Unavailable) Text(state.error, style = NovaType.Caption, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
    if (state.retrySeconds > 0) Text("Nova consulta em ${state.retrySeconds}s", style = NovaType.Caption, color = NovaOnSurfaceVariant)
}

private val SafetyGuidance = listOf(
    "Aguarde as orientações da brigada de incêndio." to false,
    "Se houver fogo ou fumaça no seu andar, saia imediatamente do local." to true,
    "Utilize as escadas. Não use os elevadores." to false,
    "Em caso de fumaça, permaneça abaixado." to false,
    "Siga as orientações de evacuação dos brigadistas." to false,
)

/** Alarme ativo (Figma 28): prevalece sobre qualquer outro estado e permanece enquanto a central indicar alarme. */
@Composable
private fun AlarmContent(state: FireState, illustration: Dp, openEvent: (CieLog) -> Unit) {
    val log = state.activeAlarm
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Assertive },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NovaIllustration(R.drawable.nova_fire_alarm, Modifier.size(illustration * 200f / 159f, illustration), contentScale = ContentScale.Crop)
            NovaStatusBadge("Alarme ativo", NovaAlarm, NovaAlarmContainer)
            Text("ALARME DE INCÊNDIO", Modifier.fillMaxWidth().semantics { heading() }, style = NovaType.HeadlineHome.copy(fontWeight = FontWeight.Bold),
                color = NovaAlarm, textAlign = TextAlign.Center)
            log?.placeLine(" - ")?.let { Text(it, Modifier.fillMaxWidth(), style = NovaType.HeadlineHome, color = NovaInk, textAlign = TextAlign.Center) }
            log?.instant()?.let { Text("Ocorrência iniciada às ${it.clockTime()}", style = NovaType.Supporting, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center) }
        }
        HorizontalDivider(color = NovaOutline)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Orientações de segurança", Modifier.semantics { heading() }, style = NovaType.Highlight, color = NovaInk)
            SafetyGuidance.forEach { (text, emphasis) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 38.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(NovaAlarm, CircleShape))
                    Text(text, style = if (emphasis) NovaType.BodyEmphasis else NovaType.Body, color = NovaInk)
                }
            }
        }
        if (log != null) NovaButton("Ver detalhes do evento", { openEvent(log) }, style = NovaButtonStyle.Outlined)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { Footnotes(state) }
    }
}

private data class EventLook(val color: Color, val container: Color, val icon: Int, val iconColor: Color = color)

// Ícones do histórico (Figma 29, 13/10/2026): sirene, alerta e chave inglesa, sem círculo de fundo.
private fun CieLog.look(): EventLook = when (category()) {
    "alarme" -> EventLook(NovaAlarm, NovaAlarmContainer, R.drawable.nova_event_alarm, NovaError)
    "falha", "supervisao" -> EventLook(NovaWarning, NovaWarningContainer, R.drawable.nova_event_warning)
    "bloqueio" -> EventLook(NovaOffline, NovaOfflineContainer, R.drawable.nova_event_operation)
    else -> EventLook(NovaPrimary, if (event?.severity == "ok") NovaSuccessContainer else NovaNeutralContainer, R.drawable.nova_event_operation)
}

// Filtros do Figma 29; supervisão acompanha as falhas e bloqueio acompanha as operações da central.
private val HistoryFilters = listOf(
    "Todos" to null,
    "Alarmes" to setOf("alarme"),
    "Falhas" to setOf("falha", "supervisao"),
    "Operação" to setOf("operacao", "bloqueio"),
)

@Composable
private fun FireHistoryScreen(state: FireState, back: () -> Unit, reload: () -> Unit, open: (CieLog) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val types = HistoryFilters[filter].second
    // Até 200 eventos: agrupar só quando o histórico ou o filtro mudam, não a cada recomposição.
    val groups = remember(state.history, filter) {
        state.history.orEmpty().filter { types == null || it.category() in types }.groupBy { it.instant()?.dayLabel() ?: "Sem data" }.toList()
    }
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar("Histórico de eventos", back)
        NovaFilterBar(HistoryFilters.map { it.first }, filter) { filter = it }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 24.dp)) {
            when {
                state.historyLoading && state.history == null -> item { NovaLoading("Carregando eventos…") }
                state.historyError != null && state.history == null -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NovaNotice(state.historyError)
                        NovaButton("Tentar novamente", reload, style = NovaButtonStyle.Secondary)
                    }
                }
                groups.isEmpty() -> item {
                    Text(if (filter == 0) "Nenhum evento registrado nos últimos 30 dias." else "Nenhum evento deste tipo nos últimos 30 dias.",
                        style = NovaType.Body, color = NovaOnSurfaceVariant)
                }
                // Cada evento é um item preguiçoso: um dia pode ter dezenas de eventos, e montá-los de uma vez travava a tela.
                else -> groups.forEachIndexed { groupIndex, (day, logs) ->
                    item(key = "day:$day") {
                        Text(day, Modifier.padding(top = if (groupIndex == 0) 0.dp else 32.dp, bottom = 24.dp).semantics { heading() },
                            style = NovaType.Body, color = NovaOnSurfaceVariant)
                    }
                    itemsIndexed(logs, key = { _, log -> log.key }) { index, log ->
                        HistoryRow(log, first = index == 0, last = index == logs.lastIndex) { open(log) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(log: CieLog, first: Boolean, last: Boolean, open: () -> Unit) {
    val look = log.look()
    Row(Modifier.fillMaxWidth().novaCardSegment(first, last).clickable(role = Role.Button, onClick = open).heightIn(min = 88.dp).padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(look.icon), null, Modifier.size(24.dp), tint = look.iconColor)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(log.categoryLabel(), style = NovaType.Supporting, color = look.color)
            Text(log.historyTitle(), style = NovaType.ItemTitle, color = NovaOnSurface)
            log.instant()?.let { Text(it.clockTime(), style = NovaType.Caption, color = NovaOnSurfaceVariant) }
        }
        Icon(painterResource(R.drawable.nova_chevron_forward), null, Modifier.size(20.dp), tint = NovaOnSurfaceVariant)
    }
}

/** Detalhes do evento (Figma 30a–30c): somente os campos retornados pela API. */
@Composable
private fun FireEventScreen(log: CieLog, back: () -> Unit) {
    val look = log.look()
    val instant = log.instant()
    var technical by rememberSaveable(log.key) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar("Detalhes do evento", back)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NovaStatusBadge(log.categoryLabel(), look.color, look.container, height = 24.dp, textStyle = NovaType.NavLabel.copy(letterSpacing = 0.sp))
                    Text(log.detailTitle(), Modifier.semantics { heading() }, style = NovaType.ScreenTitle, color = NovaOnSurface)
                    listOfNotNull(log.loopAddress(), log.zoneNameRaw()).joinToString(" - ").ifEmpty { null }?.let {
                        Text(it, style = NovaType.ScreenTitle, color = NovaOnSurface)
                    }
                    Text(listOfNotNull(instant?.fullTime(), "ID ${log.id}").joinToString(" · "), style = NovaType.Supporting, color = NovaOnSurfaceVariant)
                }
            }
            DetailSection("Ocorrência", listOf("Tipo" to (log.event?.label ?: log.detailTitle()), "ID" to log.id.toString(), "Data e hora" to instant?.fullTime()))
            DetailSection("Localização", listOf("Zona" to log.zoneNameRaw(), "Laço/Endereço" to log.loopAddress(), "Andar" to log.floorLabel()?.replaceFirstChar { it.uppercase() }))
            DetailSection("Dispositivo", listOf("Nome" to log.deviceNameRaw(), "Tipo" to log.deviceType()))
            DetailSection("Estado", listOf("Evento (código)" to log.eventType?.toString(), "Bloqueado" to log.blocked?.let { if (it) "Sim" else "Não" }))
            log.raw?.let { raw ->
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { technical = !technical }.heightIn(min = 52.dp).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Dados técnicos", style = NovaType.BodyEmphasis.copy(lineHeight = 18.sp), color = NovaInk)
                        Text(if (technical) "Ocultar payload bruto" else "Ver payload bruto", style = NovaType.Caption, color = NovaOnSurfaceVariant)
                    }
                    Text(if (technical) "⌄" else "›", fontSize = 20.sp, lineHeight = 20.sp, color = NovaOnSurfaceVariant)
                }
                if (technical) Text(raw, Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp, color = NovaOnSurface)
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, rows: List<Pair<String, String?>>) {
    val visible = rows.filter { it.second != null }
    if (visible.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, Modifier.semantics { heading() }, style = NovaType.SectionTitle, color = NovaInk)
        visible.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.width(128.dp), style = NovaType.Supporting, color = NovaOnSurfaceVariant)
                Text(value!!, Modifier.weight(1f), style = NovaType.Supporting.copy(fontWeight = FontWeight.Medium), color = NovaInk)
            }
        }
    }
}

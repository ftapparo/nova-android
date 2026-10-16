package br.com.novaresidence.feature.fire

import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*
import kotlinx.coroutines.delay

private enum class FirePage { Status, History, Event }

/**
 * Central de incêndio (Figma 26–30c), somente leitura. [fullScreen] oculta o menu inferior no histórico e no detalhe.
 * [notificationsReady]: aparelho registrado e notificações do alarme permitidas; senão a tela 26 oferece [enableNotifications].
 * [showHistory]: a versão 1.0.0 sai para moradores e só a equipe vê o histórico de eventos (decisão de 15/10/2026).
 */
@Composable
fun FireFeature(
    state: FireState, refresh: () -> Unit, loadHistory: () -> Unit, fullScreen: (Boolean) -> Unit = {},
    notificationsReady: Boolean = false, enableNotifications: (() -> Unit)? = null, showHistory: Boolean = true,
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
        else -> FireStatusScreen(state, refresh, if (showHistory) ({ loadHistory(); page = FirePage.History }) else null,
            notificationsReady, enableNotifications) { open(it, FirePage.Status) }
    }
}

private data class StatusLook(
    val image: Int, val grayscale: Boolean, val badge: UiText, val color: Color, val container: Color,
    val title: UiText, val text: UiText, val info: UiText?,
)

@Composable
private fun FireStatusScreen(
    state: FireState, refresh: () -> Unit, openHistory: (() -> Unit)?, notificationsReady: Boolean, enableNotifications: (() -> Unit)?,
    openEvent: (CieLog) -> Unit,
) {
    val status = state.status
    NovaPage(stringResource(R.string.fire_title), background = NovaServiceBackground, barBackground = NovaServiceBackground, titleColor = NovaInk) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // Em telas mais baixas que o frame, a ilustração encolhe antes de empurrar orientações e ações para fora da tela.
            val available = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = NovaSpacing.Margin, end = NovaSpacing.Margin, top = 16.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (status == FireStatus.Alarm) AlarmContent(state, (available - 504.dp).coerceIn(120.dp, 200.dp), openEvent)
                    else StatusContent(state, (available - 420.dp).coerceIn(120.dp, 200.dp), refresh, openHistory, notificationsReady, enableNotifications)
                }
            }
        }
    }
}

@Composable
private fun StatusContent(
    state: FireState, illustration: Dp, refresh: () -> Unit, openHistory: (() -> Unit)?,
    notificationsReady: Boolean, enableNotifications: (() -> Unit)?,
) {
    val status = state.status
    val look = when (status) {
        FireStatus.Normal -> StatusLook(R.drawable.nova_fire_normal, false, uiText(R.string.fire_badge_online), NovaSuccess, NovaSuccessContainer,
            // Só promete aviso com o aparelho registrado e o alarme permitido (AGENTS.md, tela 26); a pessoa pode
            // desligar as notificações a qualquer momento, por isso o estado é reconferido a cada volta ao app.
            uiText(R.string.fire_normal_title), uiText(R.string.fire_normal),
            uiText(if (notificationsReady) R.string.fire_push_ready else R.string.fire_push_off))
        FireStatus.Attention -> StatusLook(R.drawable.nova_fire_attention, false, uiText(R.string.fire_badge_checking), NovaWarning, NovaWarningContainer,
            uiText(R.string.fire_attention_title), uiText(R.string.fire_attention), uiText(R.string.fire_attention_info))
        FireStatus.Offline -> StatusLook(R.drawable.nova_fire_normal, true, uiText(R.string.fire_badge_offline), NovaOffline, NovaOfflineContainer,
            uiText(R.string.fire_offline_title), uiText(R.string.fire_offline), uiText(R.string.fire_offline_info))
        FireStatus.Loading -> StatusLook(R.drawable.nova_fire_normal, true, uiText(R.string.fire_badge_loading), NovaPrimary, NovaPrimarySoft,
            uiText(R.string.fire_loading_title), uiText(R.string.fire_loading), null)
        else -> StatusLook(R.drawable.nova_fire_normal, true, uiText(R.string.fire_badge_unavailable), NovaOffline, NovaOfflineContainer,
            uiText(R.string.fire_unavailable_title), state.error ?: uiText(R.string.fire_unavailable), null)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NovaIllustration(look.image, Modifier.size(illustration), contentScale = ContentScale.Crop,
            colorFilter = if (look.grayscale) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
        NovaStatusBadge(look.badge.text(), look.color, look.container, Modifier.widthIn(min = if (status == FireStatus.Attention) 132.dp else 122.dp))
        NovaStateHeadline(look.title.text(), look.text.text())
        if (status == FireStatus.Attention) state.activeEvents.firstNotNullOfOrNull { it.placeLine() }?.let { place ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.fire_warning_place), style = NovaType.Caption, color = NovaOnSurfaceVariant)
                Text(place, style = NovaType.ItemTitle, color = NovaInk, textAlign = TextAlign.Center)
            }
        }
        look.info?.let { NovaInfoStrip(it.text(), accent = look.color, container = look.container) }
        if (status == FireStatus.Normal && !notificationsReady && enableNotifications != null) NovaTextLink(stringResource(R.string.push_enable), enableNotifications)
        if (status == FireStatus.Unavailable) NovaButton(stringResource(R.string.fire_try_again), refresh, enabled = state.retrySeconds == 0L)
        if (openHistory != null) NovaButton(stringResource(R.string.fire_view_history), openHistory, style = NovaButtonStyle.Outlined)
        Footnotes(state)
    }
}

@Composable
private fun Footnotes(state: FireState) {
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(15_000); value = System.currentTimeMillis() } }
    state.panel?.lastUpdated?.let { updated ->
        Text(if (now - updated < 60_000) stringResource(R.string.fire_updated_now) else stringResource(R.string.fire_updated_at, updated.clockTime()),
            style = NovaType.Caption, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
    }
    if (state.error != null && state.status != FireStatus.Unavailable) Text(state.error.text(), style = NovaType.Caption, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
    if (state.retrySeconds > 0) Text(stringResource(R.string.fire_next_check_in, state.retrySeconds), style = NovaType.Caption, color = NovaOnSurfaceVariant)
}

private val SafetyGuidance = listOf(
    R.string.fire_safety_1 to false,
    R.string.fire_safety_2 to true,
    R.string.fire_safety_3 to false,
    R.string.fire_safety_4 to false,
    R.string.fire_safety_5 to false,
)

/** Alarme ativo (Figma 28): prevalece sobre qualquer outro estado e permanece enquanto a central indicar alarme. */
@Composable
private fun AlarmContent(state: FireState, illustration: Dp, openEvent: (CieLog) -> Unit) {
    val log = state.activeAlarm
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Assertive },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NovaIllustration(R.drawable.nova_fire_alarm, Modifier.size(illustration), contentScale = ContentScale.Crop)
            NovaStatusBadge(stringResource(R.string.fire_badge_alarm), NovaAlarm, NovaAlarmContainer, Modifier.widthIn(min = 112.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.fire_alarm_title), Modifier.fillMaxWidth().semantics { heading() }, style = NovaType.AlarmTitle,
                    color = NovaAlarm, textAlign = TextAlign.Center)
                log?.placeLine(" - ")?.let { Text(it, Modifier.fillMaxWidth(), style = NovaType.AlarmPlace, color = NovaInk, textAlign = TextAlign.Center) }
                log?.instant()?.let { Text(stringResource(R.string.fire_alarm_started_at, it.clockTime()), style = NovaType.Supporting, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center) }
            }
        }
        NovaDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.fire_safety_title), Modifier.semantics { heading() }, style = NovaType.Highlight, color = NovaInk)
            SafetyGuidance.forEach { (text, emphasis) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 38.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(NovaAlarm, NovaShapes.Pill))
                    Text(stringResource(text), style = if (emphasis) NovaType.BodyEmphasis else NovaType.Body, color = NovaInk)
                }
            }
        }
        if (log != null) NovaButton(stringResource(R.string.fire_view_event), { openEvent(log) }, style = NovaButtonStyle.Outlined)
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
    R.string.fire_filter_all to null,
    R.string.fire_filter_alarms to setOf("alarme"),
    R.string.fire_filter_failures to setOf("falha", "supervisao"),
    R.string.fire_filter_operation to setOf("operacao", "bloqueio"),
)

@Composable
private fun FireHistoryScreen(state: FireState, back: () -> Unit, reload: () -> Unit, open: (CieLog) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val types = HistoryFilters[filter].second
    // Até 200 eventos: agrupar só quando o histórico ou o filtro mudam, não a cada recomposição.
    val groups = remember(state.history, filter) {
        state.history.orEmpty().filter { types == null || it.category() in types }.groupBy { it.instant()?.dayLabel() ?: uiText(R.string.date_none) }.toList()
    }
    NovaPage(stringResource(R.string.fire_history_title), back = back) {
        NovaFilterBar(HistoryFilters.map { stringResource(it.first) }, filter) { filter = it }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = NovaSpacing.Margin, end = NovaSpacing.Margin, top = 16.dp, bottom = 24.dp)) {
            when {
                state.historyLoading && state.history == null -> item { NovaLoading(stringResource(R.string.fire_history_loading)) }
                state.historyError != null && state.history == null -> item {
                    NovaRetryNotice(state.historyError.text(), stringResource(R.string.fire_try_again), reload)
                }
                groups.isEmpty() -> item {
                    Text(stringResource(if (filter == 0) R.string.fire_history_empty else R.string.fire_history_empty_filter),
                        style = NovaType.Body, color = NovaOnSurfaceVariant)
                }
                // Cada evento é um item preguiçoso: um dia pode ter dezenas de eventos, e montá-los de uma vez travava a tela.
                else -> groups.forEachIndexed { groupIndex, (day, logs) ->
                    item(key = "day:$day") { NovaDayHeader(day.text(), first = groupIndex == 0) }
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
    NovaGroupRow(look.icon, look.iconColor, log.categoryLabel().text(), look.color, log.historyTitle().text(), log.instant()?.clockTime(), first, last, open)
}

/** Detalhes do evento (Figma 30a–30c): somente os campos retornados pela API. */
@Composable
private fun FireEventScreen(log: CieLog, back: () -> Unit) {
    val look = log.look()
    val instant = log.instant()
    var technical by rememberSaveable(log.key) { mutableStateOf(false) }
    NovaPage(stringResource(R.string.fire_event_title), back = back) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = NovaSpacing.Margin, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NovaStatusBadge(log.categoryLabel().text(), look.color, look.container, height = 24.dp, textStyle = NovaType.BadgeCompact)
                    Text(log.detailTitle().text(), Modifier.semantics { heading() }, style = NovaType.ScreenTitle, color = NovaOnSurface)
                    listOfNotNull(log.loopAddress(), log.zoneNameRaw()).joinToString(" - ").ifEmpty { null }?.let {
                        Text(it, style = NovaType.ScreenTitle, color = NovaOnSurface)
                    }
                    Text(listOfNotNull(instant?.fullTime(), stringResource(R.string.fire_event_id, log.id)).joinToString(" · "), style = NovaType.Supporting, color = NovaOnSurfaceVariant)
                }
            }
            DetailSection(stringResource(R.string.fire_section_occurrence), listOf(stringResource(R.string.fire_field_type) to (log.event?.label ?: log.detailTitle().text()), stringResource(R.string.fire_field_id) to log.id.toString(), stringResource(R.string.fire_field_datetime) to instant?.fullTime()))
            DetailSection(stringResource(R.string.fire_section_location), listOf(stringResource(R.string.fire_field_zone) to log.zoneNameRaw(), stringResource(R.string.fire_field_loop_address) to log.loopAddress(), stringResource(R.string.fire_field_floor) to log.floorLabel()?.replaceFirstChar { it.uppercase() }))
            DetailSection(stringResource(R.string.fire_section_device), listOf(stringResource(R.string.fire_field_name) to log.deviceNameRaw(), stringResource(R.string.fire_field_type) to log.deviceType()))
            DetailSection(stringResource(R.string.fire_section_state), listOf(stringResource(R.string.fire_field_event_code) to log.eventType?.toString(), stringResource(R.string.fire_field_blocked) to log.blocked?.let { stringResource(if (it) R.string.common_yes else R.string.common_no) }))
            log.raw?.let { raw ->
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { technical = !technical }.heightIn(min = 52.dp).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.fire_technical_data), style = NovaType.DisclosureTitle, color = NovaInk)
                        Text(stringResource(if (technical) R.string.fire_hide_raw else R.string.fire_show_raw), style = NovaType.Caption, color = NovaOnSurfaceVariant)
                    }
                    Text(if (technical) "⌄" else "›", style = NovaType.Glyph, color = NovaOnSurfaceVariant)
                }
                if (technical) Text(raw, Modifier.fillMaxWidth().background(NovaSurface).padding(12.dp), style = NovaType.Code, color = NovaOnSurface)
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
                Text(value!!, Modifier.weight(1f), style = NovaType.SupportingEmphasis, color = NovaInk)
            }
        }
    }
}

package br.com.novaresidence.feature.message

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*

private val PageIndicator = Color(0xFFC7C9D0)

private data class MessageLook(val icon: Int, val color: Color, val container: Color)

// Ícones e cores por categoria (Figma 17); o amarelo segue o tom acessível do app (DESIGN_GUIDE §4).
private fun CondoMessage.look(): MessageLook = when (category) {
    "manutencao" -> MessageLook(R.drawable.nova_event_operation, NovaPrimary, NovaPrimarySoft)
    "aviso" -> MessageLook(R.drawable.nova_event_warning, NovaWarning, NovaWarningContainer)
    "urgente" -> MessageLook(R.drawable.nova_event_alarm, NovaError, NovaAlarmContainer)
    else -> MessageLook(R.drawable.nova_message_notice, NovaPrimary, NovaPrimarySoft)
}

@Composable
private fun MessageBadge(message: CondoMessage, dot: Boolean) {
    val look = message.look()
    Row(Modifier.background(look.container, CircleShape).heightIn(min = 24.dp).padding(horizontal = 9.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot) Box(Modifier.size(8.dp).background(look.color, CircleShape))
        Text(message.badgeLabel(), style = NovaType.NavLabel.copy(letterSpacing = 0.sp), color = look.color, maxLines = 1)
    }
}

/**
 * "Informativos do condomínio" na home (Figma 08d): carrossel de cartões de 356 dp com "Ver mais"
 * e indicador de página. Sem comunicados, mostra o cartão vazio do 08a.
 */
@Composable
fun MessageCarousel(state: MessageState, open: (CondoMessage) -> Unit) {
    val messages = state.latest
    if (messages.isEmpty()) {
        Box(Modifier.fillMaxWidth().border(1.dp, NovaOutline, RoundedCornerShape(16.dp)).heightIn(min = 68.dp).padding(16.dp), contentAlignment = Alignment.CenterStart) {
            Text(state.latestError?.let { "Não foi possível carregar os informativos." } ?: "Nenhuma mensagem nova",
                style = NovaType.Body, color = NovaOnSurfaceVariant)
        }
        return
    }
    val pager = rememberPagerState { messages.size }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // O próximo cartão aparece até a borda da folha; o primeiro continua alinhado à margem de 28 dp.
            HorizontalPager(pager, Modifier.wrapContentWidth(Alignment.Start, unbounded = true).width(maxWidth + 28.dp),
                pageSize = PageSize.Fixed(356.dp), pageSpacing = 12.dp, verticalAlignment = Alignment.Top) { page ->
                val message = messages[page]
                val shape = RoundedCornerShape(16.dp)
                Column(Modifier.height(208.dp).fillMaxWidth().clip(shape).background(Color.White).border(1.dp, NovaOutline, shape)
                    .clickable(role = Role.Button, onClickLabel = "Ver mais") { open(message) }
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MessageBadge(message, dot = true)
                    Text(message.title, style = NovaType.ItemTitle, color = NovaOnSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(message.body, Modifier.weight(1f), style = NovaType.Body, color = NovaOnSurface, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                        Text("Ver mais", style = NovaType.Button, color = NovaPrimary)
                        Icon(painterResource(R.drawable.nova_chevron_forward), null, Modifier.size(18.dp), tint = NovaPrimary)
                    }
                }
            }
        }
        if (messages.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(messages.size) { index ->
                val current = index == pager.currentPage
                Box(Modifier.size(if (current) 8.dp else 6.dp).background(if (current) NovaPrimary else PageIndicator, CircleShape))
            }
        }
    }
}

/**
 * Mensagens (Figma 17) e detalhe (18). [initialMessage] abre direto o detalhe (vindo do carrossel da
 * home); nesse caso, voltar do detalhe sai da tela.
 */
@Composable
fun MessagesFeature(state: MessageState, initialMessage: Long?, back: () -> Unit, loadList: (Boolean) -> Unit, markRead: (CondoMessage) -> Unit) {
    var openId by rememberSaveable { mutableStateOf(initialMessage) }
    val fromHome = remember { initialMessage != null }
    LaunchedEffect(Unit) { loadList(state.unreadOnly) }
    val all = (state.list.orEmpty() + state.latest)
    val open = all.firstOrNull { it.id == openId }
    BackHandler(openId != null) { if (fromHome) back() else openId = null }
    if (openId != null && open != null) {
        LaunchedEffect(open.id) { markRead(open) }
        MessageDetail(open) { if (fromHome) back() else openId = null }
    } else MessageList(state, back, loadList) { openId = it.id }
}

@Composable
private fun MessageList(state: MessageState, back: () -> Unit, loadList: (Boolean) -> Unit, open: (CondoMessage) -> Unit) {
    val groups = remember(state.list) {
        state.list.orEmpty().groupBy { it.publishedInstant?.dayLabel() ?: "Sem data" }.toList()
    }
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar("Mensagens", back)
        NovaFilterBar(listOf("Todas", "Não lidas"), if (state.unreadOnly) 1 else 0, height = 36.dp) { loadList(it == 1) }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 24.dp)) {
            when {
                state.listLoading && state.list == null -> item { NovaLoading("Carregando mensagens…") }
                state.listError != null && state.list == null -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NovaNotice(state.listError)
                        NovaButton("Tentar novamente", { loadList(state.unreadOnly) }, style = NovaButtonStyle.Secondary)
                    }
                }
                groups.isEmpty() -> item {
                    Text(if (state.unreadOnly) "Nenhuma mensagem não lida." else "Nenhuma mensagem no momento.", style = NovaType.Body, color = NovaOnSurfaceVariant)
                }
                // Cada mensagem é um item preguiçoso, como no histórico da central.
                else -> groups.forEachIndexed { groupIndex, (day, messages) ->
                    item(key = "day:$day") {
                        Text(day, Modifier.padding(top = if (groupIndex == 0) 0.dp else 32.dp, bottom = 24.dp).semantics { heading() },
                            style = NovaType.Body, color = NovaOnSurfaceVariant)
                    }
                    itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                        MessageRow(message, first = index == 0, last = index == messages.lastIndex) { open(message) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: CondoMessage, first: Boolean, last: Boolean, open: () -> Unit) {
    val look = message.look()
    Row(Modifier.fillMaxWidth().novaCardSegment(first, last).clickable(role = Role.Button, onClick = open).heightIn(min = 88.dp)
        .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(look.icon), null, Modifier.size(24.dp), tint = look.color)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message.categoryLabel(), style = NovaType.Supporting, color = look.color)
            Text(message.title, style = NovaType.ItemTitle, color = NovaOnSurface)
            message.publishedInstant?.let { Text(it.clockTime(), style = NovaType.Caption, color = NovaOnSurfaceVariant) }
        }
        Icon(painterResource(R.drawable.nova_chevron_forward), null, Modifier.size(20.dp), tint = NovaOnSurfaceVariant)
    }
}

@Composable
private fun MessageDetail(message: CondoMessage, back: () -> Unit) {
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar("Mensagem", back)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)) {
            NovaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MessageBadge(message, dot = false)
                    Text(message.title, Modifier.semantics { heading() }, style = NovaType.ScreenTitle, color = NovaOnSurface)
                    message.publishedInstant?.let { Text(it.dayAndTime(), style = NovaType.Supporting, color = NovaOnSurfaceVariant) }
                }
            }
            Text(message.body, style = NovaType.Body.copy(lineHeight = 21.sp), color = NovaOnSurface)
        }
    }
}

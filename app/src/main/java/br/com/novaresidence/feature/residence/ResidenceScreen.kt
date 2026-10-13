package br.com.novaresidence.feature.residence

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import br.com.novaresidence.R
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.ui.*

enum class ServiceTone { Neutral, Running, Alarm }

/** Estado de um serviço na home (Figma 08a–08c): [highlight] é a linha em destaque, [text] a de apoio. */
data class ServiceStatus(val text: String, val highlight: String? = null, val tone: ServiceTone = ServiceTone.Neutral)

/** Conteúdo de uma aba de serviço; [fullScreen] oculta o menu inferior (histórico e detalhe da central). */
typealias ServiceContent = @Composable (unit: ResidenceUnit?, selectUnit: () -> Unit, fullScreen: (Boolean) -> Unit) -> Unit

internal enum class ResidencePage { Home, Fire, Exhaust, Messages, Settings, SelectUnit, SelectLanguage, Privacy, Terms, PrivacyPolicy, PersonalData, DeleteAccount }

private val Tabs = setOf(ResidencePage.Home, ResidencePage.Fire, ResidencePage.Exhaust)

// Configurações e seus filhos usam conteúdo branco sob a barra cinza (Figma 09–16).
private val WhitePages = setOf(ResidencePage.Settings, ResidencePage.SelectUnit, ResidencePage.SelectLanguage, ResidencePage.Privacy,
    ResidencePage.Terms, ResidencePage.PrivacyPolicy, ResidencePage.PersonalData, ResidencePage.DeleteAccount)

private val PageStackSaver = listSaver<List<ResidencePage>, String>({ it.map(ResidencePage::name) }, { it.map(ResidencePage::valueOf) })

@Composable
fun ResidenceScreen(
    state: ResidenceState, user: AuthUser, select: (Int) -> Unit, refresh: () -> Unit,
    logout: () -> Unit, busy: Boolean, saveLanguage: () -> Unit = {},
    exhaustStatus: ServiceStatus? = null, fireStatus: ServiceStatus? = null,
    unreadMessages: Int = 0,
    /** Carrossel de comunicados da home (08d); recebe a ação de abrir um comunicado. */
    homeMessages: (@Composable (open: (Long) -> Unit) -> Unit)? = null,
    /** Mensagens (17/18); [initialMessage] abre direto o detalhe. */
    messagesContent: (@Composable (initialMessage: Long?, back: () -> Unit) -> Unit)? = null,
    fireContent: ServiceContent? = null,
    /** Termo de Uso ([terms] = true) ou Política de Privacidade (13 e 14), publicados pela API. */
    legalContent: (@Composable (terms: Boolean, back: () -> Unit) -> Unit)? = null,
    /** Excluir conta (16); [deleteError] é a falha da última tentativa. */
    deleteAccount: () -> Unit = {}, deleteError: String? = null,
    exhaustContent: ServiceContent? = null,
) {
    // Abas reiniciam a pilha; as demais telas empilham sobre a atual e voltam na ordem inversa.
    var stack by rememberSaveable(stateSaver = PageStackSaver) { mutableStateOf(listOf(ResidencePage.Home)) }
    val page = stack.last()
    val navigate: (ResidencePage) -> Unit = { next -> stack = if (next in Tabs) listOf(next) else stack + next }
    val back: () -> Unit = { stack = if (stack.size > 1) stack.dropLast(1) else listOf(ResidencePage.Home) }
    BackHandler(page != ResidencePage.Home, back)
    val view = LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = page != ResidencePage.Home
        }
    }
    val residence = state.residence
    val unit = residence?.units?.firstOrNull { it.sequencia == state.selectedUnit }
    val services = residence?.canSeeServices(user.role) == true
    val canOpenFire = services && fireContent != null
    val canOpenExhaust = services && exhaustContent != null
    val selectUnit = { navigate(ResidencePage.SelectUnit) }
    var fullScreen by remember(page) { mutableStateOf(false) }
    var pendingMessage by rememberSaveable { mutableStateOf<Long?>(null) }
    val openMessages: (Long?) -> Unit = { pendingMessage = it; navigate(ResidencePage.Messages) }
    val setFullScreen: (Boolean) -> Unit = { fullScreen = it }
    val navVisible = page in Tabs && !fullScreen
    val top = when {
        page == ResidencePage.Home -> NovaHeader
        page == ResidencePage.Fire && !fullScreen -> NovaServiceBackground
        else -> NovaBackground
    }
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        Spacer(Modifier.fillMaxWidth().background(top).windowInsetsTopHeight(WindowInsets.statusBars))
        Box(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))) {
            when (page) {
                ResidencePage.Home -> Home(state, user, unit, services, canOpenFire, canOpenExhaust, fireStatus, exhaustStatus, refresh, navigate,
                    unreadMessages, homeMessages?.let { content -> { content { id -> openMessages(id) } } }) { openMessages(null) }
                ResidencePage.Fire -> fireContent?.invoke(unit, selectUnit, setFullScreen)
                ResidencePage.Exhaust -> exhaustContent?.invoke(unit, selectUnit, setFullScreen)
                ResidencePage.Messages -> messagesContent?.invoke(pendingMessage, back)
                ResidencePage.Terms, ResidencePage.PrivacyPolicy -> legalContent?.invoke(page == ResidencePage.Terms, back)
                else -> ResidenceSettings(page, state, user, navigate, back, select, logout, busy, saveLanguage, deleteAccount, deleteError)
            }
        }
        if (navVisible) NavigationBarFigma {
            NavItem(page == ResidencePage.Home, "Início", R.drawable.nova_home, R.drawable.nova_home_selected) { navigate(ResidencePage.Home) }
            NavItem(page == ResidencePage.Fire, "Alarme", R.drawable.nova_alarm_nav, R.drawable.nova_alarm_selected, enabled = canOpenFire) { navigate(ResidencePage.Fire) }
            NavItem(page == ResidencePage.Exhaust, "Exaustor", R.drawable.nova_exhaust_nav, R.drawable.nova_exhaust_selected, enabled = canOpenExhaust) { navigate(ResidencePage.Exhaust) }
        }
        Spacer(Modifier.fillMaxWidth().background(if (navVisible || page in WhitePages) Color.White else NovaBackground)
            .windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/**
 * Menu inferior (componente "Navigation bar" do Figma, 157:1183): 80 dp, fundo branco, borda superior
 * de 1 dp e sombra projetada para cima (0 −2 8, rgba(64,52,80,.1)) desenhada fora dos limites da barra.
 */
@Composable
private fun NavigationBarFigma(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth()
        .drawBehind {
            val shadow = 8.dp.toPx()
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x1A403450)), startY = -shadow, endY = 0f),
                topLeft = Offset(0f, -shadow), size = Size(size.width, shadow))
        }
        .background(Color.White)
        .drawBehind { drawRect(NovaOutline, size = Size(size.width, 1.dp.toPx())) }
        .height(80.dp).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically, content = content)
}

/**
 * Botão do menu (componentes "… Menu Button", Figma de 13/10/2026): normal = ícone de contorno 24 dp cinza
 * + rótulo 11 sp; selecionado = ícone preenchido roxo de 32 dp, sem rótulo visível (vira descrição de acessibilidade).
 */
@Composable
private fun NavItem(selected: Boolean, label: String, icon: Int, selectedIcon: Int, enabled: Boolean = true, click: () -> Unit) {
    Box(Modifier.width(96.dp).height(60.dp).clickable(enabled = enabled, role = Role.Tab, onClick = click)
        .semantics(mergeDescendants = true) { this.selected = selected; if (selected) contentDescription = label },
        contentAlignment = Alignment.Center) {
        // O vetor de cada ícone preenchido já tem o tamanho do Figma (prédio 28×32, chama e ventilador 32×32).
        if (selected) Icon(painterResource(selectedIcon), null, Modifier.padding(bottom = 4.dp), tint = NovaPrimary)
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val color = if (enabled) NovaOnSurfaceVariant else NovaDisabledContent
            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = color)
            Text(label, style = NovaType.NavLabel, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Home(
    state: ResidenceState, user: AuthUser, unit: ResidenceUnit?, services: Boolean, canOpenFire: Boolean, canOpenExhaust: Boolean,
    fireStatus: ServiceStatus?, exhaustStatus: ServiceStatus?, refresh: () -> Unit, navigate: (ResidencePage) -> Unit,
    unreadMessages: Int, messages: (@Composable () -> Unit)?, openMessages: () -> Unit,
) {
    val residence = state.residence
    Column(Modifier.fillMaxSize().background(NovaHeader)) {
        // Figma 08a (revisão do cabeçalho): pílula a 24 dp do topo, "Olá" 16 dp abaixo dela e folha a 132 dp.
        // Os botões redondos têm alvo de 48 dp; os espaços descontam essa diferença para manter as posições.
        Column(Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f, fill = false).padding(end = 16.dp).widthIn(min = 194.dp).height(40.dp)
                    .clip(CircleShape).background(NovaHeaderControl)
                    .clickable(enabled = !residence?.units.isNullOrEmpty(), role = Role.Button, onClickLabel = "Selecionar unidade") { navigate(ResidencePage.SelectUnit) }
                    .padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.nova_units), null, Modifier.size(24.dp), tint = Color.White)
                    Text(unit?.homeLabel() ?: "Nenhuma unidade", style = NovaType.BodyEmphasis, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.offset(x = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderButton(R.drawable.nova_bell, if (unreadMessages > 0) "Mensagens, $unreadMessages não lidas" else "Mensagens", 42,
                        badge = unreadMessages, click = openMessages)
                    HeaderButton(R.drawable.nova_profile_nav, "Configurações", 40) { navigate(ResidencePage.Settings) }
                }
            }
            val firstName = residence?.person?.nome?.takeIf { it.isNotBlank() }?.personName()?.substringBefore(' ')
            Text(if (firstName != null) "Olá, $firstName!" else "Olá!", Modifier.semantics { heading() }, style = NovaType.HeadlineHome, color = Color.White)
        }
        Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(NovaBackground)
            .verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 16.dp)) {
            val notices = listOfNotNull(state.error, residence?.notice(), unit?.blockedMessage())
            if (notices.isNotEmpty() || residence?.status != "ativo") Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.error?.let { NovaNotice(it, state.requestId) }
                residence?.notice()?.let { NovaNotice(it) }
                unit?.blockedMessage()?.let { NovaNotice(it) }
                if (state.error != null || residence?.status != "ativo") NovaButton(
                    if (state.retrySeconds > 0) "Tente novamente em ${state.retrySeconds}s" else "Atualizar cadastro", refresh,
                    enabled = !state.loading && state.retrySeconds == 0L, style = NovaButtonStyle.Secondary,
                )
            }
            if (services) Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Serviços do condomínio", Modifier.semantics { heading() }, style = NovaType.SectionTitle, color = NovaOnSurface)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceCard("Central de Incêndio", R.drawable.nova_alarm_nav, fireStatus ?: ServiceStatus("Consultando a central…"), canOpenFire) { navigate(ResidencePage.Fire) }
                    ServiceCard("Exaustor", R.drawable.nova_exhaust_nav, exhaustStatus ?: ServiceStatus("Consultando equipamento…"), canOpenExhaust) { navigate(ResidencePage.Exhaust) }
                }
            }
            // Comunicados (Figma 08a/08d): o carrossel vem da feature de mensagens; sem acesso a ela, o cartão vazio.
            Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Informativos do condomínio", Modifier.semantics { heading() }, style = NovaType.SectionTitle, color = NovaOnSurface)
                if (messages != null && services) messages()
                else Box(Modifier.fillMaxWidth().border(1.dp, NovaOutline, RoundedCornerShape(16.dp)).heightIn(min = 68.dp).padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Nenhuma mensagem nova", style = NovaType.Body, color = NovaOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(icon: Int, description: String, size: Int, badge: Int = 0, click: () -> Unit) {
    // O círculo visível tem 40–42 dp (Figma); o alvo de toque chega a 48 dp.
    Box(Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = description, onClick = click)
        .semantics(mergeDescendants = true) { contentDescription = description }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(size.dp).background(NovaHeaderControl, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = Color.White)
        }
        // Contador de não lidas (Figma 08d): círculo vermelho de 13 dp sobre o sino.
        if (badge > 0) Box(Modifier.align(Alignment.TopStart).offset(x = (48 - size).dp / 2 + 23.dp, y = (48 - size).dp / 2 + 8.dp)
            .defaultMinSize(13.dp, 13.dp).background(NovaError, CircleShape).padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
            Text(if (badge > 9) "9+" else badge.toString(), fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun ServiceCard(title: String, icon: Int, status: ServiceStatus, enabled: Boolean, open: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val (container, tint) = when (status.tone) {
        ServiceTone.Alarm -> NovaError to Color.White
        ServiceTone.Running -> Color.Transparent to NovaSuccess
        ServiceTone.Neutral -> Color.Transparent to NovaPrimary
    }
    Row(Modifier.fillMaxWidth().clip(shape).border(1.dp, NovaOutline, shape).clickable(enabled = enabled, role = Role.Button, onClick = open)
        .heightIn(min = 76.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(container, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = tint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = NovaType.ItemTitle, color = NovaOnSurface)
            status.highlight?.let { Text(it, style = NovaType.Highlight.copy(lineHeight = NovaType.ItemTitle.lineHeight), color = if (status.tone == ServiceTone.Running) NovaSuccess else NovaOnSurface) }
            Text(status.text, style = if (status.highlight != null) NovaType.Body else NovaType.Supporting, color = NovaOnSurfaceVariant)
        }
        if (enabled) Icon(painterResource(R.drawable.nova_chevron_forward), null, Modifier.size(20.dp), tint = Color(0xFF1D1B20))
    }
}

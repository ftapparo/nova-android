package br.com.novaresidence.feature.residence

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import br.com.novaresidence.R
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.ui.*

private val HomeUnitContext = Color(0xBF3C075B)
private val ServiceIconContainer = Color(0xFFEADDFF)
private val MessagesBar = Color(0xFFF1F1F8)

enum class ServiceTone { Neutral, Running, Alarm }

/** Estado de um serviço na home (Figma 08a–08c): [highlight] é a linha em destaque, [text] a de apoio. */
data class ServiceStatus(val text: String, val highlight: String? = null, val tone: ServiceTone = ServiceTone.Neutral)

/** Conteúdo de um serviço; [fullScreen] oculta a navegação inferior (telas 12 e 13 do Figma). */
typealias ServiceContent = @Composable (unit: ResidenceUnit?, back: () -> Unit, selectUnit: () -> Unit, fullScreen: (Boolean) -> Unit) -> Unit

@Composable
fun ResidenceScreen(
    state: ResidenceState, user: AuthUser, select: (Int) -> Unit, refresh: () -> Unit,
    logout: () -> Unit, busy: Boolean, saveLanguage: () -> Unit = {},
    exhaustStatus: ServiceStatus? = null, fireStatus: ServiceStatus? = null,
    fireContent: ServiceContent? = null,
    exhaustContent: ServiceContent? = null,
) {
    var page by rememberSaveable { mutableStateOf(ResidencePage.Home) }
    var origin by rememberSaveable { mutableStateOf(ResidencePage.Home) }
    val back: () -> Unit = {
        page = when (page) {
            ResidencePage.SelectUnit -> origin
            ResidencePage.SelectLanguage -> ResidencePage.Profile
            else -> ResidencePage.Home
        }
    }
    val navigate: (ResidencePage) -> Unit = { next ->
        if (next == ResidencePage.SelectUnit) origin = page
        page = next
    }
    val view = LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = page != ResidencePage.Home
        }
    }
    BackHandler(page != ResidencePage.Home, back)
    val residence = state.residence
    val unit = residence?.units?.firstOrNull { it.sequencia == state.selectedUnit }
    val services = residence?.canSeeServices(user.role) == true
    val canOpenFire = services && fireContent != null
    val canOpenExhaust = services && exhaustContent != null
    val selectUnit = { navigate(ResidencePage.SelectUnit) }
    var fullScreen by remember(page) { mutableStateOf(false) }
    val setFullScreen: (Boolean) -> Unit = { fullScreen = it }
    Column(Modifier.fillMaxSize().background(if (page == ResidencePage.Home) NovaHeader else NovaBackground).systemBarsPadding()) {
        Box(Modifier.weight(1f).background(NovaBackground)) {
            when (page) {
                ResidencePage.Fire -> fireContent?.invoke(unit, back, selectUnit, setFullScreen)
                ResidencePage.Exhaust -> exhaustContent?.invoke(unit, back, selectUnit, setFullScreen)
                ResidencePage.Home -> Home(state, unit, services, canOpenFire, canOpenExhaust, fireStatus, exhaustStatus, refresh, navigate)
                else -> ResidenceSettings(page, state, user, navigate, back, select, logout, busy, saveLanguage)
            }
        }
        // Telas de seleção (22, 23) e o histórico da central (12, 13) não têm navegação inferior no Figma.
        if (!fullScreen && page !in setOf(ResidencePage.SelectUnit, ResidencePage.SelectLanguage)) NavigationBarFigma {
            NavItem(page == ResidencePage.Home, "Início", R.drawable.nova_home) { navigate(ResidencePage.Home) }
            NavItem(page == ResidencePage.Fire, "Alarme", R.drawable.nova_alarm_nav, enabled = canOpenFire) { navigate(ResidencePage.Fire) }
            NavItem(page == ResidencePage.Exhaust, "Exaustores", R.drawable.nova_exhaust_nav, enabled = canOpenExhaust) { navigate(ResidencePage.Exhaust) }
            NavItem(page in setOf(ResidencePage.Profile, ResidencePage.SelectLanguage) || (page == ResidencePage.SelectUnit && origin == ResidencePage.Profile),
                "Perfil", R.drawable.nova_profile_nav) { navigate(ResidencePage.Profile) }
        }
    }
}

/**
 * Menu inferior (componente "Navigation bar" do Figma, 157:1183): 72 dp, fundo branco, borda superior
 * de 1 dp e sombra projetada para cima (0 −2 8, rgba(64,52,80,.1)) desenhada fora dos limites da barra.
 */
@Composable
private fun NavigationBarFigma(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth()
        .drawBehind {
            val shadow = 8.dp.toPx()
            drawRect(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0x1A403450)), startY = -shadow, endY = 0f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, -shadow), size = androidx.compose.ui.geometry.Size(size.width, shadow),
            )
        }
        .background(Color.White)
        .drawBehind { drawRect(NovaOutline, size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())) }
        .height(72.dp).padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top, content = content)
}

/**
 * Botão do menu (componentes "… Menu Button"): normal = ícone 24 dp cinza em área 64×32 + rótulo 11 sp;
 * selecionado = quadrado roxo 40 dp (raio 15) com o ícone em branco e sem rótulo visível.
 */
@Composable
private fun RowScope.NavItem(selected: Boolean, label: String, icon: Int, enabled: Boolean = true, click: () -> Unit) {
    Box(Modifier.weight(1f).height(60.dp).clickable(enabled = enabled, role = Role.Tab, onClick = click)
        .semantics(mergeDescendants = true) { this.selected = selected; if (selected) contentDescription = label },
        contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.size(40.dp).background(NovaPrimary, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = Color.White)
        } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val color = if (enabled) NovaOnSurfaceVariant else NovaDisabledContent
            Box(Modifier.size(64.dp, 32.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(24.dp), tint = color)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Home(
    state: ResidenceState, unit: ResidenceUnit?, services: Boolean, canOpenFire: Boolean, canOpenExhaust: Boolean,
    fireStatus: ServiceStatus?, exhaustStatus: ServiceStatus?, refresh: () -> Unit, navigate: (ResidencePage) -> Unit,
) {
    val residence = state.residence
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(NovaHeader).padding(horizontal = 28.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(residence?.person?.nome?.takeIf { it.isNotBlank() }?.let { "Olá, ${it.personName().substringBefore(' ')}" } ?: "Olá",
                color = Color.White, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
            Box(Modifier.padding(bottom = 8.dp).size(43.dp, 2.dp).background(NovaAccent))
            Row(
                Modifier.fillMaxWidth().background(HomeUnitContext, RoundedCornerShape(12.dp))
                    .clickable(enabled = !residence?.units.isNullOrEmpty()) { navigate(ResidencePage.SelectUnit) }
                    .heightIn(min = 54.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.nova_units), null, Modifier.size(24.dp), tint = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Unidade ativa", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text(unit?.homeLabel() ?: "Nenhuma unidade ativa", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.error?.let { NovaNotice(it, state.requestId) }
            residence?.notice()?.let { NovaNotice(it) }
            unit?.blockedMessage()?.let { NovaNotice(it) }
            if (state.error != null || residence?.status != "ativo") NovaButton(
                if (state.retrySeconds > 0) "Tente novamente em ${state.retrySeconds}s" else "Atualizar cadastro", refresh,
                enabled = !state.loading && state.retrySeconds == 0L, secondary = true,
            )
            if (services) {
                ServiceRow("Central de Incêndio", R.drawable.nova_alarm_nav, fireStatus ?: ServiceStatus("Consultando a central…"), canOpenFire) { navigate(ResidencePage.Fire) }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = NovaOutline)
                ServiceRow("Exaustor", R.drawable.nova_exhaust_nav, exhaustStatus ?: ServiceStatus("Consultando equipamento…"), canOpenExhaust) { navigate(ResidencePage.Exhaust) }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = NovaOutline)
            }
        }
        // Comunicados (Figma 08d/08e) ainda não têm rota na API: só o estado vazio é exibido.
        Column(Modifier.fillMaxWidth().background(MessagesBar).heightIn(min = 48.dp).padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            Icon(painterResource(R.drawable.nova_chevron_up), null, Modifier.size(10.dp, 7.dp), tint = Color(0xFF49454F))
            Text("Nenhuma mensagem nova", style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp), color = NovaOnSurfaceVariant)
        }
    }
}

@Composable
private fun ServiceRow(title: String, icon: Int, status: ServiceStatus, enabled: Boolean, open: () -> Unit) {
    val (container, tint) = when (status.tone) {
        ServiceTone.Alarm -> NovaError to Color.White
        ServiceTone.Running -> NovaSuccessContainer to NovaSuccess
        ServiceTone.Neutral -> ServiceIconContainer to NovaPrimary
    }
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = open).heightIn(min = 48.dp).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(container, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = tint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium, color = NovaOnSurface)
            status.highlight?.let {
                Text(it, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium,
                    color = if (status.tone == ServiceTone.Running) NovaSuccess else NovaOnSurface)
            }
            Text(status.text, fontSize = 12.sp, lineHeight = 16.sp, color = NovaOnSurfaceVariant)
        }
        if (enabled) Icon(painterResource(R.drawable.nova_chevron_forward), null, Modifier.size(20.dp), tint = Color(0xFF1D1B20))
    }
}

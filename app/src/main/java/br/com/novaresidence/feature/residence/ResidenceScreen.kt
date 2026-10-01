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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import br.com.novaresidence.R
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.ui.*

private val HomeUnitContext = Color(0xBF3C075B)

/** Resumo do exaustor na home; [running] destaca em verde o equipamento ligado (Figma 08b). */
data class ExhaustHomeStatus(val summary: String, val running: Boolean)

@Composable
fun ResidenceScreen(
    state: ResidenceState, user: AuthUser, select: (Int) -> Unit, refresh: () -> Unit,
    logout: () -> Unit, busy: Boolean, saveLanguage: () -> Unit = {},
    exhaustStatus: ExhaustHomeStatus? = null,
    exhaustContent: (@Composable (unit: ResidenceUnit?, back: () -> Unit, selectUnit: () -> Unit) -> Unit)? = null,
) {
    var page by remember { mutableStateOf(ResidencePage.Home) }
    var origin by remember { mutableStateOf(ResidencePage.Home) }
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
    val canOpenExhaust = services && exhaustContent != null
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = if (maxWidth < 400.dp) 16.dp else 20.dp
        Column(Modifier.fillMaxSize().background(if (page == ResidencePage.Home) NovaHeader else NovaBackground).systemBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (page) {
                    ResidencePage.Exhaust -> exhaustContent?.invoke(unit, back, { navigate(ResidencePage.SelectUnit) })
                    ResidencePage.Home -> Home(state, unit, horizontal, services, canOpenExhaust, exhaustStatus, refresh, navigate)
                    else -> ResidenceSettings(page, state, user, navigate, back, select, logout, busy, saveLanguage)
                }
            }
            NavigationBar(modifier = Modifier.heightIn(min = 80.dp), containerColor = Color.White, windowInsets = WindowInsets(0)) {
                val colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedTextColor = NovaPrimary, selectedIconColor = NovaPrimary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                NavItem(page == ResidencePage.Home, "Início", R.drawable.nova_home, colors) { navigate(ResidencePage.Home) }
                // A central de incêndio (telas 09–13) ainda não foi implementada.
                NavItem(false, "Alarme", R.drawable.nova_alarm_nav, colors, enabled = false) {}
                NavItem(page == ResidencePage.Exhaust, "Exaustor", R.drawable.nova_exhaust_nav, colors, enabled = canOpenExhaust) { navigate(ResidencePage.Exhaust) }
                NavItem(page in setOf(ResidencePage.Profile, ResidencePage.SelectLanguage) || (page == ResidencePage.SelectUnit && origin == ResidencePage.Profile),
                    "Configurações", R.drawable.nova_profile_nav, colors) { navigate(ResidencePage.Profile) }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(selected: Boolean, label: String, icon: Int, colors: NavigationBarItemColors, enabled: Boolean = true, click: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = click, enabled = enabled, colors = colors,
        icon = { Icon(painterResource(icon), null, Modifier.size(24.dp)) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) })
}

@Composable
private fun Home(
    state: ResidenceState, unit: ResidenceUnit?, horizontal: androidx.compose.ui.unit.Dp, services: Boolean, canOpenExhaust: Boolean,
    exhaustStatus: ExhaustHomeStatus?, refresh: () -> Unit, navigate: (ResidencePage) -> Unit,
) {
    val residence = state.residence
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = horizontal + 8.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(residence?.person?.nome?.takeIf { it.isNotBlank() }?.let { "Olá, ${it.firstName()}" } ?: "Olá", color = Color.White, style = MaterialTheme.typography.titleMedium)
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
        Surface(Modifier.weight(1f).fillMaxWidth(), color = NovaBackground) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(horizontal = horizontal + 8.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.error?.let { NovaNotice(it, state.requestId) }
                    residence?.notice()?.let { NovaNotice(it) }
                    unit?.blockedMessage()?.let { NovaNotice(it) }
                    if (state.error != null || residence?.status != "ativo") NovaButton(
                        if (state.retrySeconds > 0) "Tente novamente em ${state.retrySeconds}s" else "Atualizar cadastro", refresh,
                        enabled = !state.loading && state.retrySeconds == 0L, secondary = true,
                    )
                    if (services) {
                        ServiceRow("Central de Incêndio", "Em breve", R.drawable.nova_fire, enabled = false)
                        HorizontalDivider(color = NovaOutline, modifier = Modifier.padding(start = 16.dp))
                        ServiceRow("Exaustor", exhaustStatus?.summary ?: "Consultando equipamento…", R.drawable.nova_exhaust_nav,
                            enabled = canOpenExhaust, highlight = exhaustStatus?.running == true) { navigate(ResidencePage.Exhaust) }
                        HorizontalDivider(color = NovaOutline, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

private fun String.firstName(): String = trim().split(Regex("\\s+")).first().lowercase().replaceFirstChar { it.titlecase() }

@Composable
private fun ServiceRow(title: String, status: String, icon: Int, enabled: Boolean, highlight: Boolean = false, open: () -> Unit = {}) {
    Surface(onClick = open, enabled = enabled, modifier = Modifier.fillMaxWidth(), color = NovaBackground) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(if (highlight) Color(0x1A189D1F) else MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(24.dp), tint = if (highlight) NovaSuccess else NovaPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                if (highlight) Text("Em funcionamento", style = MaterialTheme.typography.bodySmall, color = NovaSuccess)
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (enabled) Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

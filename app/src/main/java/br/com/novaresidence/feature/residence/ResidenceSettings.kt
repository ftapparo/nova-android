package br.com.novaresidence.feature.residence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.BuildConfig
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.ui.*

internal enum class ResidencePage { Home, Fire, Exhaust, Profile, SelectUnit, SelectLanguage }

// Margem lateral das telas 21–23 no Figma.
private val Margin = 24.dp
private val RadioOutline = Color(0xFF49454F)

@Composable
internal fun ResidenceSettings(
    page: ResidencePage, state: ResidenceState, user: AuthUser, navigate: (ResidencePage) -> Unit,
    back: () -> Unit, select: (Int) -> Unit, logout: () -> Unit, busy: Boolean, saveLanguage: () -> Unit,
) {
    val units = state.residence?.units.orEmpty()
    val previousSave = remember(page) { state.languageSaves }
    LaunchedEffect(state.languageSaves) {
        if (page == ResidencePage.SelectLanguage && state.languageSaves > previousSave) back()
    }
    val title = when (page) {
        ResidencePage.Profile -> "Configurações"
        ResidencePage.SelectUnit -> "Selecionar unidade"
        else -> "Selecionar idioma"
    }
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
        NovaTopBar(title, back)
        BoxWithConstraints(Modifier.weight(1f)) {
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).heightIn(min = maxHeight), verticalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (page == ResidencePage.Profile) 4.dp else 5.dp)) {
                    when (page) {
                        ResidencePage.Profile -> {
                            Spacer(Modifier.height(12.dp))
                            SectionLabel("Conta")
                            val person = state.residence?.person
                            SettingsRow(person?.nome?.takeIf { it.isNotBlank() }?.personName() ?: "Conta Nova Residence", listOfNotNull(
                                user.email?.let { "E-mail: $it" },
                                person?.let { "Código Freedom: ${it.sequencia}" },
                            ))
                            SectionLabel("Unidade")
                            SettingsRow("Minhas unidades", listOf(if (units.size == 1) "1 unidade vinculada" else "${units.size} unidades vinculadas"),
                                click = if (units.isEmpty()) null else ({ navigate(ResidencePage.SelectUnit) }))
                            SectionLabel("Preferências")
                            SettingsRow("Idioma", listOf("Português (Brasil)")) { navigate(ResidencePage.SelectLanguage) }
                            // O link ocupa 48 dp de toque; 2 dp + 14 dp internos reproduzem os 16 dp do Figma abaixo do divisor.
                            HorizontalDivider(Modifier.padding(start = Margin, end = Margin, top = 16.dp, bottom = 2.dp), color = NovaOutline)
                            NovaTextLink("Desconectar", logout, Modifier.padding(horizontal = Margin), enabled = !busy, color = NovaDanger)
                            if (busy) NovaLoading()
                        }
                        ResidencePage.SelectUnit -> {
                            Guidance("Selecione a unidade que deseja usar no aplicativo.")
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                units.forEach { entry ->
                                    val selected = entry.sequencia == state.selectedUnit
                                    SettingsChoice(entry.selectorLabel(), entry.blockedMessage() ?: if (selected) "Unidade selecionada" else "Unidade vinculada", selected, minHeight = 68.dp) {
                                        select(entry.sequencia)
                                    }
                                }
                            }
                            if (units.isEmpty()) Text(state.residence?.notice() ?: "Nenhuma unidade vinculada.", Modifier.padding(horizontal = Margin), style = MaterialTheme.typography.bodyMedium)
                            Row(Modifier.padding(start = Margin, end = Margin, top = 32.dp).fillMaxWidth().background(NovaSecondaryContainer, RoundedCornerShape(12.dp)).padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("i", Modifier.width(20.dp), style = MaterialTheme.typography.labelLarge, color = NovaPrimary, textAlign = TextAlign.Center)
                                Text("Os vínculos são atualizados pelo cadastro do condomínio.", fontSize = 13.sp, lineHeight = 16.sp, color = NovaOnSurfaceVariant)
                            }
                        }
                        ResidencePage.SelectLanguage -> {
                            Guidance("O aplicativo será exibido no idioma escolhido.")
                            // Inglês e espanhol aparecem no Figma, mas ficam desabilitados até existir tradução completa.
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                SettingsChoice("Português (Brasil)", null, true) {}
                                SettingsChoice("English", null, false, enabled = false) {}
                                SettingsChoice("Español", null, false, enabled = false) {}
                            }
                        }
                        else -> Unit
                    }
                    state.preferenceError?.let { Box(Modifier.padding(horizontal = Margin, vertical = 8.dp)) { NovaNotice(it) } }
                    if (state.savingPreference) NovaLoading()
                }
                Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp)) {
                    when (page) {
                        ResidencePage.Profile -> Text("v${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth(), fontSize = 12.sp, lineHeight = 16.sp, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
                        ResidencePage.SelectLanguage -> NovaButton("Salvar idioma", saveLanguage, enabled = !state.savingPreference)
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun Guidance(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(horizontal = Margin, vertical = 16.dp), style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp), color = NovaOnSurfaceVariant)
}

@Composable
private fun SectionLabel(title: String) {
    Text(title, Modifier.fillMaxWidth().padding(horizontal = Margin), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = NovaOnSurfaceVariant)
}

@Composable
private fun SettingsRow(title: String, details: List<String>, click: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().background(Color.White).then(if (click != null) Modifier.clickable(role = Role.Button, onClick = click) else Modifier)
        .heightIn(min = 48.dp).padding(horizontal = Margin, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = NovaOnSurface)
            details.forEach { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = NovaOnSurfaceVariant) }
        }
        if (click != null) Text("›", fontSize = 22.sp, lineHeight = 24.sp, color = NovaOnSurfaceVariant)
    }
}

/** Linha de escolha do Figma (22/23): texto à esquerda e rádio de 20 dp à direita, com 24 dp de margem. */
@Composable
private fun SettingsChoice(title: String, subtitle: String?, selected: Boolean, enabled: Boolean = true, minHeight: Dp = 64.dp, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = click)
        .heightIn(min = minHeight).padding(horizontal = Margin, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = if (enabled) NovaOnSurface else NovaDisabledContent)
            subtitle?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = NovaOnSurfaceVariant) }
        }
        val ring = when { !enabled -> NovaDisabledContent; selected -> NovaPrimary; else -> RadioOutline }
        Box(Modifier.size(20.dp).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(8.dp).background(ring, CircleShape))
        }
    }
}

package br.com.novaresidence.feature.residence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.BuildConfig
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.ui.*

internal enum class ResidencePage { Home, Exhaust, Profile, SelectUnit, SelectLanguage }

private val SettingsNote = Color(0xFFECEBF3)

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
            val available = maxHeight
            val horizontal = if (maxWidth < 400.dp) 16.dp else 24.dp
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).heightIn(min = available), verticalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.fillMaxWidth().padding(top = if (page == ResidencePage.Profile) 16.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (page) {
                        ResidencePage.Profile -> {
                            SettingsHeading("Conta", horizontal)
                            val person = state.residence?.person
                            SettingsRow(person?.nome?.takeIf { it.isNotBlank() } ?: "Conta Nova Residence", horizontal, listOfNotNull(
                                user.email?.let { "E-mail: $it" },
                                person?.let { "Código Freedom: ${it.sequencia}" },
                            ))
                            SettingsHeading("Unidade", horizontal)
                            SettingsRow("Minhas unidades", horizontal, listOf(if (units.size == 1) "1 unidade vinculada" else "${units.size} unidades vinculadas"),
                                click = if (units.isEmpty()) null else ({ navigate(ResidencePage.SelectUnit) }))
                            SettingsHeading("Preferências", horizontal)
                            SettingsRow("Idioma", horizontal, listOf("Português (Brasil)")) { navigate(ResidencePage.SelectLanguage) }
                            HorizontalDivider(Modifier.padding(horizontal = horizontal, vertical = 16.dp), color = NovaOutline)
                            TextButton(logout, Modifier.fillMaxWidth().padding(horizontal = horizontal).heightIn(min = 48.dp), enabled = !busy,
                                colors = ButtonDefaults.textButtonColors(contentColor = NovaDanger)) { Text("Desconectar", style = MaterialTheme.typography.labelLarge) }
                            if (busy) NovaLoading()
                        }
                        ResidencePage.SelectUnit -> {
                            Guidance("Selecione a unidade que deseja usar no aplicativo.", horizontal)
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                units.forEach { entry ->
                                    val selected = entry.sequencia == state.selectedUnit
                                    SettingsChoice(entry.selectorLabel(), entry.blockedMessage() ?: if (selected) "Unidade selecionada" else "Unidade vinculada", selected, horizontal, minHeight = 68.dp) {
                                        select(entry.sequencia)
                                    }
                                }
                            }
                            if (units.isEmpty()) Text(state.residence?.notice() ?: "Nenhuma unidade vinculada.", Modifier.padding(horizontal = horizontal), style = MaterialTheme.typography.bodyMedium)
                            Row(Modifier.padding(start = horizontal, end = horizontal, top = 32.dp).fillMaxWidth().background(SettingsNote, RoundedCornerShape(12.dp)).padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("i", Modifier.width(20.dp), style = MaterialTheme.typography.labelLarge, color = NovaPrimary, textAlign = TextAlign.Center)
                                Text("Os vínculos são atualizados pelo cadastro do condomínio.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        ResidencePage.SelectLanguage -> {
                            Guidance("O aplicativo será exibido no idioma escolhido.", horizontal)
                            // Inglês e espanhol aparecem no Figma, mas ficam desabilitados até existir tradução completa.
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SettingsChoice("Português (Brasil)", null, true, horizontal) {}
                                SettingsChoice("English", null, false, horizontal, enabled = false) {}
                                SettingsChoice("Español", null, false, horizontal, enabled = false) {}
                            }
                        }
                        else -> Unit
                    }
                    state.preferenceError?.let { Box(Modifier.padding(horizontal = horizontal, vertical = 8.dp)) { NovaNotice(it) } }
                    if (state.savingPreference) NovaLoading()
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = horizontal, vertical = 24.dp)) {
                    when (page) {
                        ResidencePage.Profile -> Text("v${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        ResidencePage.SelectLanguage -> NovaButton("Salvar idioma", saveLanguage, enabled = !state.savingPreference)
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun Guidance(text: String, horizontal: Dp) {
    Text(text, Modifier.fillMaxWidth().padding(horizontal = horizontal, vertical = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingsHeading(title: String, horizontal: Dp) {
    Text(title, Modifier.fillMaxWidth().padding(start = horizontal, end = horizontal, top = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingsRow(title: String, horizontal: Dp, details: List<String>, click: (() -> Unit)? = null) {
    Surface(onClick = { click?.invoke() }, enabled = click != null, color = Color.White) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = horizontal, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (click != null) Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsChoice(title: String, subtitle: String?, selected: Boolean, horizontal: Dp, enabled: Boolean = true, minHeight: Dp = 64.dp, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = click)
        .heightIn(min = minHeight).padding(start = horizontal, end = 12.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        RadioButton(selected, onClick = null, enabled = enabled, modifier = Modifier.size(48.dp), colors = RadioButtonDefaults.colors(selectedColor = NovaPrimary))
    }
}

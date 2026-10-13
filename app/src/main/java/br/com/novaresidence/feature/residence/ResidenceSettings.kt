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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.BuildConfig
import br.com.novaresidence.R
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.ui.*

// Margem lateral das telas 09–16 no Figma.
private val Margin = 28.dp
private val RadioOutline = Color(0xFF6B7280)

@Composable
internal fun ResidenceSettings(
    page: ResidencePage, state: ResidenceState, user: AuthUser, navigate: (ResidencePage) -> Unit,
    back: () -> Unit, select: (Int) -> Unit, logout: () -> Unit, busy: Boolean, saveLanguage: () -> Unit,
    deleteAccount: () -> Unit, deleteError: String?,
) {
    val previousSave = remember(page) { state.languageSaves }
    LaunchedEffect(state.languageSaves) {
        if (page == ResidencePage.SelectLanguage && state.languageSaves > previousSave) back()
    }
    val title = when (page) {
        ResidencePage.SelectUnit -> "Selecionar unidade"
        ResidencePage.SelectLanguage -> "Selecionar idioma"
        ResidencePage.Privacy -> "Privacidade"
        ResidencePage.PersonalData -> "Dados pessoais"
        ResidencePage.DeleteAccount -> "Excluir conta"
        else -> "Configurações"
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        NovaTopBar(title, back)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).heightIn(min = maxHeight), verticalArrangement = Arrangement.SpaceBetween) {
                when (page) {
                    ResidencePage.SelectUnit -> SelectUnit(state, select)
                    ResidencePage.SelectLanguage -> SelectLanguage(state, saveLanguage)
                    ResidencePage.Privacy -> Privacy(navigate)
                    ResidencePage.PersonalData -> PersonalData(state, user, navigate)
                    ResidencePage.DeleteAccount -> DeleteAccount(user, back, deleteAccount, busy, deleteError)
                    else -> Settings(state, user, navigate, logout, busy)
                }
            }
        }
    }
}

/** Configurações (Figma 09). */
@Composable
private fun ColumnScope.Settings(state: ResidenceState, user: AuthUser, navigate: (ResidencePage) -> Unit, logout: () -> Unit, busy: Boolean) {
    val units = state.residence?.units.orEmpty()
    val person = state.residence?.person
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Column {
            SectionTitle("Conta")
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(person?.nome?.takeIf { it.isNotBlank() }?.personName() ?: "Conta Nova Residence", style = NovaType.ItemTitle, color = NovaOnSurface)
                user.email?.let { Text(it, style = NovaType.Body, color = NovaOnSurfaceVariant) }
                person?.let { Text("Código Freedom: ${it.sequencia}", style = NovaType.Body, color = NovaOnSurfaceVariant) }
            }
        }
        Column {
            SectionTitle("Configurações")
            SettingsRow("Minhas unidades", if (units.size == 1) "1 unidade vinculada" else "${units.size} unidades vinculadas",
                click = if (units.isEmpty()) null else ({ navigate(ResidencePage.SelectUnit) }))
            HorizontalDivider(color = NovaOutline)
            SettingsRow("Idioma", "Português (Brasil)") { navigate(ResidencePage.SelectLanguage) }
            HorizontalDivider(color = NovaOutline)
            SettingsRow("Privacidade", "Política, termos e dados pessoais") { navigate(ResidencePage.Privacy) }
        }
    }
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NovaTextLink("Desconectar", logout, Modifier.width(160.dp), enabled = !busy, color = NovaError,
            style = NovaType.ItemTitle, alignment = Alignment.CenterStart, underline = true)
        if (busy) NovaLoading()
        Text("Versão do app ${BuildConfig.VERSION_NAME}", style = NovaType.Supporting, color = NovaOnSurfaceVariant)
    }
}

/** Selecionar unidade (Figma 10): a escolha vale durante a sessão. */
@Composable
private fun SelectUnit(state: ResidenceState, select: (Int) -> Unit) {
    val units = state.residence?.units.orEmpty()
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)) {
        Guidance("Selecione a unidade que deseja usar no aplicativo.")
        Spacer(Modifier.height(36.dp))
        Column(Modifier.selectableGroup()) {
            units.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Margin), color = NovaOutline)
                val selected = entry.sequencia == state.selectedUnit
                Choice(entry.selectorLabel(), entry.blockedMessage() ?: if (selected) "Unidade selecionada" else "Unidade vinculada", selected, minHeight = 76.dp) {
                    select(entry.sequencia)
                }
            }
        }
        if (units.isEmpty()) Text(state.residence?.notice() ?: "Nenhuma unidade vinculada.", Modifier.padding(horizontal = Margin), style = NovaType.Body)
        Row(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.nova_info_outline), null, Modifier.padding(top = 1.dp).size(18.dp), tint = NovaPrimary)
            Text("Os vínculos são atualizados pelo cadastro do condomínio.", Modifier.weight(1f, fill = false),
                style = NovaType.Supporting, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Selecionar idioma (Figma 11). Inglês e espanhol ficam desabilitados até existir tradução completa. */
@Composable
private fun ColumnScope.SelectLanguage(state: ResidenceState, saveLanguage: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Guidance("O aplicativo será exibido no idioma escolhido.")
        Spacer(Modifier.height(36.dp))
        Column(Modifier.selectableGroup()) {
            Choice("Português (Brasil)", null, true) {}
            HorizontalDivider(Modifier.padding(horizontal = Margin), color = NovaOutline)
            Choice("English", null, false, enabled = false) {}
            HorizontalDivider(Modifier.padding(horizontal = Margin), color = NovaOutline)
            Choice("Español", null, false, enabled = false) {}
        }
        state.preferenceError?.let { Box(Modifier.padding(horizontal = Margin, vertical = 16.dp)) { NovaNotice(it) } }
        if (state.savingPreference) NovaLoading()
    }
    NovaButton("Salvar idioma", saveLanguage, Modifier.padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp), enabled = !state.savingPreference)
}

/** Privacidade (Figma 12). */
@Composable
private fun Privacy(navigate: (ResidencePage) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp)) {
        SettingsRow("Termos de uso", "Regras e condições de uso do aplicativo") { navigate(ResidencePage.Terms) }
        HorizontalDivider(color = NovaOutline)
        SettingsRow("Política de privacidade", "Como seus dados são utilizados e protegidos") { navigate(ResidencePage.PrivacyPolicy) }
        HorizontalDivider(color = NovaOutline)
        SettingsRow("Dados pessoais", "Consulte os dados vinculados à sua conta") { navigate(ResidencePage.PersonalData) }
        HorizontalDivider(color = NovaOutline)
        SettingsRow("Solicitar exclusão da conta", "Solicite a remoção do acesso e dos dados do aplicativo") { navigate(ResidencePage.DeleteAccount) }
    }
}

/**
 * Dados pessoais (Figma 15). O CPF vem mascarado da API; os "dados complementares" do Figma
 * não existem no contrato e não são exibidos.
 */
@Composable
private fun PersonalData(state: ResidenceState, user: AuthUser, navigate: (ResidencePage) -> Unit) {
    val residence = state.residence
    val unit = residence?.units?.firstOrNull { it.sequencia == state.selectedUnit }
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)) {
        Guidance("Dados vinculados à sua conta e ao cadastro do condomínio.")
        Column(Modifier.padding(horizontal = Margin)) {
            val rows = listOfNotNull(
                residence?.person?.nome?.takeIf { it.isNotBlank() }?.let { "Nome" to it.personName() },
                residence?.person?.cpfMasked?.let { "CPF" to it },
                user.email?.let { "E-mail" to it },
            )
            rows.forEach { (label, value) ->
                DataRow(label, value)
                HorizontalDivider(color = NovaOutline)
            }
            DataRow("Unidade ativa", unit?.selectorLabel() ?: "Nenhuma unidade",
                click = if (residence?.units.isNullOrEmpty()) null else ({ navigate(ResidencePage.SelectUnit) }))
        }
    }
}

/**
 * Excluir conta (Figma 16): a própria tela é a confirmação. A API exclui na hora; contas da equipe são
 * removidas pela administração (a API responde 403), então o botão nem é oferecido a elas.
 */
@Composable
private fun ColumnScope.DeleteAccount(user: AuthUser, back: () -> Unit, delete: () -> Unit, busy: Boolean, error: String?) {
    val staff = user.role in setOf("porteiro", "sindico", "admin")
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Excluir sua conta", Modifier.semantics { heading() }, style = NovaType.ScreenTitle.copy(fontWeight = FontWeight.SemiBold), color = NovaOnSurface)
        Text("Ao excluir sua conta, seu acesso ao aplicativo será encerrado. Alguns dados podem continuar armazenados pelo condomínio quando necessários para obrigações legais, administrativas ou de segurança.",
            style = NovaType.Body.copy(lineHeight = 21.sp), color = NovaOnSurface)
        Column(Modifier.fillMaxWidth().background(NovaErrorContainer, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Antes de continuar", style = NovaType.BodyEmphasis, color = NovaError)
            Text("A exclusão é imediata e não pode ser desfeita. Se precisar voltar a utilizar o app, será necessário criar uma nova conta e um novo vínculo pela administração do condomínio.",
                style = NovaType.Supporting, color = NovaOnSurfaceVariant)
        }
        error?.let { NovaNotice(it) }
    }
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        NovaButton("Cancelar", back, enabled = !busy, style = NovaButtonStyle.Neutral)
        NovaButton(if (busy) "Excluindo conta…" else "Excluir conta", delete, enabled = !busy && !staff, style = NovaButtonStyle.Danger)
        if (staff) Text("Contas da equipe são removidas pela administração do condomínio.",
            Modifier.fillMaxWidth(), style = NovaType.Supporting, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Guidance(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(horizontal = Margin), style = NovaType.Body, color = NovaOnSurfaceVariant)
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, Modifier.semantics { heading() }, style = NovaType.Section, color = NovaOnSurface)
}

/** Linha das configurações (Figma 09 e 12): título, apoio e círculo com chevron. */
@Composable
private fun SettingsRow(title: String, detail: String, click: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (click != null) Modifier.clickable(role = Role.Button, onClick = click) else Modifier)
        .heightIn(min = 84.dp).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = NovaType.ItemTitle, color = NovaOnSurface)
            Text(detail, style = NovaType.Body, color = NovaOnSurfaceVariant)
        }
        if (click != null) NovaChevronAction()
    }
}

/** Linha dos dados pessoais (Figma 15): rótulo e valor, com chevron quando abre outra tela. */
@Composable
private fun DataRow(label: String, value: String, click: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (click != null) Modifier.clickable(role = Role.Button, onClick = click) else Modifier)
        .heightIn(min = 112.dp).padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = NovaType.ItemTitle, color = NovaOnSurface)
            Text(value, style = NovaType.Body, color = NovaOnSurfaceVariant)
        }
        if (click != null) NovaChevronAction()
    }
}

/** Linha de escolha do Figma (10/11): texto à esquerda e rádio de 20 dp à direita. */
@Composable
private fun Choice(title: String, subtitle: String?, selected: Boolean, enabled: Boolean = true, minHeight: Dp = 68.dp, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = click)
        .heightIn(min = minHeight).padding(horizontal = Margin, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = NovaType.ItemTitle, color = if (enabled) NovaOnSurface else NovaDisabledContent)
            subtitle?.let { Text(it, style = NovaType.Body, color = NovaOnSurfaceVariant) }
        }
        val ring = when { !enabled -> NovaDisabledContent; selected -> NovaPrimary; else -> RadioOutline }
        Box(Modifier.size(20.dp).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(8.dp).background(ring, CircleShape))
        }
    }
}

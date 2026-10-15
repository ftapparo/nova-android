package br.com.novaresidence.feature.residence

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
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
    back: () -> Unit, select: (Int) -> Unit, logout: () -> Unit, busy: Boolean, saveLanguage: (AppLanguage) -> Unit,
    deleteAccount: () -> Unit, deleteError: UiText?, notifications: Boolean = false,
) {
    val previousSave = remember(page) { state.languageSaves }
    val previousLanguage = remember(page) { state.language }
    val context = LocalContext.current
    LaunchedEffect(state.languageSaves) {
        if (page == ResidencePage.SelectLanguage && state.languageSaves > previousSave) {
            back()
            // O idioma é aplicado em attachBaseContext: recriar a Activity troca todos os textos de uma vez.
            if (state.language != previousLanguage) context.findActivity()?.recreate()
        }
    }
    val title = when (page) {
        ResidencePage.SelectUnit -> stringResource(R.string.settings_title_select_unit)
        ResidencePage.SelectLanguage -> stringResource(R.string.settings_title_select_language)
        ResidencePage.Privacy -> stringResource(R.string.settings_title_privacy)
        ResidencePage.PersonalData -> stringResource(R.string.settings_title_personal_data)
        ResidencePage.DeleteAccount -> stringResource(R.string.settings_title_delete_account)
        else -> stringResource(R.string.settings_title)
    }
    Column(Modifier.fillMaxSize().background(NovaBackground)) {
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
                    else -> Settings(state, user, navigate, logout, busy, notifications)
                }
            }
        }
    }
}

/** Configurações (Figma 09). */
@Composable
private fun ColumnScope.Settings(
    state: ResidenceState, user: AuthUser, navigate: (ResidencePage) -> Unit, logout: () -> Unit, busy: Boolean, notifications: Boolean,
) {
    val units = state.residence?.units.orEmpty()
    val person = state.residence?.person
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Column {
            SectionTitle(stringResource(R.string.settings_account))
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(person?.nome?.takeIf { it.isNotBlank() }?.personName() ?: stringResource(R.string.settings_account_fallback), style = NovaType.ItemTitle, color = NovaOnSurface)
                user.email?.let { Text(it, style = NovaType.Body, color = NovaOnSurfaceVariant) }
                person?.let { Text(stringResource(R.string.settings_freedom_code, it.sequencia), style = NovaType.Body, color = NovaOnSurfaceVariant) }
            }
        }
        Column {
            SectionTitle(stringResource(R.string.settings_title))
            SettingsRow(stringResource(R.string.settings_my_units), pluralStringResource(R.plurals.settings_units_linked, units.size, units.size),
                click = if (units.isEmpty()) null else ({ navigate(ResidencePage.SelectUnit) }))
            HorizontalDivider(color = NovaOutline)
            SettingsRow(stringResource(R.string.settings_language), state.language.displayName()) { navigate(ResidencePage.SelectLanguage) }
            HorizontalDivider(color = NovaOutline)
            if (notifications) {
                SettingsRow(stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_detail)) { navigate(ResidencePage.Notifications) }
                HorizontalDivider(color = NovaOutline)
            }
            SettingsRow(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_detail)) { navigate(ResidencePage.Privacy) }
        }
    }
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NovaTextLink(stringResource(R.string.settings_sign_out), logout, Modifier.width(160.dp), enabled = !busy, color = NovaError,
            style = NovaType.ItemTitle, alignment = Alignment.CenterStart, underline = true)
        if (busy) NovaLoading()
        Text(stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME), style = NovaType.Supporting, color = NovaOnSurfaceVariant)
    }
}

/** Selecionar unidade (Figma 13): a escolha vale durante a sessão. */
@Composable
private fun SelectUnit(state: ResidenceState, select: (Int) -> Unit) {
    val units = state.residence?.units.orEmpty()
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)) {
        Guidance(stringResource(R.string.settings_select_unit_guidance))
        Spacer(Modifier.height(28.dp))
        Column(Modifier.selectableGroup()) {
            units.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Margin), color = NovaOutline)
                val selected = entry.sequencia == state.selectedUnit
                Choice(entry.selectorLabel().text(), entry.blockedMessage()?.text() ?: stringResource(if (selected) R.string.settings_unit_selected else R.string.settings_unit_linked), selected, minHeight = 76.dp) {
                    select(entry.sequencia)
                }
            }
        }
        if (units.isEmpty()) Text(state.residence?.notice()?.text() ?: stringResource(R.string.settings_no_linked_unit), Modifier.padding(horizontal = Margin), style = NovaType.Body)
        NovaInfoStrip(stringResource(R.string.settings_links_info), Modifier.padding(start = Margin, end = Margin, top = 28.dp))
    }
}

/** Selecionar idioma (Figma 11). Na primeira abertura o app já veio no idioma do aparelho (AppLanguageStore). */
@Composable
private fun ColumnScope.SelectLanguage(state: ResidenceState, saveLanguage: (AppLanguage) -> Unit) {
    var choice by rememberSaveable { mutableStateOf(state.language) }
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Guidance(stringResource(R.string.settings_language_guidance))
        Spacer(Modifier.height(28.dp))
        Column(Modifier.selectableGroup()) {
            AppLanguage.entries.forEachIndexed { index, language ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Margin), color = NovaOutline)
                Choice(language.displayName(), null, choice == language) { choice = language }
            }
        }
        state.preferenceError?.let { Box(Modifier.padding(horizontal = Margin, vertical = 16.dp)) { NovaNotice(it.text()) } }
        if (state.savingPreference) NovaLoading()
    }
    NovaButton(stringResource(R.string.settings_save_language), { saveLanguage(choice) }, Modifier.padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp), enabled = !state.savingPreference)
}

/** Privacidade (Figma 12). */
@Composable
private fun Privacy(navigate: (ResidencePage) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp)) {
        SettingsRow(stringResource(R.string.privacy_terms), stringResource(R.string.privacy_terms_detail)) { navigate(ResidencePage.Terms) }
        HorizontalDivider(color = NovaOutline)
        SettingsRow(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_detail)) { navigate(ResidencePage.PrivacyPolicy) }
        HorizontalDivider(color = NovaOutline)
        SettingsRow(stringResource(R.string.privacy_personal_data), stringResource(R.string.privacy_personal_data_detail)) { navigate(ResidencePage.PersonalData) }
        HorizontalDivider(color = NovaOutline)
        SettingsRow(stringResource(R.string.privacy_delete_account), stringResource(R.string.privacy_delete_account_detail)) { navigate(ResidencePage.DeleteAccount) }
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
        Guidance(stringResource(R.string.personal_data_guidance))
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = Margin), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val rows = listOfNotNull(
                residence?.person?.nome?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.personal_data_name) to it.personName() },
                residence?.person?.cpfMasked?.let { stringResource(R.string.personal_data_cpf) to it },
                user.email?.let { stringResource(R.string.personal_data_email) to it },
            )
            rows.forEach { (label, value) ->
                DataRow(label, value)
                HorizontalDivider(color = NovaOutline)
            }
            DataRow(stringResource(R.string.personal_data_active_unit), unit?.selectorLabel()?.text() ?: stringResource(R.string.home_no_unit),
                click = if (residence?.units.isNullOrEmpty()) null else ({ navigate(ResidencePage.SelectUnit) }))
        }
    }
}

/**
 * Excluir conta (Figma 16): a própria tela é a confirmação. A API exclui na hora; contas da equipe são
 * removidas pela administração (a API responde 403), então o botão nem é oferecido a elas.
 */
@Composable
private fun ColumnScope.DeleteAccount(user: AuthUser, back: () -> Unit, delete: () -> Unit, busy: Boolean, error: UiText?) {
    val staff = user.role in setOf("porteiro", "sindico", "admin")
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.delete_title), Modifier.semantics { heading() }, style = NovaType.ScreenTitle.copy(fontWeight = FontWeight.SemiBold), color = NovaOnSurface)
        Text(stringResource(R.string.delete_body),
            style = NovaType.Body.copy(lineHeight = 21.sp), color = NovaOnSurface)
        Column(Modifier.fillMaxWidth().background(NovaErrorContainer, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.delete_warning_title), style = NovaType.BodyEmphasis, color = NovaError)
            Text(stringResource(R.string.delete_warning),
                style = NovaType.Supporting, color = NovaOnSurfaceVariant)
        }
        error?.let { NovaNotice(it.text()) }
    }
    Column(Modifier.fillMaxWidth().padding(start = Margin, end = Margin, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        NovaButton(stringResource(R.string.delete_cancel), back, enabled = !busy, style = NovaButtonStyle.Neutral)
        NovaButton(stringResource(if (busy) R.string.delete_in_progress else R.string.delete_confirm), delete, enabled = !busy && !staff, style = NovaButtonStyle.Danger)
        if (staff) Text(stringResource(R.string.auth_delete_staff),
            Modifier.fillMaxWidth(), style = NovaType.Supporting, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Guidance(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(horizontal = Margin).heightIn(min = 40.dp), style = NovaType.Body, color = NovaOnSurfaceVariant)
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

/** Nome de cada idioma escrito nele mesmo, como nas listas de idioma do Android. */
private fun AppLanguage.displayName(): String = when (this) {
    AppLanguage.Portuguese -> "Português (Brasil)"
    AppLanguage.English -> "English"
    AppLanguage.Spanish -> "Español"
}

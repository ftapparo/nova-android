package br.com.novaresidence.feature.residence

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.novaresidence.R
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.ui.*
import br.com.novaresidence.feature.legal.LegalLinksText

/**
 * Vínculo automático da conta (Figma 07b e 07c), obrigatório para conta pendente que não é da equipe.
 * [leave] sai da conta (a seta de voltar do formulário); [linked] recebe a situação já vinculada.
 */
@Composable
fun ResidenceLinkScreen(
    state: ResidenceLinkState, user: AuthUser, viewModel: ResidenceLinkViewModel,
    leave: () -> Unit, linked: (Residence) -> Unit, openLegal: (String) -> Unit,
) {
    val check = state.check
    BackHandler { if (check != null) viewModel.back() else if (!state.busy) leave() }
    if (check == null) LinkForm(state, viewModel, leave, openLegal)
    else LinkConfirmation(state, check, user, viewModel::back) { viewModel.confirm(linked) }
}

/** 07b: CPF e celular com DDI. Os valores ficam só na composição, como as senhas da entrada. */
@Composable
private fun LinkForm(state: ResidenceLinkState, viewModel: ResidenceLinkViewModel, leave: () -> Unit, openLegal: (String) -> Unit) {
    var cpf by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var countryIso by rememberSaveable { mutableStateOf("BR") }
    var picking by remember { mutableStateOf(false) }
    val country = PhoneCountries.first { it.iso == countryIso }
    val canAct = !state.busy && state.retrySeconds == 0L
    val submit = { viewModel.check(cpf, country, phone) }
    NovaPage(stringResource(R.string.link_title), Modifier.systemBarsPadding().imePadding(), back = leave) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight).padding(start = NovaSpacing.Margin, end = NovaSpacing.Margin, top = 43.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    NovaIntroduction(stringResource(R.string.link_intro_title), stringResource(R.string.link_intro))
                    Spacer(Modifier.height(36.dp - 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NovaTextField(stringResource(R.string.link_cpf), cpf, { cpf = it }, state.cpfError?.text(), !state.busy,
                            keyboard = KeyboardType.Number, placeholder = "000.000.000-00",
                            inputTransformation = DigitsOnly(11), outputTransformation = CpfMask)
                        // O campo é recriado ao trocar de país: máscara e limite de dígitos mudam com o DDI.
                        key(country.iso) {
                            NovaTextField(stringResource(R.string.link_phone), phone, { phone = it }, state.phoneError?.text(), !state.busy,
                                keyboard = KeyboardType.Phone, placeholder = if (country.isBrazil) "(00) 00000-0000" else "",
                                submit = submit, inputTransformation = DigitsOnly(if (country.isBrazil) 11 else 14),
                                outputTransformation = if (country.isBrazil) BrazilPhoneMask else null,
                                leadingIcon = { CountryButton(country, enabled = !state.busy) { picking = true } })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    NovaButton(stringResource(R.string.link_continue), submit, enabled = canAct)
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.error?.let { NovaNotice(it.text(), state.requestId) }
                        if (state.retrySeconds > 0) Text(stringResource(R.string.auth_retry_in, state.retrySeconds), style = NovaType.Body)
                        if (state.busy) NovaLoading()
                        NovaInfoStrip(stringResource(R.string.link_info))
                    }
                }
                LegalLinksText(R.string.legal_continue_agreement, openLegal, Modifier.fillMaxWidth().padding(top = 24.dp), textAlign = TextAlign.Center)
            }
        }
    }
    if (picking) CountryPicker(countryIso, { countryIso = it; phone = ""; picking = false }) { picking = false }
}

/** Bandeira e DDI dentro do campo do celular; o toque abre o seletor de país. */
@Composable
private fun CountryButton(country: PhoneCountry, enabled: Boolean, open: () -> Unit) {
    val description = stringResource(R.string.link_country_description, country.name(), country.ddi)
    TextButton(open, enabled = enabled, modifier = Modifier.padding(start = 4.dp).semantics { contentDescription = description }) {
        Text("${country.flag} +${country.ddi}", style = NovaType.Field, color = NovaOnSurface)
    }
}

/** Lista de países com busca por nome ou DDI. */
@Composable
private fun CountryPicker(selected: String, choose: (String) -> Unit, dismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val countries = remember(query) {
        val text = query.trim().removePrefix("+")
        PhoneCountries.filter { text.isEmpty() || it.name().contains(text, ignoreCase = true) || it.ddi.startsWith(text) }
    }
    Dialog(dismiss) {
        NovaCard(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            Column(Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                Text(stringResource(R.string.link_country_title), Modifier.padding(horizontal = 20.dp), style = NovaType.Section, color = NovaOnSurface)
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    NovaTextField(stringResource(R.string.link_country_search), query, { query = it }, null, true)
                }
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(countries, key = { it.iso }) { country ->
                        Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { choose(country.iso) }.heightIn(min = 48.dp)
                            .padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(country.flag, style = NovaType.Field)
                            Text(country.name(), Modifier.weight(1f), style = if (country.iso == selected) NovaType.BodyEmphasis else NovaType.Body,
                                color = NovaOnSurface)
                            Text("+${country.ddi}", style = NovaType.Body, color = NovaOnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** 07c: confere a pessoa e as unidades encontradas antes de vincular. */
@Composable
private fun LinkConfirmation(state: ResidenceLinkState, check: SelfLinkCheck, user: AuthUser, back: () -> Unit, confirm: () -> Unit) {
    NovaPage(stringResource(R.string.link_confirm_title), Modifier.systemBarsPadding(), back = back) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight), verticalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Guidance(stringResource(R.string.link_confirm_guidance))
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.padding(horizontal = NovaSpacing.Margin), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val units = check.units.map { it.selectorLabel().text() }
                        val rows = listOfNotNull(
                            stringResource(R.string.personal_data_name) to check.person.nome.personName(),
                            check.person.cpfMasked?.let { stringResource(R.string.personal_data_cpf) to it },
                            user.email?.let { stringResource(R.string.personal_data_email) to it },
                            (if (units.size > 1) stringResource(R.string.link_confirm_units) else stringResource(R.string.personal_data_active_unit)) to
                                units.joinToString("\n"),
                        )
                        rows.forEachIndexed { index, (label, value) ->
                            DataRow(label, value)
                            if (index < rows.lastIndex) NovaDivider()
                        }
                        state.error?.let { NovaNotice(it.text(), state.requestId) }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(start = NovaSpacing.Margin, end = NovaSpacing.Margin, top = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.busy) NovaLoading()
                    NovaButton(stringResource(R.string.link_confirm), confirm, enabled = !state.busy)
                    NovaButton(stringResource(R.string.link_back), back, enabled = !state.busy, style = NovaButtonStyle.Neutral)
                }
            }
        }
    }
}

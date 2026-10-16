package br.com.novaresidence.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.insert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.novaresidence.R

/**
 * Campo de formulário do Figma (entrada e vínculo da conta): contorno com legenda sempre acima, placeholder,
 * erro ou dica abaixo. Com [secret], senha com o botão de mostrar/ocultar. [inputTransformation] filtra o que
 * entra (ex.: só dígitos) e [outputTransformation] só desenha a máscara: o valor entregue a [change] fica sem ela.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NovaTextField(
    label: String, value: String, change: (String) -> Unit, error: String?, enabled: Boolean,
    secret: Boolean = false, keyboard: KeyboardType = KeyboardType.Text, placeholder: String = "",
    submit: (() -> Unit)? = null, hint: String? = null,
    inputTransformation: InputTransformation? = null, outputTransformation: OutputTransformation? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val input = remember { TextFieldState(initialText = value) }
    LaunchedEffect(input) { snapshotFlow { input.text.toString() }.collect(change) }
    val options = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard, imeAction = if (submit != null) ImeAction.Done else ImeAction.Next)
    val keyboardAction = KeyboardActionHandler { performDefaultAction -> if (submit != null) submit() else performDefaultAction() }
    val colors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = NovaFieldOutline, focusedBorderColor = NovaPrimary,
        unfocusedLabelColor = NovaFieldVariant, focusedLabelColor = NovaPrimary,
        unfocusedPlaceholderColor = NovaFieldVariant, focusedPlaceholderColor = NovaFieldVariant,
    )
    val textStyle = NovaType.Field
    val supporting = error ?: hint
    var revealed by remember { mutableStateOf(false) }
    if (secret) OutlinedSecureTextField(
        state = input, modifier = Modifier.fillMaxWidth(), enabled = enabled, textStyle = textStyle,
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true), label = { Text(label) },
        placeholder = { Text(placeholder) }, isError = error != null, colors = colors,
        supportingText = if (supporting != null) ({ Text(supporting) }) else null,
        trailingIcon = {
            IconButton({ revealed = !revealed }) {
                Icon(painterResource(if (revealed) R.drawable.nova_visibility_off else R.drawable.nova_visibility),
                    stringResource(if (revealed) R.string.auth_hide_password else R.string.auth_show_password), Modifier.size(24.dp), tint = NovaFieldVariant)
            }
        },
        keyboardOptions = options, onKeyboardAction = keyboardAction,
        textObfuscationMode = if (revealed) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
        shape = NovaShapes.Field,
    ) else OutlinedTextField(
        state = input, modifier = Modifier.fillMaxWidth(), enabled = enabled, textStyle = textStyle,
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true), label = { Text(label) },
        placeholder = { Text(placeholder) }, isError = error != null, colors = colors,
        supportingText = if (supporting != null) ({ Text(supporting) }) else null,
        leadingIcon = leadingIcon,
        inputTransformation = inputTransformation, outputTransformation = outputTransformation,
        keyboardOptions = options, onKeyboardAction = keyboardAction, lineLimits = TextFieldLineLimits.SingleLine,
        shape = NovaShapes.Field,
    )
}

/** Aceita só dígitos, até [max]: o teclado numérico ainda deixa colar texto com máscara. */
class DigitsOnly(private val max: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val digits = toString().filter(Char::isDigit).take(max)
        if (digits != toString()) replace(0, length, digits)
    }
}

/** Desenha a máscara "000.000.000-00" sobre os dígitos do CPF. */
object CpfMask : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        if (length > 9) insert(9, "-")
        if (length > 6) insert(6, ".")
        if (length > 3) insert(3, ".")
    }
}

/** Desenha "(00) 00000-0000" (ou "(00) 0000-0000" com 10 dígitos) sobre o celular brasileiro. */
object BrazilPhoneMask : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val hyphen = if (length >= 11) 7 else 6
        if (length > hyphen) insert(hyphen, "-")
        if (length > 2) insert(2, ") ")
        if (length > 0) insert(0, "(")
    }
}

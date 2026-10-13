package br.com.novaresidence.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*

// O OutlinedTextField reserva 8 dp acima da borda para a legenda; os espaços abaixo
// descontam esse valor para que a distância visível seja a mesma do Figma.
private val FieldLabelInset = 8.dp
private val FieldVariant = Color(0xFF49454F)

@Composable
fun AuthScreen(state: AuthState, viewModel: AuthViewModel) {
    BackHandler(state.page != AuthPage.Welcome) {
        if (!state.busy && state.page != AuthPage.ChangePassword) viewModel.back()
    }
    Column(Modifier.fillMaxSize().background(NovaBackground).systemBarsPadding().imePadding()) {
        if (state.page != AuthPage.Welcome) NovaTopBar(when (state.page) {
            AuthPage.Login -> "Acessar conta"
            AuthPage.Signup -> "Criar conta"
            AuthPage.Recovery -> "Recuperar acesso"
            AuthPage.RecoveryConfirm -> "Validar conta"
            AuthPage.ChangePassword -> "Trocar senha"
            else -> "Não foi possível entrar"
        }, if (state.page == AuthPage.ChangePassword) null else viewModel::back, !state.busy)
        Box(Modifier.weight(1f)) {
            key(state.page) {
                when (state.page) {
                    AuthPage.Welcome -> WelcomeContent(state, viewModel)
                    AuthPage.Error -> ErrorContent(state, viewModel)
                    else -> AuthForm(state, viewModel)
                }
            }
        }
    }
}

/** Corpo das telas 03–07: 23 dp abaixo da barra, margens de 28 dp e coluna de no máximo 480 dp. */
@Composable
private fun AuthContent(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 43.dp, bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun WelcomeContent(state: AuthState, viewModel: AuthViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.nova_logo), "Nova Residence", Modifier.size(226.dp, 170.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bem-vindo", style = NovaType.ScreenTitle, textAlign = TextAlign.Center)
                    Text("Acompanhe informações e acesse os serviços do seu condomínio em um só lugar.", Modifier.widthIn(max = 340.dp),
                        style = NovaType.Body, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(28.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    NovaButton("Entrar", { viewModel.navigate(AuthPage.Login) }, enabled = !state.busy)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = NovaOutline)
                        Text("ou continue com", style = NovaType.Caption, color = NovaOnSurfaceVariant)
                        HorizontalDivider(Modifier.weight(1f), color = NovaOutline)
                    }
                    GoogleButton(state, viewModel)
                    AuthFeedback(state)
                }
            }
            SignupPrompt(state, viewModel)
        }
    }
}

@Composable
private fun GoogleButton(state: AuthState, viewModel: AuthViewModel) {
    val context = LocalContext.current
    Button({ viewModel.google { googleIdToken(context) } }, Modifier.fillMaxWidth().height(48.dp), enabled = !state.busy && state.retrySeconds == 0L,
        shape = RoundedCornerShape(999.dp), elevation = null,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NovaOnSurfaceVariant,
            disabledContainerColor = Color.White, disabledContentColor = NovaDisabledContent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        Image(painterResource(R.drawable.nova_google_account), null, Modifier.size(32.dp))
        Spacer(Modifier.width(8.dp))
        Text("Google", style = NovaType.Button)
    }
}

@Composable
private fun SignupPrompt(state: AuthState, viewModel: AuthViewModel) {
    val prompt = "Ainda não tem uma conta?"
    val action = @Composable {
        TextButton(onClick = { viewModel.navigate(AuthPage.Signup) }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text("Criar conta", style = NovaType.Button, color = NovaPrimary)
        }
    }
    // Com fonte ampliada, texto e ação empilham para não cortar nenhum dos dois.
    if (LocalDensity.current.fontScale >= 1.3f) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(prompt, style = NovaType.Body, color = NovaOnSurface)
        action()
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(prompt, style = NovaType.Body, color = NovaOnSurface, modifier = Modifier.weight(1f, false))
        action()
    }
}

@Composable
private fun ErrorContent(state: AuthState, viewModel: AuthViewModel) {
    AuthContent {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NovaIntroduction("Seus dados não conferem", "Confira seus dados de acesso e sua senha ou recupere seu acesso.")
            NovaNotice("Nenhuma alteração foi feita na sua conta.", title = "Acesso não concluído")
            NovaButton("Tentar novamente", viewModel::back, enabled = !state.busy)
            NovaButton("Recuperar acesso", { viewModel.navigate(AuthPage.Recovery) }, enabled = !state.busy, style = NovaButtonStyle.Secondary)
            if (state.retrySeconds > 0) Text("Tente novamente em ${state.retrySeconds}s", style = NovaType.Body)
            Text("Se o problema continuar, fale com a administração do condomínio.", Modifier.fillMaxWidth(),
                style = NovaType.Supporting, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AuthForm(state: AuthState, viewModel: AuthViewModel) {
    // Senhas ficam apenas na composição, sem persistência no estado da Activity.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val canAct = !state.busy && state.retrySeconds == 0L
    val page = state.page
    val signup = page == AuthPage.Signup
    val submit: () -> Unit = {
        when (page) {
            AuthPage.Login -> viewModel.login(email, password)
            AuthPage.Signup -> viewModel.signup(email, password)
            AuthPage.Recovery -> viewModel.recover(email)
            AuthPage.RecoveryConfirm -> viewModel.confirm(code, password)
            AuthPage.ChangePassword -> viewModel.change(current, password)
            else -> Unit
        }
    }
    val intro = when (page) {
        AuthPage.Login -> "Vamos identificar você" to "Informe seus dados de acesso."
        AuthPage.Signup -> "Vamos identificar você" to "Informe seus dados para cadastro."
        AuthPage.Recovery -> "Vamos ajudar você a entrar" to "Informe seu e-mail e enviaremos as próximas instruções."
        AuthPage.RecoveryConfirm -> "Digite o código" to "Se houver uma conta com este e-mail, você receberá um código de 6 dígitos. Ele expira em 15 minutos."
        else -> "Crie uma nova senha" to "Para continuar, substitua sua senha temporária por uma senha pessoal."
    }
    AuthContent {
        NovaIntroduction(intro.first, intro.second)
        // Login e cadastro têm uma linha de respiro a mais entre a introdução e o primeiro campo no Figma.
        val introGap = if (page in setOf(AuthPage.Login, AuthPage.Signup)) 36.dp else 16.dp
        if (page == AuthPage.RecoveryConfirm) {
            Spacer(Modifier.height(16.dp))
            RecoveryCodeInput(code, { code = it }, state.fields["code"], !state.busy)
            Spacer(Modifier.height(16.dp - FieldLabelInset))
        } else Spacer(Modifier.height(introGap - FieldLabelInset))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp - FieldLabelInset)) {
            if (page in setOf(AuthPage.Login, AuthPage.Signup, AuthPage.Recovery)) {
                AuthField("E-mail", email, { email = it }, state.fields["email"], !state.busy, keyboard = KeyboardType.Email,
                    placeholder = when (page) { AuthPage.Signup -> "Digite um contato válido"; AuthPage.Recovery -> "nome@exemplo.com"; else -> "Digite seu e-mail" }, submit = if (page == AuthPage.Recovery) submit else null)
            }
            if (page == AuthPage.ChangePassword) AuthField("Senha atual", current, { current = it }, state.fields["currentPassword"], !state.busy, secret = true, placeholder = "Senha temporária")
            if (page != AuthPage.Recovery) {
                val isNew = page != AuthPage.Login
                AuthField(if (signup) "Crie uma senha" else if (isNew) "Nova senha" else "Senha", password, { password = it },
                    state.fields[if (isNew) "newPassword" else "password"] ?: state.fields["password"], !state.busy,
                    secret = true, placeholder = if (isNew) "Mínimo de 8 caracteres" else "Digite sua senha", submit = submit,
                    hint = if (isNew) "Use pelo menos 8 caracteres." else null)
            }
        }
        Spacer(Modifier.height(16.dp))
        NovaButton(when (page) {
            AuthPage.Login -> "Entrar"
            AuthPage.Signup -> "Continuar"
            AuthPage.Recovery -> "Enviar instruções"
            AuthPage.RecoveryConfirm -> "Validar e continuar"
            else -> "Salvar nova senha"
        }, submit, enabled = canAct)
        // O link ocupa 48 dp de toque com o texto centralizado: 2 dp + 14 dp internos = 16 dp visíveis.
        if (page == AuthPage.Login) {
            Spacer(Modifier.height(2.dp))
            NovaTextLink("Esqueci minha senha", { viewModel.navigate(AuthPage.Recovery) }, enabled = !state.busy)
        }
        if (page == AuthPage.ChangePassword) {
            Spacer(Modifier.height(2.dp))
            NovaTextLink("Sair", viewModel::logout, enabled = !state.busy)
        }
        if (page == AuthPage.RecoveryConfirm) {
            Spacer(Modifier.height(16.dp))
            val remaining = state.retrySeconds
            NovaButton(if (remaining > 0) "Reenviar código em %02d:%02d".format(remaining / 60, remaining % 60) else "Reenviar código",
                { viewModel.recover(state.recoveryEmail) }, enabled = canAct, style = NovaButtonStyle.Secondary)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(painterResource(R.drawable.nova_info_outline), null, Modifier.padding(top = 1.dp).size(18.dp), tint = NovaPrimary)
                Text("Confira também a caixa de spam ou mensagens bloqueadas.", Modifier.weight(1f),
                    style = NovaType.Supporting, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        if (state.busy || state.error != null || state.retrySeconds > 0) {
            Spacer(Modifier.height(16.dp))
            AuthFeedback(state)
        }
    }
}

@Composable
private fun RecoveryCodeInput(code: String, change: (String) -> Unit, error: String?, enabled: Boolean) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(code, { change(it.filter { char -> char in '0'..'9' }.take(6)) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.semantics { contentDescription = "Código de recuperação, 6 dígitos" },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        decorationBox = { inner ->
            Box {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val active = focused && index == code.length.coerceAtMost(5)
                        Box(Modifier.weight(1f).height(56.dp).background(Color.White, RoundedCornerShape(12.dp))
                            .border(if (active) 2.dp else 1.dp, if (error != null) NovaError else if (active) NovaPrimary else NovaOutline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(code.getOrNull(index)?.toString() ?: "•", style = NovaType.ScreenTitle)
                        }
                    }
                }
                Box(Modifier.size(1.dp)) { inner() }
            }
        })
    error?.let { Text(it, Modifier.padding(top = 4.dp), color = NovaError, style = NovaType.Caption) }
}

@Composable
private fun AuthFeedback(state: AuthState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.error?.let { NovaNotice(it, state.requestId) }
        if (state.retrySeconds > 0) Text("Tente novamente em ${state.retrySeconds}s", style = NovaType.Body)
        if (state.busy) {
            if (state.page == AuthPage.Recovery) Row(Modifier.fillMaxWidth().background(NovaPrimarySoft, RoundedCornerShape(12.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(32.dp), color = NovaPrimary, strokeWidth = 3.dp)
                Text("Preparando uma forma segura de recuperação...", style = NovaType.Body, color = NovaHeader)
            } else NovaLoading()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AuthField(
    label: String, value: String, change: (String) -> Unit, error: String?, enabled: Boolean,
    secret: Boolean = false, keyboard: KeyboardType = KeyboardType.Text, placeholder: String = "",
    submit: (() -> Unit)? = null, hint: String? = null,
) {
    val input = remember { TextFieldState(initialText = value) }
    LaunchedEffect(input) { snapshotFlow { input.text.toString() }.collect(change) }
    val options = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard, imeAction = if (submit != null) ImeAction.Done else ImeAction.Next)
    val keyboardAction = KeyboardActionHandler { performDefaultAction -> if (submit != null) submit() else performDefaultAction() }
    val colors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color(0xFF79747E), focusedBorderColor = NovaPrimary,
        unfocusedLabelColor = FieldVariant, focusedLabelColor = NovaPrimary,
        unfocusedPlaceholderColor = FieldVariant, focusedPlaceholderColor = FieldVariant,
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
                    if (revealed) "Ocultar senha" else "Mostrar senha", Modifier.size(24.dp), tint = FieldVariant)
            }
        },
        keyboardOptions = options, onKeyboardAction = keyboardAction,
        textObfuscationMode = if (revealed) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
        shape = RoundedCornerShape(4.dp),
    ) else OutlinedTextField(
        state = input, modifier = Modifier.fillMaxWidth(), enabled = enabled, textStyle = textStyle,
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true), label = { Text(label) },
        placeholder = { Text(placeholder) }, isError = error != null, colors = colors,
        supportingText = if (error != null) ({ Text(error) }) else null,
        keyboardOptions = options, onKeyboardAction = keyboardAction, lineLimits = TextFieldLineLimits.SingleLine,
        shape = RoundedCornerShape(4.dp),
    )
}

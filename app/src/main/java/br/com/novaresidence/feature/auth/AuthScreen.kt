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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*

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

@Composable
private fun AuthContent(
    top: Dp = 24.dp, bottom: Dp = 20.dp, gap: Dp = 16.dp,
    footer: @Composable ColumnScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = if (maxWidth < 400.dp) 16.dp else 20.dp
        val vertical = if (maxHeight < 700.dp) 16.dp else top
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontal).padding(top = vertical, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap), content = body)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), content = footer)
        }
    }
}

@Composable
private fun WelcomeContent(state: AuthState, viewModel: AuthViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = if (maxWidth < 400.dp) 16.dp else 20.dp
        val available = maxHeight
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).heightIn(min = available)
                .padding(horizontal = horizontal, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Em telas altas a marca desce um pouco; em telas baixas o espaço some e nada é cortado.
                Spacer(Modifier.height((available * 0.12f).coerceIn(0.dp, 120.dp)))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(R.drawable.nova_logo), "Nova Residence", Modifier.size(226.dp, 170.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bem-vindo", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Text("Acompanhe informações em tempo real do seu condomínio entre outras ações", Modifier.widthIn(max = 332.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NovaButton("Entrar", { viewModel.navigate(AuthPage.Login) }, enabled = !state.busy)
                    Text("- ou acesse usando -", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    GoogleButton(state, viewModel)
                }
                AuthFeedback(state)
            }
            SignupPrompt(state, viewModel)
        }
    }
}

@Composable
private fun GoogleButton(state: AuthState, viewModel: AuthViewModel) {
    val context = LocalContext.current
    Button({ viewModel.google { googleIdToken(context) } }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy && state.retrySeconds == 0L,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        Image(painterResource(R.drawable.nova_google_account), null, Modifier.size(32.dp))
        Spacer(Modifier.width(8.dp))
        Text("Conta Google", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SignupPrompt(state: AuthState, viewModel: AuthViewModel) {
    val prompt = "Ainda não tem uma conta?"
    val action = @Composable { TextButton(onClick = { viewModel.navigate(AuthPage.Signup) }, enabled = !state.busy) { Text("Criar conta", style = MaterialTheme.typography.labelLarge) } }
    // Com fonte ampliada, texto e ação empilham para não cortar nenhum dos dois.
    if (LocalDensity.current.fontScale >= 1.3f) Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action()
    } else Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(prompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f, false))
        action()
    }
}

@Composable
private fun ErrorContent(state: AuthState, viewModel: AuthViewModel) {
    AuthContent(top = 24.dp, gap = 16.dp) {
        NovaIntroduction("Seus dados não conferem", "Confira seus dados de acesso e sua senha ou recupere seu acesso.")
        NovaNotice("Nenhuma alteração foi feita na sua conta.", state.requestId, title = "Acesso não concluído")
        NovaButton("Tentar novamente", viewModel::back, enabled = !state.busy)
        NovaButton("Recuperar acesso", { viewModel.navigate(AuthPage.Recovery) }, enabled = !state.busy, secondary = true)
        if (state.retrySeconds > 0) Text("Tente novamente em ${state.retrySeconds}s", style = MaterialTheme.typography.bodyMedium)
        Text("Se o problema continuar, fale com a administração do condomínio.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AuthForm(state: AuthState, viewModel: AuthViewModel) {
    // Senhas ficam apenas na composição, sem persistência no estado da Activity.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val canAct = !state.busy && state.retrySeconds == 0L
    val signup = state.page == AuthPage.Signup
    val recoveryConfirm = state.page == AuthPage.RecoveryConfirm
    val submit: () -> Unit = {
        when (state.page) {
            AuthPage.Login -> viewModel.login(email, password)
            AuthPage.Signup -> viewModel.signup(email, password, confirmation)
            AuthPage.Recovery -> viewModel.recover(email)
            AuthPage.RecoveryConfirm -> viewModel.confirm(code, password, confirmation)
            AuthPage.ChangePassword -> viewModel.change(current, password, confirmation)
            else -> Unit
        }
    }
    val intro = when (state.page) {
        AuthPage.Login -> "Vamos identificar você" to "Informe seus dados de acesso."
        AuthPage.Signup -> "Vamos identificar você" to "Informe seus dados para cadastro."
        AuthPage.Recovery -> "Vamos ajudar você a entrar" to "Informe seu e-mail e enviaremos as próximas instruções."
        AuthPage.RecoveryConfirm -> "Digite o código" to "Se houver uma conta com este e-mail, você receberá um código de 6 dígitos. Ele expira em 15 minutos."
        else -> "Crie uma nova senha" to "Para continuar, substitua sua senha temporária por uma senha pessoal."
    }
    AuthContent(top = 24.dp, gap = 0.dp, footer = {
        when (state.page) {
            AuthPage.RecoveryConfirm -> Text("Confira também a caixa de spam ou mensagens bloqueadas.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            AuthPage.ChangePassword -> NovaButton("Sair", viewModel::logout, enabled = !state.busy, quiet = true)
            else -> Unit
        }
    }) {
        NovaIntroduction(intro.first, intro.second)
        if (recoveryConfirm) {
            Spacer(Modifier.height(16.dp))
            RecoveryCodeInput(code, { code = it }, state.fields["code"], !state.busy)
        }
        // O campo Material reserva 8 dp acima da borda para a legenda flutuante.
        Spacer(Modifier.height(if (signup) 8.dp else 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.page in setOf(AuthPage.Login, AuthPage.Signup, AuthPage.Recovery)) {
                AuthField("E-mail", email, { email = it }, state.fields["email"], !state.busy, keyboard = KeyboardType.Email,
                    placeholder = if (signup) "Digite um contato válido" else "Digite seu e-mail", submit = if (state.page == AuthPage.Recovery) submit else null)
            }
            if (state.page == AuthPage.ChangePassword) AuthField("Senha atual", current, { current = it }, state.fields["currentPassword"], !state.busy, secret = true)
            if (state.page != AuthPage.Recovery) {
                val isNew = state.page != AuthPage.Login
                AuthField(if (signup) "Crie uma senha" else if (isNew) "Nova senha" else "Senha", password, { password = it }, state.fields[if (isNew) "newPassword" else "password"] ?: state.fields["password"], !state.busy,
                    secret = true, placeholder = if (isNew) "Mínimo de 8 caracteres" else "Digite sua senha", submit = if (state.page == AuthPage.Login) submit else null)
            }
            if (state.page in setOf(AuthPage.Signup, AuthPage.RecoveryConfirm, AuthPage.ChangePassword)) {
                AuthField(if (signup) "Confirme sua senha" else "Confirme a nova senha", confirmation, { confirmation = it }, state.fields["confirmation"], !state.busy, secret = true, placeholder = if (signup) "Mínimo de 8 caracteres" else "", submit = submit)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (signup) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.nova_info), null, Modifier.size(18.dp))
                Text("A validação do cadastro pode levar até 24h.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
        }
        NovaButton(when (state.page) {
            AuthPage.Login -> "Entrar"
            AuthPage.Signup -> "Continuar"
            AuthPage.Recovery -> "Enviar instruções"
            AuthPage.RecoveryConfirm -> "Validar e continuar"
            else -> "Salvar nova senha"
        }, submit, enabled = canAct)
        if (state.page == AuthPage.Login) {
            Spacer(Modifier.height(8.dp))
            NovaButton("Esqueci minha senha", { viewModel.navigate(AuthPage.Recovery) }, enabled = !state.busy, quiet = true)
        }
        if (recoveryConfirm) {
            Spacer(Modifier.height(16.dp))
            val remaining = state.retrySeconds
            NovaButton(if (remaining > 0) "Reenviar código em %02d:%02d".format(remaining / 60, remaining % 60) else "Reenviar código", { viewModel.recover(state.recoveryEmail) }, enabled = canAct, secondary = true)
        }
        if (state.busy || state.error != null || state.retrySeconds > 0) {
            Spacer(Modifier.height(20.dp))
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
                        Box(Modifier.weight(1f).heightIn(min = 56.dp).background(Color.White, RoundedCornerShape(12.dp))
                            .border(if (active) 2.dp else 1.dp, if (error != null) MaterialTheme.colorScheme.error else if (active) NovaPrimary else NovaOutline, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(code.getOrNull(index)?.toString() ?: "•", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
                Box(Modifier.size(1.dp)) { inner() }
            }
        })
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun AuthFeedback(state: AuthState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.error?.let { NovaNotice(it, state.requestId) }
        if (state.retrySeconds > 0) Text("Tente novamente em ${state.retrySeconds}s", style = MaterialTheme.typography.bodyMedium)
        if (state.busy) {
            if (state.page == AuthPage.Recovery) Row(Modifier.fillMaxWidth().background(NovaSecondaryContainer, RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(32.dp), color = NovaPrimary, strokeWidth = 3.dp)
                Text("Preparando uma forma segura de recuperação...", style = MaterialTheme.typography.bodyMedium, color = NovaHeader)
            } else NovaLoading()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AuthField(
    label: String, value: String, change: (String) -> Unit, error: String?, enabled: Boolean,
    secret: Boolean = false, keyboard: KeyboardType = KeyboardType.Text, placeholder: String = "",
    submit: (() -> Unit)? = null,
) {
    val input = remember { TextFieldState(initialText = value) }
    LaunchedEffect(input) { snapshotFlow { input.text.toString() }.collect(change) }
    val options = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard, imeAction = if (submit != null) ImeAction.Done else ImeAction.Next)
    val keyboardAction = KeyboardActionHandler { performDefaultAction -> if (submit != null) submit() else performDefaultAction() }
    if (secret) OutlinedSecureTextField(
        state = input, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true), label = { Text(label) },
        placeholder = { Text(placeholder) }, isError = error != null,
        supportingText = if (error != null) ({ Text(error) }) else null,
        keyboardOptions = options, onKeyboardAction = keyboardAction, textObfuscationMode = TextObfuscationMode.Hidden,
        shape = RoundedCornerShape(10.dp),
    ) else OutlinedTextField(
        state = input, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true), label = { Text(label) },
        placeholder = { Text(placeholder) }, isError = error != null,
        supportingText = if (error != null) ({ Text(error) }) else null,
        keyboardOptions = options, onKeyboardAction = keyboardAction, lineLimits = TextFieldLineLimits.SingleLine,
        shape = RoundedCornerShape(10.dp),
    )
}

package br.com.novaresidence

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.test.platform.app.InstrumentationRegistry
import br.com.novaresidence.core.network.*
import br.com.novaresidence.core.session.*
import br.com.novaresidence.core.ui.NovaResidenceTheme
import br.com.novaresidence.core.ui.NovaSplash
import br.com.novaresidence.feature.auth.*
import br.com.novaresidence.feature.residence.*
import br.com.novaresidence.feature.exhaust.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import java.io.File

class ScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun edgeToEdge() { compose.activityRule.scenario.onActivity { it.enableEdgeToEdge() } }
    private fun auth(): AuthViewModel {
        val store = object : SessionStore {
            override fun read(): SessionData? = null
            override fun write(session: SessionData) = Unit
            override fun clear() = Unit
        }
        // These UI tests never contact a live backend.
        val coordinator = SessionCoordinator(store) { error("No network in visual tests") }
        return AuthViewModel(AuthRepository(ApiClient(ApiTransport("http://127.0.0.1:1"), coordinator)))
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = context.getExternalFilesDir("screenshots")!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            val bitmap = if (name.endsWith("_dialog")) InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                else compose.onRoot().captureToImage().asAndroidBitmap()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun authScreensAndValidation() {
        val model = auth()
        compose.setContent { val state by model.state.collectAsState(); NovaResidenceTheme { AuthScreen(state, model) } }
        compose.onNodeWithText("Bem-vindo").assertIsDisplayed()
        compose.onNodeWithText("Conta Google").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Criar conta").performScrollTo().assertIsDisplayed()
        capture("welcome")
        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("E-mail").assertIsDisplayed()
        compose.onNodeWithText("Conta Google").assertDoesNotExist()
        compose.onNodeWithText("Criar conta").assertDoesNotExist()
        compose.onNodeWithText("Esqueci minha senha").performScrollTo().assertIsDisplayed()
        capture("login")
        compose.onNodeWithText("Entrar").performScrollTo()
        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("Informe um e-mail válido.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Informe sua senha.").performScrollTo().assertIsDisplayed()
        capture("login_validation")
        compose.runOnIdle { model.navigate(AuthPage.Signup) }
        capture("signup")
        compose.onNodeWithText("CPF").assertDoesNotExist()
        compose.onNodeWithText("Confirme sua senha").assertIsDisplayed()
        compose.onNodeWithText("A validação do cadastro pode levar até 24h.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { model.signup("novo@example.com", "senha1234", "diferente") }
        compose.onNodeWithText("As senhas não conferem.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertFalse(model.state.value.busy) }
        compose.runOnIdle { model.navigate(AuthPage.Recovery) }
        capture("recovery")
        compose.runOnIdle { model.navigate(AuthPage.RecoveryConfirm) }
        compose.onNodeWithText("Se houver uma conta com este e-mail, você receberá um código de 6 dígitos. Ele expira em 15 minutos.").assertIsDisplayed()
        capture("recovery_confirm")
        compose.runOnIdle { model.navigate(AuthPage.ChangePassword) }
        compose.onNodeWithText("Senha atual").assertIsDisplayed()
        capture("password_change")
        compose.onNodeWithText("Validar conta").assertIsDisplayed()
        compose.runOnIdle { model.navigate(AuthPage.Error); model.showMessage("E-mail ou senha incorretos.") }
        compose.onNodeWithText("Confira seus dados de acesso e sua senha ou recupere seu acesso.").assertIsDisplayed()
        capture("auth_error")
    }

    @Test fun busyAndRateLimitedScreensDisableSubmission() {
        val model = auth()
        var state by mutableStateOf(AuthState(page = AuthPage.Welcome, busy = true))
        compose.setContent { NovaResidenceTheme { AuthScreen(state, model) } }
        compose.onNodeWithText("Conta Google").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(page = AuthPage.Login) }
        compose.onNodeWithText("Entrar").assertIsNotEnabled()
        capture("login_loading")
        compose.runOnIdle { state = state.copy(busy = false, retrySeconds = 42, error = "Muitas tentativas. Aguarde para tentar novamente.") }
        compose.onNodeWithText("Entrar").assertIsNotEnabled()
        compose.onNodeWithText("Tente novamente em 42s").performScrollTo().assertIsDisplayed()
        capture("login_rate_limit")
        compose.runOnIdle { state = AuthState(page = AuthPage.Recovery, busy = true) }
        compose.onNodeWithText("Enviar instruções").assertIsNotEnabled()
        compose.onNodeWithText("Preparando uma forma segura de recuperação...").performScrollTo().assertIsDisplayed()
        capture("recovery_loading")
    }

    @Test fun realResidenceStatesAndUnitSelection() {
        val user = AuthUser("test", "resident@example.com", "morador", false)
        val first = ResidenceUnit(1, "B", "84", "B-84", true, false, true, false, null)
        val second = first.copy(sequencia = 2, quadra = "A", label = "A-84", blocked = true, blockedReason = "nao-morador")
        var state by mutableStateOf(ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(first, second)), 1))
        compose.setContent { NovaResidenceTheme { ResidenceScreen(state, user, { state = state.copy(selectedUnit = it) }, {}, {}, false,
            exhaustStatus = ExhaustHomeStatus("Disponível para utilização.", false)) } }
        compose.onNodeWithText("Olá, Morador").assertIsDisplayed()
        compose.onNodeWithText("Apto. 84 - Torre B").assertIsDisplayed()
        compose.onNodeWithText("Disponível para utilização.").assertIsDisplayed()
        compose.onNodeWithText("Nenhuma mensagem nova").assertDoesNotExist()
        compose.onNodeWithText("Manutenção programada").assertDoesNotExist()
        capture("home")
        compose.onNodeWithText("Apto. 84 - Torre B").performClick()
        compose.onNodeWithText("Selecione a unidade que deseja usar no aplicativo.").assertIsDisplayed()
        capture("unit_selection")
        compose.onNodeWithText("Torre A · Apto. 84").performClick()
        compose.runOnIdle { assertEquals(2, state.selectedUnit) }
        compose.onNodeWithText("Você não consta como morador desta unidade.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Você não consta como morador desta unidade.").assertIsDisplayed()
        capture("home_unit_blocked")
        compose.onNodeWithText("Configurações").performClick()
        capture("settings")
        compose.onNodeWithText("Início").performClick()
        compose.runOnIdle { state = ResidenceState(Residence("pendente", true, null, emptyList())) }
        compose.onNodeWithText("Cadastro em análise pela administração").assertIsDisplayed()
        compose.onNodeWithText("Central de Incêndio").assertDoesNotExist()
        capture("home_pending")
        compose.runOnIdle { state = ResidenceState(Residence("sem-unidade", true, null, emptyList())) }
        capture("home_no_unit")
        compose.runOnIdle { state = ResidenceState(error = "Não foi possível conectar. Verifique sua conexão e tente novamente.") }
        capture("home_error")
    }

    @Test fun encryptedSessionSurvivesRecreationWithoutPlaintext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.createDeviceProtectedStorageContext()
        val store = EncryptedSessionStore(context)
        val value = SessionData("distinct-access-token", "distinct-refresh-token", "bearer", 900, 1790636017, AuthUser("test", null, null, false))
        try {
            store.write(value)
            val bytes = File(context.filesDir, "session.bin").readBytes().decodeToString()
            assertFalse(bytes.contains(value.accessToken))
            assertFalse(bytes.contains(value.refreshToken))
            assertEquals(value, EncryptedSessionStore(context).read())
            store.clear()
            assertNull(store.read())
        } finally { store.clear() }
    }

    @Test fun splashUsesOfficialBrand() {
        compose.setContent { NovaResidenceTheme { NovaSplash() } }
        compose.onNodeWithContentDescription("Nova Residence").assertIsDisplayed()
        capture("splash")
    }

    @Test fun exhaustScreensUseServerStatesAndDisableUnavailableActions() {
        val unit = ResidenceUnit(1, "B", "84", "B-84", true, false, true, false, null)
        val residence = ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(unit)), 1)
        val off = Exhaust("B4", "B", 4, false, null, "executado", true)
        var state by mutableStateOf(ExhaustState(off, loading = false, nowMillis = 1000))
        var commands = 0
        var restarts = 0
        compose.setContent {
            NovaResidenceTheme {
                ResidenceScreen(residence, AuthUser("test", null, "morador", false), {}, {}, {}, false,
                    exhaustStatus = ExhaustHomeStatus(state.homeSummary(), state.phase == ExhaustPhase.Running)) { current, back, selectUnit ->
                    ExhaustScreen(state, current?.homeLabel(), null, { commands++ }, { restarts++ }, {}, back, selectUnit)
                }
            }
        }
        compose.onAllNodesWithText("Exaustor").onLast().performClick()
        compose.onNodeWithText("Torre B · Prumada 4").assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").performScrollTo().assertIsEnabled()
        capture("exhaust_off")
        compose.runOnIdle { state = state.copy(sending = true) }
        compose.onNodeWithText("Ligando ...").performScrollTo().assertIsNotEnabled()
        capture("exhaust_starting")
        compose.runOnIdle { state = ExhaustState(off.copy(on = true, expiresAt = 10_801_000, command = "ligar"), loading = false, nowMillis = 1000) }
        compose.onNodeWithText("03:00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").assertDoesNotExist()
        compose.onNodeWithText("Reiniciar tempo").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, restarts) }
        capture("exhaust_running")
        compose.runOnIdle { state = state.copy(nowMillis = 10_802_000) }
        compose.onNodeWithText("Desligando").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reiniciar tempo").assertDoesNotExist()
        capture("exhaust_finishing")
        compose.runOnIdle { state = ExhaustState(off.copy(processStatus = "iniciando", command = "desligar"), loading = false) }
        compose.onNodeWithText("Desligando").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ligando").assertDoesNotExist()
        compose.runOnIdle { state = ExhaustState(off.copy(moduleOnline = false), loading = false) }
        compose.onNodeWithText("Offline").assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").assertDoesNotExist()
        capture("exhaust_offline")
        compose.runOnIdle { state = ExhaustState(off.copy(maintenance = true), loading = false) }
        compose.onNodeWithText("Manutenção").assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").assertDoesNotExist()
        capture("exhaust_maintenance")
        compose.runOnIdle { state = ExhaustState(off.copy(processStatus = "erro"), loading = false) }
        compose.onNodeWithText("Falha na ativação").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performScrollTo().assertIsEnabled()
        capture("exhaust_error")
        assertEquals(0, commands)
    }

    @Test fun settingsUnitSelectionAndLanguage() {
        val first = ResidenceUnit(1, "B", "84", "B-84", true, false, true, false, null)
        val second = first.copy(sequencia = 2, quadra = "A", label = "A-84")
        var state by mutableStateOf(ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(first, second)), 1))
        compose.setContent { NovaResidenceTheme {
            ResidenceScreen(state, AuthUser("test", "resident@example.com", "morador", false), { state = state.copy(selectedUnit = it) }, {}, {}, false)
        } }
        compose.onNodeWithText("Configurações").performClick()
        compose.onNodeWithText("E-mail: resident@example.com").assertIsDisplayed()
        compose.onNodeWithText("Código Freedom: 91").assertIsDisplayed()
        compose.onNodeWithText("Desconectar").performScrollTo().assertIsEnabled()
        capture("settings")
        compose.onNodeWithText("Minhas unidades").performScrollTo().performClick()
        compose.onNodeWithText("Unidade selecionada").assertIsDisplayed()
        capture("my_units")
        compose.onNodeWithText("Torre A · Apto. 84").performClick()
        compose.runOnIdle { assertEquals(2, state.selectedUnit) }
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Idioma").performScrollTo().performClick()
        compose.onNodeWithText("Português (Brasil)").assertIsDisplayed()
        compose.onNodeWithText("English").assertIsNotEnabled()
        compose.onNodeWithText("Salvar idioma").performScrollTo().assertIsEnabled()
        capture("language_selection")
    }
}

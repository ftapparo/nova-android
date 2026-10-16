package br.com.novaresidence

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.feature.auth.*
import br.com.novaresidence.feature.residence.*
import br.com.novaresidence.feature.exhaust.*
import br.com.novaresidence.feature.fire.*
import br.com.novaresidence.feature.message.*
import br.com.novaresidence.feature.legal.*
import br.com.novaresidence.feature.notification.*
import br.com.novaresidence.core.push.PushStatus
import br.com.novaresidence.core.push.NotificationPrompt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import java.io.File

class ScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun edgeToEdge() {
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            // Capturas e gestos precisam de uma janela desenhada; o J4 pode apagar a tela durante a suíte.
            it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    private fun auth(): AuthViewModel {
        val store = object : SessionStore {
            override fun read(): SessionData? = null
            override fun write(session: SessionData) = Unit
            override fun clear() = Unit
        }
        // These UI tests never contact a live backend.
        val coordinator = SessionCoordinator(store) { error("No network in visual tests") }
        return AuthViewModel(AuthRepository(ApiClient(ApiTransport("http://127.0.0.1:1"), coordinator))) { "1.0" }
    }
    /** Texto do recurso no idioma do aparelho: o teste não quebra quando a redação é revisada. */
    private fun text(id: Int, vararg args: Any): String = compose.activity.getString(id, *args)

    private fun capture(name: String) {
        waitForContent()
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
    private fun waitForContent() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }
    private fun tab(label: String) = compose.onNode(hasText(label) and SemanticsMatcher.expectValue(
        androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))

    @Test fun authScreensAndValidation() {
        val model = auth()
        compose.setContent { val state by model.state.collectAsState(); NovaResidenceTheme { AuthScreen(state, model) {} } }
        compose.onNodeWithText("Bem-vindo").assertIsDisplayed()
        compose.onNodeWithText("ou continue com").assertIsDisplayed()
        compose.onNodeWithText("Google").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Criar conta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.legal_continue_agreement, text(R.string.legal_terms), text(R.string.legal_privacy))).assertExists()
        capture("02_welcome")
        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("E-mail").assertIsDisplayed()
        compose.onNodeWithText("Google").assertDoesNotExist()
        compose.onNodeWithText("Esqueci minha senha").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Mostrar senha").assertIsDisplayed()
        capture("03_login")
        compose.onNodeWithText("Entrar").performScrollTo()
        compose.onNodeWithText("Entrar").performClick()
        compose.onNodeWithText("Informe um e-mail válido.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Informe sua senha.").performScrollTo().assertIsDisplayed()
        capture("03_login_validation")
        compose.runOnIdle { model.navigate(AuthPage.Signup) }
        compose.onNodeWithText("Use pelo menos 8 caracteres.").assertIsDisplayed()
        compose.onNodeWithText("Confirme sua senha").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar senha").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Ocultar senha").assertIsDisplayed()
        capture("04_signup")
        compose.runOnIdle { model.signup("invalido", "senha1234") }
        compose.onNodeWithText("Informe um e-mail válido.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertFalse(model.state.value.busy) }
        compose.runOnIdle { model.navigate(AuthPage.Recovery) }
        capture("06_recovery")
        compose.runOnIdle { model.navigate(AuthPage.RecoveryConfirm) }
        compose.onNodeWithText(text(R.string.auth_intro_code)).assertIsDisplayed()
        compose.onNodeWithText("Validar conta").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.auth_check_spam)).performScrollTo().assertIsDisplayed()
        capture("05_recovery_confirm")
        compose.runOnIdle { model.navigate(AuthPage.ChangePassword) }
        compose.onNodeWithText("Senha atual").assertIsDisplayed()
        capture("password_change")
        compose.runOnIdle { model.navigate(AuthPage.Error); model.showMessage(uiText(R.string.error_unauthorized)) }
        compose.onNodeWithText(text(R.string.auth_error_body)).assertIsDisplayed()
        capture("07_auth_error")
    }

    @Test fun busyAndRateLimitedScreensDisableSubmission() {
        val model = auth()
        var state by mutableStateOf(AuthState(page = AuthPage.Welcome, busy = true))
        compose.setContent { NovaResidenceTheme { AuthScreen(state, model) {} } }
        compose.onNodeWithText("Google").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(page = AuthPage.Login) }
        compose.onNodeWithText("Entrar").assertIsNotEnabled()
        capture("login_loading")
        compose.runOnIdle { state = state.copy(busy = false, retrySeconds = 42, error = uiText(R.string.error_rate_limited)) }
        compose.onNodeWithText("Entrar").assertIsNotEnabled()
        compose.onNodeWithText("Tente novamente em 42s").performScrollTo().assertIsDisplayed()
        capture("login_rate_limit")
        compose.runOnIdle { state = AuthState(page = AuthPage.Recovery, busy = true) }
        compose.onNodeWithText("Enviar instruções").assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.auth_preparing_recovery)).performScrollTo().assertIsDisplayed()
        capture("recovery_loading")
    }

    @Test fun realResidenceStatesAndUnitSelection() {
        val user = AuthUser("test", "resident@example.com", "morador", false)
        val first = ResidenceUnit(1, "B", "84", "B-84", true, false, true, false, null)
        val second = first.copy(sequencia = 2, quadra = "A", label = "A-84", blocked = true, blockedReason = "nao-morador")
        var state by mutableStateOf(ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(first, second)), 1))
        compose.setContent { NovaResidenceTheme { ResidenceScreen(state, user, { state = state.copy(selectedUnit = it) }, {}, {}, false,
            exhaustStatus = ServiceStatus(uiText(R.string.exhaust_home_off)),
            messagesContent = { initial, back -> MessagesFeature(MessageState(list = emptyList()), initial, back, {}, {}) }) } }
        compose.onNodeWithText("Olá, Morador!").assertIsDisplayed()
        compose.onNodeWithText("Apto. 84 - Torre B").assertIsDisplayed()
        compose.onNodeWithText("Serviços do condomínio").assertIsDisplayed()
        compose.onNodeWithText("Disponível para utilização.").assertIsDisplayed()
        compose.onNodeWithText("Nenhuma mensagem nova").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Manutenção programada").assertDoesNotExist()
        tab("Perfil").assertDoesNotExist()
        capture("08a_home")
        compose.onNodeWithContentDescription("Mensagens").performClick()
        compose.onNodeWithText("Nenhuma mensagem no momento.").assertIsDisplayed()
        tab("Alarme").assertDoesNotExist()
        capture("17_messages")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Apto. 84 - Torre B").performClick()
        compose.onNodeWithText("Selecione a unidade que deseja usar no aplicativo.").assertIsDisplayed()
        capture("10_unit_selection")
        compose.onNodeWithText("Torre A · Apto. 84").performClick()
        compose.runOnIdle { assertEquals(2, state.selectedUnit) }
        compose.onNodeWithText("Você não consta como morador desta unidade.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Você não consta como morador desta unidade.").assertIsDisplayed()
        capture("home_unit_blocked")
        compose.runOnIdle { state = ResidenceState(Residence("pendente", true, null, emptyList())) }
        compose.onNodeWithText("Cadastro em análise pela administração").assertIsDisplayed()
        compose.onNodeWithText("Central de Incêndio").assertDoesNotExist()
        compose.onNodeWithText(text(R.string.home_news)).assertDoesNotExist()
        capture("home_pending")
        compose.runOnIdle { state = ResidenceState(Residence("sem-unidade", true, null, emptyList())) }
        capture("home_no_unit")
        compose.runOnIdle { state = ResidenceState(error = uiText(R.string.error_network)) }
        capture("home_error")
    }

    @Test fun messagesCarouselBadgeListAndDetail() {
        // Comunicados fictícios: este teste não consulta a API.
        val now = java.time.Instant.now()
        val pump = CondoMessage(1, "manutencao", "Manutenção programada", "Manutenção da bomba de recalque hoje à tarde",
            "A equipe técnica realizará a troca de peças da bomba de recalque entre 14h e 16h.", now.toString())
        val garage = CondoMessage(2, "urgente", null, "Interdição temporária da garagem", "A garagem ficará fechada.", now.minusSeconds(86_400).toString(), read = true)
        var state by mutableStateOf(MessageState(latest = listOf(pump, garage), unreadCount = 1, list = listOf(pump, garage)))
        val read = mutableListOf<Long>()
        val unit = ResidenceUnit(1, "A", "124", "A-124", true, false, true, false, null)
        compose.setContent { NovaResidenceTheme {
            ResidenceScreen(ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(unit)), 1),
                AuthUser("test", null, "morador", false), {}, {}, {}, false,
                unreadMessages = state.unreadCount,
                homeMessages = { open -> MessageCarousel(state) { open(it.id) } },
                messagesContent = { initial, back -> MessagesFeature(state, initial, back, { unread ->
                    state = state.copy(unreadOnly = unread, list = if (unread) listOf(pump) else listOf(pump, garage))
                }, { read += it.id; state = state.copy(unreadCount = 0) }) })
        } }
        compose.onNodeWithText("Manutenção programada").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.resources.getQuantityString(R.plurals.home_messages_unread, 1, 1)).assertIsDisplayed()
        capture("08d_home_messages")
        compose.onAllNodesWithText("Ver mais")[0].performScrollTo().performClick()
        compose.onNodeWithText("Mensagem").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(1L), read) }
        capture("18_message_detail")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Olá, Morador!").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mensagens").performClick()
        compose.onNodeWithText("Interdição temporária da garagem").assertIsDisplayed()
        compose.onNodeWithText("Urgente").assertIsDisplayed()
        capture("17_messages")
        compose.onNodeWithText("Não lidas").performClick()
        compose.onNodeWithText("Interdição temporária da garagem").assertDoesNotExist()
        compose.onNodeWithText("Manutenção da bomba de recalque hoje à tarde").performClick()
        compose.onNodeWithText("Mensagem").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Todas").assertIsDisplayed()
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
        capture("01_splash")
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
                    exhaustStatus = ServiceStatus(state.homeSummary())) { current, selectUnit, _ ->
                    ExhaustScreen(state, current?.cardLabel(), null, { commands++ }, { restarts++ }, {}, selectUnit)
                }
            }
        }
        waitForContent()
        tab("Exaustor").performClick()
        compose.onNodeWithText("Apto. 84 · Torre B").assertIsDisplayed()
        compose.onNodeWithText("Disponível").assertIsDisplayed()
        compose.onNodeWithText("Pronto para utilização.").assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").performScrollTo().assertIsEnabled()
        capture("19_exhaust_off")
        compose.runOnIdle { state = state.copy(sending = true) }
        compose.onNodeWithText("Ligando...").performScrollTo().assertIsNotEnabled()
        capture("20_exhaust_starting")
        compose.runOnIdle { state = ExhaustState(off.copy(on = true, expiresAt = 10_801_000, command = "ligar"), loading = false, nowMillis = 1000) }
        compose.onNodeWithText("Tempo restante · 03:00:00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").assertDoesNotExist()
        compose.onNodeWithText("Reiniciar tempo").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, restarts) }
        capture("21_exhaust_running")
        compose.runOnIdle { state = state.copy(nowMillis = 10_802_000) }
        compose.onNodeWithText("Desligando").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reiniciar tempo").assertDoesNotExist()
        capture("22_exhaust_finishing")
        compose.runOnIdle { state = ExhaustState(off.copy(processStatus = "iniciando", command = "desligar"), loading = false) }
        compose.onNodeWithText("Desligando").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ligando").assertDoesNotExist()
        compose.runOnIdle { state = ExhaustState(off.copy(moduleOnline = false), loading = false) }
        compose.onNodeWithText("Não foi possível se comunicar com o exaustor.").assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").assertDoesNotExist()
        capture("23_exhaust_offline")
        compose.runOnIdle { state = ExhaustState(off.copy(maintenance = true), loading = false) }
        compose.onNodeWithText("Manutenção").assertIsDisplayed()
        compose.onNodeWithText("Em manutenção").assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").assertDoesNotExist()
        capture("24_exhaust_maintenance")
        compose.runOnIdle { state = ExhaustState(off.copy(processStatus = "erro"), loading = false) }
        compose.onNodeWithText("Falha na ativação").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performScrollTo().assertIsEnabled()
        capture("exhaust_error")
        assertEquals(0, commands)
    }

    @Test fun exhaustLocationStatesOfferTheRightAction() {
        val unit = ResidenceUnit(1, "B", "84", "B-84", true, false, true, false, null)
        val residence = ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(unit)), 1)
        val off = ExhaustState(Exhaust("B4", "B", 4, false, null, "executado", true), loading = false, nowMillis = 1000)
        var state by mutableStateOf(off.copy(sending = true, locating = true))
        var blocked by mutableStateOf(false)
        var commands = 0
        val resolved = mutableListOf<LocationIssue>()
        compose.setContent {
            NovaResidenceTheme {
                ResidenceScreen(residence, AuthUser("test", null, "morador", false), {}, {}, {}, false,
                    exhaustStatus = ServiceStatus(state.homeSummary())) { current, selectUnit, _ ->
                    ExhaustScreen(state, current?.cardLabel(), null, { commands++ }, {}, {}, selectUnit, blocked) { resolved += it }
                }
            }
        }
        waitForContent()
        tab("Exaustor").performClick()
        compose.onNodeWithText("Verificando localização...").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Ligando").assertDoesNotExist()
        capture("25_exhaust_locating")
        val cases = listOf(
            Triple(LocationIssue.Permission, "Localização necessária", "Permitir localização"),
            Triple(LocationIssue.Approximate, "Localização precisa necessária", "Permitir localização precisa"),
            Triple(LocationIssue.Disabled, "Localização desligada", "Ativar localização"),
        )
        for ((issue, title, action) in cases) {
            compose.runOnIdle { state = off.copy(location = issue) }
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithText(action).performScrollTo().assertIsEnabled().performClick()
            capture("25_exhaust_location_${issue.name.lowercase()}")
        }
        compose.runOnIdle { assertEquals(cases.map { it.first }, resolved); blocked = true; state = off.copy(location = LocationIssue.Permission) }
        compose.onNodeWithText("Localização não permitida").assertIsDisplayed()
        compose.onNodeWithText("Abrir configurações").performScrollTo().assertIsEnabled()
        capture("25_exhaust_location_denied")
        compose.runOnIdle { state = off.copy(location = LocationIssue.Outside) }
        compose.onNodeWithText("Fora do condomínio").assertIsDisplayed()
        compose.onNodeWithText("Ligar exaustor").performScrollTo().assertIsEnabled()
        capture("25_exhaust_location_outside")
        compose.runOnIdle { state = off.copy(location = LocationIssue.Inaccurate) }
        compose.onNodeWithText("Localização imprecisa").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performScrollTo().assertIsEnabled()
        capture("25_exhaust_location_inaccurate")
        compose.runOnIdle { state = off.copy(location = LocationIssue.Mock) }
        compose.onNodeWithText("Localização simulada").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        capture("25_exhaust_location_mock")
        compose.runOnIdle { assertEquals(1, commands) }
        // Aviso só substitui estados com botão de comando; offline continua prevalecendo.
        compose.runOnIdle { state = off.copy(exhaust = off.exhaust!!.copy(moduleOnline = false), location = LocationIssue.Outside) }
        compose.onNodeWithText("Fora do condomínio").assertDoesNotExist()
    }

    @Test fun settingsUnitSelectionLanguageAndPrivacy() {
        val first = ResidenceUnit(1, "B", "84", "B-84", true, false, true, false, null)
        val second = first.copy(sequencia = 2, quadra = "A", label = "A-84")
        var state by mutableStateOf(ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste", "•••.456.789-••"), listOf(first, second)), 1))
        var deletions = 0
        // Sem WebView nem rede: o documento aparece no estado de falha, com o título e a ação de tentar de novo.
        val offline = LegalState(error = uiText(R.string.legal_load_failed))
        compose.setContent { NovaResidenceTheme {
            ResidenceScreen(state, AuthUser("test", "resident@example.com", "morador", false), { state = state.copy(selectedUnit = it) }, {}, {}, false,
                legalContent = { terms, back -> LegalDocumentScreen(offline, if (terms) TermsDocument else PrivacyDocument, back) {} },
                deleteAccount = { deletions++ })
        } }
        compose.onNodeWithContentDescription("Configurações").performClick()
        compose.onNodeWithText("resident@example.com").assertIsDisplayed()
        compose.onNodeWithText("Código Freedom: 91").assertIsDisplayed()
        compose.onNodeWithText("Desconectar").performScrollTo().assertIsEnabled()
        capture("09_settings")
        compose.onNodeWithText("Minhas unidades").performScrollTo().performClick()
        compose.onNodeWithText("Unidade selecionada").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.settings_links_info)).assertIsDisplayed()
        compose.onNodeWithText("Torre A · Apto. 84").performClick()
        compose.runOnIdle { assertEquals(2, state.selectedUnit) }
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Idioma").performScrollTo().performClick()
        compose.onNodeWithText("Português (Brasil)").assertIsDisplayed()
        // Os três idiomas têm tradução completa e podem ser escolhidos.
        compose.onNodeWithText("English").assertIsEnabled()
        compose.onNodeWithText("Español").assertIsEnabled()
        compose.onNodeWithText("Salvar idioma").performScrollTo().assertIsEnabled()
        capture("11_language_selection")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Privacidade").performScrollTo().performClick()
        capture("12_privacy")
        compose.onNodeWithText("Termos de uso").performClick()
        compose.onNodeWithText(text(R.string.legal_terms)).assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").assertIsEnabled()
        capture("13_terms")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Política de privacidade").performClick()
        compose.onNodeWithText("Política de Privacidade").assertIsDisplayed()
        capture("14_privacy_policy")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Dados pessoais").performClick()
        // Nome mascarado como na conferência do vínculo (07c).
        compose.onNodeWithText("Mo***** de Te***").assertIsDisplayed()
        compose.onNodeWithText("Torre A · Apto. 84").assertIsDisplayed()
        compose.onNodeWithText("CPF").assertIsDisplayed()
        compose.onNodeWithText("•••.456.789-••").assertIsDisplayed()
        capture("15_personal_data")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText(text(R.string.privacy_delete_account)).performClick()
        compose.onNodeWithText("Cancelar").performScrollTo().assertIsEnabled()
        capture("16_delete_account")
        compose.onNode(hasText("Excluir conta") and hasClickAction()).performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, deletions) }
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Termos de uso").assertIsDisplayed()
    }

    @Test fun fireScreensShowStatusAlarmHistoryAndDetail() {
        // Dados fictícios: este teste não consulta a API nem a central real.
        val alarmLog = CieLog("alarme:1", "alarme", 940, zone = 2, loop = 1, address = 100, deviceName = "8 ANDAR B T-B", zoneName = "TORRE B",
            deviceTypeLabel = "Sensor de Fumaça", eventType = 1, blocked = false, occurredAt = "2026-09-21T18:48:10.000Z", raw = "{\"id\": 940}")
        val failure = CieLog("falha:2", "falha", 2, deviceName = "3 ANDAR T-A", zoneName = "TORRE A", deviceTypeLabel = "Acionador Manual", occurredAt = "2026-09-20T12:00:00.000Z")
        val normal = FireState(CiePanel(online = true, connected = true, lastUpdated = System.currentTimeMillis()), CieAlarms(false), loading = false, history = listOf(alarmLog, failure))
        var state by mutableStateOf(normal)
        var notificationsReady by mutableStateOf(false)
        var permissionRequests = 0
        val unit = ResidenceUnit(1, "A", "124", "A-124", true, false, true, false, null)
        compose.setContent {
            NovaResidenceTheme {
                ResidenceScreen(ResidenceState(Residence("ativo", false, ResidencePerson(91, "MORADOR DE TESTE"), listOf(unit)), 1),
                    AuthUser("test", null, "morador", false), {}, {}, {}, false,
                    fireStatus = ServiceStatus(state.homeSummary(), state.homeAlarmPlace(), if (state.status == FireStatus.Alarm) ServiceTone.Alarm else ServiceTone.Neutral),
                    fireContent = { _, _, fullScreen -> FireFeature(state, {}, {}, fullScreen,
                        notificationsReady = notificationsReady, enableNotifications = { permissionRequests++ }) })
            }
        }
        compose.onNodeWithText("Nenhum alarme de incêndio ativo.").assertIsDisplayed()
        tab("Alarme").performClick()
        compose.onNodeWithText(text(R.string.fire_normal_title)).assertIsDisplayed()
        compose.onNodeWithText("Central online").assertIsDisplayed()
        compose.onNodeWithText("Atualizado agora").performScrollTo().assertIsDisplayed()
        capture("26_fire_normal")
        compose.onNodeWithText("Ativar notificações").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, permissionRequests); notificationsReady = true }
        compose.onNodeWithText("Ativar notificações").assertDoesNotExist()
        compose.onNodeWithText("Você será avisado se o estado da central mudar.").performScrollTo().assertIsDisplayed()
        capture("26_fire_notifications_ready")
        compose.runOnIdle { state = normal.copy(alarms = CieAlarms(false, CieCounters(falha = 1)), activeEvents = listOf(failure)) }
        compose.onNodeWithText(text(R.string.fire_badge_checking)).assertIsDisplayed()
        compose.onNodeWithText("Torre A · 3º andar").performScrollTo().assertIsDisplayed()
        capture("27_fire_attention")
        compose.onNodeWithText("Ver histórico de eventos").performScrollTo().performClick()
        compose.onNodeWithText("Torre B · 8º andar").assertIsDisplayed()
        tab("Início").assertDoesNotExist()
        capture("29_fire_history")
        compose.onNodeWithText("Falhas").performClick()
        compose.onNodeWithText("Torre B · 8º andar").assertDoesNotExist()
        compose.onNodeWithText("Torre A · 3º andar").assertIsDisplayed()
        compose.onNodeWithText("Todos").performClick()
        compose.onNodeWithText("Torre B · 8º andar").performClick()
        compose.onNodeWithText("Detalhes do evento").assertIsDisplayed()
        compose.onAllNodesWithText("Evento de alarme")[0].assertIsDisplayed()
        compose.onNodeWithText("L1D100 - TORRE B").assertIsDisplayed()
        compose.onNodeWithText("Sensor de Fumaça").assertIsDisplayed()
        capture("30a_fire_event")
        compose.onNodeWithText("Dados técnicos").performScrollTo().performClick()
        compose.onNodeWithText("{\"id\": 940}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.runOnIdle { state = normal.copy(alarms = CieAlarms(true, CieCounters(alarme = 1), activeAlarms = listOf(alarmLog))) }
        compose.onNodeWithText("ALARME DE INCÊNDIO").assertIsDisplayed()
        compose.onNodeWithText("Torre B - 8º andar").assertIsDisplayed()
        compose.onNodeWithText("Orientações de segurança").performScrollTo().assertIsDisplayed()
        capture("28_fire_alarm")
        compose.onNodeWithText("Ver detalhes do evento").performScrollTo().performClick()
        compose.onAllNodesWithText("Evento de alarme")[0].assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("ALARME DE INCÊNDIO").assertIsDisplayed()
        tab("Início").performClick()
        compose.onNodeWithText("Torre B - Andar 8").assertIsDisplayed()
        capture("08c_home_fire_alarm")
    }

    @Test fun notificationPreferencesRespectPermissionAndStaffRole() {
        var status by mutableStateOf(PushStatus.Ready)
        var staff by mutableStateOf(false)
        var state by mutableStateOf(NotificationState(preferences = NotificationPreferences()))
        var permissionRequests = 0
        var retries = 0
        val changed = mutableListOf<NotificationOption>()
        compose.setContent { NovaResidenceTheme {
            NotificationScreen(state, status, staff, {}, { option, enabled ->
                changed += option
                state = state.copy(preferences = state.preferences!!.with(option, enabled))
            }, {}, { permissionRequests++ }, { retries++ }, {})
        } }
        compose.onNodeWithText(text(R.string.notification_ready)).assertIsDisplayed()
        compose.onNodeWithText("Alertas da central").assertDoesNotExist()
        capture("09a_notifications_resident")
        compose.onNodeWithText("Comunicados").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(NotificationOption.Messages), changed) }
        compose.runOnIdle { staff = true }
        compose.onNodeWithText("Alertas da central").performScrollTo().assertIsDisplayed()
        capture("09b_notifications_staff")
        compose.runOnIdle { status = PushStatus.PermissionNeeded }
        compose.onNodeWithText("Enviar notificação de teste").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Ativar notificações").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, permissionRequests) }
        capture("09c_notifications_permission")
        compose.runOnIdle { status = PushStatus.ChannelBlocked }
        compose.onNodeWithText("Abrir configurações").performScrollTo().assertIsEnabled()
        capture("09d_notifications_channel_blocked")
        compose.runOnIdle { status = PushStatus.NotRegistered }
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        capture("09e_notifications_registration")
    }

    @Test fun notificationPromptCanBeEnabledOrDismissed() {
        var dismissed by mutableStateOf(false)
        var requests = 0
        compose.setContent { NovaResidenceTheme {
            val unit = ResidenceUnit(1, "A", "124", "A-124", true, false, true, false, null)
            ResidenceScreen(ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(unit)), 1),
                AuthUser("test", null, "morador", false), {}, {}, {}, false,
                fireStatus = ServiceStatus(uiText(R.string.fire_home_normal)),
                exhaustStatus = ServiceStatus(uiText(R.string.exhaust_home_off)),
                notificationPrompt = if (!dismissed) ({ NotificationPrompt({ requests++ }, { dismissed = true }) }) else null)
        } }
        compose.onNodeWithText("Ative as notificações").assertIsDisplayed()
        capture("08e_home_notification_prompt")
        compose.onNodeWithText("Ativar notificações").performClick()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithText("Agora não").performClick()
        compose.onNodeWithText("Ative as notificações").assertDoesNotExist()
    }

    @Test fun revisedServiceScreensReflowInThreeLanguagesWithLargeText() {
        // Contextos locais: a revisão visual não muda a preferência de idioma nem a fonte do aparelho.
        val configuration = android.content.res.Configuration(compose.activity.resources.configuration)
        var language by mutableStateOf("pt")
        var largeText by mutableStateOf(false)
        var page by mutableIntStateOf(0)
        var location by mutableStateOf<LocationIssue?>(null)
        val off = Exhaust("A4", "A", 4, false, null, "executado", true)
        var commands = 0
        var tests = 0
        compose.setContent {
            val context = remember(language) {
                compose.activity.createConfigurationContext(android.content.res.Configuration(configuration).apply {
                    setLocale(java.util.Locale.forLanguageTag(language))
                })
            }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalDensity provides Density(density.density, if (largeText) 1.3f else 1f)) {
                NovaResidenceTheme {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        when (page) {
                            0 -> ExhaustScreen(ExhaustState(off, loading = false, location = location),
                                uiText(R.string.unit_card_label, "124", "A"), null, { commands++ }, {}, {}, {})
                            1 -> FireFeature(FireState(CiePanel(online = true, connected = true), CieAlarms(false), loading = false),
                                {}, {}, notificationsReady = true)
                            else -> NotificationScreen(NotificationState(preferences = NotificationPreferences()), PushStatus.Ready,
                                false, {}, { _, _ -> }, { tests++ }, {}, {}, {})
                        }
                    }
                }
            }
        }
        for (locale in listOf("pt", "en", "es")) for (large in listOf(false, true)) {
            compose.runOnIdle { language = locale; largeText = large; page = 0; location = null }
            val context = compose.activity.createConfigurationContext(android.content.res.Configuration(configuration).apply {
                setLocale(java.util.Locale.forLanguageTag(locale))
            })
            fun localized(id: Int) = context.getString(id)
            compose.onNodeWithText(localized(R.string.exhaust_turn_on)).performScrollTo().assertIsEnabled()
            capture("revised_${locale}_${if (large) 130 else 100}_exhaust")
            compose.onNodeWithText(localized(R.string.exhaust_turn_on)).performClick()
            compose.runOnIdle { location = LocationIssue.Approximate }
            compose.onNodeWithText(localized(R.string.exhaust_allow_precise_location)).performScrollTo().assertIsEnabled()
            compose.onNodeWithText(localized(R.string.exhaust_location_privacy)).performScrollTo().assertIsDisplayed()
            capture("revised_${locale}_${if (large) 130 else 100}_location")
            compose.runOnIdle { page = 1 }
            compose.onNodeWithText(localized(R.string.fire_normal_title)).assertIsDisplayed()
            compose.onNodeWithText(localized(R.string.fire_view_history)).performScrollTo().assertIsEnabled()
            capture("revised_${locale}_${if (large) 130 else 100}_fire")
            compose.runOnIdle { page = 2 }
            compose.onNodeWithText(localized(R.string.notification_send_test)).performScrollTo().assertIsEnabled()
            compose.onNodeWithText(localized(R.string.notification_delivery_note)).performScrollTo().assertIsDisplayed()
            capture("revised_${locale}_${if (large) 130 else 100}_notifications")
            compose.onNodeWithText(localized(R.string.notification_send_test)).performScrollTo().performClick()
        }
        compose.runOnIdle { assertEquals(6, commands); assertEquals(6, tests) }
    }

    @Test fun residenceLinkFormCountryPickerAndConfirmation() {
        // Sem rede: o formulário e a conferência são estados de tela; nenhum CPF real é usado.
        val store = object : SessionStore {
            override fun read(): SessionData? = null
            override fun write(session: SessionData) = Unit
            override fun clear() = Unit
        }
        val coordinator = SessionCoordinator(store) { error("No network in visual tests") }
        val model = ResidenceLinkViewModel(ResidenceRepository(ApiClient(ApiTransport("http://127.0.0.1:1"), coordinator)))
        val user = AuthUser("test", "morador@example.com", null, false)
        val unit = ResidenceUnit(97, "A", "124", "A-124", true, true, false, false, null)
        val found = SelfLinkCheck("token", SelfLinkPerson("Mo***** de Te***", "•••.982.247-••"), listOf(unit))
        var state by mutableStateOf(ResidenceLinkState())
        var left = 0
        compose.setContent { NovaResidenceTheme { ResidenceLinkScreen(state, user, model, { left++ }, {}, {}) } }
        compose.onNodeWithText(text(R.string.link_intro_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.link_cpf)).assertIsDisplayed()
        compose.onNodeWithText("🇧🇷 +55").assertIsDisplayed()
        capture("07b_link_form")
        compose.onNodeWithText("🇧🇷 +55").performClick()
        compose.onNodeWithText(text(R.string.link_country_title)).assertIsDisplayed()
        capture("07b_link_country_dialog")
        compose.onNodeWithText("+351").performScrollTo().performClick()
        compose.onNodeWithText("🇵🇹 +351").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.common_back)).performClick()
        compose.runOnIdle { assertEquals(1, left) }
        compose.runOnIdle { state = ResidenceLinkState(check = found) }
        compose.onNodeWithText("Mo***** de Te***").assertIsDisplayed()
        compose.onNodeWithText("•••.982.247-••").assertIsDisplayed()
        compose.onNodeWithText("morador@example.com").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.link_confirm)).performScrollTo().assertIsEnabled()
        capture("07c_link_confirmation")
    }

    @Test fun homeWithServicesAndMessages() {
        // Início completo, com dados fictícios: central normal, exaustor disponível e comunicados (capturas da loja).
        val now = java.time.Instant.now()
        val pump = CondoMessage(1, "manutencao", "Manutenção programada", "Manutenção da bomba de recalque hoje à tarde",
            "A equipe técnica realizará a troca de peças da bomba de recalque entre 14h e 16h.", now.toString())
        val pool = CondoMessage(2, "comunicado", null, "Piscina reaberta no sábado", "A piscina volta a funcionar no horário normal.", now.toString())
        val messages = MessageState(latest = listOf(pump, pool), unreadCount = 2, list = listOf(pump, pool))
        val unit = ResidenceUnit(1, "A", "124", "A-124", true, false, true, false, null)
        compose.setContent { NovaResidenceTheme {
            ResidenceScreen(ResidenceState(Residence("ativo", false, ResidencePerson(91, "Morador de teste"), listOf(unit)), 1),
                AuthUser("test", null, "morador", false), {}, {}, {}, false,
                fireStatus = ServiceStatus(uiText(R.string.fire_home_normal)),
                exhaustStatus = ServiceStatus(uiText(R.string.exhaust_home_off)),
                unreadMessages = messages.unreadCount,
                homeMessages = { open -> MessageCarousel(messages) { open(it.id) } })
        } }
        compose.onNodeWithText(text(R.string.fire_home_normal)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.exhaust_home_off)).assertIsDisplayed()
        compose.onNodeWithText("Manutenção programada").performScrollTo().assertIsDisplayed()
        capture("store_home")
    }
}

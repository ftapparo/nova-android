package br.com.novaresidence

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.novaresidence.core.network.*
import br.com.novaresidence.core.session.*
import br.com.novaresidence.core.ui.*
import br.com.novaresidence.feature.auth.*
import br.com.novaresidence.feature.residence.*
import br.com.novaresidence.feature.exhaust.*
import br.com.novaresidence.feature.fire.*
import br.com.novaresidence.feature.message.*
import br.com.novaresidence.feature.legal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppViewModel(context: Context) : ViewModel() {
    private val transport = ApiTransport(BuildConfig.V3_BASE_URL)
    val session = SessionCoordinator(EncryptedSessionStore(context.applicationContext)) { token ->
        transport.post<SessionData>("auth/refresh", buildJsonObject { put("refreshToken", token) })
    }
    private val api = ApiClient(transport, session)
    val auth = AuthRepository(api)
    val residence = ResidenceRepository(api)
    val exhaust = ExhaustRepository(api)
    val fire = FireRepository(api)
    val messages = MessageRepository(api)
    val legal = LegalRepository(api)
    val residencePreferences = ResidencePreferences(context)
    val ready = MutableStateFlow(false)
    var restoreError: String? = null
        private set

    init {
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { session.restore() } }
            catch (error: SessionStorageException) { restoreError = error.userMessage }
            finally { ready.value = true }
        }
    }
}

@Composable
fun App(app: AppViewModel) {
    val auth: AuthViewModel = viewModel { AuthViewModel(app.auth, app.legal::termsVersion) }
    val legal: LegalViewModel = viewModel { LegalViewModel(app.legal) }
    val residence: ResidenceViewModel = viewModel { ResidenceViewModel(app.residence, app.auth, app.residencePreferences) }
    val exhaust: ExhaustViewModel = viewModel { ExhaustViewModel(app.exhaust, residence::refresh) }
    val fire: FireViewModel = viewModel { FireViewModel(app.fire) }
    val messages: MessageViewModel = viewModel { MessageViewModel(app.messages) }
    val ready by app.ready.collectAsStateWithLifecycle()
    val user by app.session.user.collectAsStateWithLifecycle()
    val authState by auth.state.collectAsStateWithLifecycle()
    val residenceState by residence.state.collectAsStateWithLifecycle()
    val legalState by legal.state.collectAsStateWithLifecycle()
    // Termo ou Política aberto a partir das telas de entrada ou do aceite (nas configurações, a navegação é da residência).
    var legalDocument by rememberSaveable { mutableStateOf<String?>(null) }
    val openLegal: (String) -> Unit = { legal.load(); legalDocument = it }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null && (!ready || user == null || user?.mustChangePassword == true || user?.mustAcceptTerms == true || legalDocument != null)) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = ready
        }
    }
    val resources = view.context.resources
    // Decodifica em segundo plano as ilustrações mais prováveis, para a primeira abertura das abas não travar.
    LaunchedEffect(user?.id) {
        if (user != null) NovaIllustrations.preload(resources, listOf(R.drawable.nova_exhaust_off, R.drawable.nova_fire_normal, R.drawable.nova_exhaust_on))
    }
    LaunchedEffect(ready, user?.id, user?.mustChangePassword) {
        if (!ready) return@LaunchedEffect
        if (user == null) {
            residence.clear()
            app.restoreError?.let(auth::showMessage)
        } else if (user?.mustChangePassword == true) {
            residence.clear()
            auth.requirePasswordChange()
        } else residence.refresh()
    }
    LifecycleStartEffect(ready, user?.id, user?.mustChangePassword) {
        if (ready && user != null && user?.mustChangePassword == false) residence.refresh()
        onStopOrDispose { residence.stop() }
    }
    NovaResidenceTheme { Box(Modifier.fillMaxSize()) {
        // A tela de baixo continua composta (o formulário não perde o que foi digitado), mas fica fora da
        // acessibilidade e não recebe toques enquanto o documento está aberto.
        Box(if (legalDocument != null) Modifier.fillMaxSize().clearAndSetSemantics {} else Modifier.fillMaxSize()) {
        if (!ready) NovaSplash()
        else if (user == null || user?.mustChangePassword == true) AuthScreen(
            if (user?.mustChangePassword == true) authState.copy(page = AuthPage.ChangePassword) else authState, auth, openLegal,
        )
        else if (user?.mustAcceptTerms == true) {
            LaunchedEffect(Unit) { legal.load() }
            TermsAcceptanceScreen(legalState, { legal.acceptTerms { app.auth.me() } }, auth::logout, openLegal) { legal.load(refresh = true) }
        }
        else if (residenceState.loading && residenceState.residence == null && residenceState.error == null) NovaSplash()
        else {
            val access = residenceState.residence
            val unit = access?.units?.firstOrNull { it.sequencia == residenceState.selectedUnit }
            val allowed = access?.status == "ativo" && !access.blocked && unit?.blocked == false
            val services = access?.canSeeServices(user?.role) == true
            // A home mostra o estado do exaustor e da central (Figma 08a–08c): as consultas valem para toda a
            // área logada visível, e não só para a tela de cada serviço. Param em onStop.
            LifecycleStartEffect(user?.id, unit?.sequencia, allowed) {
                exhaust.start(unit?.quadra, unit?.lote, allowed)
                onStopOrDispose { exhaust.stop() }
            }
            // Comunicados: consultados ao voltar para a área logada (sem push; ver AGENTS.md).
            LifecycleStartEffect(user?.id, services) {
                messages.start(services)
                onStopOrDispose {}
            }
            LifecycleStartEffect(user?.id, services) {
                fire.start(services)
                onStopOrDispose { fire.stop() }
            }
            val exhaustState by exhaust.state.collectAsStateWithLifecycle()
            val fireState by fire.state.collectAsStateWithLifecycle()
            val messageState by messages.state.collectAsStateWithLifecycle()
            val exhaustStatus = when {
                !allowed -> ServiceStatus(unit?.blockedMessage() ?: "Indisponível para esta unidade.")
                exhaustState.phase == ExhaustPhase.Running -> ServiceStatus(exhaustState.homeSummary(), "Em funcionamento", ServiceTone.Running)
                else -> ServiceStatus(exhaustState.homeSummary())
            }
            val fireStatus = if (fireState.status == FireStatus.Alarm) ServiceStatus(fireState.homeSummary(), fireState.homeAlarmPlace(), ServiceTone.Alarm)
                else ServiceStatus(fireState.homeSummary())
            ResidenceScreen(residenceState, user!!, residence::select, residence::refresh, auth::logout, authState.busy, residence::saveLanguage,
                exhaustStatus = exhaustStatus, fireStatus = fireStatus,
                unreadMessages = messageState.unreadCount,
                homeMessages = { open -> MessageCarousel(messageState) { open(it.id) } },
                messagesContent = { initial, back -> MessagesFeature(messageState, initial, back, messages::loadList, messages::markRead) },
                fireContent = { _, _, fullScreen -> FireFeature(fireState, fire::refresh, fire::loadHistory, fullScreen) },
                legalContent = { terms, back ->
                    LaunchedEffect(Unit) { legal.load() }
                    LegalDocumentScreen(legalState, if (terms) TermsDocument else PrivacyDocument, back) { legal.load(refresh = true) }
                },
                deleteAccount = auth::deleteAccount, deleteError = authState.deleteError,
            ) { current, selectUnit, _ ->
                ExhaustScreen(exhaustState, current?.cardLabel(), current?.blockedMessage() ?: access?.notice(),
                    exhaust::activate, exhaust::restart, exhaust::refresh, selectUnit)
            }
        }
        }
        legalDocument?.let { document ->
            BackHandler { legalDocument = null }
            Box(Modifier.fillMaxSize().pointerInput(Unit) {}.background(NovaBackground).systemBarsPadding()) {
                LegalDocumentScreen(legalState, document, { legalDocument = null }) { legal.load(refresh = true) }
            }
        }
    } }
}

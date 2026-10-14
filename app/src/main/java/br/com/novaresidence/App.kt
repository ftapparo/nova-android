package br.com.novaresidence

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
import br.com.novaresidence.core.push.*
import br.com.novaresidence.core.realtime.RealtimeClient
import br.com.novaresidence.core.session.*
import br.com.novaresidence.core.ui.*
import br.com.novaresidence.feature.auth.*
import br.com.novaresidence.feature.residence.*
import br.com.novaresidence.feature.exhaust.*
import br.com.novaresidence.feature.fire.*
import br.com.novaresidence.feature.message.*
import br.com.novaresidence.feature.legal.*
import br.com.novaresidence.feature.notification.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: NovaApplication) : ViewModel() {
    val graph = application.graph
    private val api = graph.api
    val session = graph.session
    val push = graph.push
    val auth = AuthRepository(api, graph::signOutDevice, graph::forgetDevice)
    val residence = ResidenceRepository(api)
    val exhaust = ExhaustRepository(api)
    val exhaustLocator = ExhaustLocator(application)
    val fire = FireRepository(api)
    val messages = MessageRepository(api)
    val legal = LegalRepository(api)
    val notifications = NotificationRepository(api)
    val residencePreferences = ResidencePreferences(application)
    val ready = MutableStateFlow(false)
    var restoreError: String? = null
        private set
    /** Destino de uma notificação tocada, aplicado quando a área logada estiver disponível. */
    val pendingOpen = MutableStateFlow<PushOpen?>(null)
    /** Tempo real (`/v3/ws`) só com a área logada visível; o push cuida do app fechado. */
    val realtime = RealtimeClient(api, BuildConfig.V3_BASE_URL.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://"), viewModelScope)

    init {
        viewModelScope.launch {
            try { restoreError = withContext(Dispatchers.IO) { graph.restoreSession() } }
            finally { ready.value = true }
        }
    }

    fun registerPush(force: Boolean = false) {
        viewModelScope.launch { graph.registerPush(force) }
    }
}

@Composable
fun App(app: AppViewModel) {
    val auth: AuthViewModel = viewModel { AuthViewModel(app.auth, app.legal::termsVersion) }
    val legal: LegalViewModel = viewModel { LegalViewModel(app.legal) }
    val residence: ResidenceViewModel = viewModel { ResidenceViewModel(app.residence, app.auth, app.residencePreferences) }
    val exhaust: ExhaustViewModel = viewModel { ExhaustViewModel(app.exhaust, app.exhaustLocator::current, residence::refresh) }
    val fire: FireViewModel = viewModel { FireViewModel(app.fire) }
    val messages: MessageViewModel = viewModel { MessageViewModel(app.messages) }
    val notifications: NotificationViewModel = viewModel { NotificationViewModel(app.notifications) }
    val ready by app.ready.collectAsStateWithLifecycle()
    val user by app.session.user.collectAsStateWithLifecycle()
    val authState by auth.state.collectAsStateWithLifecycle()
    val residenceState by residence.state.collectAsStateWithLifecycle()
    val legalState by legal.state.collectAsStateWithLifecycle()
    val pushStatus by app.push.status.collectAsStateWithLifecycle()
    val pushPromptDismissed by app.push.promptDismissed.collectAsStateWithLifecycle()
    val pendingOpen by app.pendingOpen.collectAsStateWithLifecycle()
    val enableNotifications = rememberNotificationEnabler(app.push) { app.push.refreshStatus(); app.registerPush(force = true) }
    val locationEnabler = rememberLocationEnabler(exhaust::clearLocation)
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
    // Push: registra com a sessão completa e reconfere a cada volta ao app (a pessoa pode ter mudado a
    // permissão ou o canal do alarme nas configurações do Android). Sem mudança, não chama a API.
    LifecycleStartEffect(ready, user?.id, user?.mustChangePassword, user?.mustAcceptTerms) {
        app.push.refreshStatus()
        if (ready && user != null && user?.mustChangePassword == false && user?.mustAcceptTerms == false) app.registerPush()
        onStopOrDispose {}
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
            // Recarrega o catálogo: o aceite costuma vir de uma versão nova publicada com o app aberto.
            LaunchedEffect(Unit) { legal.load(refresh = true) }
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
            // A equipe é isenta da verificação de localização na API: o app nem pede a permissão.
            val staff = user?.role in StaffRoles
            LifecycleStartEffect(user?.id, unit?.sequencia, allowed, staff) {
                exhaust.start(unit?.quadra, unit?.lote, allowed, staff)
                onStopOrDispose { exhaust.stop() }
            }
            // Aviso recebido com o app aberto: a tela correspondente reconsulta o estado na hora.
            LaunchedEffect(user?.id) {
                app.graph.pushEvents.collect { push ->
                    when (push.target) {
                        PushTarget.Fire -> fire.refresh()
                        PushTarget.Exhaust -> exhaust.refresh()
                        PushTarget.Message -> {
                            messages.refreshLatest()
                            // Com a lista aberta a notificação não aparece; o comunicado novo entra nela na hora.
                            if (app.graph.visibleScreen == PushTarget.Message) messages.loadList()
                        }
                        PushTarget.Home -> Unit
                    }
                }
            }
            // Tempo real: um socket enquanto a área logada estiver visível, para quem tem serviços (equipe ou
            // morador com unidade liberada; os demais seriam recusados com 403). Os eventos só disparam
            // reconsultas; com o socket conectado, as consultas periódicas ficam mais espaçadas.
            LifecycleStartEffect(user?.id, services) {
                if (services) app.realtime.start()
                onStopOrDispose { app.realtime.stop() }
            }
            LaunchedEffect(user?.id) {
                app.realtime.events.collect { event ->
                    fire.onRealtime(event)
                    exhaust.onRealtime(event)
                }
            }
            val realtimeConnected by app.realtime.connected.collectAsStateWithLifecycle()
            LaunchedEffect(realtimeConnected) {
                fire.setLive(realtimeConnected)
                exhaust.setLive(realtimeConnected)
            }
            // Comunicados: consultados ao voltar para a área logada; um push de comunicado novo reconsulta.
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
                fireContent = { _, _, fullScreen ->
                    FireFeature(fireState, fire::refresh, fire::loadHistory, fullScreen, pushStatus == PushStatus.Ready, enableNotifications)
                },
                notificationPrompt = if (pushStatus == PushStatus.PermissionNeeded && !pushPromptDismissed) {
                    { NotificationPrompt(enableNotifications, app.push::dismissPrompt) }
                } else null,
                notificationsContent = { back ->
                    val notificationState by notifications.state.collectAsStateWithLifecycle()
                    NotificationScreen(notificationState, pushStatus, user?.role in StaffRoles, notifications::load, notifications::set,
                        notifications::sendTest, enableNotifications, { app.registerPush(force = true) }, back)
                },
                pendingOpen = pendingOpen, consumeOpen = { app.pendingOpen.value = null },
                visibleScreen = { app.graph.visibleScreen = it },
                legalContent = { terms, back ->
                    LaunchedEffect(Unit) { legal.load() }
                    LegalDocumentScreen(legalState, if (terms) TermsDocument else PrivacyDocument, back) { legal.load(refresh = true) }
                },
                deleteAccount = auth::deleteAccount, deleteError = authState.deleteError,
            ) { current, selectUnit, _ ->
                ExhaustScreen(exhaustState, current?.cardLabel(), current?.blockedMessage() ?: access?.notice(),
                    exhaust::activate, exhaust::restart, exhaust::refresh, selectUnit,
                    locationEnabler.permissionBlocked(), locationEnabler::resolve)
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

private val StaffRoles = setOf("porteiro", "sindico", "admin")

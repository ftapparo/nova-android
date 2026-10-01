package br.com.novaresidence

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
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
    val auth: AuthViewModel = viewModel { AuthViewModel(app.auth) }
    val residence: ResidenceViewModel = viewModel { ResidenceViewModel(app.residence, app.auth, app.residencePreferences) }
    val exhaust: ExhaustViewModel = viewModel { ExhaustViewModel(app.exhaust, residence::refresh) }
    val ready by app.ready.collectAsStateWithLifecycle()
    val user by app.session.user.collectAsStateWithLifecycle()
    val authState by auth.state.collectAsStateWithLifecycle()
    val residenceState by residence.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null && (!ready || user == null || user?.mustChangePassword == true)) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = ready
        }
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
        onStopOrDispose {}
    }
    NovaResidenceTheme {
        if (!ready) NovaSplash()
        else if (user == null || user?.mustChangePassword == true) AuthScreen(
            if (user?.mustChangePassword == true) authState.copy(page = AuthPage.ChangePassword) else authState, auth,
        )
        else if (residenceState.loading && residenceState.residence == null && residenceState.error == null) NovaSplash()
        else {
            val access = residenceState.residence
            val unit = access?.units?.firstOrNull { it.sequencia == residenceState.selectedUnit }
            val allowed = access?.status == "ativo" && !access.blocked && unit?.blocked == false
            // A home também mostra o estado do exaustor (Figma 08b): a consulta vale para toda a área
            // logada visível, e não só para a tela do exaustor. Continua parando em onStop.
            LifecycleStartEffect(user?.id, unit?.sequencia, allowed) {
                exhaust.start(unit?.quadra, unit?.lote, allowed)
                onStopOrDispose { exhaust.stop() }
            }
            val exhaustState by exhaust.state.collectAsStateWithLifecycle()
            val exhaustStatus = if (allowed) ExhaustHomeStatus(exhaustState.homeSummary(), exhaustState.phase == ExhaustPhase.Running)
                else ExhaustHomeStatus(unit?.blockedMessage() ?: "Indisponível para esta unidade.", false)
            ResidenceScreen(residenceState, user!!, residence::select, residence::refresh, auth::logout, authState.busy, residence::saveLanguage, exhaustStatus) { current, back, selectUnit ->
                ExhaustScreen(exhaustState, current?.homeLabel(), current?.blockedMessage() ?: access?.notice(),
                    exhaust::activate, exhaust::restart, exhaust::refresh, back, selectUnit)
            }
        }
    }
}

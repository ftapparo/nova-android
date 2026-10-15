package br.com.novaresidence

import android.app.Application
import android.content.Context
import android.util.Log
import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.network.ApiTransport
import br.com.novaresidence.core.push.PushChannels
import br.com.novaresidence.core.push.PushDevice
import br.com.novaresidence.core.push.PushMessage
import br.com.novaresidence.core.push.PushRegistration
import br.com.novaresidence.core.push.PushTarget
import br.com.novaresidence.core.push.SharedPreferencesPushStore
import br.com.novaresidence.core.push.firebaseToken
import br.com.novaresidence.core.session.EncryptedSessionStore
import br.com.novaresidence.core.session.SessionCoordinator
import br.com.novaresidence.core.session.SessionData
import br.com.novaresidence.core.session.SessionStorageException
import br.com.novaresidence.core.ui.AppLanguageStore
import br.com.novaresidence.core.ui.UiText
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NovaApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguageStore.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        PushChannels.createBase(this)
    }
}

/**
 * Dependências do processo. O serviço do FCM roda sem Activity (app fechado), então sessão, cliente da API
 * e registro do push não podem viver só no ViewModel da tela.
 */
class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    val language = AppLanguageStore(appContext)
    val transport = ApiTransport(BuildConfig.V3_BASE_URL, language = { language.current().tag })
    val session = SessionCoordinator(EncryptedSessionStore(appContext)) { token ->
        transport.post<SessionData>("auth/refresh", buildJsonObject { put("refreshToken", token) })
    }
    val api = ApiClient(transport, session)
    val push = PushRegistration(api, SharedPreferencesPushStore(appContext), ::firebaseToken, { PushDevice.current(appContext, language.current().tag) })
    /** Avisos recebidos com o app aberto: as telas visíveis reconsultam o estado na hora. */
    val pushEvents = MutableSharedFlow<PushMessage>(extraBufferCapacity = 16)
    /** Tela aberta com o app em primeiro plano (nula em segundo plano); lida pelo serviço do FCM em outra thread. */
    @Volatile var visibleScreen: PushTarget? = null

    private val restoreGuard = Any()
    private var restored = false
    private var restoreError: UiText? = null

    /** Lê a sessão salva uma vez por processo (tela ou serviço do FCM, o que vier primeiro). Devolve o erro para exibir. */
    fun restoreSession(): UiText? = synchronized(restoreGuard) {
        if (!restored) {
            restored = true
            try { session.restore() } catch (error: SessionStorageException) { restoreError = error.userMessage }
        }
        restoreError
    }

    /** Registra o aparelho; falha (sem rede, sem Google Play, Firebase fora) fica para a próxima abertura do app. */
    suspend fun registerPush(force: Boolean = false) {
        try {
            push.register(force)
        } catch (error: CancellationException) {
            throw error
        } catch (error: ApiException) {
            Log.w(TAG, "Registro do aparelho recusado: ${error.kind} (${error.requestId})")
            push.refreshStatus()
        } catch (error: Exception) {
            Log.w(TAG, "Registro do aparelho indisponível: ${error.javaClass.simpleName}")
            push.refreshStatus()
        }
    }

    /**
     * Antes do logout: remove o aparelho na API com o access token ainda válido e troca o token do FCM, para
     * o aparelho não receber avisos da conta mesmo se a remoção falhar (a API invalida o token antigo).
     */
    suspend fun signOutDevice(accessToken: String) {
        try {
            push.unregister(accessToken)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Remoção do aparelho falhou; o token do FCM será trocado: ${error.javaClass.simpleName}")
        }
        forgetDevice()
    }

    /** Conta excluída ou sessão encerrada: a API já não envia para esta conta; o token do FCM é trocado. */
    fun forgetDevice() {
        push.forget()
        try {
            FirebaseMessaging.getInstance().deleteToken()
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Firebase indisponível ao trocar o token: ${error.message}")
        }
    }

    private companion object {
        const val TAG = "NovaPush"
    }
}

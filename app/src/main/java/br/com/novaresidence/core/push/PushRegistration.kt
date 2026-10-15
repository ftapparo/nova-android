package br.com.novaresidence.core.push

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import br.com.novaresidence.BuildConfig
import br.com.novaresidence.core.network.ApiClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Situação do push neste aparelho, usada pela tela da central (26), pela home e pelas configurações. */
enum class PushStatus {
    /** Aparelho registrado na API e notificações permitidas, inclusive o canal do alarme. */
    Ready,
    /** Notificações desligadas para o app (ou permissão do Android 13+ não concedida). */
    PermissionNeeded,
    /** Notificações permitidas, mas o canal do alarme de incêndio foi bloqueado. */
    ChannelBlocked,
    /** Ainda não registrado (sem rede, Firebase indisponível ou registro pendente). */
    NotRegistered,
}

data class PushDevice(
    val notificationsEnabled: Boolean,
    val alarmChannelEnabled: Boolean,
    val appVersion: String,
    val osVersion: String,
    val deviceModel: String,
    /** Idioma do app: a API envia os avisos deste aparelho nele. */
    val language: String = "pt-BR",
) {
    companion object {
        fun current(context: Context, language: String): PushDevice {
            val manager = context.getSystemService(NotificationManager::class.java)
            val alarm = manager.getNotificationChannel(PushChannels.FireAlarm)
            return PushDevice(
                notificationsEnabled = PushNotifier.canNotify(context),
                alarmChannelEnabled = alarm == null || alarm.importance != NotificationManager.IMPORTANCE_NONE,
                appVersion = BuildConfig.VERSION_NAME,
                osVersion = "Android ${Build.VERSION.RELEASE}",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".take(80),
                language = language,
            )
        }
    }
}

/** Estado local do push. Não guarda tokens de sessão (exclusivos de core/session) nem o token FCM em claro. */
interface PushStore {
    val installationId: String
    var registration: String?
    var registeredAt: Long
    var permissionAsked: Boolean
    var promptDismissed: Boolean
}

class SharedPreferencesPushStore(context: Context) : PushStore {
    private val preferences = context.applicationContext.getSharedPreferences("push_preferences", Context.MODE_PRIVATE)

    override val installationId: String
        get() = preferences.getString("installation_id", null) ?: UUID.randomUUID().toString().also {
            preferences.edit { putString("installation_id", it) }
        }
    override var registration: String?
        get() = preferences.getString("registration", null)
        set(value) { preferences.edit { putString("registration", value) } }
    override var registeredAt: Long
        get() = preferences.getLong("registered_at", 0L)
        set(value) { preferences.edit { putLong("registered_at", value) } }
    override var permissionAsked: Boolean
        get() = preferences.getBoolean("permission_asked", false)
        set(value) { preferences.edit { putBoolean("permission_asked", value) } }
    override var promptDismissed: Boolean
        get() = preferences.getBoolean("prompt_dismissed", false)
        set(value) { preferences.edit { putBoolean("prompt_dismissed", value) } }
}

@Serializable
data class RegisteredDevice(val installationId: String, val notificationsEnabled: Boolean, val alarmChannelEnabled: Boolean)

/** Token de registro do FCM deste aparelho. */
suspend fun firebaseToken(): String = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful && task.result != null) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("Token do FCM indisponível."))
    }
}

/**
 * Registro do aparelho na API (`/notifications/devices/android`). O app registra a cada abertura, mas só
 * chama a API quando algo mudou (conta, token, permissão, canal do alarme, versão, idioma) ou depois de 24 h, para a
 * API renovar o papel da conta e manter o aparelho como ativo.
 */
class PushRegistration(
    private val api: ApiClient,
    private val store: PushStore,
    private val token: suspend () -> String,
    private val device: () -> PushDevice,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow(PushStatus.NotRegistered)
    val status = mutableStatus.asStateFlow()
    private val mutablePromptDismissed = MutableStateFlow(store.promptDismissed)
    /** O cartão da home que pede a permissão foi dispensado ("Agora não"). */
    val promptDismissed = mutablePromptDismissed.asStateFlow()

    fun refreshStatus() {
        val current = device()
        val account = api.session.user.value?.id
        mutableStatus.value = when {
            !current.notificationsEnabled -> PushStatus.PermissionNeeded
            !current.alarmChannelEnabled -> PushStatus.ChannelBlocked
            account != null && store.registration?.startsWith("$account|") == true -> PushStatus.Ready
            else -> PushStatus.NotRegistered
        }
    }

    /** Registra se a sessão estiver completa; lança [br.com.novaresidence.core.network.ApiException] ou falha do FCM. */
    suspend fun register(force: Boolean = false) = mutex.withLock {
        val user = api.session.user.value
        if (user == null || user.mustChangePassword || user.mustAcceptTerms) return@withLock refreshStatus()
        val current = device()
        val fcmToken = token()
        val key = "${user.id}|" + digest(listOf(fcmToken, current.notificationsEnabled, current.alarmChannelEnabled, current.appVersion, current.language).joinToString("|"))
        if (!force && store.registration == key && now() - store.registeredAt < DAY_MS) return@withLock refreshStatus()

        api.put<RegisteredDevice>("notifications/devices/android", buildJsonObject {
            put("installationId", store.installationId)
            put("token", fcmToken)
            put("appVersion", current.appVersion)
            put("osVersion", current.osVersion)
            put("deviceModel", current.deviceModel)
            put("notificationsEnabled", current.notificationsEnabled)
            put("alarmChannelEnabled", current.alarmChannelEnabled)
            put("language", current.language)
        })
        store.registration = key
        store.registeredAt = now()
        refreshStatus()
    }

    /**
     * Remove o aparelho no logout. Recebe o access token porque a sessão local já foi limpa quando o
     * logout chama esta função (ver SessionCoordinator.logout).
     */
    suspend fun unregister(accessToken: String) {
        try {
            api.transport.request("notifications/devices/android/${store.installationId}", token = accessToken, delete = true)
        } finally {
            forget()
        }
    }

    /** Esquece o registro local (logout, conta excluída ou sessão encerrada). */
    fun forget() {
        store.registration = null
        store.registeredAt = 0L
        refreshStatus()
    }

    fun permissionAsked() = store.permissionAsked
    fun markPermissionAsked() { store.permissionAsked = true }
    fun dismissPrompt() {
        store.promptDismissed = true
        mutablePromptDismissed.value = true
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

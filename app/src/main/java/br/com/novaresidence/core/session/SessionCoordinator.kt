package br.com.novaresidence.core.session

import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionCoordinator(
    private val store: SessionStore,
    private val refresh: suspend (String) -> SessionData,
) {
    private val guard = Any()
    private val refreshMutex = Mutex()
    private var current: SessionData? = null
    private var generation = 0L
    private val mutableUser = MutableStateFlow<AuthUser?>(null)
    val user = mutableUser.asStateFlow()

    fun restore() = synchronized(guard) {
        current = store.read()
        mutableUser.value = current?.user
    }

    fun generation(): Long = synchronized(guard) { generation }

    fun replace(value: SessionData, expectedGeneration: Long) = synchronized(guard) {
        if (generation != expectedGeneration) throw CancellationException("Session changed")
        if (value.accessToken.isBlank() || value.refreshToken.isBlank() || value.tokenType != "bearer") {
            throw ApiException(ApiErrorKind.InvalidResponse)
        }
        store.write(value)
        current = value
        mutableUser.value = value.user
    }

    fun updateUser(user: AuthUser, expectedGeneration: Long) = synchronized(guard) {
        current?.takeIf { generation == expectedGeneration }?.let { replace(it.copy(user = user), expectedGeneration) }
    }

    fun clear() = synchronized(guard) {
        generation++
        current = null
        mutableUser.value = null
        store.clear()
    }

    suspend fun logout(revoke: suspend (String) -> Unit) {
        val token = synchronized(guard) {
            val access = current?.accessToken
            clear()
            access
        }
        if (token != null) revoke(token)
    }

    private fun snapshot(): Pair<Long, SessionData> = synchronized(guard) {
        generation to (current ?: throw ApiException(ApiErrorKind.Unauthorized))
    }

    private suspend fun refreshOnce(failed: SessionData, expectedGeneration: Long): SessionData = refreshMutex.withLock {
        val (epoch, latest) = snapshot()
        if (epoch != expectedGeneration) throw CancellationException("Session changed")
        if (latest.accessToken != failed.accessToken) return@withLock latest
        // Refresh tokens are single-use. Only one request may exchange a pair at a time.
        try {
            val updated = refresh(latest.refreshToken)
            replace(updated, expectedGeneration)
            updated
        } catch (error: CancellationException) {
            throw error
        } catch (error: ApiException) {
            // Only a definitive rejection ends the session. A phone waking from sleep often has no network
            // for a few seconds; logging out then would hide the fire panel exactly when it matters.
            if (error.kind == ApiErrorKind.Unauthorized || error.kind == ApiErrorKind.Validation) {
                synchronized(guard) { if (generation == expectedGeneration) clear() }
            }
            throw error
        } catch (error: Exception) {
            synchronized(guard) { if (generation == expectedGeneration) clear() }
            throw error
        }
    }

    suspend fun <T> authorize(block: suspend (String) -> T): T {
        val (epoch, session) = snapshot()
        try {
            return block(session.accessToken).also {
                if (generation() != epoch) throw CancellationException("Session changed")
            }
        } catch (error: ApiException) {
            if (error.kind != ApiErrorKind.Unauthorized) throw error
        }
        val renewed = refreshOnce(session, epoch)
        try {
            return block(renewed.accessToken).also {
                if (generation() != epoch) throw CancellationException("Session changed")
            }
        } catch (error: ApiException) {
            if (error.kind == ApiErrorKind.Unauthorized) synchronized(guard) { if (generation == epoch) clear() }
            throw error
        }
    }
}

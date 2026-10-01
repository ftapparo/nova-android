package br.com.novaresidence.feature.exhaust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.session.SessionStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExhaustViewModel(private val repository: ExhaustRepository, private val accessChanged: () -> Unit = {}) : ViewModel() {
    private val mutableState = MutableStateFlow(ExhaustState())
    val state = mutableState.asStateFlow()
    private var polling: Job? = null
    private var command: Job? = null
    private var clock: Job? = null
    private var target: Pair<String, String>? = null
    private var generation = 0L
    private var retryAt = 0L
    private var refreshing = false

    fun start(tower: String?, apartment: String?, allowed: Boolean) {
        stop()
        generation++
        val current = generation
        target = if (allowed && tower != null && apartment != null) tower to apartment else null
        mutableState.value = ExhaustState(loading = target != null)
        if (target == null) return
        clock = viewModelScope.launch {
            while (true) {
                mutableState.update { it.copy(nowMillis = System.currentTimeMillis(), retrySeconds = remainingCooldown()) }
                delay(1000)
            }
        }
        polling = viewModelScope.launch {
            while (generation == current) {
                refreshNow(current)
                delay(if (state.value.phase in setOf(ExhaustPhase.Starting, ExhaustPhase.Finishing)) 2000 else 5000)
            }
        }
    }

    fun stop() {
        generation++
        polling?.cancel(); command?.cancel(); clock?.cancel()
        polling = null; command = null; clock = null
        target = null
        refreshing = false
        mutableState.value = ExhaustState(loading = false)
    }

    private fun remainingCooldown() = ((retryAt - System.nanoTime() + 999_999_999) / 1_000_000_000).coerceAtLeast(0)

    private fun report(error: ApiException, uncertain: Boolean = false) {
        if (error.retryAfterSeconds > 0) retryAt = System.nanoTime() + error.retryAfterSeconds * 1_000_000_000
        // O 409 do exaustor (ex.: manutenção) não é o conflito de cadastro da mensagem genérica; o detail é seguro para exibir.
        val message = if (error.kind == ApiErrorKind.Conflict) error.detail ?: "O equipamento recusou o comando." else error.userMessage()
        mutableState.update { it.copy(error = message, requestId = error.requestId,
            retrySeconds = remainingCooldown(), activationUncertain = uncertain || it.activationUncertain) }
    }

    private suspend fun refreshNow(current: Long) {
        val unit = target ?: return
        if (refreshing || state.value.sending || remainingCooldown() > 0) return
        refreshing = true
        val firstLoad = state.value.exhaust == null
        if (firstLoad) mutableState.update { it.copy(loading = true) }
        try {
            val item = repository.list().forUnit(unit.first, unit.second)
            if (current != generation) return
            mutableState.update { it.copy(exhaust = item, loading = false, error = null, requestId = null, activationUncertain = false) }
        } catch (error: ApiException) {
            if (current == generation) {
                report(error)
                if (error.kind == ApiErrorKind.Forbidden) accessChanged()
            }
        } catch (error: SessionStorageException) {
            if (current == generation) mutableState.update { it.copy(error = error.userMessage) }
        } catch (error: CancellationException) { throw error }
        finally {
            if (current == generation) {
                refreshing = false
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    fun refresh() { viewModelScope.launch { refreshNow(generation) } }

    fun activate() {
        if (!state.value.canActivate) return
        send(restart = false)
    }

    /** Reenvia o /on com o exaustor ligado: a API recalcula o prazo sem mexer no relé. */
    fun restart() {
        if (!state.value.canRestart) return
        send(restart = true)
    }

    private fun send(restart: Boolean) {
        if (refreshing || command?.isActive == true || remainingCooldown() > 0) return
        val item = state.value.exhaust ?: return
        val current = generation
        mutableState.update { it.copy(sending = true, restarting = restart, error = null, requestId = null) }
        command = viewModelScope.launch {
            try {
                val updated = repository.activate(item.id)
                if (current == generation) {
                    if (updated.id != item.id) throw ApiException(ApiErrorKind.InvalidResponse)
                    mutableState.update { it.copy(exhaust = updated, activationUncertain = false) }
                }
            } catch (error: ApiException) {
                if (current == generation) {
                    report(error, uncertain = true)
                    if (error.kind in setOf(ApiErrorKind.NotFound, ApiErrorKind.Forbidden)) {
                        mutableState.update { it.copy(exhaust = null) }
                        accessChanged()
                    }
                }
            } catch (error: SessionStorageException) {
                if (current == generation) {
                    mutableState.update { it.copy(error = error.userMessage, activationUncertain = true) }
                }
            } catch (error: CancellationException) { throw error }
            finally {
                if (current == generation) mutableState.update { it.copy(sending = false, restarting = false) }
            }
        }
    }
}

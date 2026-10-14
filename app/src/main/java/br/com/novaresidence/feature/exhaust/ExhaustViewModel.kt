package br.com.novaresidence.feature.exhaust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.network.NovaJson
import br.com.novaresidence.core.realtime.RealtimeEvent
import br.com.novaresidence.core.session.SessionStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException

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
    /** Evento chegou durante uma consulta: a resposta em curso pode ser anterior a ele. */
    private var refreshAgain = false
    private var live = false
    private val wake = Channel<Unit>(Channel.CONFLATED)

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
                val transition = state.value.phase in setOf(ExhaustPhase.Starting, ExhaustPhase.Finishing)
                // Com o socket de tempo real, a mudança chega por evento e a consulta vira rede de segurança.
                val interval = when {
                    live -> if (transition) 5_000L else 30_000L
                    else -> if (transition) 2_000L else 5_000L
                }
                withTimeoutOrNull(interval) { wake.receive() }
            }
        }
    }

    fun stop() {
        generation++
        polling?.cancel(); command?.cancel(); clock?.cancel()
        polling = null; command = null; clock = null
        target = null
        refreshing = false
        refreshAgain = false
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
        if (refreshing) { refreshAgain = true; return }
        if (state.value.sending || remainingCooldown() > 0) return
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
                if (refreshAgain) { refreshAgain = false; refresh() }
            }
        }
    }

    fun refresh() { viewModelScope.launch { refreshNow(generation) } }

    /** Socket de tempo real conectado ou não; ao cair, a consulta volta ao ritmo normal na hora. */
    fun setLive(value: Boolean) {
        if (live == value) return
        live = value
        wake.trySend(Unit)
    }

    /** Eventos do `/v3/ws`: reconsulta quando muda o exaustor da unidade ativa. Nunca envia comando. */
    fun onRealtime(event: RealtimeEvent) {
        val unit = target ?: return
        when (event.name) {
            RealtimeEvent.Connected -> refresh()
            "exhaust.changed" -> {
                val changed = try { NovaJson.decodeFromJsonElement(Exhaust.serializer(), event.data) }
                    catch (_: SerializationException) { return } catch (_: IllegalArgumentException) { return }
                if (listOf(changed).forUnit(unit.first, unit.second) != null) refresh()
            }
        }
    }

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

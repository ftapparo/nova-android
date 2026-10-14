package br.com.novaresidence.feature.fire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.realtime.RealtimeEvent
import br.com.novaresidence.core.session.SessionStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import java.time.Instant

class FireViewModel(
    private val repository: FireRepository,
    /** Intervalo mínimo entre reconsultas disparadas pelo tempo real (rajadas de eventos). */
    private val realtimeThrottleMs: Long = 2_000,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FireState())
    val state = mutableState.asStateFlow()
    private var polling: Job? = null
    private var historyJob: Job? = null
    private var generation = 0L
    private var retryAt = 0L
    private var live = false
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var signature: String? = null
    private var lastRealtimeRefresh = 0L
    private var pendingRealtime: Job? = null

    /** Consulta a central enquanto a área logada estiver visível; [allowed] segue a permissão residencial. */
    fun start(allowed: Boolean) {
        stop()
        val current = ++generation
        mutableState.value = FireState(loading = allowed)
        if (!allowed) return
        signature = null
        polling = viewModelScope.launch {
            while (generation == current) {
                refreshNow(current)
                // Com o socket de tempo real a tela muda pelos eventos e a consulta vira rede de segurança;
                // sem ele, 10 s mantêm o aviso próximo do painel. A troca de modo acorda o laço na hora.
                withTimeoutOrNull(if (live) 60_000L else 10_000L) { wake.receive() }
            }
        }
    }

    fun stop() {
        generation++
        polling?.cancel(); historyJob?.cancel(); pendingRealtime?.cancel()
        polling = null; historyJob = null; pendingRealtime = null
        mutableState.update { it.copy(loading = false, historyLoading = false) }
    }

    fun refresh() { val current = generation; viewModelScope.launch { refreshNow(current) } }

    /** Socket de tempo real conectado ou não; ao cair, a consulta volta a 10 s imediatamente. */
    fun setLive(value: Boolean) {
        if (live == value) return
        live = value
        wake.trySend(Unit)
    }

    /** Eventos do `/v3/ws`: só disparam reconsulta do REST quando o estado da central pode ter mudado. */
    fun onRealtime(event: RealtimeEvent) {
        if (polling == null) return
        when (event.name) {
            RealtimeEvent.Connected -> { signature = null; refreshFromRealtime() }
            "cie.active.updated", "cie.connection.changed" -> refreshFromRealtime()
            "upstream.connection.changed" ->
                if (((event.data as? JsonObject)?.get("service") as? JsonPrimitive)?.contentOrNull == "cie") refreshFromRealtime()
            "cie.status.updated" -> {
                val next = cieSignature(event.data) ?: return
                val previous = signature
                signature = next
                // A primeira assinatura depois de conectar só serve de base: a reconexão já reconsultou.
                if (previous != null && previous != next) refreshFromRealtime()
            }
        }
    }

    private fun refreshFromRealtime() {
        if (pendingRealtime?.isActive == true) return
        val current = generation
        val wait = lastRealtimeRefresh + realtimeThrottleMs * 1_000_000 - System.nanoTime()
        pendingRealtime = viewModelScope.launch {
            if (wait > 0) delay(wait / 1_000_000)
            lastRealtimeRefresh = System.nanoTime()
            refreshNow(current)
        }
    }

    private fun cooldown() = ((retryAt - System.nanoTime() + 999_999_999) / 1_000_000_000).coerceAtLeast(0)

    private suspend fun refreshNow(current: Long) {
        if (cooldown() > 0) { mutableState.update { it.copy(retrySeconds = cooldown()) }; return }
        try {
            val (panel, alarms) = coroutineScope {
                val panel = async { repository.panel() }
                val alarms = async { repository.alarms() }
                panel.await() to alarms.await()
            }
            val attention = !alarms.isTriggered && (alarms.counters.falha > 0 || alarms.counters.supervisao > 0)
            val active = if (attention) activeEvents() else emptyList()
            if (current == generation) mutableState.update {
                it.copy(panel = panel, alarms = alarms, activeEvents = active, loading = false, error = null, requestId = null, retrySeconds = 0)
            }
        } catch (error: ApiException) {
            if (error.retryAfterSeconds > 0) retryAt = System.nanoTime() + error.retryAfterSeconds * 1_000_000_000
            if (current == generation) mutableState.update {
                it.copy(loading = false, error = error.userMessage(), requestId = error.requestId, retrySeconds = cooldown())
            }
        } catch (error: SessionStorageException) {
            if (current == generation) mutableState.update { it.copy(loading = false, error = error.userMessage) }
        } catch (error: CancellationException) { throw error }
    }

    /**
     * Local das falhas e supervisões ativas (Figma 27). É um complemento: sem ele o estado de atenção
     * continua correto, então uma falha nesta consulta só respeita o Retry-After e não derruba a tela.
     */
    private suspend fun activeEvents(): List<CieLog> = try {
        repository.activeEvents().events.let { it["falha"].orEmpty() + it["supervisao"].orEmpty() }
    } catch (error: ApiException) {
        if (error.retryAfterSeconds > 0) retryAt = System.nanoTime() + error.retryAfterSeconds * 1_000_000_000
        emptyList()
    }

    /** Histórico dos últimos 30 dias, paginado pelo cursor retornado em `data`. */
    fun loadHistory() {
        if (historyJob?.isActive == true) return
        val current = generation
        historyJob = viewModelScope.launch {
            mutableState.update { it.copy(historyLoading = true, historyError = null) }
            try {
                val limit = now().minus(Duration.ofDays(30))
                val events = mutableListOf<CieLog>()
                var cursor: String? = null
                // Até 4 páginas de 50 eventos: suficiente para 30 dias sem prender a tela em um painel muito ruidoso.
                for (attempt in 1..4) {
                    val page = repository.logs(cursor)
                    val recent = page.items.filter { log -> log.instant()?.isAfter(limit) == true }
                    events += recent
                    cursor = page.nextCursor
                    if (cursor == null || recent.size < page.items.size) break
                }
                val history = withContext(Dispatchers.Default) { events.distinctBy { log -> log.key }.sortedByDescending { log -> log.instant() } }
                if (current == generation) mutableState.update { it.copy(history = history, historyLoading = false) }
            } catch (error: ApiException) {
                if (current == generation) mutableState.update { it.copy(historyLoading = false, historyError = error.userMessage()) }
            } catch (error: SessionStorageException) {
                if (current == generation) mutableState.update { it.copy(historyLoading = false, historyError = error.userMessage) }
            } catch (error: CancellationException) { throw error }
        }
    }
}

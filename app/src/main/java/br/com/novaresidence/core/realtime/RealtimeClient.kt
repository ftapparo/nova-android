package br.com.novaresidence.core.realtime

import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.network.NovaJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Evento do `/v3/ws` (`event` + `data`). [Connected] é emitido pelo app depois de cada (re)conexão. */
data class RealtimeEvent(val name: String, val data: JsonElement = JsonNull) {
    companion object {
        /** As telas recarregam o estado por REST: os eventos do servidor são deltas, e os perdidos não voltam. */
        const val Connected = "realtime.connected"
    }
}

@Serializable
data class WsTicket(val ticket: String, val expiresIn: Int)

/**
 * Socket de tempo real com o app em primeiro plano (`APP-GUIDE.md` §6). O servidor só envia; o app nunca manda
 * comando por aqui. Os eventos servem de gatilho para as telas reconsultarem o REST; o estado continua vindo dele.
 */
class RealtimeClient(
    private val api: ApiClient,
    /** Ex.: `wss://api.novaresidence.com.br`. */
    wsBaseUrl: String,
    private val scope: CoroutineScope,
    // O cliente do ApiTransport tem readTimeout de 20 s e derrubaria o socket ocioso; o ping do app
    // (além do ping do servidor a cada 30 s) é o que detecta conexão morta.
    private val client: OkHttpClient = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS).build(),
    /** Espera antes da tentativa [attempt] (1, 2, 3…): 1, 2, 4… até 30 s. */
    private val backoff: (attempt: Int) -> Long = { attempt -> (1_000L shl (attempt - 1).coerceIn(0, 5)).coerceAtMost(30_000L) },
) {
    private val url = wsBaseUrl.trimEnd('/') + "/v3/ws"
    private val mutableEvents = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events = mutableEvents.asSharedFlow()
    private val mutableConnected = MutableStateFlow(false)
    val connected = mutableConnected.asStateFlow()
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { run() }
    }

    fun stop() {
        job?.cancel()
        job = null
        mutableConnected.value = false
    }

    private enum class Next { Now, TicketRejected, Retry, Stop }
    private class Outcome(val next: Next, val established: Boolean)

    private suspend fun run() {
        var attempt = 0
        var ticketRetried = false
        while (currentCoroutineContext().isActive) {
            val outcome = try {
                connectOnce()
            } catch (error: CancellationException) {
                throw error
            } catch (error: ApiException) {
                // Sem sessão (o authorize já tentou renovar) ou sem acesso: tentar de novo não resolve.
                if (error.kind == ApiErrorKind.Unauthorized || error.kind == ApiErrorKind.Forbidden) return
                Outcome(Next.Retry, established = false)
            }
            mutableConnected.value = false
            if (outcome.established) { attempt = 0; ticketRetried = false }
            when (outcome.next) {
                Next.Stop -> return
                Next.Now -> continue
                Next.TicketRejected -> if (!ticketRetried) { ticketRetried = true; continue }
                Next.Retry -> Unit
            }
            attempt++
            delay(backoff(attempt))
        }
    }

    /** Pede o ticket, abre o socket e suspende até ele fechar. Um socket por vez (a API aceita 3 por conta). */
    private suspend fun connectOnce(): Outcome {
        val ticket = api.post<WsTicket>("ws/ticket").ticket
        val done = CompletableDeferred<Outcome>()
        var established = false
        // Depois do stop() o socket ainda pode entregar algo antes de fechar; nada dele vale mais.
        val open = AtomicBoolean(true)
        val socket = client.newWebSocket(Request.Builder().url("$url?ticket=$ticket").build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!open.get()) return
                val event = parse(text) ?: return
                if (event.name == "connection.established") {
                    established = true
                    mutableConnected.value = true
                    mutableEvents.tryEmit(RealtimeEvent(RealtimeEvent.Connected))
                }
                mutableEvents.tryEmit(event)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                // 4001: o access token que pediu o ticket expirou; o próximo ticket já sai com o token renovado.
                done.complete(Outcome(if (code == 4001) Next.Now else Next.Retry, established))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                done.complete(Outcome(if (code == 4001) Next.Now else Next.Retry, established))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val next = when (response?.code) {
                    401 -> Next.TicketRejected
                    // Conta sem nada a receber (pendente, sem unidade liberada): só um novo start() tenta de novo.
                    403 -> Next.Stop
                    else -> Next.Retry
                }
                done.complete(Outcome(next, established))
            }
        })
        try {
            return done.await()
        } finally {
            // Saída por stop() ou fim normal: nunca deixa um socket aberto para trás.
            open.set(false)
            socket.close(1000, null)
        }
    }

    private fun parse(text: String): RealtimeEvent? = try {
        val message = NovaJson.parseToJsonElement(text) as? JsonObject
        val name = (message?.get("event") as? JsonPrimitive)?.contentOrNull
        if (name.isNullOrBlank()) null else RealtimeEvent(name, message["data"] ?: JsonNull)
    } catch (_: IllegalArgumentException) {
        // Mensagem fora do formato: ignorada, como evento desconhecido.
        null
    }
}

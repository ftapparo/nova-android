package br.com.novaresidence.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.session.SessionStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessageViewModel(private val repository: MessageRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(MessageState())
    val state = mutableState.asStateFlow()
    private var latestJob: Job? = null
    private var listJob: Job? = null
    private var allowed = false

    /**
     * Consulta os comunicados quando a área logada aparece. [allowed] segue a permissão residencial:
     * conta sem unidade liberada e sem papel de equipe recebe 403 da API, então nem consulta.
     */
    fun start(allowed: Boolean) {
        this.allowed = allowed
        if (!allowed) { latestJob?.cancel(); listJob?.cancel(); mutableState.value = MessageState(); return }
        refreshLatest()
    }

    fun refreshLatest() {
        if (!allowed || latestJob?.isActive == true) return
        latestJob = viewModelScope.launch {
            try {
                val result = repository.carousel()
                mutableState.update { it.copy(latest = result.items, unreadCount = result.unreadCount, latestError = null) }
            } catch (error: ApiException) {
                mutableState.update { it.copy(latestError = error.userMessage()) }
            } catch (error: SessionStorageException) {
                mutableState.update { it.copy(latestError = error.userMessage) }
            } catch (error: CancellationException) { throw error }
        }
    }

    fun loadList(unreadOnly: Boolean = state.value.unreadOnly) {
        if (!allowed) return
        listJob?.cancel()
        listJob = viewModelScope.launch {
            mutableState.update { it.copy(unreadOnly = unreadOnly, listLoading = true, listError = null, list = if (it.unreadOnly == unreadOnly) it.list else null) }
            try {
                val result = repository.list(unreadOnly = unreadOnly)
                mutableState.update { it.copy(list = result.items, unreadCount = result.unreadCount, listLoading = false) }
            } catch (error: ApiException) {
                mutableState.update { it.copy(listLoading = false, listError = error.userMessage()) }
            } catch (error: SessionStorageException) {
                mutableState.update { it.copy(listLoading = false, listError = error.userMessage) }
            } catch (error: CancellationException) { throw error }
        }
    }

    /** Marca como lido ao abrir o detalhe. A tela já mostra o conteúdo; só o contador depende da resposta. */
    fun markRead(message: CondoMessage) {
        if (message.read || !allowed) return
        viewModelScope.launch {
            try {
                repository.markRead(message.id)
                mutableState.update { state ->
                    fun List<CondoMessage>.marked() = map { if (it.id == message.id) it.copy(read = true) else it }
                    state.copy(
                        latest = state.latest.marked(),
                        list = if (state.unreadOnly) state.list?.filterNot { it.id == message.id } else state.list?.marked(),
                        unreadCount = (state.unreadCount - 1).coerceAtLeast(0),
                    )
                }
                // Comunicado com TTL 0 sai do carrossel quando é lido; a API decide, o app só reconsulta.
                refreshLatest()
            } catch (error: ApiException) {
                // Não impede a leitura: o comunicado continua como não lido e será marcado na próxima abertura.
                mutableState.update { it.copy(listError = error.userMessage()) }
            } catch (error: SessionStorageException) {
                mutableState.update { it.copy(listError = error.userMessage) }
            } catch (error: CancellationException) { throw error }
        }
    }
}

package br.com.novaresidence.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationState(
    val preferences: NotificationPreferences? = null,
    val loading: Boolean = false,
    /** Opção sendo gravada; o interruptor fica desabilitado até a resposta. */
    val saving: NotificationOption? = null,
    val error: String? = null,
    val testing: Boolean = false,
    val testMessage: String? = null,
)

class NotificationViewModel(private val repository: NotificationRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(NotificationState())
    val state = mutableState.asStateFlow()

    fun load() {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val preferences = repository.preferences()
                mutableState.update { it.copy(preferences = preferences, loading = false) }
            } catch (error: ApiException) {
                mutableState.update { it.copy(loading = false, error = error.userMessage()) }
            } catch (error: CancellationException) { throw error }
        }
    }

    /** Mostra a escolha na hora e desfaz se a API recusar. */
    fun set(option: NotificationOption, enabled: Boolean) {
        val previous = mutableState.value.preferences ?: return
        if (mutableState.value.saving != null) return
        mutableState.update { it.copy(preferences = previous.with(option, enabled), saving = option, error = null) }
        viewModelScope.launch {
            try {
                val saved = repository.update(option, enabled)
                mutableState.update { it.copy(preferences = saved, saving = null) }
            } catch (error: ApiException) {
                mutableState.update { it.copy(preferences = previous, saving = null, error = error.userMessage()) }
            } catch (error: CancellationException) { throw error }
        }
    }

    fun sendTest() {
        if (mutableState.value.testing) return
        mutableState.update { it.copy(testing = true, testMessage = null) }
        viewModelScope.launch {
            val message = try {
                val result = repository.sendTest()
                when {
                    result.targets == 0 -> "Nenhum aparelho registrado nesta conta. Ative as notificações e tente de novo."
                    result.sent > 0 -> "Notificação de teste enviada. Ela pode levar alguns instantes para chegar."
                    else -> "Não foi possível entregar a notificação de teste."
                }
            } catch (error: ApiException) {
                // O detail da API diz, por exemplo, que o envio está desativado no servidor.
                error.detail ?: error.userMessage()
            } catch (error: CancellationException) { throw error }
            mutableState.update { it.copy(testing = false, testMessage = message) }
        }
    }
}

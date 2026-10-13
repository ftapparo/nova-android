package br.com.novaresidence.feature.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.session.SessionStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LegalViewModel(private val repository: LegalRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(LegalState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun load(refresh: Boolean = false) {
        if (loadJob?.isActive == true || (!refresh && state.value.documents != null)) return
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val documents = repository.documents(refresh)
                mutableState.update { it.copy(documents = documents, loading = false) }
            } catch (error: ApiException) {
                mutableState.update { it.copy(loading = false, error = error.userMessage()) }
            } catch (error: CancellationException) { throw error }
        }
    }

    /**
     * Aceita a versão do Termo exibida e, em seguida, [refreshUser] recarrega a conta (o `mustAcceptTerms`
     * só sai da sessão quando a API confirma).
     */
    fun acceptTerms(refreshUser: suspend () -> Unit) {
        val version = state.value.find(TermsDocument)?.version ?: return
        if (state.value.accepting) return
        viewModelScope.launch {
            mutableState.update { it.copy(accepting = true, acceptError = null) }
            try {
                repository.acceptTerms(version)
                refreshUser()
            } catch (error: ApiException) {
                val message = if (error.kind == ApiErrorKind.Conflict) {
                    // Versão nova publicada enquanto a tela estava aberta: mostra a vigente antes de aceitar.
                    load(refresh = true)
                    "O Termo de Uso foi atualizado. Leia a versão vigente antes de aceitar."
                } else error.userMessage()
                mutableState.update { it.copy(acceptError = message) }
            } catch (error: SessionStorageException) {
                mutableState.update { it.copy(acceptError = error.userMessage) }
            } catch (error: CancellationException) { throw error }
            finally { mutableState.update { it.copy(accepting = false) } }
        }
    }
}

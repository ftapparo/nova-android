package br.com.novaresidence.feature.residence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.session.SessionStorageException
import br.com.novaresidence.feature.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class ResidenceState(
    val residence: Residence? = null,
    val selectedUnit: Int? = null,
    // The first residence lookup decides which navigation items exist. Keep the
    // branded launch state visible until it finishes instead of showing a partial home.
    val loading: Boolean = true,
    val error: String? = null,
    val requestId: String? = null,
    val retrySeconds: Long = 0,
    val savingPreference: Boolean = false,
    val preferenceError: String? = null,
    val languageSaves: Int = 0,
)

class ResidenceViewModel(
    private val repository: ResidenceRepository,
    private val auth: AuthRepository,
    private val preferences: ResidencePreferences? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ResidenceState())
    val state = mutableState.asStateFlow()
    private var load: Job? = null
    private var save: Job? = null

    fun clear() { load?.cancel(); save?.cancel(); mutableState.value = ResidenceState() }
    fun saveLanguage() {
        val store = preferences ?: return
        if (state.value.savingPreference) return
        save = viewModelScope.launch {
            mutableState.update { it.copy(savingPreference = true, preferenceError = null) }
            try {
                withContext(Dispatchers.IO) { store.savePortuguese() }
                mutableState.update { it.copy(languageSaves = it.languageSaves + 1) }
            } catch (_: IOException) {
                mutableState.update { it.copy(preferenceError = "Não foi possível salvar o idioma. Tente novamente.") }
            } finally { mutableState.update { it.copy(savingPreference = false) } }
        }
    }
    fun select(id: Int) { if (state.value.residence?.units?.any { it.sequencia == id } == true) mutableState.update { it.copy(selectedUnit = id) } }
    fun refresh() {
        if (load?.isActive == true || state.value.retrySeconds > 0) return
        load = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null, requestId = null) }
            try {
                val user = auth.me()
                if (user.mustChangePassword) { mutableState.value = ResidenceState(); return@launch }
                val residence = repository.me()
                mutableState.update { it.copy(residence = residence, selectedUnit = selectResidenceUnit(residence, it.selectedUnit)) }
            } catch (error: ApiException) {
                if (error.kind == br.com.novaresidence.core.network.ApiErrorKind.Forbidden) {
                    try { auth.me() } catch (failure: ApiException) {
                        mutableState.update { it.copy(error = failure.userMessage(), requestId = failure.requestId, residence = null) }
                        return@launch
                    }
                }
                mutableState.update { it.copy(error = error.userMessage(), requestId = error.requestId, residence = null) }
                if (error.retryAfterSeconds > 0) {
                    mutableState.update { it.copy(retrySeconds = error.retryAfterSeconds) }
                    viewModelScope.launch {
                        val end = System.nanoTime() + error.retryAfterSeconds * 1_000_000_000
                        while (state.value.retrySeconds > 0) {
                            delay(1000)
                            mutableState.update { it.copy(retrySeconds = ((end - System.nanoTime() + 999_999_999) / 1_000_000_000).coerceAtLeast(0)) }
                        }
                    }
                }
            } catch (error: SessionStorageException) {
                mutableState.update { it.copy(error = error.userMessage, residence = null) }
            } catch (error: CancellationException) { throw error }
            finally { mutableState.update { it.copy(loading = false) } }
        }
    }
}

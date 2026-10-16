package br.com.novaresidence.feature.residence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.R
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.session.SessionStorageException
import br.com.novaresidence.core.ui.UiText
import br.com.novaresidence.core.ui.asUiText
import br.com.novaresidence.core.ui.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResidenceLinkState(
    /** Pessoa encontrada pela conferência (07c); nula enquanto o morador preenche CPF e celular (07b). */
    val check: SelfLinkCheck? = null,
    val busy: Boolean = false,
    val cpfError: UiText? = null,
    val phoneError: UiText? = null,
    val error: UiText? = null,
    val requestId: String? = null,
    val retrySeconds: Long = 0,
)

/**
 * Vínculo automático da conta (Figma 07b e 07c). CPF e celular ficam só na tela e na chamada à API, que
 * os compara com o cadastro do condomínio e os descarta; o app também não guarda nenhum dos dois.
 */
class ResidenceLinkViewModel(private val repository: ResidenceRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ResidenceLinkState())
    val state = mutableState.asStateFlow()

    fun check(cpf: String, country: PhoneCountry, phone: String) {
        val current = state.value
        if (current.busy || current.retrySeconds > 0) return
        val cpfError = if (isValidCpf(cpf)) null else uiText(R.string.link_invalid_cpf)
        val phoneError = if (isPlausiblePhone(country, phone)) null
            else uiText(if (country.isBrazil) R.string.link_invalid_phone_br else R.string.link_invalid_phone)
        if (cpfError != null || phoneError != null) {
            mutableState.update { it.copy(cpfError = cpfError, phoneError = phoneError, error = null) }
            return
        }
        run { mutableState.update { it.copy(check = repository.checkSelfLink(cpf, country.ddi, phone)) } }
    }

    /** Confirma o vínculo; [linked] recebe a situação residencial já atualizada. */
    fun confirm(linked: (Residence) -> Unit) {
        val confirmation = state.value.check?.confirmation ?: return
        if (state.value.busy) return
        run(expiredGoesBack = true) {
            val residence = repository.confirmSelfLink(confirmation)
            mutableState.value = ResidenceLinkState()
            linked(residence)
        }
    }

    /** "Voltar" da conferência (07c): volta ao formulário, que precisa ser enviado de novo. */
    fun back() { if (!state.value.busy) mutableState.update { ResidenceLinkState(retrySeconds = it.retrySeconds) } }

    fun clear() { mutableState.value = ResidenceLinkState() }

    private fun run(expiredGoesBack: Boolean = false, block: suspend () -> Unit) {
        mutableState.update { it.copy(busy = true, error = null, cpfError = null, phoneError = null, requestId = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: ApiException) {
                val message = when (error.kind) {
                    // A API responde igual para CPF inexistente, celular diferente ou pessoa sem unidade.
                    ApiErrorKind.NotFound -> uiText(if (expiredGoesBack) R.string.link_expired else R.string.link_not_matched)
                    ApiErrorKind.Conflict -> error.detail?.asUiText() ?: uiText(R.string.link_person_taken)
                    ApiErrorKind.Validation -> uiText(R.string.link_invalid_cpf)
                    else -> error.userMessage()
                }
                mutableState.update {
                    it.copy(
                        // Conferência vencida ou já usada: o morador precisa informar os dados de novo.
                        check = if (expiredGoesBack && error.kind == ApiErrorKind.NotFound) null else it.check,
                        cpfError = if (error.kind == ApiErrorKind.Validation) message else null,
                        error = if (error.kind == ApiErrorKind.Validation) null else message,
                        requestId = error.requestId,
                    )
                }
                if (error.retryAfterSeconds > 0) cooldown(error.retryAfterSeconds)
            } catch (error: SessionStorageException) {
                mutableState.update { it.copy(error = error.userMessage) }
            } catch (error: CancellationException) {
                throw error
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private fun cooldown(seconds: Long) {
        val end = System.nanoTime() + seconds * 1_000_000_000
        mutableState.update { it.copy(retrySeconds = seconds) }
        viewModelScope.launch {
            while (state.value.retrySeconds > 0) {
                delay(1000)
                mutableState.update { it.copy(retrySeconds = ((end - System.nanoTime() + 999_999_999) / 1_000_000_000).coerceAtLeast(0)) }
            }
        }
    }
}

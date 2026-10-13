package br.com.novaresidence.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import br.com.novaresidence.core.session.SessionStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

enum class AuthPage { Welcome, Login, Signup, Recovery, RecoveryConfirm, ChangePassword, Error }

data class AuthState(
    val page: AuthPage = AuthPage.Welcome,
    val busy: Boolean = false,
    val error: String? = null,
    val requestId: String? = null,
    val fields: Map<String, String> = emptyMap(),
    val retrySeconds: Long = 0,
    val recoveryEmail: String = "",
    /** Falha da exclusão da conta (16): aparece na área logada, separada dos erros das telas de entrada. */
    val deleteError: String? = null,
)

/** [termsVersion]: versão vigente do Termo de Uso, a que o cadastro e o login com Google mostram. */
class AuthViewModel(private val repository: AuthRepository, private val termsVersion: suspend () -> String) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()
    private var cooldownEnd = 0L
    private var lastPage = AuthPage.Login

    fun navigate(page: AuthPage) {
        if (state.value.busy) return
        mutableState.update { it.copy(page = page, error = null, fields = emptyMap(), requestId = null) }
    }

    fun back() = navigate(when (state.value.page) {
        AuthPage.Error -> lastPage
        AuthPage.RecoveryConfirm -> AuthPage.Recovery
        AuthPage.Recovery -> AuthPage.Login
        else -> AuthPage.Welcome
    })

    fun requirePasswordChange() {
        mutableState.update { it.copy(page = AuthPage.ChangePassword, error = null, fields = emptyMap()) }
    }

    fun showMessage(message: String) { mutableState.update { it.copy(error = message) } }

    private fun action(errors: Map<String, String> = emptyMap(), allowDuringCooldown: Boolean = false, block: suspend () -> Unit) {
        if (state.value.busy || (!allowDuringCooldown && state.value.retrySeconds > 0)) return
        if (errors.isNotEmpty()) { mutableState.update { it.copy(fields = errors, error = null) }; return }
        mutableState.update { it.copy(busy = true, error = null, fields = emptyMap(), requestId = null, deleteError = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: ApiException) {
                mutableState.update { it.copy(
                    error = error.userMessage(), requestId = error.requestId,
                    fields = error.fieldErrors.associate { field -> field.path.removePrefix("/") to field.message },
                ) }
                if (error.retryAfterSeconds > 0) cooldown(error.retryAfterSeconds)
                if (error.kind == br.com.novaresidence.core.network.ApiErrorKind.Unauthorized && state.value.page == AuthPage.Login) {
                    lastPage = AuthPage.Login
                    mutableState.update { it.copy(page = AuthPage.Error) }
                }
            } catch (error: SessionStorageException) {
                showMessage(error.userMessage)
            } catch (error: CancellationException) {
                throw error
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private fun cooldown(seconds: Long) {
        cooldownEnd = System.nanoTime() + seconds * 1_000_000_000
        mutableState.update { it.copy(retrySeconds = seconds) }
        viewModelScope.launch {
            while (state.value.retrySeconds > 0) {
                delay(1000)
                mutableState.update { it.copy(retrySeconds = max(0, (cooldownEnd - System.nanoTime() + 999_999_999) / 1_000_000_000)) }
            }
        }
    }

    fun login(email: String, password: String) = action(validateAuthForm(email, password)) { repository.login(email.trim(), password) }
    fun signup(email: String, password: String) = action(validateAuthForm(email, password, newPassword = true)) {
        repository.signup(email.trim(), password, termsVersion())
    }
    fun recover(email: String) = action(validateAuthForm(email)) {
        repository.recover(email.trim())
        mutableState.update { it.copy(page = AuthPage.RecoveryConfirm, recoveryEmail = email.trim()) }
    }
    fun confirm(code: String, password: String) = action(validateAuthForm(password = password, newPassword = true, code = code)) {
        repository.confirmRecovery(state.value.recoveryEmail, code, password)
    }
    fun change(current: String, password: String) = action(
        validateAuthForm(password = password, newPassword = true) +
            if (current.isEmpty()) mapOf("currentPassword" to "Informe sua senha atual.") else emptyMap(),
    ) { repository.changePassword(current, password) }

    fun google(getToken: suspend () -> String) = action {
        val token = try { getToken() }
        catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) { return@action }
        catch (_: androidx.credentials.exceptions.NoCredentialException) {
            showMessage("Nenhuma conta Google disponível. Adicione uma conta ao dispositivo ou entre com e-mail.")
            return@action
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) {
            showMessage("Não foi possível entrar com Google. Tente novamente ou use seu e-mail.")
            return@action
        }
        val version = try { termsVersion() } catch (_: ApiException) { null }
        repository.google(token, version)
    }

    fun logout() = action(allowDuringCooldown = true) {
        try { repository.logout() } finally { mutableState.update { it.copy(page = AuthPage.Welcome) } }
    }

    /** Excluir conta (16). Em caso de sucesso a sessão já foi limpa e a pessoa volta às boas-vindas. */
    fun deleteAccount() {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, deleteError = null) }
        viewModelScope.launch {
            try {
                repository.deleteAccount()
                mutableState.value = AuthState(error = "Sua conta foi excluída.")
            } catch (error: ApiException) {
                mutableState.update { it.copy(deleteError = when (error.kind) {
                    ApiErrorKind.Forbidden -> "Contas da equipe são removidas pela administração do condomínio."
                    // 503: o login não foi excluído e a API desfez o restante.
                    ApiErrorKind.Unavailable -> "Serviço indisponível. Nenhum dado foi excluído; tente novamente."
                    else -> error.userMessage()
                }) }
            } catch (error: SessionStorageException) {
                mutableState.update { it.copy(deleteError = error.userMessage) }
            } catch (error: CancellationException) {
                throw error
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}

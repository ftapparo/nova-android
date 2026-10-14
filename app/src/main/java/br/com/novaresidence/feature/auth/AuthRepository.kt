package br.com.novaresidence.feature.auth

import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.session.SessionData
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * [beforeSignOut] recebe o access token antes do logout na API (remove o aparelho do push; ver AppGraph) e
 * [afterAccountDeleted] roda depois da exclusão confirmada. Nenhum dos dois pode impedir a saída.
 */
class AuthRepository(
    private val api: ApiClient,
    private val beforeSignOut: suspend (accessToken: String) -> Unit = {},
    private val afterAccountDeleted: () -> Unit = {},
) {
    private suspend fun authenticate(path: String, vararg fields: Pair<String, String?>) {
        val generation = api.session.generation()
        val session = api.transport.post<SessionData>(path, buildJsonObject { fields.forEach { (key, value) -> if (value != null) put(key, value) } })
        api.session.replace(session, generation)
    }

    suspend fun login(email: String, password: String) = authenticate("auth/login", "email" to email, "password" to password)
    /** [termsVersion]: versão vigente do Termo de Uso aceita na tela de cadastro (a API recusa outra). */
    suspend fun signup(email: String, password: String, termsVersion: String) =
        authenticate("auth/signup", "email" to email, "password" to password, "acceptedTermsVersion" to termsVersion)
    // A API registra o aceite só na primeira vez de cada versão. Sem a versão (catálogo indisponível), a
    // conta volta com `mustAcceptTerms` e o app pede o aceite depois, em vez de impedir o login.
    suspend fun google(idToken: String, termsVersion: String?) =
        authenticate("auth/google", "idToken" to idToken, "acceptedTermsVersion" to termsVersion)
    suspend fun recover(email: String) {
        api.transport.request("auth/password/recover", buildJsonObject { put("email", email) }, post = true)
    }
    suspend fun confirmRecovery(email: String, code: String, newPassword: String) = authenticate(
        "auth/password/recover/confirm", "email" to email, "code" to code, "newPassword" to newPassword,
    )
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val generation = api.session.generation()
        val updated = api.post<SessionData>("auth/password/change", buildJsonObject {
            put("currentPassword", currentPassword); put("newPassword", newPassword)
        })
        api.session.replace(updated, generation)
    }
    suspend fun me(): AuthUser {
        val generation = api.session.generation()
        return api.get<AuthUser>("auth/me").also { api.session.updateUser(it, generation) }
    }
    suspend fun logout() = api.session.logout {
        beforeSignOut(it)
        api.transport.request("auth/logout", token = it, post = true)
    }

    /**
     * Exclui a própria conta (`DELETE /auth/me`). Só limpa a sessão local depois da confirmação da API:
     * numa falha, nada foi excluído e a pessoa continua conectada para tentar de novo.
     */
    suspend fun deleteAccount() {
        api.session.authorize { api.transport.request("auth/me", buildJsonObject { put("confirm", true) }, it, delete = true) }
        api.session.clear()
        afterAccountDeleted()
    }
}

fun validateAuthForm(
    email: String? = null,
    password: String? = null,
    newPassword: Boolean = false,
    code: String? = null,
): Map<String, String> = buildMap {
    if (email != null && !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())) put("email", "Informe um e-mail válido.")
    if (password != null && (password.isEmpty() || (newPassword && password.length < 8))) {
        put(if (newPassword) "newPassword" else "password", if (newPassword) "Use pelo menos 8 caracteres." else "Informe sua senha.")
    }
    if (code != null && !Regex("^[0-9]{6}$").matches(code)) put("code", "Informe o código de 6 dígitos.")
}

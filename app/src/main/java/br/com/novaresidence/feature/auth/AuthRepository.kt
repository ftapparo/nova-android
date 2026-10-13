package br.com.novaresidence.feature.auth

import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.session.AuthUser
import br.com.novaresidence.core.session.SessionData
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(private val api: ApiClient) {
    private suspend fun authenticate(path: String, vararg fields: Pair<String, String>) {
        val generation = api.session.generation()
        val session = api.transport.post<SessionData>(path, buildJsonObject { fields.forEach { put(it.first, it.second) } })
        api.session.replace(session, generation)
    }

    suspend fun login(email: String, password: String) = authenticate("auth/login", "email" to email, "password" to password)
    suspend fun signup(email: String, password: String) = authenticate("auth/signup", "email" to email, "password" to password)
    suspend fun google(idToken: String) = authenticate("auth/google", "idToken" to idToken)
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
    suspend fun logout() = api.session.logout { api.transport.request("auth/logout", token = it, post = true) }
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

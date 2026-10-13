package br.com.novaresidence.core.session

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String?,
    val role: String?,
    val mustChangePassword: Boolean,
    /** Conta antiga ou Termo de Uso em versão nova: o app pede o aceite antes de seguir. */
    val mustAcceptTerms: Boolean = false,
)

@Serializable
data class SessionData(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val expiresAt: Long,
    val user: AuthUser,
)

interface SessionStore {
    fun read(): SessionData?
    fun write(session: SessionData)
    fun clear()
}

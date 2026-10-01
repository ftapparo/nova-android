package br.com.novaresidence.core.session

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String?,
    val role: String?,
    val mustChangePassword: Boolean,
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

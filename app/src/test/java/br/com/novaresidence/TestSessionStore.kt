package br.com.novaresidence

import br.com.novaresidence.core.session.*

class TestSessionStore(var value: SessionData? = null) : SessionStore {
    override fun read() = value
    override fun write(session: SessionData) { value = session }
    override fun clear() { value = null }
}

fun testSession(access: String = "access", refresh: String = "refresh", mandatory: Boolean = false) = SessionData(
    access, refresh, "bearer", 900, 1790636017, AuthUser("test-account", "morador@example.com", "morador", mandatory),
)

package br.com.novaresidence.feature.auth

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.testSession
import br.com.novaresidence.core.network.*
import br.com.novaresidence.core.session.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {
    private val server = MockWebServer()
    private val store = TestSessionStore()
    private lateinit var session: SessionCoordinator
    private lateinit var auth: AuthRepository
    @Before fun setup() {
        server.start()
        val transport = ApiTransport(server.url("/").toString())
        session = SessionCoordinator(store) { transport.post("auth/refresh", buildJsonObject { put("refreshToken", it) }) }
        auth = AuthRepository(ApiClient(transport, session))
    }
    @After fun cleanup() { server.shutdown() }
    private fun enqueueSession(mandatory: Boolean = false) {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":${NovaJson.encodeToString(testSession(mandatory = mandatory))},"meta":{}}"""))
    }
    private fun takeBody(path: String): JsonObject {
        val request = server.takeRequest()
        assertEquals("/v3/api/$path", request.path)
        assertEquals("POST", request.method)
        return NovaJson.parseToJsonElement(request.body.readUtf8()).jsonObject
    }

    @Test fun `login and signup store session and send exactly email and password`() = runBlocking {
        enqueueSession(true)
        auth.login("morador@example.com", "temporary")
        assertEquals(setOf("email", "password"), takeBody("auth/login").keys)
        assertTrue(session.user.value!!.mustChangePassword)
        enqueueSession()
        auth.signup("new@example.com", "password123")
        assertEquals(setOf("email", "password"), takeBody("auth/signup").keys)
        assertFalse(session.user.value!!.mustChangePassword)
    }

    @Test fun `Google sends only idToken and recovery uses six digit code and fresh password`() = runBlocking {
        enqueueSession()
        auth.google("google-token")
        assertEquals(setOf("idToken"), takeBody("auth/google").keys)
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"success":true,"data":null,"meta":{}}"""))
        auth.recover("morador@example.com")
        assertEquals(setOf("email"), takeBody("auth/password/recover").keys)
        enqueueSession()
        auth.confirmRecovery("morador@example.com", "123456", "password123")
        val body = takeBody("auth/password/recover/confirm")
        assertEquals(setOf("email", "code", "newPassword"), body.keys)
        assertEquals("123456", body["code"]!!.jsonPrimitive.content)
    }

    @Test fun `password change replaces mandatory session and logout clears it`() = runBlocking {
        session.replace(testSession(mandatory = true), session.generation())
        enqueueSession()
        auth.changePassword("temporary", "password123")
        assertEquals(setOf("currentPassword", "newPassword"), takeBody("auth/password/change").keys)
        assertFalse(session.user.value!!.mustChangePassword)
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null,"meta":{}}"""))
        auth.logout()
        assertEquals("/v3/api/auth/logout", server.takeRequest().path)
        assertNull(store.value)
    }

    @Test fun `validation accepts login passwords below signup minimum and preserves password whitespace`() {
        assertTrue(validateAuthForm("resident@example.com", "abc").isEmpty())
        assertEquals(setOf("newPassword"), validateAuthForm("resident@example.com", "abc", true).keys)
        assertEquals(setOf("email", "code"), validateAuthForm("invalid", "password123", true, "123").keys)
        assertTrue(validateAuthForm(password = "        ", newPassword = true, code = "012345").isEmpty())
    }
}

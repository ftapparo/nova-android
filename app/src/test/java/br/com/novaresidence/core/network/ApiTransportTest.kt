package br.com.novaresidence.core.network

import br.com.novaresidence.core.session.AuthUser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ApiTransportTest {
    private val server = MockWebServer()
    private lateinit var transport: ApiTransport
    @Before fun setup() { server.start(); transport = ApiTransport(server.url("/").toString()) }
    @After fun cleanup() { server.shutdown() }

    @Test fun `success decodes typed data and sends bearer only to protected request`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"id":"id","email":null,"role":"morador","mustChangePassword":false},"meta":{"requestId":"req-1"}}"""))
        val user = transport.get<AuthUser>("auth/me", "token")
        assertEquals("morador", user.role)
        assertNull(user.email)
        val request = server.takeRequest()
        assertEquals("/v3/api/auth/me", request.path)
        assertEquals("Bearer token", request.getHeader("Authorization"))
    }

    @Test fun `error types override changing detail text and carry retry and request reference`() = runBlocking {
        val cases = listOf(400 to "validation-error", 401 to "unauthorized", 403 to "forbidden", 404 to "not-found", 409 to "conflict", 429 to "rate-limited", 503 to "upstream-error", 500 to "internal-error")
        val expected = listOf(ApiErrorKind.Validation, ApiErrorKind.Unauthorized, ApiErrorKind.Forbidden, ApiErrorKind.NotFound, ApiErrorKind.Conflict, ApiErrorKind.RateLimited, ApiErrorKind.Unavailable, ApiErrorKind.Internal)
        cases.forEachIndexed { index, (status, type) ->
            server.enqueue(MockResponse().setResponseCode(status).setHeader("Retry-After", "42").setBody("""{"success":false,"error":{"type":"urn:nova-api:$type","detail":"Texto variável","validationErrors":[{"path":"/email","message":"Inválido"}]},"meta":{"requestId":"req-test"}}"""))
            val failure = try { transport.request("auth/login", post = true); error("Expected failure") } catch (error: ApiException) { error }
            assertEquals(expected[index], failure.kind)
            assertEquals("req-test", failure.requestId)
            assertEquals(42L, failure.retryAfterSeconds)
            assertEquals(FieldError("/email", "Inválido"), failure.fieldErrors.single())
        }
    }

    @Test fun `malformed envelope and typed response become recoverable failures`() = runBlocking {
        for (body in listOf("not json", "[]", "{}", "{\"success\":true}", "{\"success\":true,\"data\":null}")) {
            server.enqueue(MockResponse().setBody(body))
            try { transport.get<AuthUser>("auth/me"); fail("Expected invalid response") }
            catch (error: ApiException) { assertEquals(ApiErrorKind.InvalidResponse, error.kind) }
        }
    }

    @Test fun `disconnected server becomes network error without retrying a post`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        try { transport.request("auth/login", post = true); fail("Expected network failure") }
        catch (error: ApiException) { assertEquals(ApiErrorKind.Network, error.kind) }
        assertEquals(1, server.requestCount)
    }

    @Test fun `bodyless logout does not advertise an invalid empty JSON document`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":null}"""))
        transport.request("auth/logout", token = "token", post = true)
        val request = server.takeRequest()
        assertEquals("/v3/api/auth/logout", request.path)
        assertEquals(0L, request.bodySize)
        assertNull(request.getHeader("Content-Type"))
    }
}

package br.com.novaresidence.feature.legal

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.testSession
import br.com.novaresidence.core.network.*
import br.com.novaresidence.core.session.SessionCoordinator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LegalRepositoryTest {
    private val server = MockWebServer()
    private lateinit var legal: LegalRepository
    @Before fun setup() {
        server.start()
        val transport = ApiTransport(server.url("/").toString())
        val session = SessionCoordinator(TestSessionStore(testSession())) { error("sem renovação neste teste") }
        session.restore()
        legal = LegalRepository(ApiClient(transport, session))
    }
    @After fun cleanup() { server.shutdown() }

    private val catalog = """{"success":true,"data":[
        {"document":"termo-de-uso","title":"Termo de Uso","version":"1.0","effectiveDate":"2026-10-13","sha256":"ab",
         "htmlUrl":"https://novaresidence.com.br/termos-de-uso","pdfUrl":"https://novaresidence.com.br/termos-de-uso/pdf"},
        {"document":"politica-de-privacidade","title":"Política de Privacidade","version":"1.0","effectiveDate":"2026-10-13","sha256":"cd",
         "htmlUrl":"https://novaresidence.com.br/politica-de-privacidade","pdfUrl":"https://novaresidence.com.br/politica-de-privacidade/pdf"}
    ],"meta":{}}"""

    @Test fun `catalog is public, kept in memory and gives the current terms version`() = runBlocking {
        server.enqueue(MockResponse().setBody(catalog))
        assertEquals("1.0", legal.termsVersion())
        assertEquals(2, legal.documents().size)
        val request = server.takeRequest()
        assertEquals("/v3/api/legal", request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals(1, server.requestCount)
    }

    @Test fun `acceptance sends the version shown and surfaces a newer version as conflict`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"termsVersion":"1.0","acceptedVersion":"1.0","acceptedAt":null,"mustAcceptTerms":false},"meta":{}}"""))
        legal.acceptTerms("1.0")
        val request = server.takeRequest()
        assertEquals("/v3/api/legal/terms/accept", request.path)
        assertEquals("Bearer access", request.getHeader("Authorization"))
        assertEquals("1.0", NovaJson.parseToJsonElement(request.body.readUtf8()).jsonObject["version"]!!.jsonPrimitive.content)
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"success":false,"error":{"type":"urn:nova-api:conflict","status":409},"meta":{}}"""))
        val failure = runCatching { legal.acceptTerms("1.0") }.exceptionOrNull() as ApiException
        assertEquals(ApiErrorKind.Conflict, failure.kind)
    }
}

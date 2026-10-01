package br.com.novaresidence.core.session

import br.com.novaresidence.TestSessionStore
import br.com.novaresidence.testSession
import br.com.novaresidence.core.network.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SessionCoordinatorTest {
    @Test fun `concurrent unauthorized requests exchange refresh token once and replace pair atomically`() = runBlocking {
        val store = TestSessionStore(testSession())
        val calls = AtomicInteger()
        val coordinator = SessionCoordinator(store) { token ->
            assertEquals("refresh", token)
            calls.incrementAndGet(); delay(50)
            testSession("renewed-access", "renewed-refresh")
        }
        coordinator.restore()
        val results = (1..8).map { async {
            coordinator.authorize { token ->
                if (token == "access") throw ApiException(ApiErrorKind.Unauthorized)
                token
            }
        } }.awaitAll()
        assertTrue(results.all { it == "renewed-access" })
        assertEquals(1, calls.get())
        assertEquals("renewed-refresh", store.value?.refreshToken)
    }

    @Test fun `logout during refresh never restores revoked local session`() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val store = TestSessionStore(testSession())
        val coordinator = SessionCoordinator(store) { entered.complete(Unit); finish.await(); testSession("new", "new-refresh") }
        coordinator.restore()
        val request = async { coordinator.authorize { throw ApiException(ApiErrorKind.Unauthorized) } }
        entered.await()
        coordinator.logout { assertEquals("access", it) }
        finish.complete(Unit)
        try { request.await(); fail("Expected cancelled stale session") } catch (_: CancellationException) { }
        assertNull(store.value)
        assertNull(coordinator.user.value)
    }

    @Test fun `second unauthorized logs out and only retries once`() = runBlocking {
        val store = TestSessionStore(testSession())
        val coordinator = SessionCoordinator(store) { testSession("new", "new-refresh") }
        coordinator.restore()
        var attempts = 0
        try { coordinator.authorize { attempts++; throw ApiException(ApiErrorKind.Unauthorized) }; fail("Expected unauthorized") }
        catch (error: ApiException) { assertEquals(ApiErrorKind.Unauthorized, error.kind) }
        assertEquals(2, attempts)
        assertNull(store.value)
    }

    @Test fun `refresh failure and logout failure both clear local tokens`() = runBlocking {
        val store = TestSessionStore(testSession())
        val coordinator = SessionCoordinator(store) { throw ApiException(ApiErrorKind.Network) }
        coordinator.restore()
        try { coordinator.authorize { throw ApiException(ApiErrorKind.Unauthorized) } } catch (_: ApiException) { }
        assertNull(store.value)
        coordinator.replace(testSession(), coordinator.generation())
        try { coordinator.logout { throw ApiException(ApiErrorKind.Network) } } catch (_: ApiException) { }
        assertNull(store.value)
    }

    @Test fun `in flight authentication cannot replace session after logout`() {
        val store = TestSessionStore()
        val coordinator = SessionCoordinator(store) { testSession() }
        val generation = coordinator.generation()
        coordinator.clear()
        try { coordinator.replace(testSession(), generation); fail("Expected stale response rejected") }
        catch (_: CancellationException) { }
        assertNull(store.value)
    }
}

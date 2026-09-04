@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.SessionClearer
import cz.b2brental.data.remote.createB2bHttpClient
import cz.b2brental.data.repository.PaymentRepositoryImpl
import cz.b2brental.domain.model.PaymentStatus
import cz.b2brental.domain.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testy PaymentRepository — parsování statusů, akce pay, konflikt 409.
 */
public class PaymentRepositoryTest {

    /**
     * GET /payments → 200 s polem plateb paid/unpaid/overdue.
     */
    @Test
    public fun `list payments parses statuses`(): Unit = runTest {
        val listJson = """
            [
              {"id":1,"contractId":1,"period":1,"amount":"9000.00","dueDate":"2026-08-04","status":"paid","paidAt":"2026-08-04T00:00:00Z"},
              {"id":3,"contractId":1,"period":3,"amount":"9000.00","dueDate":"2026-10-04","status":"unpaid","paidAt":null},
              {"id":4,"contractId":1,"period":4,"amount":"9000.00","dueDate":"2026-11-04","status":"overdue","paidAt":null}
            ]
        """.trimIndent()
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/payments") {
                respond(
                    content = listJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            } else {
                respond("Not found", status = HttpStatusCode.NotFound)
            }
        }
        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), FakeTestTokenStorage(), FakeTestSessionClearer())
        val repository = PaymentRepositoryImpl(apiClient)

        val result = repository.list(null)

        assertEquals(3, result.size)
        assertEquals(PaymentStatus.PAID, result[0].status)
        assertNotNull(result[0].paidAt)
        assertEquals(PaymentStatus.UNPAID, result[1].status)
        assertNull(result[1].paidAt)
        assertEquals(PaymentStatus.OVERDUE, result[2].status)
        assertEquals("9000.00", result[0].amount)
        assertEquals("2026-10-04", result[1].dueDate)
    }

    /**
     * POST /payments/3/pay → 200 s PaymentActionResponse (id, status=paid, paidAt).
     */
    @Test
    public fun `pay payment returns updated status`(): Unit = runTest {
        val actionJson = """{"id":3,"status":"paid","paidAt":"2026-09-04T12:00:00Z"}"""
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/payments/3/pay") {
                respond(
                    content = actionJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            } else {
                respond("Not found", status = HttpStatusCode.NotFound)
            }
        }
        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), FakeTestTokenStorage(), FakeTestSessionClearer())
        val repository = PaymentRepositoryImpl(apiClient)

        val action = repository.pay(3L)

        assertEquals(3L, action.id)
        assertEquals(PaymentStatus.PAID, action.status)
        assertEquals("2026-09-04T12:00:00Z", action.paidAt)
    }

    /**
     * POST /payments/1/pay → 409 → ApiException(code=CONFLICT, httpStatus=409).
     */
    @Test
    public fun `pay payment 409 throws CONFLICT`(): Unit = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/payments/1/pay") {
                respond(
                    content = """{"error":{"code":"CONFLICT","message":"Platba již byla zaplacena"}}""",
                    status = HttpStatusCode.Conflict,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            } else {
                respond("Not found", status = HttpStatusCode.NotFound)
            }
        }
        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), FakeTestTokenStorage(), FakeTestSessionClearer())
        val repository = PaymentRepositoryImpl(apiClient)

        try {
            repository.pay(1L)
            throw AssertionError("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals("CONFLICT", e.code)
            assertEquals(409, e.httpStatus)
        }
    }

    private class FakeTestTokenStorage : TokenStorage {
        private val _session = MutableStateFlow<UserProfile?>(null)
        override val session: Flow<UserProfile?> = _session

        @Suppress("RedundantNullableReturnType")
        override suspend fun currentToken(): String? = "test-token"

        override suspend fun save(profile: UserProfile) { _session.value = profile }
        override suspend fun clear() { _session.value = null }
    }

    private class FakeTestSessionClearer : SessionClearer {
        override suspend fun clearSession() { /* no-op */ }
    }
}
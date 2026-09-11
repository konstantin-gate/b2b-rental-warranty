@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.*
import cz.b2brental.data.remote.dto.TicketCreateRequestDto
import cz.b2brental.data.repository.TicketRepositoryImpl
import cz.b2brental.domain.model.Severity
import cz.b2brental.domain.model.TicketStatus
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.WarrantyVerdict
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testy TicketRepository — přímé přesměrování na B2bApiClient.
 * Ověřuje parsování AI diagnostiky a chybové stavy (404, offline).
 */
class TicketRepositoryTest {

    /**
     * POST /tickets → 201 s AI diagnostikou (severity, warrantyVerdict, aiRecommendation).
     */
    @Test
    fun createTicketReturns201WithAiDiagnosis(): Unit = runTest {
        val ticketJson = """
            {"id":11,"equipmentId":1,"equipmentModel":"Liebherr GKv 5790","companyName":"Kuchyně s.r.o.","description":"Kompresor nechladí, teplota 12 °C","photoBase64":null,"severity":"critical","status":"new","warrantyVerdict":"covered","warrantyReason":"Porucha vznikla v záruční době","aiRecommendation":"Zkontrolujte kompresor a přívod chladiva","technicianId":null,"resolution":null,"createdAt":"2026-09-04T06:00:00Z","resolvedAt":null}
        """.trimIndent()
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/tickets") {
                respond(
                    content = ticketJson,
                    status = HttpStatusCode.Created,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            } else {
                respond("Not found", status = HttpStatusCode.NotFound)
            }
        }
        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), FakeTestTokenStorage(), FakeTestSessionClearer())
        val repository = TicketRepositoryImpl(apiClient)

        val ticket = repository.create(
            TicketCreateRequestDto(
                equipmentId = 1L,
                description = "Kompresor nechladí, teplota 12 °C",
            ),
        )

        assertEquals(11L, ticket.id)
        assertEquals(Severity.CRITICAL, ticket.severity)
        assertEquals(TicketStatus.NEW, ticket.status)
        assertEquals(WarrantyVerdict.COVERED, ticket.warrantyVerdict)
        assertEquals("Zkontrolujte kompresor a přívod chladiva", ticket.aiRecommendation)
    }

    /**
     * GET /tickets → 200 s polem tiketů různých stavů; nullable severity = null.
     */
    @Test
    fun listTicketsParsesStatuses(): Unit = runTest {
        val listJson = """
            [
              {"id":1,"equipmentId":5,"equipmentModel":"Infina NG 150","companyName":"Kuchyně s.r.o","description":"Chladnička nevychladuje, teplota 12 °C","status":"new","createdAt":"2026-09-01T08:00:00Z"},
              {"id":2,"equipmentId":3,"equipmentModel":"Gram Eco Mid K 410","companyName":"Kuchyně s.r.o","description":"Kompressor se nezapíná","status":"in_progress","createdAt":"2026-09-02T09:00:00Z"}
            ]
        """.trimIndent()
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/tickets") {
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
        val repository = TicketRepositoryImpl(apiClient)

        val result = repository.list()

        assertEquals(2, result.size)
        assertEquals(TicketStatus.NEW, result[0].status)
        assertEquals(TicketStatus.IN_PROGRESS, result[1].status)
        assertNull(result[1].severity)
    }

    /**
     * GET /tickets/99 → 404 → ApiException(code=NOT_FOUND, httpStatus=404).
     */
    @Test
    fun getTicket404ThrowsApiException(): Unit = runTest {
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/tickets/99") {
                respond(
                    content = """{"error":{"code":"NOT_FOUND","message":"Tiket nenalezen"}}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            } else {
                respond("Not found", status = HttpStatusCode.NotFound)
            }
        }
        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), FakeTestTokenStorage(), FakeTestSessionClearer())
        val repository = TicketRepositoryImpl(apiClient)

        try {
            repository.get(99L)
            throw AssertionError("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals("NOT_FOUND", e.code)
            assertEquals(404, e.httpStatus)
        }
    }

    /**
     * POST /tickets s IOException → OfflineException (transformace v safeApiCall).
     */
    @Test
    fun createTicketOfflineThrowsOfflineException(): Unit = runTest {
        val mockEngine = MockEngine { _ ->
            throw java.io.IOException("Connection refused")
        }
        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), FakeTestTokenStorage(), FakeTestSessionClearer())
        val repository = TicketRepositoryImpl(apiClient)

        try {
            repository.create(
                TicketCreateRequestDto(
                    equipmentId = 1L,
                    description = "Test offline",
                ),
            )
            throw AssertionError("Expected OfflineException")
        } catch (_: OfflineException) {
            // očekáváno
        }
    }

    private class FakeTestTokenStorage : TokenStorage {
        private val _session = MutableStateFlow<UserProfile?>(null)
        override val session: StateFlow<UserProfile?> = _session

        @Suppress("RedundantNullableReturnType")
        override suspend fun currentToken(): String? = "test-token"

        override suspend fun save(profile: UserProfile) {
            _session.value = profile
        }

        override suspend fun clear() {
            _session.value = null
        }
    }

    private class FakeTestSessionClearer : SessionClearer {
        override suspend fun clearSession() { /* no-op */
        }
    }
}

@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.SessionClearer
import cz.b2brental.data.remote.createB2bHttpClient
import cz.b2brental.data.repository.NotificationRepositoryImpl
import cz.b2brental.domain.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Testy NotificationRepository — parsování seznamu notifikací a označení všech jako přečtených.
 */
public class NotificationRepositoryTest {

    /**
     * GET /notifications → 200 s JSON polem notifikací; pole první položky jsou správně naparsovány.
     */
    @Test
    public fun `getNotifications parses list`(): Unit = runTest {
        val listJson = """
            [
              {"id":1,"message":"Nový servisní tiket #5 (Mrazicí skříň)","isRead":false,"createdAt":"2026-09-07T09:00:00Z"}
            ]
        """.trimIndent()
        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath == "/notifications" && request.method == HttpMethod.Get) {
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
        val repository = NotificationRepositoryImpl(apiClient)

        val result = repository.getNotifications(false)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("Nový servisní tiket #5 (Mrazicí skříň)", result[0].message)
        assertEquals(false, result[0].isRead)
        assertEquals("2026-09-07T09:00:00Z", result[0].createdAt)
    }

    /**
     * POST /notifications/read-all → 200 s {"count":0}; metoda provedla POST na správnou cestu.
     */
    @Test
    public fun `markAllRead returns zero`(): Unit = runTest {
        var lastMethod: HttpMethod? = null
        var lastPath: String? = null
        val mockEngine = MockEngine { request ->
            lastMethod = request.method
            lastPath = request.url.encodedPath
            respond(
                content = """{"count":0}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), FakeTestTokenStorage(), FakeTestSessionClearer())
        val repository = NotificationRepositoryImpl(apiClient)

        val result = repository.markAllRead()

        assertEquals(0, result)
        assertEquals(HttpMethod.Post, lastMethod)
        assertEquals("/notifications/read-all", lastPath)
        assertNotNull(lastPath)
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

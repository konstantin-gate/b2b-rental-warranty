@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.SessionClearer
import cz.b2brental.data.remote.createB2bHttpClient
import cz.b2brental.domain.model.UserProfile
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Fake implementace TokenStorage pro testy API klienta. */
private class FakeTokenStorageForApi : TokenStorage {
    private val _session = MutableStateFlow<UserProfile?>(null)
    override val session: StateFlow<UserProfile?> = _session
    private var storedToken: String? = "test-token"

    override suspend fun currentToken(): String? = storedToken

    override suspend fun save(profile: UserProfile): Unit {
        storedToken = profile.token
        _session.value = profile
    }

    override suspend fun clear(): Unit {
        storedToken = null
        _session.value = null
    }
}

/** Fake implementace SessionClearer pro testy — sleduje počet zavolání. */
private class FakeSessionClearerForApi : SessionClearer {
    var calledCount: Int = 0
    override suspend fun clearSession(): Unit {
        calledCount++
    }
}

/**
 * Testy B2bApiClient — ověření zpracování odpovědí (200, 401, 500)
 * a interakce se SessionClearer při vypršení session.
 */
public class B2bApiClientTest {

    @Test
    public fun `401 response triggers session clear and unauthorized event`(): TestResult = runTest {
        val fakeTokenStorage = FakeTokenStorageForApi()
        val fakeSessionClearer = FakeSessionClearerForApi()

        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":{"code":"UNAUTHORIZED","message":"Session expired"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val apiClient = B2bApiClient(
            createB2bHttpClient(mockEngine),
            fakeTokenStorage,
            fakeSessionClearer,
        )

        try {
            apiClient.getCatalog(null, null)
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals(401, e.httpStatus)
        }

        assertEquals(1, fakeSessionClearer.calledCount)
        assertNull(fakeTokenStorage.session.first())
    }

    @Test
    public fun `200 login response succeeds without session clear`(): TestResult = runTest {
        val fakeTokenStorage = FakeTokenStorageForApi()
        val fakeSessionClearer = FakeSessionClearerForApi()

        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"token":"new-token","userId":1,"role":"client","companyId":1,"email":"a@b.c"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val apiClient = B2bApiClient(
            createB2bHttpClient(mockEngine),
            fakeTokenStorage,
            fakeSessionClearer,
        )

        val result = apiClient.login("a@b.c", "p")
        assertEquals("new-token", result.token)
        assertEquals(0, fakeSessionClearer.calledCount)
    }

    @Test
    public fun `500 response throws ApiException without session clear`(): TestResult = runTest {
        val fakeTokenStorage = FakeTokenStorageForApi()
        val fakeSessionClearer = FakeSessionClearerForApi()

        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":{"code":"INTERNAL_ERROR","message":"Server error"}}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val apiClient = B2bApiClient(
            createB2bHttpClient(mockEngine),
            fakeTokenStorage,
            fakeSessionClearer,
        )

        try {
            apiClient.getCatalog(null, null)
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals(500, e.httpStatus)
        }

        assertEquals(0, fakeSessionClearer.calledCount)
    }
}

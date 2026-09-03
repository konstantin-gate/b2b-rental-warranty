@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.SessionClearer
import cz.b2brental.data.remote.createB2bHttpClient
import cz.b2brental.data.repository.AuthRepositoryImpl
import cz.b2brental.domain.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Fake implementace TokenStorage pro testy. */
private class FakeTokenStorage : TokenStorage {
    private val _session = MutableStateFlow<UserProfile?>(null)
    override val session: Flow<UserProfile?> = _session
    private var storedToken: String? = null

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

/** Fake implementace SessionClearer pro testy. */
private class FakeSessionClearer : SessionClearer {
    var calledCount: Int = 0
    override suspend fun clearSession(): Unit {
        calledCount++
    }
}

/**
 * Unit testy repozitáře [AuthRepositoryImpl].
 */
public class AuthRepositoryTest {

    private lateinit var fakeTokenStorage: FakeTokenStorage
    private lateinit var fakeSessionClearer: FakeSessionClearer

    /**
     * Příprava testovacího prostředí před každým testem.
     */
    @Before
    public fun setup(): Unit {
        fakeTokenStorage = FakeTokenStorage()
        fakeSessionClearer = FakeSessionClearer()
    }

    @Test
    public fun `login 200 saves session`(): TestResult = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/login" -> respond(
                    content = """{"token":"test-token","userId":1,"role":"client","companyId":1,"email":"test@test.com"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), fakeTokenStorage, fakeSessionClearer)
        val repository = AuthRepositoryImpl(apiClient, fakeTokenStorage)

        repository.login("test@test.com", "password123")

        val session = fakeTokenStorage.session.first()
        assertNotNull(session)
        assertEquals("test-token", session?.token)
        assertEquals(1L, session?.userId)
        assertEquals("test@test.com", session?.email)
    }

    @Test
    public fun `login 401 throws ApiException with UNAUTHORIZED`(): TestResult = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/login" -> respond(
                    content = """{"error":{"code":"UNAUTHORIZED","message":"Neplatné přihlašovací údaje"}}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), fakeTokenStorage, fakeSessionClearer)
        val repository = AuthRepositoryImpl(apiClient, fakeTokenStorage)

        try {
            repository.login("wrong@test.com", "wrongpass")
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals("UNAUTHORIZED", e.code)
            assertEquals(401, e.httpStatus)
        }
    }

    @Test
    public fun `network error throws OfflineException`(): TestResult = runTest {
        val mockEngine = MockEngine { request ->
            throw java.io.IOException("Connection refused")
        }

        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), fakeTokenStorage, fakeSessionClearer)
        val repository = AuthRepositoryImpl(apiClient, fakeTokenStorage)

        try {
            repository.login("test@test.com", "password123")
            fail("Expected OfflineException")
        } catch (_: OfflineException) {
            // Expected
        }
    }

    @Test
    public fun `register then auto-login saves session`(): TestResult = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/register-company" -> respond(
                    content = """{"companyId":2,"userId":3,"email":"admin@test.com","role":"admin"}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                )
                "/auth/login" -> respond(
                    content = """{"token":"reg-token","userId":3,"role":"admin","companyId":2,"email":"admin@test.com"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                )
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), fakeTokenStorage, fakeSessionClearer)
        val repository = AuthRepositoryImpl(apiClient, fakeTokenStorage)

        repository.registerCompany(
            companyName = "Test Corp",
            inn = "28745001",
            address = "Praha 1",
            adminEmail = "admin@test.com",
            password = "password123",
            phone = null,
        )

        val session = fakeTokenStorage.session.first()
        assertNotNull(session)
        assertEquals("reg-token", session?.token)
        assertEquals(3L, session?.userId)
    }
}

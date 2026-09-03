@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.local.dao.CatalogDao
import cz.b2brental.data.local.entity.CatalogEntity
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.SessionClearer
import cz.b2brental.data.remote.createB2bHttpClient
import cz.b2brental.data.repository.CatalogRepositoryImpl
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.domain.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testy CatalogRepository — online režim s API a offline režim z cache.
 */
class CatalogRepositoryTest {

    private lateinit var fakeTokenStorage: FakeTestTokenStorage
    private lateinit var fakeSessionClearer: FakeTestSessionClearer
    private lateinit var fakeDao: FakeTestCatalogDao

    @Before
    public fun setup(): Unit {
        fakeTokenStorage = FakeTestTokenStorage()
        fakeSessionClearer = FakeTestSessionClearer()
        fakeDao = FakeTestCatalogDao()
    }

    @Test
    public fun `online mode returns items from API and saves to cache`(): Unit = runTest {
        val catalogJson = """
            [
                {"id":1,"categoryId":1,"categoryName":"Chladici","model":"Chladic 1","serialNumber":"SN-CHK-001","price":"9000.00","monthlyRate":"4500.00","description":null,"photoUrl":null,"status":"available"},
                {"id":2,"categoryId":1,"categoryName":"Chladici","model":"Chladic 2","serialNumber":"SN-CHK-002","price":"7200.00","monthlyRate":"3600.00","description":null,"photoUrl":null,"status":"available"}
            ]
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/catalog" -> respond(
                    content = catalogJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
                else -> respond("Not found", status = HttpStatusCode.NotFound)
            }
        }

        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), fakeTokenStorage, fakeSessionClearer)
        val repository = CatalogRepositoryImpl(apiClient, fakeDao)

        val result = repository.list(null, null)

        assertEquals(2, result.items.size)
        assertFalse(result.isOffline)
        assertEquals(2, fakeDao.storedEntities.size)
        assertEquals("Chladic 1", result.items[0].model)
    }

    @Test
    public fun `offline mode returns items from cache`(): Unit = runTest {
        val mockEngine = MockEngine { _ ->
            throw java.io.IOException("Connection refused")
        }

        fakeDao.cachedEntities = listOf(
            createEntity(id = 10, model = "Cached 1"),
            createEntity(id = 20, model = "Cached 2"),
        )

        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), fakeTokenStorage, fakeSessionClearer)
        val repository = CatalogRepositoryImpl(apiClient, fakeDao)

        val result = repository.list(null, null)

        assertEquals(2, result.items.size)
        assertTrue(result.isOffline)
        assertEquals("Cached 1", result.items[0].model)
        assertEquals("Cached 2", result.items[1].model)
    }

    @Test
    public fun `get single equipment from API`(): Unit = runTest {
        val equipmentJson = """
            {"id":5,"categoryId":1,"categoryName":"Chladici","model":"Test Model","serialNumber":"SN-TEST-5","price":"9000.00","monthlyRate":"4500.00","description":"Test popis","photoUrl":null,"status":"available"}
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/catalog/5" -> respond(
                    content = equipmentJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
                else -> respond("Not found", status = HttpStatusCode.NotFound)
            }
        }

        val apiClient = B2bApiClient(createB2bHttpClient(mockEngine), fakeTokenStorage, fakeSessionClearer)
        val repository = CatalogRepositoryImpl(apiClient, fakeDao)

        val result = repository.get(5)

        assertEquals(5L, result.id)
        assertEquals("Test Model", result.model)
        assertEquals("Test popis", result.description)
    }

    // --- Pomocné funkce ---

    private fun createEntity(
        id: Long = 1,
        model: String = "Test",
        status: EquipmentStatus = EquipmentStatus.AVAILABLE,
    ): CatalogEntity {
        return CatalogEntity(
            id = id,
            categoryId = 1,
            categoryName = "Test Category",
            model = model,
            serialNumber = "SN-TEST-$id",
            price = "9000.00",
            monthlyRate = "4500.00",
            description = null,
            photoUrl = null,
            status = status,
            cachedAt = System.currentTimeMillis(),
        )
    }

    // --- Fake implementace ---

    /**
     * Testovací implementace TokenStorage, která vždy vrací pevný JWT token.
     * Potlačení upozornění IDE na neshodu s nullable typem rozhraní je záměrné:
     * fake záměrně nikdy nevrací null, ale musí respektovat kontrakt rozhraní.
     */
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

    private class FakeTestCatalogDao : CatalogDao {
        var storedEntities: MutableList<CatalogEntity> = mutableListOf()
        var cachedEntities: List<CatalogEntity> = emptyList()

        override fun getAll(): Flow<List<CatalogEntity>> = flowOf(cachedEntities)
        override suspend fun upsertAll(items: List<CatalogEntity>) { storedEntities.addAll(items) }
        override suspend fun clearAll() { storedEntities.clear() }
    }
}

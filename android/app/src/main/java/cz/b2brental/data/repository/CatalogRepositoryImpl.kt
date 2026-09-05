package cz.b2brental.data.repository

import cz.b2brental.data.local.dao.CatalogDao
import cz.b2brental.data.local.entity.CatalogEntity
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.data.remote.dto.CatalogUpsertRequestDto
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.domain.repository.CatalogPage
import cz.b2brental.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.first

/**
 * Implementace CatalogRepository — online přes API s offline cache přes Room.
 * Při nedostupnosti backendu vrací data z lokální cache.
 * @param apiClient HTTP klient pro backend API
 * @param catalogDao DAO pro přístup k Room cache katalogu
 */
public class CatalogRepositoryImpl(
    private val apiClient: B2bApiClient,
    private val catalogDao: CatalogDao,
) : CatalogRepository {

    /**
     * Získá seznam vybavení z katalogu (online přes API nebo offline z Room cache).
     * @param categoryId filtr dle ID kategorie (null = bez filtru)
     * @param status filtr dle stavu (null = bez filtru)
     * @return stránka katalogu s položkami a informací o offline režimu
     */
    override suspend fun list(categoryId: Long?, status: EquipmentStatus?): CatalogPage {
        return try {
            val wireStatus: String? = status?.let {
                cz.b2brental.data.remote.B2bJson.encodeToString(
                    EquipmentStatus.serializer(),
                    it,
                )
            }
            val items = apiClient.getCatalog(categoryId, wireStatus)
            val entities = items.map { it.toEntity() }
            catalogDao.upsertAll(entities)
            CatalogPage(items = items, isOffline = false)
        } catch (_: OfflineException) {
            val cached = catalogDao.getAll().first().map { it.toDto() }
            CatalogPage(items = cached, isOffline = true)
        }
    }

    /**
     * Získá detail konkrétního vybavení podle ID.
     * @param id ID vybavení
     * @return detail položky katalogu
     */
    override suspend fun get(id: Long): CatalogItemResponseDto {
        return apiClient.getEquipment(id)
    }

    /**
     * Vytvoří nové vybavení přes POST /catalog.
     * @param req požadavek na vytvoření
     * @return ID nového vybavení
     */
    override suspend fun create(req: CatalogUpsertRequestDto): Long {
        return apiClient.createEquipment(req).id
    }

    /**
     * Upraví vybavení přes PUT /catalog/{id}.
     * @param id ID vybavení
     * @param req požadavek na úpravu
     */
    override suspend fun update(id: Long, req: CatalogUpsertRequestDto): Unit {
        apiClient.updateEquipment(id, req)
    }

    /**
     * Smaže vybavení přes DELETE /catalog/{id}.
     * @param id ID vybavení
     */
    override suspend fun delete(id: Long): Unit {
        apiClient.deleteEquipment(id)
    }

    /**
     * Převod DTO na Room entitu pro uložení do cache.
     */
    private fun CatalogItemResponseDto.toEntity(): CatalogEntity {
        return CatalogEntity(
            id = id,
            categoryId = categoryId,
            categoryName = categoryName,
            model = model,
            serialNumber = serialNumber,
            price = price,
            monthlyRate = monthlyRate,
            description = description,
            photoUrl = photoUrl,
            status = status,
            cachedAt = System.currentTimeMillis(),
        )
    }

    /**
     * Převod Room entity na DTO pro zobrazení v UI.
     */
    private fun CatalogEntity.toDto(): CatalogItemResponseDto {
        return CatalogItemResponseDto(
            id = id,
            categoryId = categoryId,
            categoryName = categoryName,
            model = model,
            serialNumber = serialNumber,
            price = price,
            monthlyRate = monthlyRate,
            description = description,
            photoUrl = photoUrl,
            status = status,
        )
    }
}

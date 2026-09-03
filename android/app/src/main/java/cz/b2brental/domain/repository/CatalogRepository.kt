package cz.b2brental.domain.repository

import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.domain.model.EquipmentStatus

/**
 * Výsledek načtení katalogu s informací o režimu.
 * @param items seznam položek katalogu
 * @param isOffline true pokud data pocházejí z cache (offline režim)
 */
public data class CatalogPage(
    val items: List<CatalogItemResponseDto>,
    val isOffline: Boolean,
)

/**
 * Rozhraní repozitáře pro přístup ke katalogu vybavení.
 * Podporuje online i offline režim (cache přes Room).
 */
public interface CatalogRepository {

    /**
     * Získá seznam vybavení z katalogu.
     * @param categoryId filtr dle ID kategorie (null = bez filtru)
     * @param status filtr dle stavu (null = bez filtru)
     * @return stránka katalogu s informací o režimu
     */
    public suspend fun list(categoryId: Long?, status: EquipmentStatus?): CatalogPage

    /**
     * Získá detail jednoho vybavení.
     * @param id ID vybavení
     * @return detail vybavení
     */
    public suspend fun get(id: Long): CatalogItemResponseDto
}

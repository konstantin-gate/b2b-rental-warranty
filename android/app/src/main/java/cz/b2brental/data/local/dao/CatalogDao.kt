@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cz.b2brental.data.local.entity.CatalogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO pro přístup k položkám katalogu v Room cache.
 * Podporuje čtení (Flow pro reaktivní UI), zápis (upsert) a mazání.
 */
@Dao
public interface CatalogDao {

    /**
     * Získá všechny položky katalogu jako reaktivní Flow.
     * @return seznam položek katalogu
     */
    @Query("SELECT * FROM catalog_items")
    public fun getAll(): Flow<List<CatalogEntity>>

    /**
     * Vloží nebo aktualizuje seznam položek katalogu.
     * Při kolizi primárního klíče dojde k přepsání.
     * @param items seznam položek k uložení
     */
    @Upsert
    public suspend fun upsertAll(items: List<CatalogEntity>)

    /**
     * Vymaže všechny položky katalogu z cache.
     */
    @Query("DELETE FROM catalog_items")
    public suspend fun clearAll()
}

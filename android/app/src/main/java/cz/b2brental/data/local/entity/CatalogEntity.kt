@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.b2brental.domain.model.EquipmentStatus

/**
 * Room entita pro položku katalogu vybavení.
 * Slouží jako offline cache při nedostupnosti backendu.
 * @param id ID vybavení (primární klíč)
 * @param categoryId ID kategorie
 * @param categoryName název kategorie
 * @param model model vybavení
 * @param serialNumber výrobní číslo
 * @param price cena vybavení
 * @param monthlyRate měsíční sazba za pronájem
 * @param description popis (nepovinný)
 * @param photoUrl URL fotografie (nepovinné)
 * @param status stav vybavení
 * @param cachedAt časové razítko uložení do cache
 */
@Entity(tableName = "catalog_items")
public data class CatalogEntity(
    @PrimaryKey
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val model: String,
    val serialNumber: String,
    val price: String,
    val monthlyRate: String,
    val description: String?,
    val photoUrl: String?,
    val status: EquipmentStatus,
    val cachedAt: Long,
)

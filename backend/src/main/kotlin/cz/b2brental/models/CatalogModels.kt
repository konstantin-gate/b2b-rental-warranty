@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.db.EquipmentStatus
import cz.b2brental.domain.EquipmentCategoryId
import cz.b2brental.domain.MoneySerializer
import kotlinx.serialization.Serializable
import org.javamoney.moneta.Money

/** Žádost o vytvoření nebo úpravu položky katalogu */
@Serializable
public data class CatalogUpsertRequest(
    public val categoryId: EquipmentCategoryId,
    public val model: String,
    public val serialNumber: String,
    @Serializable(with = MoneySerializer::class)
    public val price: Money,
    @Serializable(with = MoneySerializer::class)
    public val monthlyRate: Money,
    public val description: String? = null,
    public val photoUrl: String? = null,
)

/** Odpověď s údaji o položce katalogu */
@Serializable
public data class CatalogItemResponse(
    public val id: Long,
    public val categoryId: Long,
    public val categoryName: String,
    public val model: String,
    public val serialNumber: String,
    @Serializable(with = MoneySerializer::class)
    public val price: Money,
    @Serializable(with = MoneySerializer::class)
    public val monthlyRate: Money,
    public val description: String?,
    public val photoUrl: String?,
    public val status: EquipmentStatus,
)

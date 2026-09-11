@file:Suppress("HardCodedStringLiteral", "KDocMissingDocumentation")

package cz.b2brental.services

import cz.b2brental.db.EquipmentCategories
import cz.b2brental.db.WarrantyRules
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate

/**
 * Data načtená v transakci pro vyhodnocení záruky (bez ResultRow/EntityID mimo transakci).
 * Společná pro vytvoření tiketu i kontrolu záruky přes AI.
 * @property contractStartDate počáteční datum aktivní smlouvy nebo null
 * @property warrantyMonths délka záruky v měsících podle pravidla nebo null, pokud pravidlo neexistuje
 * @property hasRule příznak existence záručního pravidla pro kategorii
 * @property equipmentModel model vybavení
 * @property categoryName název kategorie vybavení
 */
internal data class WarrantyPrelude(
    val contractStartDate: LocalDate?,
    val warrantyMonths: Int?,
    val hasRule: Boolean,
    val equipmentModel: String,
    val categoryName: String,
)

/**
 * Sestaví [WarrantyPrelude] podle kategorie vybavení; musí být voláno uvnitř Exposed transakce.
 * @param categoryId id kategorie vybavení (hodnota Equipment.categoryId)
 * @param equipmentModel model vybavení (hodnota Equipment.model)
 * @param contractStartDate počáteční datum aktivní smlouvy nebo null
 * @return prelude pro deterministické vyhodnocení záruky pravidly R1–R4
 */
internal fun buildWarrantyPrelude(
    categoryId: Long,
    equipmentModel: String,
    contractStartDate: LocalDate?,
): WarrantyPrelude {
    val rule =
        WarrantyRules
            .selectAll()
            .where { WarrantyRules.categoryId eq categoryId }
            .singleOrNull()
    val categoryName: String =
        EquipmentCategories
            .selectAll()
            .where { EquipmentCategories.id eq categoryId }
            .single()[EquipmentCategories.name]

    if (rule == null) {
        return WarrantyPrelude(
            contractStartDate = contractStartDate,
            warrantyMonths = null,
            hasRule = false,
            equipmentModel = equipmentModel,
            categoryName = categoryName,
        )
    }
    return WarrantyPrelude(
        contractStartDate = contractStartDate,
        warrantyMonths = rule[WarrantyRules.warrantyMonths],
        hasRule = true,
        equipmentModel = equipmentModel,
        categoryName = categoryName,
    )
}

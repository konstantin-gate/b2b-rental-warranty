@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.db.ContractStatus
import cz.b2brental.domain.EquipmentId
import cz.b2brental.domain.LocalDateSerializer
import cz.b2brental.domain.MoneySerializer
import kotlinx.serialization.Serializable
import org.javamoney.moneta.Money
import java.time.LocalDate

/** Žádost o vytvoření nájemní smlouvy */
@Serializable
public data class ContractCreateRequest(
    public val equipmentIds: List<EquipmentId>,
    public val months: Int,
    @Serializable(with = LocalDateSerializer::class) public val startDate: LocalDate,
    public val deliveryAddress: String,
)

/** Položka smlouvy v odpovědi */
@Serializable
public data class ContractItemResponse(
    public val equipmentId: Long,
    public val model: String,
)

/** Detailní odpověď o nájemní smlouvě */
@Serializable
public data class ContractResponse(
    public val id: Long,
    public val companyId: Long,
    public val companyName: String,
    public val status: ContractStatus,
    @Serializable(with = LocalDateSerializer::class) public val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class) public val endDate: LocalDate,
    public val months: Int,
    @Serializable(with = MoneySerializer::class) public val monthlyAmount: Money,
    @Serializable(with = MoneySerializer::class) public val deposit: Money,
    @Serializable(with = MoneySerializer::class) public val totalAmount: Money,
    public val deliveryAddress: String,
    public val items: List<ContractItemResponse>,
)

/** Odpověď pro akce se smlouvou (schválení / zamítnutí) */
@Serializable
public data class ContractActionResponse(
    public val id: Long,
    public val status: ContractStatus,
    public val paymentCount: Int? = null,
)

/** Odpověď s id dokumentu smlouvy (trasa POST /contracts/{id}/pdf přibude ve Vlně E) */
@Serializable
public data class ContractPdfResponse(
    public val documentId: Long,
)

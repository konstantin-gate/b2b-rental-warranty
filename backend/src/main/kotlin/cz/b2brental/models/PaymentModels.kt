@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.db.PaymentStatus
import cz.b2brental.domain.InstantSerializer
import cz.b2brental.domain.LocalDateSerializer
import cz.b2brental.domain.MoneySerializer
import kotlinx.serialization.Serializable
import org.javamoney.moneta.Money
import java.time.Instant
import java.time.LocalDate

/** Odpověď s údaji o platbě */
@Serializable
public data class PaymentResponse(
    public val id: Long,
    public val contractId: Long,
    public val period: Int,
    @Serializable(with = MoneySerializer::class) public val amount: Money,
    @Serializable(with = LocalDateSerializer::class) public val dueDate: LocalDate,
    public val status: PaymentStatus,
    @Serializable(with = InstantSerializer::class) public val paidAt: Instant?,
)

/** Odpověď po zaplacení platby */
@Serializable
public data class PaymentActionResponse(
    public val id: Long,
    public val status: PaymentStatus,
    @Serializable(with = InstantSerializer::class) public val paidAt: Instant,
)

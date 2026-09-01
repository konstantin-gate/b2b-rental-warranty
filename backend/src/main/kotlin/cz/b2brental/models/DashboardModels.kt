@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.models

import cz.b2brental.domain.MoneySerializer
import kotlinx.serialization.Serializable
import org.javamoney.moneta.Money

/** Metrikový přehled pro dashboard manažera/admina */
@Serializable
public data class DashboardMetrics(
    public val activeContracts: Int,
    public val openTickets: Int,
    public val overduePayments: Int,
    @Serializable(with = MoneySerializer::class) public val overdueAmount: Money,
    public val equipmentByStatus: EquipmentByStatus,
    public val monthStats: MonthStats,
)

/** Rozložení vybavení podle stavu */
@Serializable
public data class EquipmentByStatus(
    public val available: Int,
    public val rented: Int,
    public val maintenance: Int,
)

/** Statistika aktuálního kalendářního měsíce */
@Serializable
public data class MonthStats(
    public val newContracts: Int,
    @Serializable(with = MoneySerializer::class) public val paymentsPaidTotal: Money,
    public val resolvedTickets: Int,
)

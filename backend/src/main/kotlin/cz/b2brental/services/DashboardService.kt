@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.ContractStatus
import cz.b2brental.db.Equipment
import cz.b2brental.db.EquipmentStatus
import cz.b2brental.db.PaymentStatus
import cz.b2brental.db.Payments
import cz.b2brental.db.RentalContracts
import cz.b2brental.db.ServiceTickets
import cz.b2brental.db.TicketStatus
import cz.b2brental.domain.toCzkMoney
import cz.b2brental.models.DashboardMetrics
import cz.b2brental.models.EquipmentByStatus
import cz.b2brental.models.MonthStats
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

/** Služba metrik dashboardu pro manažera/admina */
public class DashboardService(
    private val paymentService: PaymentService,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    /** Vypočte metriky napříč systémem pomocí SQL agregací v jedné transakci */
    public fun metrics(): DashboardMetrics {
        paymentService.refreshOverdue()
        return transaction {
            val activeContracts =
                RentalContracts
                    .selectAll()
                    .where { RentalContracts.status eq ContractStatus.active }
                    .count()
                    .toInt()

            val openTickets =
                ServiceTickets
                    .selectAll()
                    .where { ServiceTickets.status inList listOf(TicketStatus.new, TicketStatus.assigned, TicketStatus.in_progress) }
                    .count()
                    .toInt()

            val overdueCount =
                Payments
                    .selectAll()
                    .where { Payments.status eq PaymentStatus.overdue }
                    .count()
                    .toInt()

            val overdueSumExpression = Payments.amount.sum()
            val overdueSum: BigDecimal =
                Payments
                    .select(overdueSumExpression)
                    .where { Payments.status eq PaymentStatus.overdue }
                    .singleOrNull()
                    ?.get(overdueSumExpression) ?: BigDecimal.ZERO

            val equipmentStatusCountExpression = Equipment.id.count()
            val equipmentCounts =
                Equipment
                    .select(Equipment.status, equipmentStatusCountExpression)
                    .groupBy(Equipment.status)
                    .associate { it[Equipment.status] to it[equipmentStatusCountExpression].toInt() }

            val startOfMonthInstant =
                LocalDate
                    .now(clock)
                    .withDayOfMonth(1)
                    .atStartOfDay(clock.zone)
                    .toInstant()

            val newContracts =
                RentalContracts
                    .selectAll()
                    .where { RentalContracts.createdAt greaterEq startOfMonthInstant }
                    .count()
                    .toInt()

            val paidSumExpression = Payments.amount.sum()
            val paidSum: BigDecimal =
                Payments
                    .select(paidSumExpression)
                    .where { (Payments.status eq PaymentStatus.paid) and (Payments.paidAt greaterEq startOfMonthInstant) }
                    .singleOrNull()
                    ?.get(paidSumExpression) ?: BigDecimal.ZERO

            val resolvedTickets =
                ServiceTickets
                    .selectAll()
                    .where {
                        (ServiceTickets.status eq TicketStatus.resolved) and (ServiceTickets.resolvedAt greaterEq startOfMonthInstant)
                    }.count()
                    .toInt()

            DashboardMetrics(
                activeContracts = activeContracts,
                openTickets = openTickets,
                overduePayments = overdueCount,
                overdueAmount = overdueSum.toCzkMoney(),
                equipmentByStatus =
                    EquipmentByStatus(
                        available = equipmentCounts[EquipmentStatus.available] ?: 0,
                        rented = equipmentCounts[EquipmentStatus.rented] ?: 0,
                        maintenance = equipmentCounts[EquipmentStatus.maintenance] ?: 0,
                    ),
                monthStats =
                    MonthStats(
                        newContracts = newContracts,
                        paymentsPaidTotal = paidSum.toCzkMoney(),
                        resolvedTickets = resolvedTickets,
                    ),
            )
        }
    }

    /** Poskytne textový přehled metrik jako kontext pro AI asistenta */
    public fun metricsAsContext(): String {
        val m = metrics()
        return "Aktivní smlouvy: ${m.activeContracts}; Otevřené tikety: ${m.openTickets}; " +
            "Platby po splatnosti: ${m.overduePayments} (celkem ${m.overdueAmount}); " +
            "Vybavení: dostupné ${m.equipmentByStatus.available}, pronajaté ${m.equipmentByStatus.rented}, " +
            "v údržbě ${m.equipmentByStatus.maintenance}; Tento měsíc: nové smlouvy ${m.monthStats.newContracts}, " +
            "vybráno na platbách ${m.monthStats.paymentsPaidTotal}, vyřešeno tiketů ${m.monthStats.resolvedTickets}."
    }
}

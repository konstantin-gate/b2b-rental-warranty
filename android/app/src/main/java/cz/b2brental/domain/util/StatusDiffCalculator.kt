@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.domain.util

import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.model.TicketStatus

/**
 * Záznam o změně stavu tiketu (pro lokální notifikace).
 * @param ticketId ID tiketu
 * @param oldStatus předchozí stav (null = tiket dosud nebyl znám)
 * @param newStatus aktuální stav
 */
public data class TicketChange(
    val ticketId: Long,
    val oldStatus: TicketStatus?,
    val newStatus: TicketStatus,
)

/**
 * Kalkulátor rozdílů stavů tiketů mezi dvěma načteními (pro lokální notifikace).
 */
public object StatusDiffCalculator {

    /**
     * Vrátí popisy změn mezi předchozí a aktuální mapou stavů tiketů.
     * První běh (prázdné `previous`) negeneruje žádné změny.
     * @param previous předchozí mapa (ticketId -> stav), prázdná při prvním spuštění
     * @param current aktuální seznam tiketů z API
     * @return seznam změn (pro sestavení textu notifikace)
     */
    public fun diff(
        previous: Map<Long, TicketStatus>,
        current: List<TicketResponseDto>,
    ): List<TicketChange> {
        val changes: MutableList<TicketChange> = mutableListOf()
        if (previous.isEmpty()) return changes
        current.forEach { ticket ->
            val old: TicketStatus? = previous[ticket.id]
            when {
                old == null -> changes.add(TicketChange(ticketId = ticket.id, oldStatus = null, newStatus = ticket.status))
                old != ticket.status -> changes.add(TicketChange(ticketId = ticket.id, oldStatus = old, newStatus = ticket.status))
                else -> Unit
            }
        }
        return changes
    }

    /**
     * Sestaví mapu stavů ze seznamu tiketů.
     * @param tickets seznam tiketů
     * @return mapa (ticketId -> stav)
     */
    public fun toStatusMap(tickets: List<TicketResponseDto>): Map<Long, TicketStatus> =
        tickets.associate { it.id to it.status }
}

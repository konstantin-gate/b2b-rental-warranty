package cz.b2brental.domain.util

import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.model.Severity
import cz.b2brental.domain.model.TicketStatus
import cz.b2brental.domain.model.WarrantyVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy kalkulátoru rozdílů stavů tiketů.
 */
@Suppress("HardCodedStringLiteral")
class StatusDiffCalculatorTest {

    private fun buildTicket(id: Long, status: TicketStatus): TicketResponseDto = TicketResponseDto(
        id = id,
        equipmentId = 1L,
        equipmentModel = "Model-$id",
        companyName = "Firma",
        description = "popis",
        photoBase64 = null,
        severity = Severity.MEDIUM,
        status = status,
        warrantyVerdict = WarrantyVerdict.COVERED,
        warrantyReason = null,
        aiRecommendation = null,
        technicianId = null,
        resolution = null,
        createdAt = "2026-01-01T00:00:00Z",
        resolvedAt = null,
    )

    @Test
    fun `bez zmen vrati prazdny seznam`() {
        val tickets: List<TicketResponseDto> = listOf(
            buildTicket(1L, TicketStatus.NEW),
            buildTicket(2L, TicketStatus.ASSIGNED),
        )
        val previous: Map<Long, TicketStatus> = StatusDiffCalculator.toStatusMap(tickets)
        val changes: List<TicketChange> = StatusDiffCalculator.diff(previous, tickets)
        assertEquals(emptyList<TicketChange>(), changes)
    }

    @Test
    fun `zmena stavu vrati jednu zmenu`() {
        val previous: Map<Long, TicketStatus> = mapOf(1L to TicketStatus.NEW)
        val current: List<TicketResponseDto> = listOf(buildTicket(1L, TicketStatus.IN_PROGRESS))
        val changes: List<TicketChange> = StatusDiffCalculator.diff(previous, current)
        assertEquals(1, changes.size)
        val change: TicketChange = changes.first()
        assertEquals(1L, change.ticketId)
        assertEquals(TicketStatus.NEW, change.oldStatus)
        assertEquals(TicketStatus.IN_PROGRESS, change.newStatus)
    }

    @Test
    fun `nova zadost negeneruje notifikaci pri prazdnem previous`() {
        val previous: Map<Long, TicketStatus> = emptyMap()
        val current: List<TicketResponseDto> = listOf(buildTicket(1L, TicketStatus.NEW))
        val changes: List<TicketChange> = StatusDiffCalculator.diff(previous, current)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `nove id pri ne-prazdnem previous generuje jednu zmenu`() {
        val previous: Map<Long, TicketStatus> = mapOf(1L to TicketStatus.NEW)
        val current: List<TicketResponseDto> = listOf(
            buildTicket(1L, TicketStatus.NEW),
            buildTicket(2L, TicketStatus.NEW),
        )
        val changes: List<TicketChange> = StatusDiffCalculator.diff(previous, current)
        assertEquals(1, changes.size)
        assertEquals(2L, changes.first().ticketId)
        assertEquals(null, changes.first().oldStatus)
    }

    @Test
    fun `toStatusMap spravne sestavi mapu`() {
        val tickets: List<TicketResponseDto> = listOf(
            buildTicket(10L, TicketStatus.NEW),
            buildTicket(20L, TicketStatus.RESOLVED),
        )
        val map: Map<Long, TicketStatus> = StatusDiffCalculator.toStatusMap(tickets)
        assertEquals(TicketStatus.NEW, map[10L])
        assertEquals(TicketStatus.RESOLVED, map[20L])
    }
}

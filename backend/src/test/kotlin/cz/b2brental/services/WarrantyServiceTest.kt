@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.db.WarrantyVerdict
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WarrantyServiceTest {
    @Test
    fun ruleR4CoveredTest() {
        val res =
            WarrantyService.evaluate(
                contractStartDate = LocalDate.of(2026, 1, 1),
                warrantyMonths = 12,
                today = LocalDate.of(2026, 6, 1),
            )
        assertEquals(WarrantyVerdict.covered, res.verdict)
        assertTrue(res.reason.startsWith("Záruka platí do"))
    }

    @Test
    fun ruleR2ExpiredTest() {
        val res =
            WarrantyService.evaluate(
                contractStartDate = LocalDate.of(2025, 1, 1),
                warrantyMonths = 12,
                today = LocalDate.of(2026, 6, 1),
            )
        assertEquals(WarrantyVerdict.not_covered, res.verdict)
        assertTrue(res.reason.contains("2026-01-01"))
    }

    @Test
    fun ruleR1NoContractTest() {
        val res =
            WarrantyService.evaluate(
                contractStartDate = null,
                warrantyMonths = null,
                today = LocalDate.of(2026, 6, 1),
            )
        assertEquals(WarrantyVerdict.review_required, res.verdict)
    }
}

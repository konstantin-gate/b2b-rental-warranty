package cz.b2brental

import cz.b2brental.domain.util.PricePreviewCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Testy předběžného výpočtu cen nájemní smlouvy.
 */
class PricePreviewCalculatorTest {

    @Test
    fun `two rates six months returns correct amounts`() {
        val result = PricePreviewCalculator.preview(
            monthlyRates = listOf("4500.00", "3600.00"),
            months = 6,
        )
        assertEquals("8100.00", result.monthlyAmount)
        assertEquals("2430.00", result.deposit)
        assertEquals("51030.00", result.totalAmount)
    }

    @Test
    fun `single rate twelve months`() {
        val result = PricePreviewCalculator.preview(
            monthlyRates = listOf("9000.00"),
            months = 12,
        )
        assertEquals("9000.00", result.monthlyAmount)
        assertEquals("2700.00", result.deposit)
        assertEquals("110700.00", result.totalAmount)
    }

    @Test
    fun `months zero throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            PricePreviewCalculator.preview(listOf("1000.00"), 0)
        }
    }

    @Test
    fun `months thirty seven throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            PricePreviewCalculator.preview(listOf("1000.00"), 37)
        }
    }

    @Test
    fun `empty rates list throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            PricePreviewCalculator.preview(emptyList(), 6)
        }
    }

    @Test
    fun `deposit uses HALF_UP rounding`() {
        // 1000.00 * 0.30 = 300.00 (no rounding needed)
        val result = PricePreviewCalculator.preview(listOf("1000.00"), 1)
        assertEquals("300.00", result.deposit)
        assertEquals("1300.00", result.totalAmount)
    }

    @Test
    fun `three rates one month`() {
        val result = PricePreviewCalculator.preview(
            monthlyRates = listOf("1000.00", "2000.00", "3000.00"),
            months = 1,
        )
        assertEquals("6000.00", result.monthlyAmount)
        assertEquals("1800.00", result.deposit)
        assertEquals("7800.00", result.totalAmount)
    }
}

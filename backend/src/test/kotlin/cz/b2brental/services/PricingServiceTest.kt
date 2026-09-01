@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.domain.CZK
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PricingServiceTest {
    @Test
    fun standardPricingTest() {
        val rates =
            listOf(
                Money.of(BigDecimal("4500.00"), CZK),
                Money.of(BigDecimal("4500.00"), CZK),
            )
        val res = PricingService.calc(rates, 12)
        assertEquals(Money.of(BigDecimal("9000.00"), CZK), res.monthlyAmount)
        assertEquals(Money.of(BigDecimal("2700.00"), CZK), res.deposit)
        assertEquals(Money.of(BigDecimal("110700.00"), CZK), res.totalAmount)
    }

    @Test
    fun roundingAndInvariantTest() {
        val rates = listOf(Money.of(BigDecimal("1000.01"), CZK))
        val res = PricingService.calc(rates, 1)
        assertEquals(Money.of(BigDecimal("300.00"), CZK), res.deposit)
        assertEquals(res.monthlyAmount.multiply(1L).add(res.deposit), res.totalAmount)
    }

    @Test
    fun validationFailsTest() {
        assertFailsWith<IllegalArgumentException> { PricingService.calc(emptyList(), 12) }
        assertFailsWith<IllegalArgumentException> { PricingService.calc(listOf(Money.of(BigDecimal("1000"), CZK)), 0) }
        assertFailsWith<IllegalArgumentException> { PricingService.calc(listOf(Money.of(BigDecimal("1000"), CZK)), 37) }
    }
}

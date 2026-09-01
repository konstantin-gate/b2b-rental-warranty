@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {
    @Test
    fun czkPercentTest() {
        val money = Money.of(BigDecimal("1000.01"), CZK)
        val deposit = money.czkPercent(DEPOSIT_RATE)
        assertEquals(Money.of(BigDecimal("300.00"), CZK), deposit)
    }

    @Test
    fun toDbBigDecimalTest() {
        val money = Money.of(BigDecimal("9000"), CZK)
        val dbDecimal = money.toDbBigDecimal()
        assertEquals(2, dbDecimal.scale())
        assertEquals(BigDecimal("9000.00"), dbDecimal)
    }

    @Test
    fun serializationTest() {
        val original = Money.of(BigDecimal("9000.00"), CZK)
        val jsonStr = Json.encodeToString(MoneySerializer, original)
        assertEquals("\"9000.00\"", jsonStr)

        val deserialized = Json.decodeFromString(MoneySerializer, "\"9000.00\"")
        assertEquals(deserialized, original)

        assertFailsWith<SerializationException> { Json.decodeFromString(MoneySerializer, "\"9000.005\"") }
        assertFailsWith<SerializationException> { Json.decodeFromString(MoneySerializer, "\"abc\"") }
    }

    @Test
    fun scaleInsensitiveEqualsTest() {
        assertEquals(
            Money.of(BigDecimal("9000.00"), CZK),
            Money.of(BigDecimal("9000.0"), CZK),
        )
    }
}

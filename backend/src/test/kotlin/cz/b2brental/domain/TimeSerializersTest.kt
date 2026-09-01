@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeSerializersTest {
    @Test
    fun localDateSerializationTest() {
        val date = LocalDate.of(2026, 8, 29)
        val str = Json.encodeToString(LocalDateSerializer, date)
        assertEquals("\"2026-08-29\"", str)
        assertEquals(date, Json.decodeFromString(LocalDateSerializer, "\"2026-08-29\""))
        assertFailsWith<SerializationException> { Json.decodeFromString(LocalDateSerializer, "\"29.08.2026\"") }
    }

    @Test
    fun instantSerializationTest() {
        val instant = Instant.parse("2026-08-29T12:34:56Z")
        val str = Json.encodeToString(InstantSerializer, instant)
        assertEquals("\"2026-08-29T12:34:56Z\"", str)
        assertEquals(instant, Json.decodeFromString(InstantSerializer, "\"2026-08-29T12:34:56Z\""))
        assertFailsWith<SerializationException> { Json.decodeFromString(InstantSerializer, "\"2026-08-29 12:34\"") }
    }
}

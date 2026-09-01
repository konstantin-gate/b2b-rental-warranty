@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IcoTest {
    @Test
    fun validIcoTest() {
        assertEquals("25596641", Ico("25596641").value)
        assertEquals("28745001", Ico("28745001").value)
    }

    @Test
    fun invalidIcoTest() {
        assertFailsWith<IllegalArgumentException> { Ico("28745002") }
        assertFailsWith<IllegalArgumentException> { Ico("12345678") }
        assertFailsWith<IllegalArgumentException> { Ico("CZ28745001") }
        assertFailsWith<IllegalArgumentException> { Ico("1234567") }
    }
}

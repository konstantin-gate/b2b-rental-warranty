@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EmailTest {
    @Test
    fun validEmailTest() {
        assertEquals("kitchen@b2b.demo", Email("kitchen@b2b.demo").value)
    }

    @Test
    fun invalidEmailTest() {
        assertFailsWith<IllegalArgumentException> { Email("foo") }
        assertFailsWith<IllegalArgumentException> { Email("@") }
        assertFailsWith<IllegalArgumentException> { Email("a@b") }
        assertFailsWith<IllegalArgumentException> { Email("a b@c.cz") }
        assertFailsWith<IllegalArgumentException> { Email("a".repeat(196) + "@b.cz") }
    }
}

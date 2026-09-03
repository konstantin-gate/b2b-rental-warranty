package cz.b2brental

import cz.b2brental.domain.util.IcoValidator
import org.junit.Assert.*
import org.junit.Test

@Suppress("HardCodedStringLiteral")
class IcoValidatorTest {

    @Test
    fun `valid ICO 28745001 returns true`() {
        assertTrue(IcoValidator.isValid("28745001"))
    }

    @Test
    fun `invalid ICO last digit wrong returns false`() {
        assertFalse(IcoValidator.isValid("28745002"))
    }

    @Test
    fun `too short ICO returns false`() {
        assertFalse(IcoValidator.isValid("2874500"))
    }

    @Test
    fun `non-numeric ICO returns false`() {
        assertFalse(IcoValidator.isValid("abcdefgh"))
    }

    @Test
    fun `empty string returns false`() {
        assertFalse(IcoValidator.isValid(""))
    }
}

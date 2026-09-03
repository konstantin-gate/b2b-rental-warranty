package cz.b2brental

import cz.b2brental.domain.util.EmailValidator
import org.junit.Assert.*
import org.junit.Test

/**
 * Testy validace e-mailových adres pomocí EmailValidator.
 */
@Suppress("HardCodedStringLiteral")
class EmailValidatorTest {

    @Test
    fun `valid email returns true`() {
        assertTrue(EmailValidator.isValid("test@example.com"))
    }

    @Test
    fun `plain address without @ returns false`() {
        assertFalse(EmailValidator.isValid("plainaddress"))
    }

    @Test
    fun `email without dot returns false`() {
        assertFalse(EmailValidator.isValid("missing@dot"))
    }

    @Test
    fun `empty string returns false`() {
        assertFalse(EmailValidator.isValid(""))
    }

    @Test
    fun `email with space returns false`() {
        assertFalse(EmailValidator.isValid("with space@x.com"))
    }
}

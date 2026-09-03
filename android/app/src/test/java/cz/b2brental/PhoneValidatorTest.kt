package cz.b2brental

import cz.b2brental.domain.util.PhoneValidator
import org.junit.Assert.*
import org.junit.Test

class PhoneValidatorTest {

    @Test
    fun `valid phone +420777123456 returns true`() {
        assertTrue(PhoneValidator.isValid("+420777123456"))
    }

    @Test
    fun `phone without country code returns false`() {
        assertFalse(PhoneValidator.isValid("777123456"))
    }

    @Test
    fun `phone with wrong country code +421 returns false`() {
        assertFalse(PhoneValidator.isValid("+421777123456"))
    }

    @Test
    fun `empty phone returns false`() {
        assertFalse(PhoneValidator.isValid(""))
    }
}

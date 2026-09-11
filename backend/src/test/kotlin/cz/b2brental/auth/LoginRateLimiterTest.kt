@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.auth

import cz.b2brental.db.LoginAttempts
import cz.b2brental.db.LoginBlocks
import cz.b2brental.utils.TooManyRequestsException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoginRateLimiterTest {
    private lateinit var limiter: LoginRateLimiter

    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:limitertest${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction {
            SchemaUtils.create(LoginAttempts, LoginBlocks)
        }
        limiter = LoginRateLimiter()
    }

    @Test
    fun petNeuspesnychPokusuAktivujeBlokovani() {
        repeat(5) { limiter.recordFailure("test@example.com") }
        val ex = assertFailsWith<TooManyRequestsException> { limiter.assertNotBlocked("test@example.com") }
        val message = ex.message ?: return
        assertTrue(message.contains("Příliš mnoho"))
    }

    @Test
    fun ctyriPokusyBlokaciNespusti() {
        repeat(4) { limiter.recordFailure("ctyri@example.com") }
        limiter.assertNotBlocked("ctyri@example.com")
    }

    @Test
    fun resetZrusiBlokovani() {
        repeat(5) { limiter.recordFailure("test2@example.com") }
        limiter.reset("test2@example.com")
        limiter.assertNotBlocked("test2@example.com")
    }
}

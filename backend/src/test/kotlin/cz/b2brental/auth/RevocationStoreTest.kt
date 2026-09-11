@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.auth

import cz.b2brental.db.RevokedTokens
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevocationStoreTest {
    private lateinit var store: RevocationStore

    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:revocationtest${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction {
            SchemaUtils.create(RevokedTokens)
        }
        store = RevocationStore()
    }

    @Test
    fun zneplatnenyTokenJeOdmitnut() {
        store.revoke("jti-1", Instant.now().plusSeconds(3600))
        assertTrue(store.isRevoked("jti-1"))
        assertFalse(store.isRevoked("jti-2"))
    }

    @Test
    fun expirovanaZaznamSeNeuklada() {
        store.revoke("jti-3", Instant.now().minusSeconds(60))
        assertFalse(store.isRevoked("jti-3"))
        val count = transaction { RevokedTokens.selectAll().count() }
        assertEquals(0L, count)
    }
}

package cz.b2brental.auth

import cz.b2brental.db.LoginAttempts
import cz.b2brental.db.LoginBlocks
import cz.b2brental.utils.TooManyRequestsException
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

/**
 * Omezovač pokusů o přihlášení: 5 neúspěšných pokusů na e-mail v okně 15 minut
 * způsobí blokaci na 15 minut (HTTP 429). Stav je uložen v databázi, takže
 * přežívá restart backendu a je sdílený mezi instancemi.
 */
public class LoginRateLimiter {
    /** Vyhodí chybu 429, pokud je e-mail aktuálně blokován.
     * @param emailKey identifikátor e-mailu (normalizovaný klíč)
     */
    public fun assertNotBlocked(emailKey: String) {
        val nowMs: Long = Instant.now().toEpochMilli()
        val blockedUntilMs: Long? =
            transaction {
                LoginBlocks
                    .selectAll()
                    .where { Op.build { LoginBlocks.email eq emailKey } }
                    .orderBy(LoginBlocks.blockedUntilMs to SortOrder.DESC)
                    .firstOrNull()
                    ?.get(LoginBlocks.blockedUntilMs)
            }
        if (blockedUntilMs != null && nowMs < blockedUntilMs) {
            throw TooManyRequestsException("Příliš mnoho neúspěšných přihlášení — zkuste to později")
        }
        if (blockedUntilMs != null) {
            // Blokace už vypršela; záznam se odstraní
            transaction {
                LoginBlocks.deleteWhere { Op.build { LoginBlocks.email eq emailKey } }
            }
        }
    }

    /** Zaznamená neúspěšný pokus; při 5 pokusech v okně 15 minut nastaví blokaci.
     * @param emailKey identifikátor e-mailu (normalizovaný klíč)
     */
    public fun recordFailure(emailKey: String) {
        val nowMs: Long = Instant.now().toEpochMilli()
        val windowStartMs: Long = nowMs - WINDOW_MS
        transaction {
            // Vyčištění pokusů a blokací s uplynulou platností
            LoginAttempts.deleteWhere { Op.build { LoginAttempts.attemptedAtMs less windowStartMs } }
            LoginBlocks.deleteWhere { Op.build { LoginBlocks.blockedUntilMs less nowMs } }
            LoginAttempts.insert {
                it[email] = emailKey
                it[attemptedAtMs] = nowMs
            }
            val failures: Long =
                LoginAttempts
                    .selectAll()
                    .where { Op.build { LoginAttempts.email eq emailKey } }
                    .count()
            if (failures >= MAX_FAILURES) {
                LoginAttempts.deleteWhere { Op.build { LoginAttempts.email eq emailKey } }
                LoginBlocks.deleteWhere { Op.build { LoginBlocks.email eq emailKey } }
                LoginBlocks.insert {
                    it[email] = emailKey
                    it[blockedUntilMs] = nowMs + BLOCK_MS
                }
            }
        }
    }

    /** Smaže historii neúspěšných pokusů i blokaci po úspěšném přihlášení.
     * @param emailKey identifikátor e-mailu (normalizovaný klíč)
     */
    public fun reset(emailKey: String) {
        transaction {
            LoginAttempts.deleteWhere { Op.build { LoginAttempts.email eq emailKey } }
            LoginBlocks.deleteWhere { Op.build { LoginBlocks.email eq emailKey } }
        }
    }

    private companion object {
        /** Maximální počet neúspěšných pokusů před blokací. */
        const val MAX_FAILURES: Int = 5

        /** Délka okna počítání pokusů v sekundách. */
        const val WINDOW_SECONDS: Long = 900

        /** Délka blokace v sekundách. */
        const val BLOCK_SECONDS: Long = 900

        /** Délka okna v milisekundách. */
        private const val WINDOW_MS: Long = WINDOW_SECONDS * 1000L

        /** Délka blokace v milisekundách. */
        private const val BLOCK_MS: Long = BLOCK_SECONDS * 1000L
    }
}

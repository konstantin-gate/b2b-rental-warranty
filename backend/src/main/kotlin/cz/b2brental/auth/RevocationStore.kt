package cz.b2brental.auth

import cz.b2brental.db.RevokedTokens
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

/**
 * Skladiště zneplatněných tokenů (jti → čas expirace). Stav je uložen v databázi,
 * takže přežívá restart backendu a je sdílený mezi instancemi.
 * Záznamy s uplynulou platností se čistí při každé kontrole i při každém zneplatnění.
 */
public class RevocationStore {
    /** Zneplatní token s daným jti do času expirace; záznamy s uplynulou platností se vyčistí.
     * @param jti jedinečný identifikátor tokenu (claim jti)
     * @param expiresAt čas expirace tokenu
     */
    public fun revoke(
        jti: String,
        expiresAt: Instant,
    ) {
        val nowMs: Long = Instant.now().toEpochMilli()
        transaction {
            RevokedTokens.deleteWhere { Op.build { RevokedTokens.expiresAtMs less nowMs } }
            if (expiresAt.toEpochMilli() > nowMs) {
                RevokedTokens.deleteWhere { Op.build { RevokedTokens.jti eq jti } }
                RevokedTokens.insert {
                    it[RevokedTokens.jti] = jti
                    it[expiresAtMs] = expiresAt.toEpochMilli()
                }
            }
        }
    }

    /** Vrátí true, pokud je token s daným jti zneplatněn.
     * @param jti jedinečný identifikátor tokenu (claim jti)
     */
    public fun isRevoked(jti: String): Boolean {
        val nowMs: Long = Instant.now().toEpochMilli()
        return transaction {
            RevokedTokens.deleteWhere { Op.build { RevokedTokens.expiresAtMs less nowMs } }
            RevokedTokens
                .selectAll()
                .where { Op.build { RevokedTokens.jti eq jti } }
                .count() > 0L
        }
    }
}

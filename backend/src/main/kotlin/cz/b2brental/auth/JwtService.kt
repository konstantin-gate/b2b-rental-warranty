@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/**
 * Služba pro tvorbu JWT tokenů (HS256, java-jwt 4.4.0).
 * Token obsahuje claims sub (userId), role a companyId (pouze pokud není null),
 * issuer "b2b-rental", audience "b2b-rental-api", platnost 24 hodin.
 */
public class JwtService(
    private val secret: String,
) {
    /** Vytvoří podepsaný token pro daného uživatele. */
    public fun makeToken(
        userId: Long,
        role: String,
        companyId: Long?,
    ): String {
        val now = Date()
        val expiration = Date(now.time + TOKEN_TTL_MS)
        val builder: JWTCreator.Builder =
            JWT
                .create()
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .withSubject(userId.toString())
                .withClaim(CLAIM_ROLE, role)

        if (companyId != null) {
            builder.withClaim(CLAIM_COMPANY_ID, companyId.toString())
        }

        return builder.sign(Algorithm.HMAC256(secret))
    }

    /** Konstanty tokenu — jediný zdroj pro JwtService i pro konfiguraci jwt{} v Application. */
    public companion object {
        /** Issuer tokenu. */
        public const val ISSUER: String = "b2b-rental"

        /** Audience tokenu. */
        public const val AUDIENCE: String = "b2b-rental-api"

        /** Název claimu s rolí. */
        public const val CLAIM_ROLE: String = "role"

        /** Název claimu s id firmy. */
        public const val CLAIM_COMPANY_ID: String = "companyId"

        private const val TOKEN_TTL_MS: Long = 24L * 60L * 60L * 1000L
    }
}

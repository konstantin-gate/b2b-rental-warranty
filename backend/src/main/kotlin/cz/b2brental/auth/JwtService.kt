@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

/**
 * Služba pro tvorbu JWT tokenů (HS256, java-jwt 4.4.0).
 * Token obsahuje claims sub (userId), role, companyId (pouze pokud není null),
 * scope ("platform" nebo "tenant") a jti (jedinečný identifikátor tokenu pro revocation).
 * Issuer "b2b-rental", audience "b2b-rental-api", platnost 24 hodin.
 * @property secret tajný klíč pro HMAC-SHA256 podpis tokenů, čten z proměnné JWT_SECRET v prostředí.
 */
public class JwtService(
    private val secret: String,
) {
    /** Vytvoří podepsaný token pro daného uživatele.
     * @param userId identifikátor uživatele (claim sub)
     * @param role role uživatele ("admin", "manager", "technician", "client")
     * @param companyId identifikátor firmy přiřazené k uživateli; null pro platformové uživatele
     * @param scope scope tokenu – "platform" nebo "tenant"
     */
    public fun makeToken(
        userId: Long,
        role: String,
        companyId: Long?,
        scope: String,
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
                .withClaim(CLAIM_SCOPE, scope)
                .withJWTId(UUID.randomUUID().toString())

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

        /** Název claimu se scope (platform/tenant). */
        public const val CLAIM_SCOPE: String = "scope"

        /** Název claimu s jedinečným identifikátorem tokenu. */
        public const val CLAIM_JWT_ID: String = "jti"

        /** Hodnota scope pro platformové uživatele (bez firmy). */
        public const val SCOPE_PLATFORM: String = "platform"

        /** Hodnota scope pro tenantské uživatele (s firmou). */
        public const val SCOPE_TENANT: String = "tenant"

        private const val TOKEN_TTL_MS: Long = 24L * 60L * 60L * 1000L
    }
}

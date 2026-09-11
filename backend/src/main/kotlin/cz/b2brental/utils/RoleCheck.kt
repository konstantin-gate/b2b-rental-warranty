@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.utils

import cz.b2brental.auth.JwtService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

/**
 * Ověření role z JWT tokenu pro chráněné trasy (volá se z Vlny D).
 * Není autentizován → UnauthorizedException (401).
 * Claim "role" chybí nebo role není v seznamu povolených → ForbiddenException (403).
 * @param roles povolené role; volající musí mít alespoň jednu z nich.
 */
public fun ApplicationCall.requireRole(vararg roles: String) {
    val jwtPrincipal: JWTPrincipal =
        this.principal()
            ?: throw UnauthorizedException("Autentizace je povinná pro tento požadavek")

    val role: String =
        jwtPrincipal[JwtService.CLAIM_ROLE]
            ?: throw ForbiddenException("Role v tokenu chybí")

    if (role !in roles) {
        throw ForbiddenException("Role $role nemá přístup k tomuto zdroji")
    }
}

/** Získání role z JWT tokenu nebo prázdného řetězce */
public fun ApplicationCall.jwtRole(): String {
    val jwtPrincipal: JWTPrincipal? = this.principal()
    return jwtPrincipal?.get(JwtService.CLAIM_ROLE) ?: ""
}

/** Získání ID společnosti z JWT tokenu */
public fun ApplicationCall.jwtCompanyId(): Long? {
    val jwtPrincipal: JWTPrincipal? = this.principal()
    return jwtPrincipal?.get(JwtService.CLAIM_COMPANY_ID)?.toLongOrNull()
}

/** Získání scope z JWT tokenu ("platform"/"tenant"); prázdné tokeny → "". */
public fun ApplicationCall.jwtScope(): String {
    val jwtPrincipal: JWTPrincipal? = this.principal()
    return jwtPrincipal?.get(JwtService.CLAIM_SCOPE) ?: ""
}

/** Ověření role a platformového scope; tenanta nebo špatnou roli odmítne (403).
 * @param roles povolené role pro platformové operace.
 */
public fun ApplicationCall.requirePlatform(vararg roles: String) {
    requireRole(*roles)
    if (jwtScope() != JwtService.SCOPE_PLATFORM) {
        throw ForbiddenException("Operace je vyhrazena personálu platformy")
    }
}

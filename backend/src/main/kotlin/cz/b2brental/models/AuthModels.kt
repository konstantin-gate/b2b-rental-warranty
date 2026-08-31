package cz.b2brental.models

import cz.b2brental.domain.Email
import cz.b2brental.domain.Ico
import kotlinx.serialization.Serializable

/**
 * Požadavek na registraci firmy. Formát e-mailu a IČO se validuje při deserializaci
 * (value třídy Email/Ico validují v init{}); servis tyto formáty nekontroluje.
 */
@Serializable
public data class RegisterCompanyRequest(
    public val companyName: String,
    public val inn: Ico,
    public val address: String,
    public val adminEmail: Email,
    public val password: String,
    public val phone: String? = null,
)

/** Požadavek na přihlášení; e-mail se validuje při deserializaci. */
@Serializable
public data class LoginRequest(
    public val email: Email,
    public val password: String,
)

/** Odpověď na registraci: nová firma a její admin. */
@Serializable
public data class RegisterResponse(
    public val companyId: Long,
    public val userId: Long,
    public val email: String,
    public val role: String,
)

/**
 * Odpověď na přihlášení: JWT token a údaje uživatele.
 * companyId je nullable — seed-uživatelé admin/manager/technician firmu nemají.
 */
@Serializable
public data class AuthResponse(
    public val token: String,
    public val userId: Long,
    public val role: String,
    public val companyId: Long?,
    public val email: String,
)

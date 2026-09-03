package cz.b2brental.domain.model

/**
 * Profil přihlášeného uživatele uložený v DataStore.
 * Slouží k určení role a identifikaci uživatele napříč obrazovkami.
 * @property token JWT token pro autorizaci API požadavků
 * @property userId ID přihlášeného uživatele v databázi
 * @property role role uživatele pro řízení přístupu
 * @property companyId ID firmy (null pro uživatele bez firmy — admin/manager/technician)
 * @property email e-mail přihlášeného uživatele
 */
public data class UserProfile(
    val token: String,
    val userId: Long,
    val role: UserRole,
    val companyId: Long?,
    val email: String,
)

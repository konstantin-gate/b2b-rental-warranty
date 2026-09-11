package cz.b2brental.domain.repository

import cz.b2brental.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Rozhraní repozitáře pro autentizaci a správu session.
 */
public interface AuthRepository {

    /**
     * Přihlášení uživatele e-mailem a heslem.
     * @param email přihlašovací e-mail
     * @param password heslo
     */
    public suspend fun login(email: String, password: String): Unit

    /**
     * Registrace nové firmy s následným automatickým přihlášením.
     * @param companyName název firmy
     * @param inn IČO firmy
     * @param address sídlo firmy
     * @param adminEmail e-mail administrátora
     * @param password heslo administrátora
     * @param phone telefon (nepovinné)
     */
    public suspend fun registerCompany(
        companyName: String,
        inn: String,
        address: String,
        adminEmail: String,
        password: String,
        phone: String?,
    ): Unit

    /**
     * Odhlášení uživatele — zneplatní token na serveru a vymaže lokální session a offline cache.
     */
    public suspend fun logout(): Unit

    /** Flow aktuální session. */
    public fun session(): Flow<UserProfile?>
}

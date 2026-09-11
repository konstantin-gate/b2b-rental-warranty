package cz.b2brental.data.repository

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.B2bJson
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.SessionClearer
import cz.b2brental.data.remote.dto.RegisterCompanyRequestDto
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.UserRole
import cz.b2brental.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementace AuthRepository — spravuje přihlášení, registraci a session přes šifrované úložiště (Android Keystore).
 * @param apiClient HTTP klient pro komunikaci s backend API
 * @param tokenStorage úložiště JWT tokenu a profilu uživatele
 * @param sessionClearer správce mazání session a offline cache
 */
public class AuthRepositoryImpl(
    private val apiClient: B2bApiClient,
    private val tokenStorage: TokenStorage,
    private val sessionClearer: SessionClearer,
) : AuthRepository {

    /**
     * Přihlášení uživatele — zavolá API a uloží session do šifrovaného úložiště.
     * @param email přihlašovací e-mail
     * @param password heslo
     */
    override suspend fun login(email: String, password: String): Unit {
        val response = apiClient.login(email, password)
        val profile = UserProfile(
            token = response.token,
            userId = response.userId,
            role = B2bJson.decodeFromString(UserRole.serializer(), response.role),
            companyId = response.companyId,
            email = response.email,
        )
        tokenStorage.save(profile)
    }

    /**
     * Registrace nové firmy s automatickým přihlášením po úspěchu.
     * @param companyName název firmy
     * @param inn IČO firmy
     * @param address sídlo firmy
     * @param adminEmail e-mail administrátora
     * @param password heslo administrátora
     * @param phone telefon (nepovinné)
     */
    override suspend fun registerCompany(
        companyName: String,
        inn: String,
        address: String,
        adminEmail: String,
        password: String,
        phone: String?,
    ): Unit {
        apiClient.registerCompany(
            RegisterCompanyRequestDto(
                companyName = companyName,
                inn = inn,
                address = address,
                adminEmail = adminEmail,
                password = password,
                phone = phone,
            )
        )
        // Automatické přihlášení po registraci
        login(adminEmail, password)
    }

    /**
     * Odhlášení uživatele — zneplatní token na serveru a vymaže session i offline cache Room.
     * Při nedostupnosti serveru se lokální session vymaže vždy.
     */
    override suspend fun logout(): Unit {
        try {
            apiClient.logout()
        } catch (_: OfflineException) {
            // Server není dostupný — lokální session se vymaže i tak
        } catch (_: ApiException) {
            // Server token již zneplatnil; lokální session se vymaže i tak
        }
        sessionClearer.clearSession()
    }

    /**
     * Poskytuje Flow s aktuálním profilem přihlášeného uživatele (nebo null).
     * @return Flow s UserProfile nebo null
     */
    override fun session(): Flow<UserProfile?> = tokenStorage.session
}

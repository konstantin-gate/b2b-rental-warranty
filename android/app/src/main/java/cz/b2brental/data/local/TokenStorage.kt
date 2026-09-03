@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cz.b2brental.data.remote.B2bJson
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "b2b_session")

/** Rozhraní pro ukládání a čtení session přihlášeného uživatele. */
public interface TokenStorage {
    /** Flow aktuální session (null = nepřihlášeno). */
    @Suppress("KDocMissingDocumentation")
    public val session: Flow<UserProfile?>

    /** Vrátí aktuální JWT token (suspend). */
    public suspend fun currentToken(): String?

    /** Uloží profil přihlášeného uživatele do DataStore.
     * @param profile profil přihlášeného uživatele */
    public suspend fun save(profile: UserProfile): Unit

    /** Vymaže session (odhlášení) z DataStore. */
    public suspend fun clear(): Unit
}

/** Implementace TokenStorage pomocí DataStore Preferences.
 * @param context kontext aplikace (pro přístup k DataStore) */
public class DataStoreTokenStorage(private val context: Context) : TokenStorage {

    private object Keys {
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_ROLE = stringPreferencesKey("user_role")
        val COMPANY_ID = stringPreferencesKey("company_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
    }

    @Suppress("KDocMissingDocumentation")
    override val session: Flow<UserProfile?> = context.dataStore.data.map { prefs ->
        val token = prefs[Keys.JWT_TOKEN] ?: return@map null
        val userId = prefs[Keys.USER_ID]?.toLongOrNull() ?: return@map null
        val roleStr = prefs[Keys.USER_ROLE] ?: return@map null
        val role = try {
            B2bJson.decodeFromString<UserRole>(roleStr)
        } catch (_: Exception) {
            return@map null
        }
        val companyId = prefs[Keys.COMPANY_ID]?.toLongOrNull()
        val email = prefs[Keys.USER_EMAIL] ?: ""
        UserProfile(token, userId, role, companyId, email)
    }

    override suspend fun currentToken(): String? {
        return context.dataStore.data.first()[Keys.JWT_TOKEN]
    }

    @Suppress("KDocMissingDocumentation")
    override suspend fun save(profile: UserProfile): Unit {
        context.dataStore.edit { prefs ->
            prefs[Keys.JWT_TOKEN] = profile.token
            prefs[Keys.USER_ID] = profile.userId.toString()
            prefs[Keys.USER_ROLE] = B2bJson.encodeToString(UserRole.serializer(), profile.role)
            prefs[Keys.COMPANY_ID] = profile.companyId?.toString() ?: ""
            prefs[Keys.USER_EMAIL] = profile.email
        }
    }

    @Suppress("KDocMissingDocumentation")
    override suspend fun clear(): Unit {
        context.dataStore.edit { it.clear() }
    }
}

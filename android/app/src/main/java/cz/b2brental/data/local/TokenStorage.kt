@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import cz.b2brental.data.remote.B2bJson
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Rozhraní pro ukládání a čtení session přihlášeného uživatele. */
public interface TokenStorage {
    /** Aktuální profil přihlášeného uživatele jako pozorovatelný stav. */
    public val session: StateFlow<UserProfile?>

    /** Vrátí aktuální JWT token (suspend). */
    public suspend fun currentToken(): String?

    /** Uloží profil přihlášeného uživatele do šifrovaného úložiště.
     * @param profile profil přihlášeného uživatele */
    public suspend fun save(profile: UserProfile)

    /** Vymaže session (odhlášení) ze šifrovaného úložiště. */
    public suspend fun clear()
}

/**
 * Bezpečné úložiště session postavené na Android Keystore (AES-256-GCM).
 * Hodnoty jsou v SharedPreferences uloženy pouze v zašifrované podobě.
 * @param context kontext aplikace (pro přístup k SharedPreferences a Keystore)
 */
@Suppress("NewApi")
public class SecureTokenStorage(private val context: Context) : TokenStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val sessionState: MutableStateFlow<UserProfile?> = MutableStateFlow(null)

    /** Aktuální profil přihlášeného uživatele jako pozorovatelný stav. */
    override val session: StateFlow<UserProfile?> = sessionState.asStateFlow()

    init {
        deleteLegacyStores()
        sessionState.value = readProfile()
    }

    private object Keys {
        const val JWT_TOKEN = "jwt_token"
        const val USER_ID = "user_id"
        const val USER_ROLE = "user_role"
        const val COMPANY_ID = "company_id"
        const val USER_EMAIL = "user_email"
    }

    /** Vrátí aktuální JWT token ze šifrovaného úložiště. */
    override suspend fun currentToken(): String? =
        withContext(Dispatchers.IO) {
            readString(Keys.JWT_TOKEN)
        }

    /** Uloží a zašifruje profil přihlášeného uživatele a aktualizuje stav session.
     * @param profile profil přihlášeného uživatele
     */
    @Suppress("NewApi")
    override suspend fun save(profile: UserProfile): Unit =
        withContext(Dispatchers.IO) {
            prefs.edit {
                putString(Keys.JWT_TOKEN, encrypt(profile.token))
                putString(Keys.USER_ID, encrypt(profile.userId.toString()))
                putString(Keys.USER_ROLE, encrypt(B2bJson.encodeToString(UserRole.serializer(), profile.role)))
                putString(Keys.COMPANY_ID, encrypt(profile.companyId?.toString() ?: ""))
                putString(Keys.USER_EMAIL, encrypt(profile.email))
            }
            sessionState.value = profile
        }

    /** Vymaže šifrované úložiště a nastaví session na null. */
    @Suppress("NewApi")
    override suspend fun clear(): Unit =
        withContext(Dispatchers.IO) {
            prefs.edit { clear() }
            sessionState.value = null
        }

    /** Přečte a dešifruje profil z úložiště; při chybě vrací null. */
    private fun readProfile(): UserProfile? {
        val token = readString(Keys.JWT_TOKEN) ?: return null
        val userId = readString(Keys.USER_ID)?.toLongOrNull() ?: return null
        val roleStr = readString(Keys.USER_ROLE) ?: return null
        val role =
            try {
                B2bJson.decodeFromString<UserRole>(roleStr)
            } catch (_: Exception) {
                return null
            }
        val companyId = readString(Keys.COMPANY_ID)?.toLongOrNull()
        val email = readString(Keys.USER_EMAIL) ?: ""
        return UserProfile(token, userId, role, companyId, email)
    }

    /** Přečte a dešifruje jednu hodnotu z úložiště; při chybě vrací null.
     * @param key klíč v SharedPreferences
     */
    private fun readString(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return decrypt(encrypted)
    }

    /** Odstraní soubory předchozích implementací úložiště session (DataStore a EncryptedSharedPreferences). */
    @Suppress("NewApi")
    private fun deleteLegacyStores() {
        File(context.filesDir, "datastore/b2b_session.preferences_pb").delete()
        context.deleteSharedPreferences("b2b_secure_prefs")
    }

    /** Zašifruje hodnotu klíčem z Android Keystore (AES-256-GCM) a vrátí Base64.
     * @param value hodnota k zašifrování
     */
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv: ByteArray = cipher.iv
        val encrypted: ByteArray = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Dešifruje Base64 hodnotu klíčem z Android Keystore; při chybě vrací null.
     * @param encoded Base64-zakódovaná zašifrovaná hodnota
     */
    private fun decrypt(encoded: String): String? =
        try {
            val combined: ByteArray = Base64.decode(encoded, Base64.NO_WRAP)
            val iv: ByteArray = combined.copyOfRange(0, GCM_IV_LENGTH)
            val payload: ByteArray = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(payload), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }

    /** Vrátí existující nebo nově vytvořený AES klíč v Android Keystore. */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        /** Název souboru SharedPreferences pro šifrovanou session. */
        private const val PREFS_NAME: String = "b2b_session_keystore"

        /** Alias klíče v Android Keystore. */
        private const val KEY_ALIAS: String = "b2b_session_key"

        /** Název poskytovatele Android Keystore. */
        private const val ANDROID_KEYSTORE: String = "AndroidKeyStore"

        /** Transformace AES-256-GCM. */
        private const val TRANSFORMATION: String = "AES/GCM/NoPadding"

        /** Délka inicializačního vektoru GCM v bajtech. */
        private const val GCM_IV_LENGTH: Int = 12

        /** Délka autentizačního tagu GCM v bitech. */
        private const val GCM_TAG_BITS: Int = 128
    }
}

package cz.b2brental.data.remote

import cz.b2brental.data.local.TokenStorage

/**
 * Rozhraní pro bezpečné vymazání session při vypršení autentizačního tokenu.
 * Odděluje závislost mezi síťovou vrstvou a DataStore.
 */
public interface SessionClearer {

    /**
     * Vymaže uložený JWT token a profil uživatele z DataStore.
     */
    public suspend fun clearSession(): Unit
}

/**
 * Implementace SessionClearer pomocí TokenStorage (DataStore).
 * @param tokenStorage úložiště session přihlášeného uživatele
 */
public class SessionClearerImpl(
    private val tokenStorage: TokenStorage,
) : SessionClearer {

    /**
     * Vymaže session voláním TokenStorage.clear().
     */
    public override suspend fun clearSession(): Unit = tokenStorage.clear()
}

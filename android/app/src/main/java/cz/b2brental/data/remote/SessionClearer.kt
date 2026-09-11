package cz.b2brental.data.remote

import cz.b2brental.data.local.B2bDatabase
import cz.b2brental.data.local.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Rozhraní pro bezpečné vymazání session při vypršení autentizačního tokenu.
 * Odděluje závislost mezi síťovou vrstvou a úložištěm session.
 */
public interface SessionClearer {

    /**
     * Vymaže uložený JWT token a profil uživatele z úložiště a offline cache Room.
     */
    public suspend fun clearSession(): Unit
}

/**
 * Implementace SessionClearer pomocí TokenStorage a Room databáze.
 * Vymaže session a offline cache Room.
 * @param tokenStorage úložiště session přihlášeného uživatele
 * @param database Room databáze pro offline cache
 */
public class SessionClearerImpl(
    private val tokenStorage: TokenStorage,
    private val database: B2bDatabase,
) : SessionClearer {

    /**
     * Vymaže session a offline cache Room.
     */
    public override suspend fun clearSession(): Unit {
        tokenStorage.clear()
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }
}

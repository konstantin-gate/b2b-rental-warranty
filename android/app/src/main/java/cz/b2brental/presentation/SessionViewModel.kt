@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.local.TokenStorage
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel spravující aktuální session přihlášeného uživatele.
 * Sleduje šifrované úložiště session a poskytuje informaci o přihlášení/odhlášení.
 * @param tokenStorage úložiště session přihlášeného uživatele
 * @param authRepository repozitář autentizace (pro odhlášení)
 */
public class SessionViewModel(
    tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Aktuální session uživatele (null = nepřihlášeno). */
    public val session: StateFlow<UserProfile?> = tokenStorage.session

    /** Odhlásí uživatele — vymaže session a offline cache. */
    public fun logout(): Unit {
        viewModelScope.launch { authRepository.logout() }
    }
}

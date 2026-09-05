@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.local.TokenStorage
import cz.b2brental.domain.model.UserProfile
import cz.b2brental.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel spravující aktuální session přihlášeného uživatele.
 * Sleduje DataStore a poskytuje informaci o přihlášení/zobrazení.
 * @param tokenStorage úložiště session přihlášeného uživatele
 * @param authRepository repozitář autentizace (pro odhlášení)
 */
public class SessionViewModel(
    tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Aktuální session uživatele (null = nepřihlášeno). */
    public val session: StateFlow<UserProfile?> = tokenStorage.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Odhlásí uživatele — vymaže session v DataStore. */
    public fun logout(): Unit {
        viewModelScope.launch { authRepository.logout() }
    }
}

package cz.b2brental.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.local.TokenStorage
import cz.b2brental.domain.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel spravující aktuální session přihlášeného uživatele.
 * Sleduje DataStore a poskytuje informaci o přihlášení/zobrazení.
 * @param tokenStorage úložiště session přihlášeného uživatele
 */
public class SessionViewModel(
    tokenStorage: TokenStorage,
) : ViewModel() {

    /** Aktuální session uživatele (null = nepřihlášeno). */
    public val session: StateFlow<UserProfile?> = tokenStorage.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

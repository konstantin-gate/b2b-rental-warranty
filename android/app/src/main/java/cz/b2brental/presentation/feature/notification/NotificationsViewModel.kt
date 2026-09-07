package cz.b2brental.presentation.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.domain.model.NotificationItem
import cz.b2brental.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky notifikací.
 * @property isLoading probíhá načítání
 * @property items seznam notifikací
 * @property error text chyby (null = bez chyby)
 */
public data class NotificationsUiState(
    val isLoading: Boolean = false,
    val items: List<NotificationItem> = emptyList(),
    val error: String? = null,
)

/**
 * ViewModel obrazovky notifikací — načtení seznamu a označení notifikací jako přečtených.
 * @property repository repozitář notifikací
 */
public class NotificationsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())

    /** Aktuální UI stav notifikací. */
    public val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    /**
     * Načte seznam notifikací přihlášeného uživatele.
     * Chyba nesmí shodit UI — uloží se do stavu jako text.
     */
    public fun load(): Unit {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val items: List<NotificationItem> = repository.getNotifications(false)
                _state.update { it.copy(items = items, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Označí notifikaci jako přečtenou a znovu načte seznam.
     * Při chybě je text výjimky uložen do stavu (zobrazí se přes ErrorBanner).
     * @param item notifikace k označení
     */
    public fun markRead(item: NotificationItem): Unit {
        viewModelScope.launch {
            try {
                repository.markRead(item.id)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
                load()
            }
        }
    }

    /**
     * Označí všechny notifikace jako přečtené a znovu načte seznam.
     */
    public fun markAllRead(): Unit {
        viewModelScope.launch {
            try {
                repository.markAllRead()
            } catch (_: Exception) {
                // Chyba nezastaví obnovení seznamu
            }
            load()
        }
    }
}

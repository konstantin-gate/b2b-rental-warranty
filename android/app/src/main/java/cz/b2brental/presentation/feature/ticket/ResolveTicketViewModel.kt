@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.domain.model.Resolution
import cz.b2brental.domain.model.toWire
import cz.b2brental.domain.repository.TicketRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky vyřešení tiketu technikem.
 * @param resolution vybraný výsledek řešení
 * @param notes poznámky technika
 * @param isLoading probíhá odeslání
 * @param error typ chyby (null = bez chyby)
 * @param isResolved true po úspěšném vyřešení
 * @param reportDocumentId ID vygenerovaného reportu (null = bez reportu)
 */
public data class ResolveTicketUiState(
    val resolution: Resolution = Resolution.REPAIRED,
    val notes: String = "",
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
    val isResolved: Boolean = false,
    val reportDocumentId: Long? = null,
)

/**
 * ViewModel obrazovky vyřešení tiketu.
 * @param ticketRepository repozitář tiketů
 */
public class ResolveTicketViewModel(
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<ResolveTicketUiState> = MutableStateFlow(ResolveTicketUiState())

    /** Aktuální UI stav. */
    public val uiState: StateFlow<ResolveTicketUiState> = _uiState.asStateFlow()

    /** Změní vybraný výsledek řešení. */
    public fun onResolutionChange(resolution: Resolution): Unit {
        _uiState.update { it.copy(resolution = resolution) }
    }

    /** Změní poznámky technika. */
    public fun onNotesChange(notes: String): Unit {
        _uiState.update { it.copy(notes = notes) }
    }

    /**
     * Odešle řešení tiketu na backend. Při prázdných poznámkách vrátí chybu NOTES_EMPTY
     * bez síťového volání.
     * @param ticketId ID tiketu
     */
    public fun submit(ticketId: Long): Unit {
        val current: ResolveTicketUiState = _uiState.value
        if (current.notes.isBlank()) {
            _uiState.update { it.copy(error = ErrorType.NOTES_EMPTY) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = ticketRepository.resolve(
                    id = ticketId,
                    result = current.resolution.toWire(),
                    notes = current.notes,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isResolved = true,
                        reportDocumentId = response.reportDocumentId,
                    )
                }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.TICKET_RESOLVE_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }
}

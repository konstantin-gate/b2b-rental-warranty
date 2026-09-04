package cz.b2brental.presentation.feature.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.model.TicketStatus
import cz.b2brental.domain.repository.TicketRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky seznamu hlášení.
 * @param tickets kompletní seznam tiketů z backendu
 * @param selectedStatus aktivní filtr stavu (null = «Vše»)
 * @param isLoading probíhá načítání
 * @param error typ chyby (null = bez chyby)
 */
public data class TicketsUiState(
    val tickets: List<TicketResponseDto> = emptyList(),
    val selectedStatus: TicketStatus? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
)

/**
 * ViewModel seznamu servisních hlášení. Filtrace po stavu probíhá lokálně
 * (backend GET /tickets filtr nepodporuje).
 * @param ticketRepository repozitář servisních tiketů
 */
public class TicketsViewModel(
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketsUiState())

    /** Aktuální UI stav seznamu hlášení. */
    public val uiState: StateFlow<TicketsUiState> = _uiState.asStateFlow()

    init {
        loadTickets()
    }

    /**
     * Načte seznam hlášení z backendu.
     * Pořadí catch: OfflineException → OFFLINE, ApiException → TICKETS_LOAD_FAILED, Exception → UNKNOWN.
     */
    public fun loadTickets(): Unit {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tickets: List<TicketResponseDto> = ticketRepository.list()
                _uiState.update { it.copy(tickets = tickets, isLoading = false) }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.TICKETS_LOAD_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /**
     * Nastaví filtr stavu (null = vše) — filtr je čistě lokální.
     * @param status stav pro filtr (null = bez filtru)
     */
    public fun onFilterSelected(status: TicketStatus?): Unit {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    /**
     * Znovu načte seznam hlášení.
     */
    public fun retry(): Unit = loadTickets()
}
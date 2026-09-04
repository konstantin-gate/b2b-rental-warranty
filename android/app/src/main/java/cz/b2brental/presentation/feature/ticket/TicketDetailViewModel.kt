package cz.b2brental.presentation.feature.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.repository.TicketRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky detailu hlášení.
 * @param ticket data tiketu (null = není načteno)
 * @param isLoading probíhá načítání
 * @param error typ chyby (null = bez chyby)
 */
public data class TicketDetailUiState(
    val ticket: TicketResponseDto? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
)

/**
 * ViewModel detailu servisního hlášení.
 * @param ticketRepository repozitář servisních tiketů
 */
public class TicketDetailViewModel(
    private val ticketRepository: TicketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())

    /** Aktuální UI stav detailu. */
    public val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    private var ticketId: Long = 0L

    /**
     * Načte detail hlášení podle ID.
     * Pořadí catch: OfflineException → OFFLINE, ApiException → TICKET_DETAIL_LOAD_FAILED, Exception → UNKNOWN.
     * @param id ID tiketu
     */
    public fun loadTicket(id: Long): Unit {
        this.ticketId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ticket: TicketResponseDto = ticketRepository.get(id)
                _uiState.update { it.copy(ticket = ticket, isLoading = false) }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.TICKET_DETAIL_LOAD_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /**
     * Znovu načte detail hlášení podle posledního známého ID.
     */
    public fun retry(): Unit = loadTicket(ticketId)
}
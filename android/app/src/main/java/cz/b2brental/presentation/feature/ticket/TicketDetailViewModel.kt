@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.TechnicianResponseDto
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.repository.TicketRepository
import cz.b2brental.domain.repository.UserRepository
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
 * @param technicians seznam techniků (pro dialog přiřazení)
 * @param showAssignDialog zobrazit dialog výběru technika
 * @param isActionInProgress probíhá akce (přiřazení/zahájení)
 */
public data class TicketDetailUiState(
    val ticket: TicketResponseDto? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
    val technicians: List<TechnicianResponseDto> = emptyList(),
    val showAssignDialog: Boolean = false,
    val isActionInProgress: Boolean = false,
)

/**
 * ViewModel detailu servisního hlášení.
 * @param ticketRepository repozitář servisních tiketů
 * @param userRepository repozitář uživatelů (technici)
 */
public class TicketDetailViewModel(
    private val ticketRepository: TicketRepository,
    private val userRepository: UserRepository,
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

    /**
     * Načte seznam techniků a otevře dialog přiřazení.
     */
    public fun loadTechnicians(): Unit {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val technicians: List<TechnicianResponseDto> = userRepository.getTechnicians()
                _uiState.update {
                    it.copy(
                        technicians = technicians,
                        showAssignDialog = true,
                        isLoading = false,
                    )
                }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.TECHNICIANS_LOAD_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /** Zavře dialog výběru technika. */
    public fun dismissAssignDialog(): Unit {
        _uiState.update { it.copy(showAssignDialog = false) }
    }

    /**
     * Přiřadí vybraného technika k tiketu.
     * @param technicianId ID technika
     */
    public fun assignTechnician(technicianId: Long): Unit {
        val id: Long = ticketId
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, error = null) }
            try {
                ticketRepository.assign(id, technicianId)
                _uiState.update { it.copy(showAssignDialog = false, isActionInProgress = false) }
                loadTicket(id)
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isActionInProgress = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isActionInProgress = false, error = ErrorType.TICKET_ASSIGN_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isActionInProgress = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /** Zahájí opravu tiketu (technik). */
    public fun startWork(): Unit {
        val id: Long = ticketId
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, error = null) }
            try {
                ticketRepository.start(id)
                _uiState.update { it.copy(isActionInProgress = false) }
                loadTicket(id)
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isActionInProgress = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isActionInProgress = false, error = ErrorType.TICKET_START_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isActionInProgress = false, error = ErrorType.UNKNOWN) }
            }
        }
    }
}

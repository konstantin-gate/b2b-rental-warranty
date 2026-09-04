package cz.b2brental.presentation.feature.ticket

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.TicketCreateRequestDto
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.repository.TicketRepository
import cz.b2brental.domain.util.PhotoEncoder
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky hlášení poruchy.
 * @param equipmentId ID vybavení, ke kterému se hlášení vztahuje
 * @param description popis poruchy zadaný uživatelem
 * @param photoUri řetězcová reprezentace Uri přiložené fotografie (null = bez fotografie)
 * @param photoBase64 kódovaná fotografie (null = bez fotografie)
 * @param isLoading probíhá odesílání hlášení
 * @param error typ chyby (null = bez chyby)
 * @param createdTicket vytvořený tiket s AI diagnostikou (null = dosud nevytvořen)
 */
public data class ReportIssueUiState(
    val equipmentId: Long = 0L,
    val description: String = "",
    val photoUri: String? = null,
    val photoBase64: String? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
    val createdTicket: TicketResponseDto? = null,
)

/**
 * ViewModel obrazovky hlášení poruchy.
 * Řídí validaci popisu, kódování fotografie a odeslání tiketu na backend.
 * @param ticketRepository repozitář servisních tiketů
 * @param photoEncoder kodér fotografií do Base64
 */
public class ReportIssueViewModel(
    private val ticketRepository: TicketRepository,
    private val photoEncoder: PhotoEncoder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportIssueUiState())

    /** Aktuální UI stav hlášení. */
    public val uiState: StateFlow<ReportIssueUiState> = _uiState.asStateFlow()

    /**
     * Inicializace ViewModelu ID vybavení (volá se z obrazovky při prvním zobrazení).
     * @param equipmentId ID vybavení
     */
    public fun onInit(equipmentId: Long): Unit {
        _uiState.update { it.copy(equipmentId = equipmentId) }
    }

    /**
     * Aktualizuje popis poruchy a vymaže případnou chybu.
     * @param value nový text popisu
     */
    public fun onDescriptionChanged(value: String): Unit {
        _uiState.update { it.copy(description = value, error = null) }
    }

    /**
     * Zpracuje nově vyfocenou fotografii: kóduje do Base64 (downscale + EXIF).
     * @param context kontext pro přístup k ContentResolveru
     * @param uri Uri fotografie z kamery
     */
    public fun onPhotoCaptured(context: Context, uri: Uri): Unit {
        encodePhoto(context, uri)
    }

    /**
     * Zpracuje fotografii vybranou z galerie (pravidlo P8) — stejný PhotoEncoder.
     * @param context kontext pro přístup k ContentResolveru
     * @param uri Uri fotografie z galerie
     */
    public fun onGalleryPhotoSelected(context: Context, uri: Uri): Unit {
        encodePhoto(context, uri)
    }

    /**
     * Odebere přiloženou fotografii ze stavu.
     */
    public fun onPhotoRemoved(): Unit {
        _uiState.update { it.copy(photoUri = null, photoBase64 = null) }
    }

    /**
     * Odešle hlášení na backend. Nejprve validuje popis (min. 10 znaků dle backendu),
     * poté zavolá TicketRepository.create. Při úspěchu uloží createdTicket
     * (obrazovka zobrazí dialog AI diagnostiky).
     */
    public fun submit(): Unit {
        val state: ReportIssueUiState = _uiState.value
        if (state.description.isBlank()) {
            _uiState.update { it.copy(error = ErrorType.EMPTY_DESCRIPTION) }
            return
        }
        if (state.description.trim().length < MIN_DESCRIPTION_LENGTH) {
            _uiState.update { it.copy(error = ErrorType.DESCRIPTION_TOO_SHORT) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ticket: TicketResponseDto = ticketRepository.create(
                    TicketCreateRequestDto(
                        equipmentId = state.equipmentId,
                        description = state.description.trim(),
                        photoBase64 = state.photoBase64,
                    ),
                )
                _uiState.update { it.copy(isLoading = false, createdTicket = ticket) }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.TICKET_CREATE_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /**
     * Potvrdí dialog AI diagnostiky: vymaže createdTicket a zavolá navigační callback.
     * @param onNavigateToTickets callback přechodu na seznam hlášení
     */
    public fun onSuccessDialogDismissed(onNavigateToTickets: () -> Unit): Unit {
        _uiState.update { it.copy(createdTicket = null) }
        onNavigateToTickets()
    }

    /**
     * Společná logika kódování fotografie (kamera i galerie).
     */
    @Suppress("KDocMissingDocumentation")
    private fun encodePhoto(context: Context, uri: Uri): Unit {
        viewModelScope.launch {
            try {
                val base64: String = photoEncoder.encodeToBase64(context, uri)
                _uiState.update { it.copy(photoUri = uri.toString(), photoBase64 = base64, error = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(error = ErrorType.PHOTO_ENCODE_FAILED) }
            }
        }
    }

    private companion object {
        /** Minimální délka popisu poruchy (stejný limit jako backend TicketService). */
        public const val MIN_DESCRIPTION_LENGTH: Int = 10
    }
}
package cz.b2brental.presentation.feature.contract

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.dto.ContractResponseDto
import cz.b2brental.domain.repository.ContractRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky detailu smlouvy.
 * @param contract data smlouvy (null = není načteno)
 * @param isLoading probíhá načítání
 * @param error chybová zpráva
 * @param pdfDocumentId ID PDF dokumentu (pro otevření prohlížeče)
 */
public data class ContractDetailUiState(
    val contract: ContractResponseDto? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
    val pdfDocumentId: Long? = null,
)

/**
 * ViewModel obrazovky detailu nájemní smlouvy.
 * @param contractRepository repozitář smluv
 */
public class ContractDetailViewModel(
    private val contractRepository: ContractRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractDetailUiState())
    /** Aktuální UI stav detailu. */
    public val uiState: StateFlow<ContractDetailUiState> = _uiState.asStateFlow()

    private var contractId: Long = 0

    /**
     * Načte detail smlouvy podle ID.
     * @param id ID smlouvy
     */
    public fun loadContract(id: Long): Unit {
        contractId = id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val contract = contractRepository.get(id)
                _uiState.value = _uiState.value.copy(contract = contract, isLoading = false)
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.CONTRACT_DETAIL_LOAD_FAILED,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    /**
     * Schválí smlouvu (pouze pro manager/admin).
     */
    public fun approveContract(): Unit {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                contractRepository.approve(contractId)
                loadContract(contractId)
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.CONTRACT_APPROVE_FAILED,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    /**
     * Zamítne smlouvu (pouze pro manager/admin).
     */
    public fun rejectContract(): Unit {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                contractRepository.reject(contractId)
                loadContract(contractId)
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.CONTRACT_REJECT_FAILED,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    /**
     * Vyžádá PDF smlouvy (pouze pro aktivní smlouvy).
     */
    public fun requestPdf(): Unit {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = contractRepository.requestPdf(contractId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pdfDocumentId = result.documentId,
                )
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.CONTRACT_PDF_FAILED,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    /**
     * Znovu načte detail smlouvy podle posledního známého ID.
     */
    public fun retry(): Unit = loadContract(contractId)
}

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
 * UI stav obrazovky seznamu smluv.
 * @param contracts seznam smluv
 * @param isLoading probíhá načítání
 * @param error chybová zpráva
 */
public data class ContractsUiState(
    val contracts: List<ContractResponseDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
)

/**
 * ViewModel obrazovky seznamu nájemních smluv.
 * @param contractRepository repozitář smluv
 */
public class ContractsViewModel(
    private val contractRepository: ContractRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractsUiState())
    /** Aktuální UI stav. */
    public val uiState: StateFlow<ContractsUiState> = _uiState.asStateFlow()

    init {
        loadContracts()
    }

    /**
     * Načte seznam smluv z API.
     */
    private fun loadContracts(): Unit {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val contracts = contractRepository.list()
                _uiState.value = _uiState.value.copy(contracts = contracts, isLoading = false)
            } catch (_: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.CONTRACT_LIST_LOAD_FAILED,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    /**
     * Znovu načte seznam smluv.
     */
    public fun retry(): Unit = loadContracts()
}

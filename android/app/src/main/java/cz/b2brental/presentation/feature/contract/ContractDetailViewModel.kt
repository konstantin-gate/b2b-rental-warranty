package cz.b2brental.presentation.feature.contract

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.data.remote.dto.ContractResponseDto
import cz.b2brental.domain.repository.CatalogRepository
import cz.b2brental.domain.repository.ContractRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky detailu smlouvy.
 * @property contract data smlouvy (null = není načteno)
 * @property isLoading probíhá načítání
 * @property error chybová zpráva
 * @property pdfDocumentId ID PDF dokumentu (pro otevření prohlížeče)
 * @property equipmentDetails detaily pozic katalogu podle equipmentId (chybějící klíč = detail se nepodařilo načíst)
 */
public data class ContractDetailUiState(
    val contract: ContractResponseDto? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
    val pdfDocumentId: Long? = null,
    val equipmentDetails: Map<Long, CatalogItemResponseDto> = emptyMap(),
)

/**
 * ViewModel obrazovky detailu nájemní smlouvy.
 * @param contractRepository repozitář smluv
 * @param catalogRepository repozitář katalogu (pro detaily pozic karet)
 */
public class ContractDetailViewModel(
    private val contractRepository: ContractRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractDetailUiState())
    /** Aktuální UI stav detailu. */
    public val uiState: StateFlow<ContractDetailUiState> = _uiState.asStateFlow()

    private var contractId: Long = 0

    /**
     * Načte detail smlouvy podle ID.
     * @param id ID smlouvy
     */
    @Suppress("KDocMissingDocumentation")
    public fun loadContract(id: Long): Unit {
        contractId = id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val contract = contractRepository.get(id)

                // Detaily pozic pro barevné zvýraznění karet; chyba jedné pozice nebrání zobrazení smlouvy.
                val details: MutableMap<Long, CatalogItemResponseDto> = mutableMapOf()
                contract.items.forEach { item ->
                    try {
                        details[item.equipmentId] = catalogRepository.get(item.equipmentId)
                    } catch (_: Exception) {
                        // Detail se nepodařilo načíst — karta zůstane bez kategorie (šedý fallback).
                    }
                }

                _uiState.value = _uiState.value.copy(
                    contract = contract,
                    equipmentDetails = details,
                    isLoading = false,
                )
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

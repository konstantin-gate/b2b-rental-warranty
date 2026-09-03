package cz.b2brental.presentation.feature.contract

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.dto.ContractCreateRequestDto
import cz.b2brental.data.remote.dto.ContractResponseDto
import cz.b2brental.domain.repository.CatalogRepository
import cz.b2brental.domain.repository.ContractRepository
import cz.b2brental.domain.util.PricePreview
import cz.b2brental.domain.util.PricePreviewCalculator
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * UI stav obrazovky vytvoření nájemní smlouvy.
 * @param selectedIds seznam ID vybraných položek
 * @param months počet měsíců (1–36)
 * @param startDate datum zahájení (null = nezadáno)
 * @param deliveryAddress adresa dodání
 * @param preview předběžný výpočet cen (null = nevypočítáno)
 * @param isLoading probíhá načítání odesílání
 * @param error chybová zpráva
 * @param createdContract vytvořená smlouva (pro přechod na detail)
 */
public data class CreateContractUiState(
    val selectedIds: List<Long> = emptyList(),
    val months: Int = 6,
    val startDate: LocalDate? = null,
    val deliveryAddress: String = "",
    val preview: PricePreview? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
    val createdContract: ContractResponseDto? = null,
)

/**
 * ViewModel obrazovky vytvoření nájemní smlouvy.
 * @param catalogRepository repozitář katalogu (pro načtení sazeb)
 * @param contractRepository repozitář smluv (pro vytvoření)
 */
public class CreateContractViewModel(
    private val catalogRepository: CatalogRepository,
    private val contractRepository: ContractRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateContractUiState())
    /** Aktuální UI stav. */
    public val uiState: StateFlow<CreateContractUiState> = _uiState.asStateFlow()

    private var monthlyRates: List<String> = emptyList()

    /**
     * Inicializuje stav s vybranými ID a načte sazby z katalogu.
     * @param selectedIds seznam ID vybraných položek
     */
    @Suppress("KDocMissingDocumentation")
    public fun init(selectedIds: List<Long>): Unit {
        _uiState.value = _uiState.value.copy(selectedIds = selectedIds)
        viewModelScope.launch {
            try {
                monthlyRates = selectedIds.map { id ->
                    catalogRepository.get(id).monthlyRate
                }
                updatePreview()
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    error = ErrorType.RATES_LOAD_FAILED,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    /**
     * Nastaví počet měsíců a přepočítá preview.
     * @param months doba pronájmu v měsících (1–36)
     */
    public fun onMonthsChange(months: Int): Unit {
        _uiState.value = _uiState.value.copy(months = months)
        updatePreview()
    }

    /**
     * Nastaví datum zahájení a přepočítá preview.
     * @param date datum zahájení pronájmu
     */
    public fun onStartDateChange(date: LocalDate): Unit {
        _uiState.value = _uiState.value.copy(startDate = date)
    }

    /**
     * Nastaví adresu dodání.
     * @param address adresa dodání vybavení
     */
    public fun onDeliveryAddressChange(address: String): Unit {
        _uiState.value = _uiState.value.copy(deliveryAddress = address)
    }

    /**
     * Odešle požadavek na vytvoření smlouvy na backend.
     */
    public fun createContract(): Unit {
        val state = _uiState.value
        if (state.startDate == null) {
            _uiState.value = state.copy(error = ErrorType.EMPTY_DATE)
            return
        }
        if (state.deliveryAddress.isBlank()) {
            _uiState.value = state.copy(error = ErrorType.EMPTY_ADDRESS)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val req = ContractCreateRequestDto(
                    equipmentIds = state.selectedIds,
                    months = state.months,
                    startDate = state.startDate.toString(),
                    deliveryAddress = state.deliveryAddress,
                )
                val contract = contractRepository.create(req)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    createdContract = contract,
                )
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.CONTRACT_CREATE_FAILED,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.UNKNOWN,
                )
            }
        }
    }

    private fun updatePreview(): Unit {
        if (monthlyRates.isEmpty()) return
        try {
            val preview = PricePreviewCalculator.preview(monthlyRates, _uiState.value.months)
            _uiState.value = _uiState.value.copy(preview = preview)
        } catch (_: IllegalArgumentException) {
            // Neplatný počet měsíců — preview se nezobrazí
        }
    }
}

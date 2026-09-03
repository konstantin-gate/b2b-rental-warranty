package cz.b2brental.presentation.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.domain.repository.CatalogRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky detailu vybavení.
 * @param item data vybavení (null = není načteno)
 * @param isLoading probíhá načítání
 * @param error chybová zpráva (null = bez chyby)
 */
public data class EquipmentDetailUiState(
    val item: CatalogItemResponseDto? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
)

/**
 * ViewModel obrazovky detailu vybavení.
 * @param catalogRepository repozitář pro přístup ke katalogu
 */
public class EquipmentDetailViewModel(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EquipmentDetailUiState())
    /** Aktuální UI stav detailu. */
    public val uiState: StateFlow<EquipmentDetailUiState> = _uiState.asStateFlow()

    private var equipmentId: Long = 0L

    /**
     * Načte detail vybavení podle ID.
     * @param equipmentId ID vybavení
     */
    public fun loadEquipment(equipmentId: Long): Unit {
        this.equipmentId = equipmentId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val item = catalogRepository.get(equipmentId)
                _uiState.value = _uiState.value.copy(item = item, isLoading = false)
            } catch (_: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.EQUIPMENT_DETAIL_LOAD_FAILED,
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
     * Znovu načte detail vybavení podle posledního známého ID.
     */
    public fun retry(): Unit = loadEquipment(equipmentId)
}

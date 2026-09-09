package cz.b2brental.presentation.feature.equipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.domain.model.ContractStatus
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.domain.repository.CatalogRepository
import cz.b2brental.domain.repository.ContractRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Položka vybavení přiřazená aktivní smlouvou.
 * @property equipmentId ID vybavení
 * @property model model vybavení
 * @property serialNumber výrobní číslo (načtené z katalogu, null = nezjištěno)
 * @property categoryId id kategorie vybavení (0 = nezjištěno, karta dostane šedý fallback)
 * @property status stav vybavení (AVAILABLE = bez druhého pruhu)
 */
public data class MyEquipmentItem(
    val equipmentId: Long,
    val model: String,
    val serialNumber: String?,
    val categoryId: Long,
    val status: EquipmentStatus,
)

/**
 * UI stav obrazovky „Moje vybavení".
 * @param items seznam vybavení z aktivních smluv
 * @param isLoading probíhá načítání
 * @param error chybová zpráva
 */
public data class MyEquipmentUiState(
    val items: List<MyEquipmentItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
)

/**
 * ViewModel obrazovky „Moje vybavení" — zobrazí vybavení z aktivních smluv.
 * @param contractRepository repozitář smluv
 * @param catalogRepository repozitář katalogu (pro sériová čísla)
 */
public class MyEquipmentViewModel(
    private val contractRepository: ContractRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyEquipmentUiState())
    /** Aktuální UI stav. */
    public val uiState: StateFlow<MyEquipmentUiState> = _uiState.asStateFlow()

    init {
        loadEquipment()
    }

    /**
     * Načte vybavení z aktivních smluv.
     */
    @Suppress("KDocMissingDocumentation")
    public fun loadEquipment(): Unit {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val contracts = contractRepository.list()
                val activeContracts = contracts.filter { it.status == ContractStatus.ACTIVE }
                val allItems = activeContracts.flatMap { it.items }

                // Deduplikace podle equipmentId
                val uniqueItems = allItems.distinctBy { it.equipmentId }

                val equipment = uniqueItems.map { item ->
                    try {
                        val detail = catalogRepository.get(item.equipmentId)
                        MyEquipmentItem(
                            equipmentId = item.equipmentId,
                            model = item.model,
                            serialNumber = detail.serialNumber,
                            categoryId = detail.categoryId,
                            status = detail.status,
                        )
                    } catch (_: Exception) {
                        MyEquipmentItem(
                            equipmentId = item.equipmentId,
                            model = item.model,
                            serialNumber = null,
                            categoryId = 0L,
                            status = EquipmentStatus.AVAILABLE,
                        )
                    }
                }

                _uiState.value = _uiState.value.copy(items = equipment, isLoading = false)
            } catch (_: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.MY_EQUIPMENT_LOAD_FAILED,
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
     * Znovu načte vybavení z aktivních smluv.
     */
    public fun retry(): Unit = loadEquipment()
}

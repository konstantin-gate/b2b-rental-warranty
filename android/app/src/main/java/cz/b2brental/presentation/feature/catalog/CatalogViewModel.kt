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
 * UI stav obrazovky katalogu.
 * @param items seznam položek katalogu
 * @param isLoading probíhá načítání
 * @param isOffline offline režim (data z cache)
 * @param error chybová zpráva (null = bez chyby)
 * @param selectedIds množina ID vybraných položek (pro vytvoření smlouvy)
 * @param activeCategoryId ID aktivního filtru kategorie (null = bez filtru)
 */
public data class CatalogUiState(
    val items: List<CatalogItemResponseDto> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: ErrorType? = null,
    val selectedIds: Set<Long> = emptySet(),
    val activeCategoryId: Long? = null,
)

/**
 * ViewModel obrazovky katalogu — spravuje načítání, filtraci a výběr položek.
 * @param catalogRepository repozitář pro přístup ke katalogu
 */
public class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())

    /** Aktuální UI stav katalogu. */
    public val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    /**
     * Načte katalog z API (s offline fallbackem). Vždy načte celý katalog —
     * filtrace kategorií probíhá lokálně, aby přepnutí filtru nepřekreslovalo celou obrazovku.
     */
    public fun loadCatalog(): Unit {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = catalogRepository.list(null, null)
                _uiState.value = _uiState.value.copy(
                    items = result.items,
                    isLoading = false,
                    isOffline = result.isOffline,
                )
            } catch (_: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorType.CATALOG_LOAD_FAILED,
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
     * Nastaví aktivní filtr kategorie. Katalog se znovu nenačítá — položky se filtrují lokálně.
     * @param categoryId ID kategorie (null = bez filtru)
     */
    public fun filterByCategory(categoryId: Long?): Unit {
        _uiState.value = _uiState.value.copy(activeCategoryId = categoryId)
    }

    /**
     * Přepne výběr položky v režimu multi-select (jen pro client + status AVAILABLE).
     * @param itemId ID položky
     */
    public fun toggleSelection(itemId: Long): Unit {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _uiState.value = _uiState.value.copy(selectedIds = current)
    }

    /**
     * Vymaže všechny vybrané položky.
     */
    public fun clearSelection(): Unit {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }
}

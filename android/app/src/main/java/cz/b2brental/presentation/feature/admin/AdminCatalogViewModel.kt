@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.CatalogItemResponseDto
import cz.b2brental.data.remote.dto.CatalogUpsertRequestDto
import cz.b2brental.domain.repository.CatalogRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * UI stav obrazovky správy katalogu.
 * @param items seznam položek katalogu
 * @param isLoading probíhá načítání
 * @param isOffline offline režim (data z cache)
 * @param error typ chyby (null = bez chyby)
 * @param editingItem upravovaná položka (null = režim vytvoření)
 * @param showEditor zobrazit dialog editoru
 * @param isSaving probíhá ukládání
 */
public data class AdminCatalogUiState(
    val items: List<CatalogItemResponseDto> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: ErrorType? = null,
    val editingItem: CatalogItemResponseDto? = null,
    val showEditor: Boolean = false,
    val isSaving: Boolean = false,
)

/**
 * ViewModel obrazovky správy katalogu.
 * @param catalogRepository repozitář katalogu
 */
public class AdminCatalogViewModel(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<AdminCatalogUiState> = MutableStateFlow(AdminCatalogUiState())

    /** Aktuální UI stav. */
    public val uiState: StateFlow<AdminCatalogUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    /** Načte katalog (s offline fallbackem). */
    public fun loadCatalog(): Unit {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = catalogRepository.list(null, null)
                _uiState.update {
                    it.copy(
                        items = result.items,
                        isLoading = false,
                        isOffline = result.isOffline,
                    )
                }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.ADMIN_CATALOG_LOAD_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /** Otevře editor pro vytvoření nové položky. */
    public fun openCreate(): Unit {
        _uiState.update { it.copy(showEditor = true, editingItem = null) }
    }

    /**
     * Otevře editor pro úpravu existující položky.
     * @param item upravovaná položka
     */
    public fun openEdit(item: CatalogItemResponseDto): Unit {
        _uiState.update { it.copy(showEditor = true, editingItem = item) }
    }

    /** Zavře editor. */
    public fun closeEditor(): Unit {
        _uiState.update { it.copy(showEditor = false, editingItem = null) }
    }

    /**
     * Validuje vstupy a uloží (vytvoření nebo úprava) položku katalogu.
     * Při nevalidních datech vrátí ADMIN_CATALOG_VALIDATION bez síťového volání.
     * @param categoryId ID kategorie (1..4)
     * @param model model vybavení
     * @param serialNumber výrobní číslo
     * @param price prodejní cena jako řetězec (formát NNN.NN)
     * @param monthlyRate měsíční sazba jako řetězec (formát NNN.NN)
     * @param description popis vybavení
     * @param photoUrl URL fotografie vybavení
     */
    public fun save(
        categoryId: Long,
        model: String,
        serialNumber: String,
        price: String,
        monthlyRate: String,
        description: String,
        photoUrl: String,
    ): Unit {
        val priceDecimal: BigDecimal? = parseAmount(price)
        val rateDecimal: BigDecimal? = parseAmount(monthlyRate)
        val valid: Boolean = categoryId in 1L..4L
            && model.isNotBlank()
            && serialNumber.isNotBlank()
            && priceDecimal != null
            && rateDecimal != null
        if (!valid) {
            _uiState.update { it.copy(error = ErrorType.ADMIN_CATALOG_VALIDATION) }
            return
        }

        val req: CatalogUpsertRequestDto = CatalogUpsertRequestDto(
            categoryId = categoryId,
            model = model,
            serialNumber = serialNumber,
            price = priceDecimal.toPlainString(),
            monthlyRate = rateDecimal.toPlainString(),
            description = description.ifBlank { null },
            photoUrl = photoUrl.ifBlank { null },
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val current: AdminCatalogUiState = _uiState.value
            try {
                if (current.editingItem == null) {
                    catalogRepository.create(req)
                } else {
                    catalogRepository.update(current.editingItem.id, req)
                }
                _uiState.update { it.copy(isSaving = false, showEditor = false, editingItem = null) }
                loadCatalog()
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isSaving = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isSaving = false, error = ErrorType.ADMIN_CATALOG_SAVE_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /**
     * Smaže položku katalogu. Při konfliktu (pronajaté vybavení) zobrazí chybu.
     * @param item položka ke smazání
     */
    public fun delete(item: CatalogItemResponseDto): Unit {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                catalogRepository.delete(item.id)
                loadCatalog()
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.ADMIN_CATALOG_DELETE_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /**
     * Privátní parser částky.
     * @param raw vstupní řetězec částky
     * @return BigDecimal hodnota nebo null při nevalidním formátu
     */
    private fun parseAmount(raw: String): BigDecimal? {
        if (raw.isBlank()) return null
        return try {
            val bd: BigDecimal = BigDecimal(raw.replace(",", ".").trim())
            if (bd.scale() > 2) null else bd
        } catch (_: NumberFormatException) {
            null
        }
    }

    }

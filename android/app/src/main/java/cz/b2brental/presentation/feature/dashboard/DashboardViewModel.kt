@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.presentation.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.DashboardMetricsDto
import cz.b2brental.domain.repository.DashboardRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky přehledu.
 * @param metrics načtené metriky (null = nebyly načteny)
 * @param isLoading probíhá načítání
 * @param error typ chyby (null = bez chyby)
 */
public data class DashboardUiState(
    val metrics: DashboardMetricsDto? = null,
    val isLoading: Boolean = false,
    val error: ErrorType? = null,
)

/**
 * ViewModel obrazovky přehledu manažera/admina.
 * @param dashboardRepository repozitář metrik přehledu
 */
public class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<DashboardUiState> = MutableStateFlow(DashboardUiState())

    /** Aktuální UI stav přehledu. */
    public val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /**
     * Načte metriky přehledu.
     * Pořadí catch: OfflineException → OFFLINE, ApiException → DASHBOARD_LOAD_FAILED, Exception → UNKNOWN.
     */
    public fun loadMetrics(): Unit {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val metrics: DashboardMetricsDto = dashboardRepository.getMetrics()
                _uiState.update { it.copy(metrics = metrics, isLoading = false) }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.DASHBOARD_LOAD_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /** Znovu načte metriky přehledu. */
    public fun retry(): Unit = loadMetrics()
}

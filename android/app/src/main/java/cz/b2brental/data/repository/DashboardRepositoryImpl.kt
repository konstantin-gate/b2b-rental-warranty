@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.data.repository

import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.dto.DashboardMetricsDto
import cz.b2brental.domain.repository.DashboardRepository

/**
 * Implementace repozitáře metrik přehledu — přímé volání API bez cache.
 * @param apiClient HTTP klient pro backend API
 */
public class DashboardRepositoryImpl(
    private val apiClient: B2bApiClient,
) : DashboardRepository {
    /**
     * Načte metriky přehledu z backendu (GET /dashboard).
     * @return metriky přehledu
     */
    override suspend fun getMetrics(): DashboardMetricsDto = apiClient.getDashboard()
}

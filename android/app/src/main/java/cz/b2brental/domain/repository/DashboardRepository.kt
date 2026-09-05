package cz.b2brental.domain.repository

import cz.b2brental.data.remote.dto.DashboardMetricsDto

/** Rozhraní repozitáře metrik přehledu (dashboard manažera/admina). */
public interface DashboardRepository {

    /**
     * Načte metriky přehledu z backendu (GET /dashboard).
     * @return metriky přehledu
     */
    public suspend fun getMetrics(): DashboardMetricsDto
}

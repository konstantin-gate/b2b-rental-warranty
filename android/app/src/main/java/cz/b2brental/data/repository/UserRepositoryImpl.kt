@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.data.repository

import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.dto.TechnicianResponseDto
import cz.b2brental.domain.repository.UserRepository

/**
 * Implementace UserRepository — přímé volání API.
 * @param apiClient HTTP klient pro backend API
 */
public class UserRepositoryImpl(
    private val apiClient: B2bApiClient,
) : UserRepository {
    /**
     * Načte seznam techniků (GET /users?role=technician).
     * @return seznam techniků
     */
    override suspend fun getTechnicians(): List<TechnicianResponseDto> = apiClient.getTechnicians()
}

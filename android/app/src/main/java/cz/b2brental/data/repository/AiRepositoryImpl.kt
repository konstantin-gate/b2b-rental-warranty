@file:Suppress("KDocMissingDocumentation")

package cz.b2brental.data.repository

import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.dto.AssistantResponseDto
import cz.b2brental.domain.repository.AiRepository

/**
 * Implementace repozitáře AI asistenta — přímé volání API.
 * @param apiClient HTTP klient pro backend API
 */
public class AiRepositoryImpl(
    private val apiClient: B2bApiClient,
) : AiRepository {
    /**
     * Položí dotaz AI asistentovi (POST /ai/assistant).
     * @param message dotaz uživatele
     * @return odpověď asistenta
     */
    override suspend fun askAssistant(message: String): AssistantResponseDto =
        apiClient.askAssistant(message)
}

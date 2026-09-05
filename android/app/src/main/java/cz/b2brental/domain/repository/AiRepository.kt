package cz.b2brental.domain.repository

import cz.b2brental.data.remote.dto.AssistantResponseDto

/** Rozhraní repozitáře AI asistenta (jen pro managera/admina). */
public interface AiRepository {

    /**
     * Položí dotaz AI asistentovi (POST /ai/assistant).
     * @param message dotaz uživatele
     * @return odpověď asistenta
     */
    public suspend fun askAssistant(message: String): AssistantResponseDto
}

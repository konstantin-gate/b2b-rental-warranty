package cz.b2brental.domain.repository

import cz.b2brental.data.remote.dto.TechnicianResponseDto

/** Rozhraní repozitáře uživatelů (seznam techniků pro přiřazení). */
public interface UserRepository {

    /**
     * Načte seznam techniků (GET /users?role=technician).
     * @return seznam techniků
     */
    public suspend fun getTechnicians(): List<TechnicianResponseDto>
}

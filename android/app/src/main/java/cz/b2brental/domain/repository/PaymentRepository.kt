package cz.b2brental.domain.repository

import cz.b2brental.data.remote.dto.PaymentActionResponseDto
import cz.b2brental.data.remote.dto.PaymentResponseDto

/**
 * Rozhraní repozitáře pro správu plateb nájemného.
 */
public interface PaymentRepository {

    /**
     * Načte seznam plateb pro přihlášeného uživatele (volitelně filtrovaných dle smlouvy).
     * @param contractId ID smlouvy (null = všechny platby uživatele)
     * @return seznam plateb
     */
    public suspend fun list(contractId: Long?): List<PaymentResponseDto>

    /**
     * Označí platbu jako zaplacenou (POST /payments/{id}/pay).
     * @param id ID platby
     * @return výsledek akce s novým stavem a datem zaplacení
     * @throws cz.b2brental.data.remote.ApiException kód CONFLICT při HTTP 409 (již zaplaceno)
     */
    public suspend fun pay(id: Long): PaymentActionResponseDto
}
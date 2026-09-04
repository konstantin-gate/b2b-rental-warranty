package cz.b2brental.data.repository

import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.dto.PaymentActionResponseDto
import cz.b2brental.data.remote.dto.PaymentResponseDto
import cz.b2brental.domain.repository.PaymentRepository

/**
 * Implementace PaymentRepository — přímé přesměrování na B2bApiClient.
 * @param apiClient HTTP klient pro backend API
 */
public class PaymentRepositoryImpl(
    private val apiClient: B2bApiClient,
) : PaymentRepository {

    /**
     * Načte seznam plateb (volitelně filtrovaných dle contractId).
     * @param contractId ID smlouvy (null = všechny platby)
     * @return seznam plateb
     */
    override suspend fun list(contractId: Long?): List<PaymentResponseDto> {
        return apiClient.getPayments(contractId)
    }

    /**
     * Označí platbu jako zaplacenou přes POST /payments/{id}/pay.
     * @param id ID platby
     * @return výsledek akce s novým stavem a datem
     */
    override suspend fun pay(id: Long): PaymentActionResponseDto {
        return apiClient.payPayment(id)
    }
}
package cz.b2brental.data.repository

import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.dto.ContractActionResponseDto
import cz.b2brental.data.remote.dto.ContractCreateRequestDto
import cz.b2brental.data.remote.dto.ContractPdfResponseDto
import cz.b2brental.data.remote.dto.ContractResponseDto
import cz.b2brental.domain.repository.ContractRepository

/**
 * Implementace ContractRepository — přímé přesměrování na B2bApiClient.
 * @param apiClient HTTP klient pro backend API
 */
public class ContractRepositoryImpl(
    private val apiClient: B2bApiClient,
) : ContractRepository {

    /**
     * Vytvoří novou nájemní smlouvu.
     * @param req požadavek na vytvoření smlouvy
     * @return odpověď s detailem smlouvy
     */
    override suspend fun create(req: ContractCreateRequestDto): ContractResponseDto {
        return apiClient.createContract(req)
    }

    /**
     * Získá seznam všech smluv.
     * @return seznam smluv
     */
    override suspend fun list(): List<ContractResponseDto> {
        return apiClient.getContracts()
    }

    /**
     * Získá detail jedné smlouvy.
     * @param id ID smlouvy
     * @return detail smlouvy
     */
    override suspend fun get(id: Long): ContractResponseDto {
        return apiClient.getContract(id)
    }

    /**
     * Schválí smlouvu.
     * @param id ID smlouvy
     * @return odpověď s aktualizovaným stavem
     */
    override suspend fun approve(id: Long): ContractActionResponseDto {
        return apiClient.approveContract(id)
    }

    /**
     * Zamítne smlouvu.
     * @param id ID smlouvy
     * @return odpověď s aktualizovaným stavem
     */
    override suspend fun reject(id: Long): ContractActionResponseDto {
        return apiClient.rejectContract(id)
    }

    /**
     * Vyžádá PDF smlouvy.
     * @param id ID smlouvy
     * @return odpověď s ID dokumentu
     */
    override suspend fun requestPdf(id: Long): ContractPdfResponseDto {
        return apiClient.requestContractPdf(id)
    }
}

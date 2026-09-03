package cz.b2brental.domain.repository

import cz.b2brental.data.remote.dto.ContractActionResponseDto
import cz.b2brental.data.remote.dto.ContractCreateRequestDto
import cz.b2brental.data.remote.dto.ContractPdfResponseDto
import cz.b2brental.data.remote.dto.ContractResponseDto

/**
 * Rozhraní repozitáře pro přístup k nájemním smlouvám.
 */
public interface ContractRepository {

    /**
     * Vytvoří novou nájemní smlouvu.
     * @param req požadavek na vytvoření smlouvy
     * @return odpověď s detailem smlouvy
     */
    public suspend fun create(req: ContractCreateRequestDto): ContractResponseDto

    /**
     * Získá seznam všech smluv.
     * @return seznam smluv
     */
    public suspend fun list(): List<ContractResponseDto>

    /**
     * Získá detail jedné smlouvy.
     * @param id ID smlouvy
     * @return detail smlouvy
     */
    public suspend fun get(id: Long): ContractResponseDto

    /**
     * Schválí smlouvu.
     * @param id ID smlouvy
     * @return odpověď s aktualizovaným stavem
     */
    public suspend fun approve(id: Long): ContractActionResponseDto

    /**
     * Zamítne smlouvu.
     * @param id ID smlouvy
     * @return odpověď s aktualizovaným stavem
     */
    public suspend fun reject(id: Long): ContractActionResponseDto

    /**
     * Vyžádá PDF smlouvy.
     * @param id ID smlouvy
     * @return odpověď s ID dokumentu
     */
    public suspend fun requestPdf(id: Long): ContractPdfResponseDto
}

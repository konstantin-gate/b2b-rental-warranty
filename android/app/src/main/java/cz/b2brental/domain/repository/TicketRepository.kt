package cz.b2brental.domain.repository

import cz.b2brental.data.remote.dto.TicketActionResponseDto
import cz.b2brental.data.remote.dto.TicketCreateRequestDto
import cz.b2brental.data.remote.dto.TicketResponseDto

/**
 * Rozhraní repozitáře pro správu servisních hlášení (tiketů).
 * Přímé přesměrování na B2bApiClient bez lokální cache (fotografie Base64
 * jsou příliš velké pro offline ukládání).
 */
public interface TicketRepository {

    /**
     * Vytvoří nový servisní tiket (POST /tickets). Backend současně provede
     * AI diagnostiku a hodnocení záruky — výsledek je v odpovědi.
     * @param req požadavek na vytvoření tiketu
     * @return vytvořený tiket včetně severity, warrantyVerdict a aiRecommendation
     * @throws cz.b2brental.data.remote.ApiException při chybové odpovědi (400/403/409/…)
     * @throws cz.b2brental.data.remote.OfflineException při nedostupnosti backendu
     */
    public suspend fun create(req: TicketCreateRequestDto): TicketResponseDto

    /**
     * Načte seznam všech tiketů dostupných pro přihlášeného uživatele.
     * @return seznam tiketů (chronologicky dle odpovědi backendu)
     */
    public suspend fun list(): List<TicketResponseDto>

    /**
     * Načte detail jednoho tiketu.
     * @param id ID tiketu
     * @return detail tiketu
     */
    public suspend fun get(id: Long): TicketResponseDto

    /**
     * Přiřadí technika k tiketu (POST /tickets/{id}/assign). Vyžaduje roli manager/admin.
     * @param id ID tiketu
     * @param technicianId ID technika
     * @return odpověď s aktualizovaným stavem tiketu
     */
    public suspend fun assign(id: Long, technicianId: Long): TicketActionResponseDto

    /**
     * Zahájí opravu tiketu (POST /tickets/{id}/start). Vyžaduje roli technician.
     * @param id ID tiketu
     * @return odpověď s aktualizovaným stavem tiketu
     */
    public suspend fun start(id: Long): TicketActionResponseDto

    /**
     * Vyřeší tiket (POST /tickets/{id}/resolve). Vyžaduje roli technician.
     * @param id ID tiketu
     * @param result výsledek řešení (repaired/replaced/not_covered)
     * @param notes poznámky technika
     * @return odpověď s aktualizovaným stavem tiketu a případným ID reportu
     */
    public suspend fun resolve(id: Long, result: String, notes: String): TicketActionResponseDto
}

package cz.b2brental.data.repository

import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.dto.TicketActionResponseDto
import cz.b2brental.data.remote.dto.TicketCreateRequestDto
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.repository.TicketRepository

/**
 * Implementace TicketRepository — přímé přesměrování na B2bApiClient.
 * @param apiClient HTTP klient pro backend API
 */
public class TicketRepositoryImpl(
    private val apiClient: B2bApiClient,
) : TicketRepository {

    /**
     * Vytvoří nový tiket přes POST /tickets.
     * @param req požadavek na vytvoření tiketu
     * @return vytvořený tiket včetně AI diagnostiky
     */
    override suspend fun create(req: TicketCreateRequestDto): TicketResponseDto {
        return apiClient.createTicket(req)
    }

    /**
     * Načte seznam tiketů přes GET /tickets (bez lokální cache).
     * @return seznam tiketů
     */
    override suspend fun list(): List<TicketResponseDto> {
        return apiClient.getTickets()
    }

    /**
     * Načte detail tiketu přes GET /tickets/{id}.
     * @param id ID tiketu
     * @return detail tiketu
     */
    override suspend fun get(id: Long): TicketResponseDto {
        return apiClient.getTicket(id)
    }

    /**
     * Přiřadí technika k tiketu přes POST /tickets/{id}/assign.
     * @param id ID tiketu
     * @param technicianId ID technika
     * @return odpověď s aktualizovaným stavem
     */
    override suspend fun assign(id: Long, technicianId: Long): TicketActionResponseDto {
        return apiClient.assignTicket(id, technicianId)
    }

    /**
     * Zahájí opravu tiketu přes POST /tickets/{id}/start.
     * @param id ID tiketu
     * @return odpověď s aktualizovaným stavem
     */
    override suspend fun start(id: Long): TicketActionResponseDto {
        return apiClient.startTicket(id)
    }

    /**
     * Vyřeší tiket přes POST /tickets/{id}/resolve.
     * @param id ID tiketu
     * @param result výsledek řešení
     * @param notes poznámky technika
     * @return odpověď s aktualizovaným stavem a případným ID reportu
     */
    override suspend fun resolve(id: Long, result: String, notes: String): TicketActionResponseDto {
        return apiClient.resolveTicket(id, result, notes)
    }
}

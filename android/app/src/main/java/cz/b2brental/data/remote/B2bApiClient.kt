@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.data.remote

import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

/**
 * Klient pro komunikaci s B2B Rental backend API.
 * Každý požadavek (kromě login/register) obsahuje Authorization header z TokenStorage.
 * Při obdržení odpovědi 401 automaticky vymaže session a oznámí UI.
 * @param client HTTP klient Ktor
 * @param tokenStorage úložiště JWT tokenu
 * @param sessionClearer správce mazání session při vypršení tokenu
 */
public class B2bApiClient(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage,
    private val sessionClearer: SessionClearer,
) {

    /**
     * Přidá Authorization Bearer header z aktuální session.
     */
    private suspend fun HttpRequestBuilder.authHeader() {
        val token = tokenStorage.currentToken()
        if (!token.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    // --- AUTH ---

    /**
     * Přihlášení uživatele (POST /auth/login). Bez autentizace.
     * @param email přihlašovací e-mail
     * @param password heslo
     * @return odpověď s JWT tokenem a profilem uživatele
     */
    public suspend fun login(email: String, password: String): AuthResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/auth/login") {
                setBody(LoginRequestDto(email, password))
            }.body()
        }
    }

    /**
     * Registrace nové firmy (POST /auth/register-company). Bez autentizace.
     * @param req požadavek na registraci firmy
     * @return odpověď s ID firmy a uživatele
     */
    public suspend fun registerCompany(req: RegisterCompanyRequestDto): RegisterResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/auth/register-company") {
                setBody(req)
            }.body()
        }
    }

    // --- CATALOG ---

    /**
     * Získání seznamu vybavení z katalogu (GET /catalog). Vyžaduje roli admin/manager.
     * @param categoryId filtr dle ID kategorie (volitelné)
     * @param status filtr dle stavu (volitelné)
     * @return seznam položek katalogu
     */
    public suspend fun getCatalog(categoryId: Long?, status: String?): List<CatalogItemResponseDto> {
        return safeApiCall(sessionClearer) {
            client.get("/catalog") {
                authHeader()
                if (categoryId != null) parameter("categoryId", categoryId)
                if (status != null) parameter("status", status)
            }.body()
        }
    }

    /**
     * Získání detailu vybavení (GET /catalog/{id}). Vyžaduje roli admin/manager.
     * @param id ID vybavení
     * @return detail vybavení
     */
    public suspend fun getEquipment(id: Long): CatalogItemResponseDto {
        return safeApiCall(sessionClearer) {
            client.get("/catalog/$id") {
                authHeader()
            }.body()
        }
    }

    /**
     * Vytvoření nového vybavení (POST /catalog). Vyžaduje roli admin/manager.
     * @param req požadavek na vytvoření vybavení
     * @return odpověď s ID nového vybavení
     */
    public suspend fun createEquipment(req: CatalogUpsertRequestDto): IdResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/catalog") {
                authHeader()
                setBody(req)
            }.body()
        }
    }

    /**
     * Úprava existujícího vybavení (PUT /catalog/{id}). Vyžaduje roli admin/manager.
     * @param id ID vybavení
     * @param req požadavek na úpravu vybavení
     * @return aktualizovaný detail vybavení
     */
    public suspend fun updateEquipment(id: Long, req: CatalogUpsertRequestDto): CatalogItemResponseDto {
        return safeApiCall(sessionClearer) {
            client.put("/catalog/$id") {
                authHeader()
                setBody(req)
            }.body()
        }
    }

    /**
     * Smazání vybavení (DELETE /catalog/{id}). Vyžaduje roli admin/manager.
     * @param id ID vybavení
     */
    public suspend fun deleteEquipment(id: Long): Unit {
        safeApiCall(sessionClearer) {
            client.delete("/catalog/$id") {
                authHeader()
            }
        }
    }

    // --- CONTRACTS ---

    /**
     * Vytvoření nájemní smlouvy (POST /contracts). Vyžaduje roli client.
     * @param req požadavek na vytvoření smlouvy
     * @return odpověď s detailem smlouvy
     */
    public suspend fun createContract(req: ContractCreateRequestDto): ContractResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/contracts") {
                authHeader()
                setBody(req)
            }.body()
        }
    }

    /**
     * Získání seznamu nájemních smluv (GET /contracts). Vyžaduje přihlášení.
     * @return seznam smluv
     */
    public suspend fun getContracts(): List<ContractResponseDto> {
        return safeApiCall(sessionClearer) {
            client.get("/contracts") {
                authHeader()
            }.body()
        }
    }

    /**
     * Získání detailu nájemní smlouvy (GET /contracts/{id}). Vyžaduje přihlášení.
     * @param id ID smlouvy
     * @return detail smlouvy
     */
    public suspend fun getContract(id: Long): ContractResponseDto {
        return safeApiCall(sessionClearer) {
            client.get("/contracts/$id") {
                authHeader()
            }.body()
        }
    }

    /**
     * Schválení nájemní smlouvy (POST /contracts/{id}/approve). Vyžaduje roli manager.
     * @param id ID smlouvy
     * @return odpověď s aktualizovaným stavem
     */
    public suspend fun approveContract(id: Long): ContractActionResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/contracts/$id/approve") {
                authHeader()
            }.body()
        }
    }

    /**
     * Zamítnutí nájemní smlouvy (POST /contracts/{id}/reject). Vyžaduje roli manager.
     * @param id ID smlouvy
     * @return odpověď s aktualizovaným stavem
     */
    public suspend fun rejectContract(id: Long): ContractActionResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/contracts/$id/reject") {
                authHeader()
            }.body()
        }
    }

    /**
     * Vyžádání PDF nájemní smlouvy (POST /contracts/{id}/pdf). Vyžaduje přihlášení.
     * @param id ID smlouvy
     * @return odpověď s ID dokumentu
     */
    public suspend fun requestContractPdf(id: Long): ContractPdfResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/contracts/$id/pdf") {
                authHeader()
            }.body()
        }
    }

    // --- PAYMENTS ---

    /**
     * Získání seznamu plateb (GET /payments). Vyžaduje přihlášení.
     * @param contractId filtr dle ID smlouvy (volitelné)
     * @return seznam plateb
     */
    public suspend fun getPayments(contractId: Long?): List<PaymentResponseDto> {
        return safeApiCall(sessionClearer) {
            client.get("/payments") {
                authHeader()
                if (contractId != null) parameter("contract_id", contractId)
            }.body()
        }
    }

    /**
     * Označení platby jako zaplacené (POST /payments/{id}/pay). Vyžaduje přihlášení.
     * @param id ID platby
     * @return odpověď s aktualizovaným stavem platby
     */
    public suspend fun payPayment(id: Long): PaymentActionResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/payments/$id/pay") {
                authHeader()
            }.body()
        }
    }

    // --- TICKETS ---

    /**
     * Vytvoření servisního tiketu (POST /tickets). Vyžaduje roli client.
     * @param req požadavek na vytvoření tiketu
     * @return odpověď s detailem tiketu
     */
    public suspend fun createTicket(req: TicketCreateRequestDto): TicketResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/tickets") {
                authHeader()
                setBody(req)
            }.body()
        }
    }

    /**
     * Získání seznamu servisních tiketů (GET /tickets). Vyžaduje přihlášení.
     * @return seznam tiketů
     */
    public suspend fun getTickets(): List<TicketResponseDto> {
        return safeApiCall(sessionClearer) {
            client.get("/tickets") {
                authHeader()
            }.body()
        }
    }

    /**
     * Získání detailu servisního tiketu (GET /tickets/{id}). Vyžaduje přihlášení.
     * @param id ID tiketu
     * @return detail tiketu
     */
    public suspend fun getTicket(id: Long): TicketResponseDto {
        return safeApiCall(sessionClearer) {
            client.get("/tickets/$id") {
                authHeader()
            }.body()
        }
    }

    /**
     * Přiřazení technika k tiketu (POST /tickets/{id}/assign). Vyžaduje roli manager/admin.
     * @param id ID tiketu
     * @param technicianId ID technika
     * @return odpověď s aktualizovaným stavem
     */
    public suspend fun assignTicket(id: Long, technicianId: Long): TicketActionResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/tickets/$id/assign") {
                authHeader()
                setBody(AssignRequestDto(technicianId))
            }.body()
        }
    }

    /**
     * Zahájení opravy tiketu (POST /tickets/{id}/start). Vyžaduje roli technician.
     * @param id ID tiketu
     * @return odpověď s aktualizovaným stavem
     */
    public suspend fun startTicket(id: Long): TicketActionResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/tickets/$id/start") {
                authHeader()
            }.body()
        }
    }

    /**
     * Vyřešení tiketu (POST /tickets/{id}/resolve). Vyžaduje roli technician.
     * @param id ID tiketu
     * @param result výsledek řešení (repaired/replaced/not_covered)
     * @param notes poznámky technika
     * @return odpověď s aktualizovaným stavem a případným ID reportu
     */
    public suspend fun resolveTicket(id: Long, result: String, notes: String): TicketActionResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/tickets/$id/resolve") {
                authHeader()
                setBody(ResolveRequestDto(result, notes))
            }.body()
        }
    }

    // --- DOCUMENTS ---

    /**
     * Stažení PDF dokumentu (GET /documents/{documentId}/pdf). Vyžaduje přihlášení.
     * @param documentId ID dokumentu
     * @return obsah PDF jako pole bajtů
     */
    public suspend fun downloadPdf(documentId: Long): ByteArray {
        return safeApiCall(sessionClearer) {
            client.get("/documents/$documentId/pdf") {
                authHeader()
            }.body()
        }
    }

    // --- AI ---

    /**
     * AI diagnostika poruchy (POST /ai/diagnose). Vyžaduje přihlášení.
     * @param req popis poruchy a případná fotografie
     * @return odpověď s diagnózou, závažností a doporučením
     */
    public suspend fun diagnose(req: DiagnoseRequestDto): DiagnoseResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/ai/diagnose") {
                authHeader()
                setBody(req)
            }.body()
        }
    }

    /**
     * Kontrola pokrytí zárukou (POST /ai/warranty-check). Vyžaduje přihlášení.
     * @param req ID vybavení a popis problému
     * @return odpověď s verdiktem zárukou a odůvodněním
     */
    public suspend fun warrantyCheck(req: WarrantyCheckRequestDto): WarrantyCheckResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/ai/warranty-check") {
                authHeader()
                setBody(req)
            }.body()
        }
    }

    /**
     * Dotaz AI asistentovi (POST /ai/assistant). Vyžaduje roli manager/admin.
     * @param message dotaz uživatele
     * @return odpověď asistenta
     */
    public suspend fun askAssistant(message: String): AssistantResponseDto {
        return safeApiCall(sessionClearer) {
            client.post("/ai/assistant") {
                authHeader()
                setBody(AssistantRequestDto(message))
            }.body()
        }
    }

    // --- DASHBOARD ---

    /**
     * Získání metrik přehledu (GET /dashboard). Vyžaduje roli manager/admin.
     * @return metriky přehledu (aktivní smlouvy, otevřené tikety, platební přehled)
     */
    public suspend fun getDashboard(): DashboardMetricsDto {
        return safeApiCall(sessionClearer) {
            client.get("/dashboard") {
                authHeader()
            }.body()
        }
    }

    // --- USERS (D.0) ---

    /**
     * Získání seznamu techniků (GET /users?role=technician). Vyžaduje roli manager/admin.
     * @return seznam techniků
     */
    public suspend fun getTechnicians(): List<TechnicianResponseDto> {
        return safeApiCall(sessionClearer) {
            client.get("/users") {
                authHeader()
                parameter("role", "technician")
            }.body()
        }
    }

    // --- NOTIFICATIONS ---

    /**
     * Získání seznamu notifikací uživatele (GET /notifications). Vyžaduje přihlášení.
     * @param unreadOnly true pro vrácení pouze nepřečtených notifikací
     * @return seznam notifikací
     */
    public suspend fun getNotifications(unreadOnly: Boolean): List<NotificationDto> {
        return safeApiCall(sessionClearer) {
            client.get("/notifications") {
                authHeader()
                if (unreadOnly) parameter("unread", "true")
            }.body()
        }
    }

    /**
     * Získání počtu nepřečtených notifikací (GET /notifications/unread-count). Vyžaduje přihlášení.
     * @return počet nepřečtených notifikací
     */
    public suspend fun getUnreadNotificationCount(): UnreadCountDto {
        return safeApiCall(sessionClearer) {
            client.get("/notifications/unread-count") {
                authHeader()
            }.body()
        }
    }

    /**
     * Označení notifikace jako přečtené (POST /notifications/{id}/read). Vyžaduje přihlášení.
     * @param id ID notifikace
     * @return aktualizovaná notifikace
     */
    public suspend fun markNotificationRead(id: Long): NotificationDto {
        return safeApiCall(sessionClearer) {
            client.post("/notifications/$id/read") {
                authHeader()
            }.body()
        }
    }

    /**
     * Označení všech notifikací jako přečtené (POST /notifications/read-all). Vyžaduje přihlášení.
     * @return odpověď s nulovým počtem nepřečtených
     */
    public suspend fun markAllNotificationsRead(): UnreadCountDto {
        return safeApiCall(sessionClearer) {
            client.post("/notifications/read-all") {
                authHeader()
            }.body()
        }
    }
}

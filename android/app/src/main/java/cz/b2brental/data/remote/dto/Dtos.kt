package cz.b2brental.data.remote.dto

import cz.b2brental.domain.model.ContractStatus
import cz.b2brental.domain.model.EquipmentStatus
import cz.b2brental.domain.model.PaymentStatus
import cz.b2brental.domain.model.Severity
import cz.b2brental.domain.model.TicketStatus
import cz.b2brental.domain.model.WarrantyVerdict
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- AUTH ---

/**
 * Požadavek na přihlášení uživatele.
 * @param email přihlašovací e-mail
 * @param password heslo
 */
@Serializable
public data class LoginRequestDto(
    val email: String,
    val password: String,
)

/**
 * Odpověď na úspěšné přihlášení — obsahuje JWT token a profil uživatele.
 * @param token JWT token pro další požadavky
 * @param userId ID přihlášeného uživatele
 * @param role role uživatele (admin/manager/technician/client)
 * @param companyId ID firmy (null pro uživatele bez firmy)
 * @param email e-mail uživatele
 */
@Serializable
public data class AuthResponseDto(
    val token: String,
    val userId: Long,
    val role: String,
    val companyId: Long?,
    val email: String,
)

/**
 * Požadavek na registraci nové firmy.
 * @param companyName název firmy
 * @param inn IČO firmy
 * @param address sídlo firmy
 * @param adminEmail e-mail administrátora
 * @param password heslo administrátora
 * @param phone telefon administrátora (nepovinné)
 */
@Serializable
public data class RegisterCompanyRequestDto(
    val companyName: String,
    val inn: String,
    val address: String,
    val adminEmail: String,
    val password: String,
    val phone: String? = null,
)

/**
 * Odpověď na úspěšnou registraci firmy.
 * @param companyId ID nově vytvořené firmy
 * @param userId ID administrátora
 * @param email e-mail administrátora
 * @param role role administrátora
 */
@Serializable
public data class RegisterResponseDto(
    val companyId: Long,
    val userId: Long,
    val email: String,
    val role: String,
)

// --- CATALOG ---

/**
 * Odpověď s detailem položky katalogu vybavení.
 * @param id ID vybavení
 * @param categoryId ID kategorie
 * @param categoryName název kategorie
 * @param model model vybavení
 * @param serialNumber výrobní číslo
 * @param price cena vybavení
 * @param monthlyRate měsíční sazba za pronájem
 * @param description popis (nepovinný)
 * @param photoUrl URL fotografie (nepovinné)
 * @param status stav vybavení (available/rented/maintenance)
 */
@Serializable
public data class CatalogItemResponseDto(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val model: String,
    val serialNumber: String,
    val price: String,
    val monthlyRate: String,
    val description: String?,
    val photoUrl: String?,
    val status: EquipmentStatus,
)

/**
 * Požadavek na vytvoření nebo úpravu vybavení.
 * @param categoryId ID kategorie
 * @param model model vybavení
 * @param serialNumber výrobní číslo
 * @param price cena
 * @param monthlyRate měsíční sazba
 * @param description popis (nepovinný)
 * @param photoUrl URL fotografie (nepovinné)
 */
@Serializable
public data class CatalogUpsertRequestDto(
    val categoryId: Long,
    val model: String,
    val serialNumber: String,
    val price: String,
    val monthlyRate: String,
    val description: String? = null,
    val photoUrl: String? = null,
)

/**
 * Odpověď obsahující ID nové entity.
 * @param id ID vytvořené entity
 */
@Serializable
public data class IdResponseDto(val id: Long)

// --- CONTRACTS ---

/**
 * Požadavek na vytvoření nájemní smlouvy.
 * @param equipmentIds seznam ID vybavení
 * @param months doba pronájmu v měsících (1–36)
 * @param startDate datum zahájení (YYYY-MM-DD)
 * @param deliveryAddress adresa dodání
 */
@Serializable
public data class ContractCreateRequestDto(
    val equipmentIds: List<Long>,
    val months: Int,
    val startDate: String,
    val deliveryAddress: String,
)

/**
 * Položka nájemní smlouvy (vybavení).
 * @param equipmentId ID vybavení
 * @param model model vybavení
 */
@Serializable
public data class ContractItemDto(
    val equipmentId: Long,
    val model: String,
)

/**
 * Odpověď s detailem nájemní smlouvy.
 * @param id ID smlouvy
 * @param companyId ID firmy
 * @param companyName název firmy
 * @param status stav smlouvy
 * @param startDate datum zahájení
 * @param endDate datum ukončení
 * @param months doba pronájmu v měsících
 * @param monthlyAmount měsíční částka
 * @param deposit depozit
 * @param totalAmount celková částka
 * @param deliveryAddress adresa dodání
 * @param items položky smlouvy
 */
@Serializable
public data class ContractResponseDto(
    val id: Long,
    val companyId: Long,
    val companyName: String,
    val status: ContractStatus,
    val startDate: String,
    val endDate: String,
    val months: Int,
    val monthlyAmount: String,
    val deposit: String,
    val totalAmount: String,
    val deliveryAddress: String,
    val items: List<ContractItemDto>,
)

/**
 * Odpověď na akci se smlouvou (schválení/zamítnutí).
 * @param id ID smlouvy
 * @param status aktualizovaný stav smlouvy
 * @param paymentCount počet plateb (nepovinné)
 */
@Serializable
public data class ContractActionResponseDto(
    val id: Long,
    val status: ContractStatus,
    val paymentCount: Int? = null,
)

/**
 * Odpověď s ID PDF dokumentu smlouvy.
 * @param documentId ID dokumentu
 */
@Serializable
public data class ContractPdfResponseDto(val documentId: Long)

// --- PAYMENTS ---

/**
 * Odpověď s detailem platby.
 * @param id ID platby
 * @param contractId ID smlouvy
 * @param period číslo období
 * @param amount částka
 * @param dueDate datum splatnosti
 * @param status stav platby
 * @param paidAt datum zaplacení (nepovinné)
 */
@Serializable
public data class PaymentResponseDto(
    val id: Long,
    val contractId: Long,
    val period: Int,
    val amount: String,
    val dueDate: String,
    val status: PaymentStatus,
    val paidAt: String? = null,
)

/**
 * Odpověď na označení platby jako zaplacené.
 * @param id ID platby
 * @param status aktualizovaný stav platby
 * @param paidAt datum zaplacení
 */
@Serializable
public data class PaymentActionResponseDto(
    val id: Long,
    val status: PaymentStatus,
    val paidAt: String,
)

// --- TICKETS ---

/**
 * Požadavek na vytvoření servisního tiketu.
 * @param equipmentId ID vybavení
 * @param description popis poruchy
 * @param photoBase64 fotografie poruchy v Base64 (nepovinné)
 */
@Serializable
public data class TicketCreateRequestDto(
    val equipmentId: Long,
    val description: String,
    val photoBase64: String? = null,
)

/**
 * Odpověď s detailem servisního tiketu.
 * @param id ID tiketu
 * @param equipmentId ID vybavení
 * @param equipmentModel model vybavení
 * @param companyName název firmy
 * @param description popis poruchy
 * @param photoBase64 fotografie (nepovinné)
 * @param severity závažnost (nepovinné)
 * @param status stav tiketu
 * @param warrantyVerdict verdikt záruky (nepovinný)
 * @param warrantyReason důvod verdiktu záruky (nepovinný)
 * @param aiRecommendation doporučení AI (nepovinné)
 * @param technicianId ID technika (nepovinné)
 * @param resolution výsledek řešení (nepovinný)
 * @param createdAt datum vytvoření
 * @param resolvedAt datum vyřešení (nepovinné)
 */
@Serializable
public data class TicketResponseDto(
    val id: Long,
    val equipmentId: Long,
    val equipmentModel: String,
    val companyName: String,
    val description: String,
    val photoBase64: String? = null,
    val severity: Severity? = null,
    val status: TicketStatus,
    val warrantyVerdict: WarrantyVerdict? = null,
    val warrantyReason: String? = null,
    val aiRecommendation: String? = null,
    val technicianId: Long? = null,
    val resolution: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null,
)

/**
 * Požadavek na přiřazení technika k tiketu.
 * @param technicianId ID technika
 */
@Serializable
public data class AssignRequestDto(val technicianId: Long)

/**
 * Požadavek na vyřešení tiketu.
 * @param result výsledek řešení (repaired/replaced/not_covered)
 * @param notes poznámky technika
 */
@Serializable
public data class ResolveRequestDto(
    val result: String,
    val notes: String,
)

/**
 * Odpověď na akci s tiketem (přiřazení/zahájení/vyřešení).
 * @param id ID tiketu
 * @param status aktualizovaný stav tiketu
 * @param reportDocumentId ID report dokumentu (nepovinné)
 */
@Serializable
public data class TicketActionResponseDto(
    val id: Long,
    val status: TicketStatus,
    val reportDocumentId: Long? = null,
)

// --- AI ---

/**
 * Požadavek na AI diagnostiku poruchy.
 * @param description popis poruchy
 * @param photoBase64 fotografie poruchy v Base64 (nepovinné)
 */
@Serializable
public data class DiagnoseRequestDto(
    val description: String,
    val photoBase64: String? = null,
)

/**
 * Odpověď s výsledkem AI diagnózy.
 * @param possibleCause pravděpodobná příčina
 * @param severity závažnost (low/medium/critical)
 * @param recommendation doporučené kroky
 */
@Serializable
public data class DiagnoseResponseDto(
    val possibleCause: String,
    val severity: Severity,
    val recommendation: String,
)

/**
 * Požadavek na kontrolu pokrytí zárukou.
 * @param equipmentId ID vybavení
 * @param description popis problému (nepovinný)
 */
@Serializable
public data class WarrantyCheckRequestDto(
    val equipmentId: Long,
    val description: String? = null,
)

/**
 * Odpověď s výsledkem kontroly zárukou.
 * @param verdict verdikt (covered/not_covered/review_required)
 * @param reason důvod verdiktu
 * @param aiExplanation vysvětlení od AI
 */
@Serializable
public data class WarrantyCheckResponseDto(
    val verdict: WarrantyVerdict,
    val reason: String,
    val aiExplanation: String,
)

/**
 * Požadavek na dotaz AI asistentovi.
 * @param message dotaz uživatele
 */
@Serializable
public data class AssistantRequestDto(val message: String)

/**
 * Odpověď AI asistenta.
 * @param reply odpověď asistenta
 */
@Serializable
public data class AssistantResponseDto(val reply: String)

// --- DASHBOARD ---

/**
 * Metriky přehledu pro managera/admina.
 * @param activeContracts počet aktivních smluv
 * @param openTickets počet otevřených tiketů
 * @param overduePayments počet plateb po splatnosti
 * @param overdueAmount celková částka po splatnosti
 * @param equipmentByStatus rozložení vybavení dle stavu
 * @param monthStats statistiky aktuálního měsíce
 */
@Serializable
public data class DashboardMetricsDto(
    val activeContracts: Int,
    val openTickets: Int,
    val overduePayments: Int,
    val overdueAmount: String,
    val equipmentByStatus: EquipmentByStatusDto,
    val monthStats: MonthStatsDto,
)

/**
 * Rozložení vybavení dle stavu.
 * @param available počet dostupných
 * @param rented počet pronajatých
 * @param maintenance počet v údržbě
 */
@Serializable
public data class EquipmentByStatusDto(
    val available: Int,
    val rented: Int,
    val maintenance: Int,
)

/**
 * Statistiky aktuálního měsíce.
 * @param newContracts počet nových smluv
 * @param paymentsPaidTotal celková částka zaplacených plateb
 * @param resolvedTickets počet vyřešených tiketů
 */
@Serializable
public data class MonthStatsDto(
    val newContracts: Int,
    val paymentsPaidTotal: String,
    val resolvedTickets: Int,
)

// --- USERS (přidává se na backend na kroku D.0) ---

/**
 * Odpověď s detailem technika.
 * @param id ID technika
 * @param email e-mail technika
 * @param phone telefon (nepovinný)
 */
@Serializable
public data class TechnicianResponseDto(
    val id: Long,
    val email: String,
    val phone: String? = null,
)

// --- ERROR ---

/**
 * Detail chyby z backendu.
 * @param code kód chyby
 * @param message popis chyby
 */
@Serializable
public data class ErrorDetailsDto(
    val code: String,
    val message: String,
)

/**
 * Odpověď backendu při chybě.
 * @param error detail chyby
 */
@Serializable
public data class ErrorResponseDto(
    @SerialName("error")
    val error: ErrorDetailsDto,
)

@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.auth.JwtService
import cz.b2brental.auth.LoginRateLimiter
import cz.b2brental.db.Companies
import cz.b2brental.db.Users
import cz.b2brental.models.AuthResponse
import cz.b2brental.models.LoginRequest
import cz.b2brental.models.RegisterCompanyRequest
import cz.b2brental.models.RegisterResponse
import cz.b2brental.utils.BadRequestException
import cz.b2brental.utils.ConflictException
import cz.b2brental.utils.UnauthorizedException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

/**
 * Služba pro registraci firmy a přihlášení uživatelů.
 * Formáty e-mailu a IČO validují value třídy Email/Ico při deserializaci;
 * zde probíhají jen validace, které deserializace pokrýt nemůže (heslo,
 * prázdná pole, duplicitní e-mail), a zápis do databáze.
 * E-mail se ukládá a porovnává malými písmeny (lowercase).
 * @property jwtService služba pro podepisování a ověřování JWT tokenů
 * @property rateLimiter omezovač rychlosti přihlašování (bruteforce ochrana)
 */
public class AuthService(
    private val jwtService: JwtService,
    private val rateLimiter: LoginRateLimiter,
) {
    /**
     * Registrace firmy a jejího administrátora.
     * credit_limit a verification_status se nastavují výchozími hodnotami tabulky companies.
     * @param req požadavek na registraci firmy (název, IČO, adresa, e-mail, heslo, telefon)
     */
    public fun registerCompany(req: RegisterCompanyRequest): RegisterResponse {
        if (req.password.length < MIN_PASSWORD_LENGTH) {
            throw BadRequestException("Heslo musí mít alespoň $MIN_PASSWORD_LENGTH znaků")
        }
        if (!req.password.any { it.isLetter() }) {
            throw BadRequestException("Heslo musí obsahovat alespoň jedno písmeno")
        }
        if (!req.password.any { it.isDigit() }) {
            throw BadRequestException("Heslo musí obsahovat alespoň jednu číslici")
        }
        if (req.companyName.isBlank()) {
            throw BadRequestException("Název firmy nesmí být prázdný")
        }
        if (req.companyName.length > 200) {
            throw BadRequestException("Název firmy musí mít max. 200 znaků")
        }
        if (req.address.isBlank()) {
            throw BadRequestException("Adresa nesmí být prázdná")
        }
        if (req.address.length > 300) {
            throw BadRequestException("Adresa musí mít max. 300 znaků")
        }
        if (req.phone != null && req.phone.length > 50) {
            throw BadRequestException("Telefonní číslo musí mít max. 50 znaků")
        }

        val emailValue: String = req.adminEmail.value.lowercase()
        val innValue: String = req.inn.value

        return transaction {
            val emailExists: Boolean =
                Users.selectAll().where { Users.email eq emailValue }.firstOrNull() != null

            if (emailExists) {
                throw ConflictException("Uživatel s tímto e-mailem už existuje")
            }

            // Pre-check duplicitního IČO (409) před unikátním indexem uq_companies_inn
            val innExists: Boolean =
                Companies.selectAll().where { Companies.inn eq innValue }.firstOrNull() != null

            if (innExists) {
                throw ConflictException("Firma s tímto IČO už existuje")
            }

            val companyIdEntity: EntityID<Long> =
                Companies.insertAndGetId {
                    it[Companies.name] = req.companyName
                    it[Companies.inn] = innValue
                    it[Companies.address] = req.address
                }

            val passwordHash: String = BCrypt.hashpw(req.password, BCrypt.gensalt())

            val userIdEntity: EntityID<Long> =
                Users.insertAndGetId {
                    it[Users.role] = ROLE_ADMIN
                    it[Users.email] = emailValue
                    it[Users.passwordHash] = passwordHash
                    it[Users.phone] = req.phone
                    it[Users.companyId] = companyIdEntity
                }

            RegisterResponse(
                companyId = companyIdEntity.value,
                userId = userIdEntity.value,
                email = emailValue,
                role = ROLE_ADMIN,
            )
        }
    }

    /**
     * Přihlášení uživatele. Příčina selhání se nerozlišuje (401 v obou případech).
     * @param req přihlašovací údaje (e-mail a heslo)
     */
    public fun login(req: LoginRequest): AuthResponse {
        val emailValue: String = req.email.value.lowercase()

        rateLimiter.assertNotBlocked(emailValue.lowercase())

        val userRow: ResultRow? =
            transaction { Users.selectAll().where { Users.email eq emailValue }.firstOrNull() }

        if (userRow == null) {
            // Fixní hash pro vyrovnání času při neexistujícím e-mailu (timing-safe)
            BCrypt.checkpw(req.password, DUMMY_HASH)
            // Neúspěch u neexistujícího e-mailu se také počítá do limitu (ochrana proti hádání e-mailů)
            rateLimiter.recordFailure(emailValue)
            throw UnauthorizedException("Přihlášení selhalo (e-mail nebo heslo není správné)")
        }

        val userId: Long = userRow[Users.id].value
        val storedHash: String = userRow[Users.passwordHash]

        if (!BCrypt.checkpw(req.password, storedHash)) {
            rateLimiter.recordFailure(emailValue.lowercase())
            throw UnauthorizedException("Přihlášení selhalo (e-mail nebo heslo není správné)")
        }

        rateLimiter.reset(emailValue.lowercase())

        val role: String = userRow[Users.role]
        val companyId: Long? = userRow[Users.companyId]?.value
        val scope: String = if (companyId == null) JwtService.SCOPE_PLATFORM else JwtService.SCOPE_TENANT

        return AuthResponse(
            token = jwtService.makeToken(userId, role, companyId, scope),
            userId = userId,
            role = role,
            companyId = companyId,
            email = userRow[Users.email],
        )
    }

    /** Konstanty služby. */
    public companion object {
        /** Minimální délka hesla při registraci. */
        public const val MIN_PASSWORD_LENGTH: Int = 12

        private const val ROLE_ADMIN: String = "admin"

        /** Fixní hash pro vyrovnání času při neexistujícím e-mailu. */
        private val DUMMY_HASH: String = BCrypt.hashpw("neexistujici-uzivatel-demo", BCrypt.gensalt())
    }
}

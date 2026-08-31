@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.auth.JwtService
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
 */
public class AuthService(
    private val jwtService: JwtService,
) {
    /**
     * Registrace firmy a jejího administrátora.
     * credit_limit a verification_status se nastavují výchozími hodnotami tabulky companies.
     */
    public fun registerCompany(req: RegisterCompanyRequest): RegisterResponse {
        if (req.password.length < MIN_PASSWORD_LENGTH) {
            throw BadRequestException("Heslo musí mít alespoň $MIN_PASSWORD_LENGTH znaků")
        }
        if (req.companyName.isBlank()) {
            throw BadRequestException("Název firmy nesmí být prázdný")
        }
        if (req.address.isBlank()) {
            throw BadRequestException("Adresa nesmí být prázdná")
        }

        val emailValue: String = req.adminEmail.value
        val innValue: String = req.inn.value

        return transaction {
            val emailExists: Boolean =
                Users.selectAll().where { Users.email eq emailValue }.firstOrNull() != null

            if (emailExists) {
                throw ConflictException("Uživatel s e-mailem $emailValue už existuje")
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

    /** Přihlášení uživatele. Příčina selhání se nerozlišuje (401 v obou případech). */
    public fun login(req: LoginRequest): AuthResponse {
        val emailValue: String = req.email.value

        val userRow: ResultRow? =
            transaction { Users.selectAll().where { Users.email eq emailValue }.firstOrNull() }

        if (userRow == null) {
            throw UnauthorizedException("Přihlášení selhalo (e-mail nebo heslo není správné)")
        }

        val userId: Long = userRow[Users.id].value
        val storedHash: String = userRow[Users.passwordHash]

        if (!BCrypt.checkpw(req.password, storedHash)) {
            throw UnauthorizedException("Přihlášení selhalo (e-mail nebo heslo není správné)")
        }

        val role: String = userRow[Users.role]
        val companyId: Long? = userRow[Users.companyId]?.value

        return AuthResponse(
            token = jwtService.makeToken(userId, role, companyId),
            userId = userId,
            role = role,
            companyId = companyId,
            email = userRow[Users.email],
        )
    }

    /** Konstanty služby. */
    public companion object {
        /** Minimální délka hesla při registraci. */
        public const val MIN_PASSWORD_LENGTH: Int = 6

        private const val ROLE_ADMIN: String = "admin"
    }
}

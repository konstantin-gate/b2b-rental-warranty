@file:Suppress("HardCodedStringLiteral")

package cz.b2brental.services

import cz.b2brental.auth.JwtService
import cz.b2brental.auth.LoginRateLimiter
import cz.b2brental.db.Companies
import cz.b2brental.db.LoginAttempts
import cz.b2brental.db.LoginBlocks
import cz.b2brental.db.Users
import cz.b2brental.domain.Email
import cz.b2brental.models.LoginRequest
import cz.b2brental.models.RegisterCompanyRequest
import cz.b2brental.utils.TooManyRequestsException
import cz.b2brental.utils.UnauthorizedException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest {
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        Database.connect(
            "jdbc:h2:mem:authtest${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction {
            SchemaUtils.create(Companies, Users, LoginAttempts, LoginBlocks)
        }
        val jwtService = JwtService("test-secret-32-znaku-minimum-pro-hs256")
        authService = AuthService(jwtService, LoginRateLimiter())
    }

    @Test
    fun loginInvalidCredentialsTest() {
        assertFailsWith<UnauthorizedException> {
            authService.login(LoginRequest(Email("nobody@example.com"), "wrongpass123"))
        }
    }

    @Test
    fun loginWithExistingUserTest() {
        transaction {
            val compId =
                Companies.insertAndGetId {
                    it[name] = "Test s.r.o."
                    it[inn] = "28745001"
                    it[address] = "Praha"
                }
            Users.insertAndGetId {
                it[companyId] = compId
                it[email] = "test@b2b.demo"
                it[passwordHash] =
                    org.mindrot.jbcrypt.BCrypt
                        .hashpw(
                            "secret123456",
                            org.mindrot.jbcrypt.BCrypt
                                .gensalt(),
                        )
                it[role] = "client"
                it[phone] = "+420123456789"
            }
        }

        val result = authService.login(LoginRequest(Email("test@b2b.demo"), "secret123456"))
        assertNotNull(result.token)
        assertTrue(result.token.isNotBlank())
        assertEquals("client", result.role)
    }

    @Test
    fun registerCompanyTest() {
        val result =
            authService.registerCompany(
                RegisterCompanyRequest(
                    companyName = "New Corp",
                    inn = cz.b2brental.domain.Ico("28745001"),
                    address = "Brno",
                    adminEmail = Email("admin@new.cz"),
                    password = "pass1234abcd",
                ),
            )
        assertNotNull(result.companyId)
        assertNotNull(result.userId)
        assertEquals("admin", result.role)
    }

    @Test
    fun hesloKratkeNez12ZnakuJeOdmittle() {
        val ex =
            assertFailsWith<cz.b2brental.utils.BadRequestException> {
                authService.registerCompany(
                    RegisterCompanyRequest(
                        companyName = "Krátke heslo",
                        inn = cz.b2brental.domain.Ico("28745001"),
                        address = "Praha",
                        adminEmail = Email("kratke@test.cz"),
                        password = "Abc12345",
                    ),
                )
            }
        val message: String = ex.message ?: return
        assertTrue(message.contains("12 znaků"))
    }

    @Test
    fun hesloBezCisliceJeOdmittle() {
        val ex =
            assertFailsWith<cz.b2brental.utils.BadRequestException> {
                authService.registerCompany(
                    RegisterCompanyRequest(
                        companyName = "Heslo bez číslice",
                        inn = cz.b2brental.domain.Ico("28745001"),
                        address = "Brno",
                        adminEmail = Email("bezcislice@test.cz"),
                        password = "Abcdefghijkl",
                    ),
                )
            }
        val message: String = ex.message ?: return
        assertTrue(message.contains("číslici"))
    }

    @Test
    fun neexistujiciEmailSePocitaDoLimitu() {
        repeat(5) {
            assertFailsWith<UnauthorizedException> {
                authService.login(LoginRequest(Email("neexistuje@example.com"), "wrongpass123"))
            }
        }
        assertFailsWith<TooManyRequestsException> {
            authService.login(LoginRequest(Email("neexistuje@example.com"), "wrongpass123"))
        }
    }
}

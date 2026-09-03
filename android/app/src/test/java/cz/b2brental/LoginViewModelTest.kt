@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.remote.ApiException
import cz.b2brental.domain.repository.AuthRepository
import cz.b2brental.presentation.feature.auth.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Fake implementace AuthRepository pro testy ViewModelu. */
private class FakeAuthRepository : AuthRepository {
    var shouldFail = false
    var loginCalled = false

    override suspend fun login(email: String, password: String) {
        loginCalled = true
        if (shouldFail) throw ApiException("UNAUTHORIZED", 400, "Neplatné přihlašovací údaje")
    }

    override suspend fun registerCompany(
        companyName: String, inn: String, address: String,
        adminEmail: String, password: String, phone: String?,
    ): Unit {}

    override suspend fun logout(): Unit {}

    override fun session() = flowOf(null)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    public fun `login success updates state`(): Unit = runTest {
        val fakeRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeRepo)

        assertEquals("", viewModel.uiState.value.email)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onEmailChange("test@test.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorType)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(fakeRepo.loginCalled)
    }

    @Test
    public fun `login failure sets error`(): Unit = runTest {
        val fakeRepo = FakeAuthRepository().apply { shouldFail = true }
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onEmailChange("test@test.com")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}

package cz.b2brental.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Typ chyby při přihlášení — slouží k mapování na string resource v UI.
 */
public enum class LoginErrorType {
    /** Vyplňte e-mail i heslo. */
    EMPTY,
    /** Neočekávaná chyba (neznámý typ výjimky). */
    UNKNOWN,
}

/**
 * Stav obrazovky přihlášení.
 * @param email zadaný e-mail
 * @param password zadané heslo
 * @param isLoading probíhá přihlášení
 * @param errorType typ chyby pro mapování na string resource (null = bez chyby)
 * @param errorMessage text chyby z backendu (zobrazení přímo, bez mapování)
 */
public data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorType: LoginErrorType? = null,
    val errorMessage: String? = null,
)

/**
 * ViewModel obrazovky přihlášení.
 * Řídí validaci a komunikaci s AuthRepository.
 * @param authRepository repozitář pro autentizaci
 */
public class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState())
    /** Aktuální UI stav přihlašovací obrazovky. */
    public val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Aktualizuje zadaný e-mail a vymaže případnou chybu.
     * @param email nový e-mail
     */
    public fun onEmailChange(email: String): Unit {
        _uiState.update { it.copy(email = email, errorType = null, errorMessage = null) }
    }

    /**
     * Aktualizuje zadané heslo a vymaže případnou chybu.
     * @param password nové heslo
     */
    public fun onPasswordChange(password: String): Unit {
        _uiState.update { it.copy(password = password, errorType = null, errorMessage = null) }
    }

    /**
     * Spustí přihlášení — ověří vstup a zavolá authRepository.login().
     * Při úspěchu se session změní a AppRoot přepne obrazovku.
     */
    public fun login(): Unit {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorType = LoginErrorType.EMPTY) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorType = null, errorMessage = null) }
            try {
                authRepository.login(state.email, state.password)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: OfflineException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorType = LoginErrorType.UNKNOWN) }
            }
        }
    }
}

package cz.b2brental.presentation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.domain.repository.AuthRepository
import cz.b2brental.domain.util.EmailValidator
import cz.b2brental.domain.util.IcoValidator
import cz.b2brental.domain.util.PhoneValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Typ chyby validace formuláře registrace — slouží k mapování na string resource v UI.
 */
public enum class RegisterErrorType {
    /** Neplatný formát e-mailu. */
    INVALID_EMAIL,
    /** Vyplňte e-mail administrátora. */
    EMPTY_EMAIL,
    /** Neplatné IČO. */
    INVALID_INN,
    /** Neplatný formát telefonu. */
    INVALID_PHONE,
    /** Heslo musí mít alespoň 6 znaků. */
    SHORT_PASSWORD,
    /** Neočekávaná chyba. */
    UNKNOWN,
}

/**
 * Stav obrazovky registrace firmy.
 * @param companyName název firmy
 * @param inn IČO
 * @param address sídlo
 * @param adminEmail e-mail administrátora
 * @param password heslo
 * @param phone telefon
 * @param isLoading probíhá registrace
 * @param error globální chyba z backendu (zobrazení přímo)
 * @param fieldErrors mapování typu chyby na přítomnost (klíč = typ chyby, hodnota = bez významu)
 */
public data class RegisterUiState(
    val companyName: String = "",
    val inn: String = "",
    val address: String = "",
    val adminEmail: String = "",
    val password: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<RegisterErrorType, Boolean> = emptyMap(),
)

/**
 * ViewModel obrazovky registrace firmy.
 * Provádí klientskou validaci (IČO, e-mail, telefon, heslo) před odesláním na backend.
 * @param authRepository repozitář pro autentizaci
 */
public class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<RegisterUiState> = MutableStateFlow(RegisterUiState())
    /** Aktuální UI stav registrační obrazovky. */
    public val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * Aktualizuje název firmy a vymaže případnou chybu.
     * @param value nový název
     */
    public fun onCompanyNameChange(value: String): Unit {
        _uiState.update { it.copy(companyName = value, error = null) }
    }

    /**
     * Aktualizuje IČO a vymaže případnou chybu.
     * @param value nové IČO
     */
    public fun onInnChange(value: String): Unit {
        _uiState.update { it.copy(inn = value, error = null) }
    }

    /**
     * Aktualizuje adresu a vymaže případnou chybu.
     * @param value nová adresa
     */
    public fun onAddressChange(value: String): Unit {
        _uiState.update { it.copy(address = value, error = null) }
    }

    /**
     * Aktualizuje e-mail administrátora a vymaže případnou chybu.
     * @param value nový e-mail
     */
    public fun onAdminEmailChange(value: String): Unit {
        _uiState.update { it.copy(adminEmail = value, error = null) }
    }

    /**
     * Aktualizuje heslo a vymaže případnou chybu.
     * @param value nové heslo
     */
    public fun onPasswordChange(value: String): Unit {
        _uiState.update { it.copy(password = value, error = null) }
    }

    /**
     * Aktualizuje telefon a vymaže případnou chybu.
     * @param value nové telefonní číslo
     */
    public fun onPhoneChange(value: String): Unit {
        _uiState.update { it.copy(phone = value, error = null) }
    }

    /**
     * Spustí registraci — ověří vstupní údaje a zavolá authRepository.registerCompany().
     * Klientská validace: IČO, e-mail, telefon, heslo.
     */
    public fun register(): Unit {
        val state = _uiState.value
        val errors = mutableMapOf<RegisterErrorType, Boolean>()

        if (state.adminEmail.isBlank()) {
            errors[RegisterErrorType.EMPTY_EMAIL] = true
        } else if (!EmailValidator.isValid(state.adminEmail)) {
            errors[RegisterErrorType.INVALID_EMAIL] = true
        }

        if (!IcoValidator.isValid(state.inn)) {
            errors[RegisterErrorType.INVALID_INN] = true
        }
        if (state.phone.isNotBlank() && !PhoneValidator.isValid(state.phone)) {
            errors[RegisterErrorType.INVALID_PHONE] = true
        }
        if (state.password.length < 6) {
            errors[RegisterErrorType.SHORT_PASSWORD] = true
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
            try {
                authRepository.registerCompany(
                    companyName = state.companyName,
                    inn = state.inn,
                    address = state.address,
                    adminEmail = state.adminEmail,
                    password = state.password,
                    phone = state.phone.ifBlank { null },
                )
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            } catch (e: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, fieldErrors = mapOf(RegisterErrorType.UNKNOWN to true)) }
            }
        }
    }
}

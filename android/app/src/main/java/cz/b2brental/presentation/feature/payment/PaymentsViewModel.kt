package cz.b2brental.presentation.feature.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.PaymentResponseDto
import cz.b2brental.domain.repository.PaymentRepository
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI stav obrazovky plateb.
 * @param payments seznam plateb
 * @param isLoading probíhá načítání
 * @param payingPaymentId ID platby, která je právě označována jako zaplacená (null = nic)
 * @param error typ chyby (null = bez chyby)
 */
public data class PaymentsUiState(
    val payments: List<PaymentResponseDto> = emptyList(),
    val isLoading: Boolean = false,
    val payingPaymentId: Long? = null,
    val error: ErrorType? = null,
)

/**
 * ViewModel obrazovky plateb — načtení seznamu a označení platby jako zaplacené.
 * @param paymentRepository repozitář plateb
 */
public class PaymentsViewModel(
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentsUiState())

    /** Aktuální UI stav plateb. */
    public val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    init {
        loadPayments()
    }

    /**
     * Načte seznam plateb přihlášeného uživatele.
     * Pořadí catch: OfflineException → OFFLINE, ApiException → PAYMENTS_LOAD_FAILED, Exception → UNKNOWN.
     */
    public fun loadPayments(): Unit {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val payments: List<PaymentResponseDto> = paymentRepository.list(null)
                _uiState.update { it.copy(payments = payments, isLoading = false) }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.OFFLINE) }
            } catch (_: ApiException) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.PAYMENTS_LOAD_FAILED) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /**
     * Označí platbu jako zaplacenou. Při úspěchu lokálně aktualizuje stav položky
     * v seznamu na hodnotu z odpovědi backendu. HTTP 409 (již zaplaceno)
     * mapuje na ErrorType.PAYMENT_ALREADY_PAID.
     * Pořadí catch: ApiException (s kontrolou 409) → PAYMENT_ALREADY_PAID/PAYMENT_PAY_FAILED,
     *               OfflineException → OFFLINE, Exception → UNKNOWN.
     * @param paymentId ID platby
     */
    @Suppress("KDocMissingDocumentation")
    public fun pay(paymentId: Long): Unit {
        viewModelScope.launch {
            _uiState.update { it.copy(payingPaymentId = paymentId, error = null) }
            try {
                val action = paymentRepository.pay(paymentId)
                _uiState.update { state ->
                    state.copy(
                        payingPaymentId = null,
                        payments = state.payments.map { payment ->
                            if (payment.id == paymentId) {
                                payment.copy(status = action.status, paidAt = action.paidAt)
                            } else {
                                payment
                            }
                        },
                    )
                }
            } catch (e: ApiException) {
                val errorType: ErrorType = if (e.httpStatus == 409) {
                    ErrorType.PAYMENT_ALREADY_PAID
                } else {
                    ErrorType.PAYMENT_PAY_FAILED
                }
                _uiState.update { it.copy(payingPaymentId = null, error = errorType) }
            } catch (_: OfflineException) {
                _uiState.update { it.copy(payingPaymentId = null, error = ErrorType.OFFLINE) }
            } catch (_: Exception) {
                _uiState.update { it.copy(payingPaymentId = null, error = ErrorType.UNKNOWN) }
            }
        }
    }

    /**
     * Znovu načte seznam plateb.
     */
    public fun retry(): Unit = loadPayments()
}
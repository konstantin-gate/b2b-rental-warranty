@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.PaymentActionResponseDto
import cz.b2brental.data.remote.dto.PaymentResponseDto
import cz.b2brental.domain.model.PaymentStatus
import cz.b2brental.domain.repository.PaymentRepository
import cz.b2brental.presentation.feature.payment.PaymentsViewModel
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Fake implementace PaymentRepository pro testy PaymentsViewModel.
 */
private class FakePaymentRepository : PaymentRepository {
    val listPayments: List<PaymentResponseDto>
    var payShouldThrow: ApiException? = null
    var payOffline: Boolean = false
    var lastPaidId: Long? = null

    init {
        listPayments = listOf(
            PaymentResponseDto(
                id = 1L,
                contractId = 1L,
                period = 1,
                amount = "9000.00",
                dueDate = "2026-08-04",
                status = PaymentStatus.PAID,
                paidAt = "2026-08-04T00:00:00Z",
            ),
            PaymentResponseDto(
                id = 2L,
                contractId = 1L,
                period = 2,
                amount = "9000.00",
                dueDate = "2026-09-04",
                status = PaymentStatus.PAID,
                paidAt = "2026-09-04T00:00:00Z",
            ),
            PaymentResponseDto(
                id = 3L,
                contractId = 1L,
                period = 3,
                amount = "9000.00",
                dueDate = "2026-10-04",
                status = PaymentStatus.UNPAID,
            ),
        )
    }

    override suspend fun list(contractId: Long?): List<PaymentResponseDto> = listPayments

    override suspend fun pay(id: Long): PaymentActionResponseDto {
        lastPaidId = id
        payShouldThrow?.let { throw it }
        if (payOffline) throw OfflineException()
        return PaymentActionResponseDto(
            id = id,
            status = PaymentStatus.PAID,
            paidAt = "2026-09-04T12:00:00Z",
        )
    }
}

/**
 * Testy PaymentsViewModel — načtení, platba, 409, validace, offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class PaymentsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    public fun setup(): Unit {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    public fun tearDown(): Unit {
        Dispatchers.resetMain()
    }

    /**
     * init { loadPayments() } → payments.size == 3, isLoading == false, error == null.
     */
    @Test
    public fun `load payments fills state`(): Unit = runTest {
        val fakeRepo = FakePaymentRepository()
        val viewModel = PaymentsViewModel(fakeRepo)

        assertEquals(3, viewModel.uiState.value.payments.size)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    /**
     * viewModel.pay(3L) → platba 3 je PAID, payingPaymentId == null, fakeRepo.lastPaidId == 3L.
     */
    @Test
    public fun `successful pay updates payment to PAID`(): Unit = runTest {
        val fakeRepo = FakePaymentRepository()
        val viewModel = PaymentsViewModel(fakeRepo)

        viewModel.pay(3L)

        val updated = viewModel.uiState.value.payments.first { payment -> payment.id == 3L }
        assertEquals(PaymentStatus.PAID, updated.status)
        assertEquals(null, viewModel.uiState.value.payingPaymentId)
        assertEquals(3L, fakeRepo.lastPaidId)
    }

    /**
     * payShouldThrow = CONFLICT(409) → error == PAYMENT_ALREADY_PAID.
     */
    @Test
    public fun `409 conflict sets PAYMENT_ALREADY_PAID`(): Unit = runTest {
        val fakeRepo = FakePaymentRepository().apply {
            payShouldThrow = ApiException("CONFLICT", 409, "Platba již byla zaplacena")
        }
        val viewModel = PaymentsViewModel(fakeRepo)

        viewModel.pay(1L)

        assertEquals(ErrorType.PAYMENT_ALREADY_PAID, viewModel.uiState.value.error)
        assertEquals(null, viewModel.uiState.value.payingPaymentId)
    }

    /**
     * payShouldThrow = VALIDATION_ERROR(400) → error == PAYMENT_PAY_FAILED.
     */
    @Test
    public fun `api error sets PAYMENT_PAY_FAILED`(): Unit = runTest {
        val fakeRepo = FakePaymentRepository().apply {
            payShouldThrow = ApiException("VALIDATION_ERROR", 400, "Bad request")
        }
        val viewModel = PaymentsViewModel(fakeRepo)

        viewModel.pay(1L)

        assertEquals(ErrorType.PAYMENT_PAY_FAILED, viewModel.uiState.value.error)
        assertEquals(null, viewModel.uiState.value.payingPaymentId)
    }

    /**
     * payOffline = true → error == OFFLINE.
     */
    @Test
    public fun `offline sets OFFLINE`(): Unit = runTest {
        val fakeRepo = FakePaymentRepository().apply { payOffline = true }
        val viewModel = PaymentsViewModel(fakeRepo)

        viewModel.pay(1L)

        assertEquals(ErrorType.OFFLINE, viewModel.uiState.value.error)
        assertEquals(null, viewModel.uiState.value.payingPaymentId)
    }
}
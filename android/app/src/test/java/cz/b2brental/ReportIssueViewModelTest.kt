@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import android.content.Context
import android.net.Uri
import cz.b2brental.data.remote.ApiException
import cz.b2brental.data.remote.OfflineException
import cz.b2brental.data.remote.dto.TicketCreateRequestDto
import cz.b2brental.data.remote.dto.TicketResponseDto
import cz.b2brental.domain.model.Severity
import cz.b2brental.domain.model.TicketStatus
import cz.b2brental.domain.model.WarrantyVerdict
import cz.b2brental.domain.repository.TicketRepository
import cz.b2brental.domain.util.PhotoEncoder
import cz.b2brental.presentation.feature.ticket.ReportIssueViewModel
import cz.b2brental.presentation.util.ErrorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Fake implementace TicketRepository pro testy ReportIssueViewModel.
 * @property shouldFailOffline true → create vyhodí OfflineException
 * @property shouldFailApi true → create vyhodí ApiException(400)
 */
private class FakeTicketRepository : TicketRepository {
    var createCalled: Boolean = false
    var lastRequest: TicketCreateRequestDto? = null
    var shouldFailOffline: Boolean = false
    var shouldFailApi: Boolean = false

    private fun createTicketResponse(): TicketResponseDto = TicketResponseDto(
        id = 100L,
        equipmentId = 1L,
        equipmentModel = "Test Model",
        companyName = "Test Company",
        description = "Test description",
        severity = Severity.MEDIUM,
        status = TicketStatus.NEW,
        warrantyVerdict = WarrantyVerdict.COVERED,
        aiRecommendation = "Test recommendation",
        createdAt = "2026-09-04T06:00:00Z",
    )

    override suspend fun create(req: TicketCreateRequestDto): TicketResponseDto {
        createCalled = true
        lastRequest = req
        if (shouldFailOffline) throw OfflineException()
        if (shouldFailApi) throw ApiException("VALIDATION_ERROR", 400, "Validation failed")
        return createTicketResponse()
    }

    override suspend fun list(): List<TicketResponseDto> = emptyList()
    override suspend fun get(id: Long): TicketResponseDto = error("not used")
    override suspend fun assign(id: Long, technicianId: Long) = error("not used")
    override suspend fun start(id: Long) = error("not used")
    override suspend fun resolve(id: Long, result: String, notes: String) = error("not used")
}

/**
 * Fake PhotoEncoder — nevolá ContentResolver, v testu nepoužíváme fotografie.
 */
private class FakePhotoEncoder : PhotoEncoder {
    override suspend fun encodeToBase64(context: Context, imageUri: Uri): String = "base64fake"
}

/**
 * Testy ReportIssueViewModel — validace popisu, mappování chyb, vytvoření tiketu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class ReportIssueViewModelTest {

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
     * Prázdný popis → EMPTY_DESCRIPTION, create se nevolá.
     */
    @Test
    public fun `empty description sets EMPTY_DESCRIPTION`(): Unit = runTest {
        val fakeRepo = FakeTicketRepository()
        val viewModel = ReportIssueViewModel(fakeRepo, FakePhotoEncoder())

        viewModel.onInit(1L)
        viewModel.onDescriptionChanged("")
        viewModel.submit()

        assertEquals(ErrorType.EMPTY_DESCRIPTION, viewModel.uiState.value.error)
        assertFalse(fakeRepo.createCalled)
        assertNull(viewModel.uiState.value.createdTicket)
    }

    /**
     * Popis kratší než 10 znaků → DESCRIPTION_TOO_SHORT, create se nevolá.
     */
    @Test
    public fun `short description sets DESCRIPTION_TOO_SHORT`(): Unit = runTest {
        val fakeRepo = FakeTicketRepository()
        val viewModel = ReportIssueViewModel(fakeRepo, FakePhotoEncoder())

        viewModel.onInit(1L)
        viewModel.onDescriptionChanged("Krátký")
        viewModel.submit()

        assertEquals(ErrorType.DESCRIPTION_TOO_SHORT, viewModel.uiState.value.error)
        assertFalse(fakeRepo.createCalled)
    }

    /**
     * Platný popis (>= 10 znaků) → volá create s trim(), createdTicket != null.
     */
    @Test
    public fun `valid description creates ticket`(): Unit = runTest {
        val fakeRepo = FakeTicketRepository()
        val viewModel = ReportIssueViewModel(fakeRepo, FakePhotoEncoder())

        viewModel.onInit(1L)
        viewModel.onDescriptionChanged("  Chladnička nevychladuje, teplota 12 °C  ")
        viewModel.submit()

        assertEquals(true, fakeRepo.createCalled)
        assertEquals("Chladnička nevychladuje, teplota 12 °C", fakeRepo.lastRequest?.description)
        assertEquals(1L, fakeRepo.lastRequest?.equipmentId)
        assertNotNull(viewModel.uiState.value.createdTicket)
        assertNull(viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    /**
     * shouldFailOffline = true → submit() → OFFLINE.
     */
    @Test
    public fun `offline error maps to OFFLINE`(): Unit = runTest {
        val fakeRepo = FakeTicketRepository().apply { shouldFailOffline = true }
        val viewModel = ReportIssueViewModel(fakeRepo, FakePhotoEncoder())

        viewModel.onInit(1L)
        viewModel.onDescriptionChanged("Chladnička nevychladuje, teplota 12 °C")
        viewModel.submit()

        assertEquals(ErrorType.OFFLINE, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.createdTicket)
    }

    /**
     * shouldFailApi = true → submit() → TICKET_CREATE_FAILED.
     */
    @Test
    public fun `api error maps to TICKET_CREATE_FAILED`(): Unit = runTest {
        val fakeRepo = FakeTicketRepository().apply { shouldFailApi = true }
        val viewModel = ReportIssueViewModel(fakeRepo, FakePhotoEncoder())

        viewModel.onInit(1L)
        viewModel.onDescriptionChanged("Chladnička nevychladuje, teplota 12 °C")
        viewModel.submit()

        assertEquals(ErrorType.TICKET_CREATE_FAILED, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.createdTicket)
    }
}
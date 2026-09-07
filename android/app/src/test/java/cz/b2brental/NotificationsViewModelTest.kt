@file:Suppress("HardCodedStringLiteral")

package cz.b2brental

import cz.b2brental.domain.model.NotificationItem
import cz.b2brental.domain.repository.NotificationRepository
import cz.b2brental.presentation.feature.notification.NotificationsViewModel
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
 * Fake implementace NotificationRepository pro testy NotificationsViewModel.
 */
private class FakeNotificationRepository : NotificationRepository {
    var notifications: List<NotificationItem> =
        listOf(
            NotificationItem(
                id = 1L,
                message = "Nový servisní tiket #5 (Mrazicí skříň)",
                isRead = false,
                createdAt = "2026-09-07T09:00:00Z",
            ),
            NotificationItem(
                id = 2L,
                message = "Smlouva #3 byla schválena",
                isRead = true,
                createdAt = "2026-09-07T08:00:00Z",
            ),
        )
    var lastMarkedReadId: Long? = null
    var markAllReadCalled: Boolean = false

    override suspend fun getNotifications(unreadOnly: Boolean): List<NotificationItem> {
        return if (unreadOnly) notifications.filter { item -> !item.isRead } else notifications
    }

    override suspend fun getUnreadCount(): Int = notifications.count { item -> !item.isRead }

    override suspend fun markRead(id: Long) {
        lastMarkedReadId = id
    }

    override suspend fun markAllRead(): Int {
        markAllReadCalled = true
        notifications = emptyList()
        return 0
    }
}

/**
 * Testy NotificationsViewModel — načtení, označení jedné a všech notifikací jako přečtených.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class NotificationsViewModelTest {

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
     * viewModel.load() → isLoading == false, items.size == 2, error == null.
     */
    @Test
    public fun `load fills state with notifications`(): Unit = runTest {
        val fakeRepo = FakeNotificationRepository()
        val viewModel = NotificationsViewModel(fakeRepo)

        viewModel.load()

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(2, viewModel.state.value.items.size)
        assertNull(viewModel.state.value.error)
    }

    /**
     * viewModel.markRead(item) → fakeRepo.lastMarkedReadId == id, seznam je znovu načten.
     */
    @Test
    public fun `markRead reloads list`(): Unit = runTest {
        val fakeRepo = FakeNotificationRepository()
        val viewModel = NotificationsViewModel(fakeRepo)

        viewModel.load()
        val item = viewModel.state.value.items.first { notification -> !notification.isRead }
        viewModel.markRead(item)

        assertEquals(item.id, fakeRepo.lastMarkedReadId)
        assertEquals(2, viewModel.state.value.items.size)
    }

    /**
     * viewModel.markAllRead() → fakeRepo.markAllReadCalled, seznam notifikací je prázdný.
     */
    @Test
    public fun `markAllRead empties items`(): Unit = runTest {
        val fakeRepo = FakeNotificationRepository()
        val viewModel = NotificationsViewModel(fakeRepo)

        viewModel.load()
        viewModel.markAllRead()

        assertEquals(true, fakeRepo.markAllReadCalled)
        assertEquals(0, viewModel.state.value.items.size)
        assertEquals(false, viewModel.state.value.isLoading)
    }
}

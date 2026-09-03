package cz.b2brental

import cz.b2brental.data.remote.SessionEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Test emise události vypršení session přes SessionEvents.
 */
class SessionEventsTest {

    @Test
    public fun `unauthorized event can be emitted and collected`(): Unit = runTest {
        var received = false
        val job = launch {
            SessionEvents.unauthorized.collect {
                received = true
            }
        }
        delay(10.milliseconds)
        SessionEvents.unauthorized.tryEmit(Unit)
        delay(10.milliseconds)
        assertTrue(received)
        job.cancel()
    }
}

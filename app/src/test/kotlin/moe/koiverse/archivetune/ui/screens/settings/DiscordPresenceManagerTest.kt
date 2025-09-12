package moe.koiverse.archivetune.ui.screens.settings

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class DiscordPresenceManagerTest {
    @Test
    fun managerInvokesUpdate() = runBlocking {
        val counter = AtomicInteger(0)

        // start manager with tiny interval
        DiscordPresenceManager.start(update = {
            counter.incrementAndGet()
        }, intervalProvider = { 50L })

        // wait a bit to allow multiple updates
        kotlinx.coroutines.delay(200L)

        // stop manager
        DiscordPresenceManager.stop()

        assertTrue("Expected at least one update", counter.get() >= 1)
    }
}

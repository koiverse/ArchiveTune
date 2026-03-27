package moe.koiverse.archivetune.network

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorTest {
    @Test
    fun validatedHandoffStaysOnlineUntilLastNetworkIsLost() =
        runTest {
            lateinit var events: NetworkEvents<String>
            var closeCount = 0

            validatedNetworkStatusFlow<String> { callback ->
                events = callback
                AutoCloseable { closeCount += 1 }
            }.test {
                assertEquals(false, awaitItem())

                events.onCapabilitiesChanged(network = "wifi", isValidatedInternet = true)
                assertEquals(true, awaitItem())

                events.onCapabilitiesChanged(network = "mobile", isValidatedInternet = true)
                expectNoEvents()

                events.onLost("wifi")
                expectNoEvents()

                events.onLost("mobile")
                assertEquals(false, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, closeCount)
        }

    @Test
    fun unvalidatedNetworksDoNotCountAsOnline() =
        runTest {
            lateinit var events: NetworkEvents<String>

            validatedNetworkStatusFlow<String> { callback ->
                events = callback
                AutoCloseable { }
            }.test {
                assertEquals(false, awaitItem())

                events.onCapabilitiesChanged(network = "wifi", isValidatedInternet = false)
                expectNoEvents()

                events.onCapabilitiesChanged(network = "wifi", isValidatedInternet = true)
                assertEquals(true, awaitItem())

                events.onCapabilitiesChanged(network = "wifi", isValidatedInternet = false)
                assertEquals(false, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }
}

package moe.koiverse.archivetune.network

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveNetworkBannerStateUseCaseTest {
    @Test
    fun initialOnlineEmitsHiddenOnly() =
        runTest {
            MutableStateFlow(true)
                .asNetworkBannerUiState()
                .test {
                    assertEquals(NetworkBannerUiState.Hidden, awaitItem())
                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun shortDisconnectDoesNotShowOfflineBanner() =
        runTest {
            val isOnline = MutableStateFlow(true)

            isOnline
                .asNetworkBannerUiState()
                .test {
                    assertEquals(NetworkBannerUiState.Hidden, awaitItem())

                    isOnline.value = false
                    advanceTimeBy(500)
                    expectNoEvents()

                    isOnline.value = true
                    advanceUntilIdle()
                    expectNoEvents()

                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun longerDisconnectShowsOfflineBanner() =
        runTest {
            val isOnline = MutableStateFlow(true)

            isOnline
                .asNetworkBannerUiState()
                .test {
                    assertEquals(NetworkBannerUiState.Hidden, awaitItem())

                    isOnline.value = false
                    advanceTimeBy(750)

                    assertEquals(NetworkBannerUiState.Offline, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun reconnectAfterOfflineShowsBackOnlineThenHides() =
        runTest {
            val isOnline = MutableStateFlow(true)

            isOnline
                .asNetworkBannerUiState()
                .test {
                    assertEquals(NetworkBannerUiState.Hidden, awaitItem())

                    isOnline.value = false
                    advanceTimeBy(750)
                    assertEquals(NetworkBannerUiState.Offline, awaitItem())

                    isOnline.value = true
                    assertEquals(NetworkBannerUiState.BackOnline, awaitItem())

                    advanceTimeBy(2500)
                    assertEquals(NetworkBannerUiState.Hidden, awaitItem())

                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun duplicateConnectivityValuesDoNotEmitDuplicateUiStates() =
        runTest {
            val isOnline = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)

            isOnline
                .asNetworkBannerUiState()
                .test {
                    isOnline.tryEmit(true)
                    assertEquals(NetworkBannerUiState.Hidden, awaitItem())

                    isOnline.tryEmit(true)
                    expectNoEvents()

                    isOnline.tryEmit(false)
                    advanceTimeBy(750)
                    assertEquals(NetworkBannerUiState.Offline, awaitItem())

                    isOnline.tryEmit(false)
                    advanceUntilIdle()
                    expectNoEvents()

                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun rapidDisconnectReconnectDisconnectCancelsStaleTimers() =
        runTest {
            val isOnline = MutableStateFlow(true)

            isOnline
                .asNetworkBannerUiState()
                .test {
                    assertEquals(NetworkBannerUiState.Hidden, awaitItem())

                    isOnline.value = false
                    advanceTimeBy(500)
                    expectNoEvents()

                    isOnline.value = true
                    advanceUntilIdle()
                    expectNoEvents()

                    isOnline.value = false
                    advanceTimeBy(750)
                    assertEquals(NetworkBannerUiState.Offline, awaitItem())

                    cancelAndIgnoreRemainingEvents()
                }
        }
}

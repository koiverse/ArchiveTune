/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */


package moe.koiverse.archivetune.network

import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

private const val OfflineBannerDebounceMillis = 750L
private const val OfflineBannerDurationMillis = 3000L
private const val BackOnlineBannerDurationMillis = 2500L

internal fun Flow<Boolean>.asNetworkBannerUiState(
    offlineDebounceMillis: Long = OfflineBannerDebounceMillis,
    offlineBannerDurationMillis: Long = OfflineBannerDurationMillis,
    backOnlineBannerDurationMillis: Long = BackOnlineBannerDurationMillis,
): Flow<NetworkBannerUiState> {
    var hasReceivedInitialValue = false
    var hasShownOfflineBanner = false

    return distinctUntilChanged().transformLatest { isOnline ->
        if (!hasReceivedInitialValue) {
            hasReceivedInitialValue = true
            if (isOnline) {
                emit(NetworkBannerUiState.Hidden)
            } else {
                delay(offlineDebounceMillis)
                hasShownOfflineBanner = true
                emit(NetworkBannerUiState.Offline)
                delay(offlineBannerDurationMillis)
                emit(NetworkBannerUiState.Hidden)
            }
            return@transformLatest
        }

        if (!isOnline) {
            delay(offlineDebounceMillis)
            hasShownOfflineBanner = true
            emit(NetworkBannerUiState.Offline)
            delay(offlineBannerDurationMillis)
            emit(NetworkBannerUiState.Hidden)
            return@transformLatest
        }

        if (hasShownOfflineBanner) {
            hasShownOfflineBanner = false
            emit(NetworkBannerUiState.BackOnline)
            delay(backOnlineBannerDurationMillis)
            emit(NetworkBannerUiState.Hidden)
        }
    }
}

class ObserveNetworkBannerStateUseCase
@Inject
constructor(
    private val networkMonitor: NetworkMonitor,
) {
    operator fun invoke(): Flow<NetworkBannerUiState> = networkMonitor.isOnline.asNetworkBannerUiState()
}

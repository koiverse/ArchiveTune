/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.network

import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

private const val OfflineBannerDebounceMillis = 750L
private const val BackOnlineBannerDurationMillis = 2500L

internal fun Flow<Boolean>.asNetworkBannerUiState(
    offlineDebounceMillis: Long = OfflineBannerDebounceMillis,
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
            }
            return@transformLatest
        }

        if (!isOnline) {
            delay(offlineDebounceMillis)
            hasShownOfflineBanner = true
            emit(NetworkBannerUiState.Offline)
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

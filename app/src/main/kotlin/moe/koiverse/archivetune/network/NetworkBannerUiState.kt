package moe.koiverse.archivetune.network

sealed interface NetworkBannerUiState {
    data object Hidden : NetworkBannerUiState

    data object Offline : NetworkBannerUiState

    data object BackOnline : NetworkBannerUiState
}

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.network

sealed interface NetworkBannerUiState {
    data object Hidden : NetworkBannerUiState

    data object Offline : NetworkBannerUiState

    data object BackOnline : NetworkBannerUiState
}

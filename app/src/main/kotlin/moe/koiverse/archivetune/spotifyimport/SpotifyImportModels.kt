/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.spotifyimport

import androidx.compose.runtime.Immutable

@Immutable
enum class SpotifyImportSourceType {
    PLAYLIST,
    LIKED_SONGS,
}

@Immutable
data class SpotifyImportSourceUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val trackCount: Int?,
    val type: SpotifyImportSourceType,
)

@Immutable
data class SpotifyImportProgressUi(
    val sourceTitle: String,
    val completedSources: Int,
    val totalSources: Int,
    val matchedTracks: Int,
    val totalTracks: Int,
    val percent: Int,
)

@Immutable
data class SpotifyImportSourceSummaryUi(
    val title: String,
    val totalTracks: Int,
    val importedTracks: Int,
    val failedTracks: Int,
)

@Immutable
data class SpotifyImportSummaryUi(
    val sources: List<SpotifyImportSourceSummaryUi>,
) {
    val sourceCount: Int
        get() = sources.size

    val totalTracks: Int
        get() = sources.sumOf { it.totalTracks }

    val importedTracks: Int
        get() = sources.sumOf { it.importedTracks }

    val failedTracks: Int
        get() = sources.sumOf { it.failedTracks }
}

@Immutable
data class SpotifyImportUiState(
    val isAuthenticated: Boolean = false,
    val accountName: String = "",
    val accountAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val sources: List<SpotifyImportSourceUi> = emptyList(),
    val selectedSourceIds: Set<String> = emptySet(),
    val progress: SpotifyImportProgressUi? = null,
    val summary: SpotifyImportSummaryUi? = null,
    val errorMessage: String? = null,
) {
    val hasSources: Boolean
        get() = sources.isNotEmpty()

    val canImport: Boolean
        get() = selectedSourceIds.isNotEmpty() && progress == null && !isLoading
}

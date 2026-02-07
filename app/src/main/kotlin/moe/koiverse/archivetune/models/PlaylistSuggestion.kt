package moe.koiverse.archivetune.models

import moe.koiverse.archivetune.innertube.models.SongItem

/**
 * Represents a song suggestion for a playlist based on the playlist name.
 *
 * @property query The search query used to generate these suggestions
 * @property songs The list of suggested songs from YouTube search
 */
data class PlaylistSuggestion(
    val query: String,
    val songs: List<SongItem>,
)

/**
 * Sealed class representing the loading state of playlist suggestions.
 */
sealed class PlaylistSuggestionsState {
    data object Idle : PlaylistSuggestionsState()
    data object Loading : PlaylistSuggestionsState()
    data class Success(val suggestion: PlaylistSuggestion) : PlaylistSuggestionsState()
    data class Error(val message: String) : PlaylistSuggestionsState()
}

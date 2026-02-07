package moe.koiverse.archivetune.models

import moe.koiverse.archivetune.innertube.models.SongItem

/**
 * Represents a song suggestion source type.
 */
enum class SuggestionSource {
    SEARCH,           // From YouTube search
    SEARCH_PAGINATION, // From search continuation
    RELATED_SONGS,    // From YouTube.next() related songs
}

/**
 * Represents a song suggestion for a playlist based on the playlist name.
 *
 * @property query The search query used to generate these suggestions
 * @property songs The list of suggested songs from YouTube search
 * @property source The source of these suggestions
 */
data class PlaylistSuggestion(
    val query: String,
    val songs: List<SongItem>,
    val source: SuggestionSource = SuggestionSource.SEARCH,
)

/**
 * Manages pagination state for search results.
 *
 * @property continuationToken The token for fetching the next page
 * @property allFetchedSongs All songs fetched so far across all pages
 * @property shownSongIds Set of song IDs that have been displayed to the user
 * @property hasMorePages Whether more pages are available
 * @property currentQueryIndex Current index in the query rotation
 * @property queries List of diversified queries being used
 */
data class SearchPaginationState(
    val continuationToken: String? = null,
    val allFetchedSongs: List<SongItem> = emptyList(),
    val shownSongIds: Set<String> = emptySet(),
    val hasMorePages: Boolean = true,
    val currentQueryIndex: Int = 0,
    val queries: List<String> = emptyList(),
    val lastRequestTime: Long = 0,
    val relatedSongQueue: List<SongItem> = emptyList(),
)

/**
 * Sealed class representing the loading state of playlist suggestions.
 */
sealed class PlaylistSuggestionsState {
    data object Idle : PlaylistSuggestionsState()
    data object Loading : PlaylistSuggestionsState()
    data object LoadingMore : PlaylistSuggestionsState() // Loading additional pages
    data class Success(
        val suggestion: PlaylistSuggestion,
        val paginationState: SearchPaginationState,
        val canLoadMore: Boolean = false,
    ) : PlaylistSuggestionsState()
    data class Error(val message: String, val isPaginationError: Boolean = false) : PlaylistSuggestionsState()
}

/**
 * Configuration for the suggestion engine.
 */
data class SuggestionConfig(
    val batchSize: Int = 10,
    val maxStoredSongs: Int = 500,
    val throttleDelayMs: Long = 1000,
    val minDelayBetweenRequestsMs: Long = 500,
    val enableQueryDiversification: Boolean = true,
    val enableRelatedSongs: Boolean = true,
    val maxQueriesPerPlaylist: Int = 5,
)

/**
 * Genre keywords for query diversification.
 */
val GENRE_KEYWORDS = listOf(
    "pop", "rock", "hip hop", "rap", "electronic", "edm", "dance",
    "rnb", "soul", "jazz", "classical", "country", "folk", "indie",
    "alternative", "metal", "punk", "blues", "reggae", "latin",
    "kpop", "jpop", "afrobeats", "house", "techno", "trance",
    "lofi", "ambient", "soundtrack", "acoustic", "cover"
)

/**
 * Mood keywords for query diversification.
 */
val MOOD_KEYWORDS = listOf(
    "chill", "upbeat", "happy", "sad", "energetic", "relaxing",
    "romantic", "party", "workout", "focus", "sleep", "morning",
    "night", "summer", "winter", "rainy day", "driving"
)

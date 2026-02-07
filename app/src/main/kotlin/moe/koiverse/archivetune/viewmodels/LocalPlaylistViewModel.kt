package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.koiverse.archivetune.constants.PlaylistSongSortDescendingKey
import moe.koiverse.archivetune.constants.PlaylistSongSortType
import moe.koiverse.archivetune.constants.PlaylistSongSortTypeKey
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.PlaylistSong
import moe.koiverse.archivetune.extensions.reversed
import moe.koiverse.archivetune.extensions.toEnum
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.models.GENRE_KEYWORDS
import moe.koiverse.archivetune.models.MOOD_KEYWORDS
import moe.koiverse.archivetune.models.PlaylistSuggestion
import moe.koiverse.archivetune.models.PlaylistSuggestionsState
import moe.koiverse.archivetune.models.SearchPaginationState
import moe.koiverse.archivetune.models.SuggestionConfig
import moe.koiverse.archivetune.models.SuggestionSource
import moe.koiverse.archivetune.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val playlistId = savedStateHandle.get<String>("playlistId")!!
    val playlist =
        database
            .playlist(playlistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val playlistSongs: StateFlow<List<PlaylistSong>> =
        combine(
            database.playlistSongs(playlistId),
            context.dataStore.data
                .map {
                    it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM) to (it[PlaylistSongSortDescendingKey]
                        ?: true)
                }.distinctUntilChanged(),
        ) { songs, (sortType, sortDescending) ->
            when (sortType) {
                PlaylistSongSortType.CUSTOM -> songs
                PlaylistSongSortType.CREATE_DATE -> songs.sortedBy { it.map.id }
                PlaylistSongSortType.NAME -> songs.sortedBy { it.song.song.title }
                PlaylistSongSortType.ARTIST -> {
                    val collator = Collator.getInstance(Locale.getDefault())
                    collator.strength = Collator.PRIMARY
                    songs
                        .sortedWith(compareBy(collator) { song -> song.song.artists.joinToString("") { artist -> artist.name } })
                        .groupBy { it.song.album?.title }
                        .flatMap { (_, songsByAlbum) ->
                            songsByAlbum.sortedBy { playlistSong ->
                                playlistSong.song.artists.joinToString("") { artist -> artist.name }
                            }
                        }
                }

                PlaylistSongSortType.PLAY_TIME -> songs.sortedBy { it.song.song.totalPlayTime }
            }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Playlist suggestions state
    private val _suggestionsState = MutableStateFlow<PlaylistSuggestionsState>(PlaylistSuggestionsState.Idle)
    val suggestionsState: StateFlow<PlaylistSuggestionsState> = _suggestionsState.asStateFlow()

    // Configuration
    private val config = SuggestionConfig()

    // Throttling and synchronization
    private val requestMutex = Mutex()
    private var lastRequestTime: Long = 0

    init {
        viewModelScope.launch {
            val sortedSongs =
                playlistSongs.first().sortedWith(compareBy({ it.map.position }, { it.map.id }))
            database.transaction {
                sortedSongs.forEachIndexed { index, playlistSong ->
                    if (playlistSong.map.position != index) {
                        update(playlistSong.map.copy(position = index))
                    }
                }
            }
        }
    }

    /**
     * Generates diversified search queries based on playlist name and content.
     */
    private fun generateSearchQueries(playlistName: String, songs: List<PlaylistSong>): List<String> {
        val queries = mutableListOf<String>()

        // Base query: playlist name
        queries.add(playlistName)

        // Extract artist names from existing songs for query diversification
        val artistNames = songs
            .flatMap { it.song.artists }
            .map { it.name }
            .distinct()
            .take(3)

        // Add queries with artist names
        artistNames.forEach { artist ->
            queries.add("$artist ${playlistName.take(20)}")
        }

        // Add genre-based queries if playlist name suggests a genre
        val lowerName = playlistName.lowercase()
        val matchingGenres = GENRE_KEYWORDS.filter { it in lowerName }
        if (matchingGenres.isNotEmpty()) {
            // Add related genre queries
            val genreIndex = GENRE_KEYWORDS.indexOf(matchingGenres.first())
            val relatedGenres = listOfNotNull(
                GENRE_KEYWORDS.getOrNull((genreIndex + 1) % GENRE_KEYWORDS.size),
                GENRE_KEYWORDS.getOrNull((genreIndex + 2) % GENRE_KEYWORDS.size)
            )
            relatedGenres.forEach { genre ->
                queries.add("$genre music")
            }
        }

        // Add mood-based queries
        val matchingMoods = MOOD_KEYWORDS.filter { it in lowerName }
        if (matchingMoods.isNotEmpty()) {
            MOOD_KEYWORDS.shuffled().take(2).forEach { mood ->
                queries.add("$mood music")
            }
        }

        // Add combined queries
        if (artistNames.isNotEmpty() && matchingGenres.isNotEmpty()) {
            queries.add("${artistNames.first()} ${matchingGenres.first()}")
        }

        return queries.distinct().take(config.maxQueriesPerPlaylist)
    }

    /**
     * Throttles API requests to avoid rate limiting.
     */
    private suspend fun throttleRequest() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < config.minDelayBetweenRequestsMs) {
            delay(config.minDelayBetweenRequestsMs - timeSinceLastRequest)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    /**
     * Loads song suggestions based on the playlist name using YouTube search.
     *
     * @param playlistName The name of the playlist to use as search query
     * @param reset Whether to reset the suggestions (true for new query, false for refresh)
     */
    fun loadPlaylistSuggestions(playlistName: String, reset: Boolean = true) {
        viewModelScope.launch {
            if (playlistName.isBlank()) {
                _suggestionsState.value = PlaylistSuggestionsState.Idle
                return@launch
            }

            requestMutex.withLock {
                val currentState = _suggestionsState.value

                // If loading, don't start another load
                if (currentState is PlaylistSuggestionsState.Loading) return@launch

                // Check if we need to reset or continue pagination
                if (reset || currentState !is PlaylistSuggestionsState.Success) {
                    _suggestionsState.value = PlaylistSuggestionsState.Loading

                    try {
                        throttleRequest()

                        // Generate diversified queries
                        val currentSongs = playlistSongs.first()
                        val queries = generateSearchQueries(playlistName, currentSongs)

                        // Start with the first query
                        val firstQuery = queries.firstOrNull() ?: playlistName

                        val result = YouTube.search(
                            query = firstQuery,
                            filter = YouTube.SearchFilter.FILTER_SONG
                        )

                        result.onSuccess { searchResult ->
                            val songs = searchResult.items
                                .filterIsInstance<SongItem>()
                                .distinctBy { it.id }

                            if (songs.isEmpty()) {
                                _suggestionsState.value = PlaylistSuggestionsState.Error("No suggestions found")
                            } else {
                                // Take first batch for display
                                val firstBatch = songs.take(config.batchSize)
                                val paginationState = SearchPaginationState(
                                    continuationToken = searchResult.continuation,
                                    allFetchedSongs = songs,
                                    shownSongIds = firstBatch.map { it.id }.toSet(),
                                    hasMorePages = searchResult.continuation != null || queries.size > 1,
                                    currentQueryIndex = 0,
                                    queries = queries,
                                    lastRequestTime = System.currentTimeMillis()
                                )

                                _suggestionsState.value = PlaylistSuggestionsState.Success(
                                    suggestion = PlaylistSuggestion(
                                        query = firstQuery,
                                        songs = firstBatch,
                                        source = SuggestionSource.SEARCH
                                    ),
                                    paginationState = paginationState,
                                    canLoadMore = paginationState.hasMorePages ||
                                            songs.size > config.batchSize
                                )
                            }
                        }.onFailure { error ->
                            _suggestionsState.value = PlaylistSuggestionsState.Error(
                                error.message ?: "Failed to load suggestions"
                            )
                        }
                    } catch (e: Exception) {
                        _suggestionsState.value = PlaylistSuggestionsState.Error(
                            e.message ?: "Failed to load suggestions"
                        )
                    }
                } else {
                    // Load more suggestions using pagination
                    loadMoreSuggestions()
                }
            }
        }
    }

    /**
     * Loads more suggestions using continuation tokens or query rotation.
     */
    fun loadMoreSuggestions() {
        viewModelScope.launch {
            val currentState = _suggestionsState.value
            if (currentState !is PlaylistSuggestionsState.Success) return@launch

            requestMutex.withLock {
                val paginationState = currentState.paginationState

                _suggestionsState.value = PlaylistSuggestionsState.LoadingMore

                try {
                    // First, try to use continuation token for current query
                    if (paginationState.continuationToken != null) {
                        throttleRequest()

                        val result = YouTube.searchContinuation(paginationState.continuationToken)

                        result.onSuccess { searchResult ->
                            val newSongs = searchResult.items
                                .filterIsInstance<SongItem>()
                                .distinctBy { it.id }
                                .filter { it.id !in paginationState.shownSongIds }

                            if (newSongs.isNotEmpty()) {
                                val nextBatch = newSongs.take(config.batchSize)
                                val updatedSongs = (paginationState.allFetchedSongs + newSongs)
                                    .distinctBy { it.id }
                                    .take(config.maxStoredSongs)

                                val newPaginationState = paginationState.copy(
                                    continuationToken = searchResult.continuation,
                                    allFetchedSongs = updatedSongs,
                                    shownSongIds = paginationState.shownSongIds + nextBatch.map { it.id },
                                    hasMorePages = searchResult.continuation != null ||
                                            paginationState.currentQueryIndex < paginationState.queries.size - 1,
                                    lastRequestTime = System.currentTimeMillis()
                                )

                                _suggestionsState.value = PlaylistSuggestionsState.Success(
                                    suggestion = PlaylistSuggestion(
                                        query = paginationState.queries.getOrElse(paginationState.currentQueryIndex) { "" },
                                        songs = nextBatch,
                                        source = SuggestionSource.SEARCH_PAGINATION
                                    ),
                                    paginationState = newPaginationState,
                                    canLoadMore = newPaginationState.hasMorePages ||
                                            updatedSongs.any { it.id !in newPaginationState.shownSongIds }
                                )
                                return@withLock
                            }
                        }
                    }

                    // If no continuation or no new songs, try next query
                    if (paginationState.currentQueryIndex < paginationState.queries.size - 1) {
                        val nextQueryIndex = paginationState.currentQueryIndex + 1
                        val nextQuery = paginationState.queries[nextQueryIndex]

                        throttleRequest()

                        val result = YouTube.search(
                            query = nextQuery,
                            filter = YouTube.SearchFilter.FILTER_SONG
                        )

                        result.onSuccess { searchResult ->
                            val newSongs = searchResult.items
                                .filterIsInstance<SongItem>()
                                .distinctBy { it.id }
                                .filter { it.id !in paginationState.shownSongIds }

                            if (newSongs.isEmpty()) {
                                // Try to show unseen songs from existing pool
                                showUnseenSongs(paginationState)
                            } else {
                                val nextBatch = newSongs.take(config.batchSize)
                                val updatedSongs = (paginationState.allFetchedSongs + newSongs)
                                    .distinctBy { it.id }
                                    .take(config.maxStoredSongs)

                                val newPaginationState = paginationState.copy(
                                    continuationToken = searchResult.continuation,
                                    allFetchedSongs = updatedSongs,
                                    shownSongIds = paginationState.shownSongIds + nextBatch.map { it.id },
                                    hasMorePages = searchResult.continuation != null ||
                                            nextQueryIndex < paginationState.queries.size - 1,
                                    currentQueryIndex = nextQueryIndex,
                                    lastRequestTime = System.currentTimeMillis()
                                )

                                _suggestionsState.value = PlaylistSuggestionsState.Success(
                                    suggestion = PlaylistSuggestion(
                                        query = nextQuery,
                                        songs = nextBatch,
                                        source = SuggestionSource.SEARCH
                                    ),
                                    paginationState = newPaginationState,
                                    canLoadMore = newPaginationState.hasMorePages ||
                                            updatedSongs.any { it.id !in newPaginationState.shownSongIds }
                                )
                            }
                        }.onFailure {
                            // On failure, try to show unseen songs from existing pool
                            showUnseenSongs(paginationState)
                        }
                    } else {
                        // No more queries, try to show unseen songs
                        showUnseenSongs(paginationState)
                    }
                } catch (e: Exception) {
                    _suggestionsState.value = PlaylistSuggestionsState.Error(
                        e.message ?: "Failed to load more suggestions",
                        isPaginationError = true
                    )
                }
            }
        }
    }

    /**
     * Shows unseen songs from the existing pool without making API calls.
     */
    private fun showUnseenSongs(paginationState: SearchPaginationState) {
        val unseenSongs = paginationState.allFetchedSongs
            .filter { it.id !in paginationState.shownSongIds }

        if (unseenSongs.isEmpty()) {
            // Reset shown songs and start over
            val firstBatch = paginationState.allFetchedSongs.take(config.batchSize)
            val newPaginationState = paginationState.copy(
                shownSongIds = firstBatch.map { it.id }.toSet()
            )

            _suggestionsState.value = PlaylistSuggestionsState.Success(
                suggestion = PlaylistSuggestion(
                    query = paginationState.queries.getOrElse(paginationState.currentQueryIndex) { "" },
                    songs = firstBatch,
                    source = SuggestionSource.SEARCH
                ),
                paginationState = newPaginationState,
                canLoadMore = paginationState.allFetchedSongs.size > config.batchSize
            )
        } else {
            val nextBatch = unseenSongs.take(config.batchSize)
            val newPaginationState = paginationState.copy(
                shownSongIds = paginationState.shownSongIds + nextBatch.map { it.id }
            )

            _suggestionsState.value = PlaylistSuggestionsState.Success(
                suggestion = PlaylistSuggestion(
                    query = paginationState.queries.getOrElse(paginationState.currentQueryIndex) { "" },
                    songs = nextBatch,
                    source = SuggestionSource.SEARCH
                ),
                paginationState = newPaginationState,
                canLoadMore = newPaginationState.allFetchedSongs.any { it.id !in newPaginationState.shownSongIds }
            )
        }
    }

    /**
     * Fetches related songs for a given video ID using YouTube.next().
     */
    fun fetchRelatedSongs(videoId: String) {
        if (!config.enableRelatedSongs) return

        viewModelScope.launch {
            val currentState = _suggestionsState.value
            if (currentState !is PlaylistSuggestionsState.Success) return@launch

            requestMutex.withLock {
                try {
                    throttleRequest()

                    val endpoint = WatchEndpoint(videoId = videoId)
                    val result = YouTube.next(endpoint)

                    result.onSuccess { nextResult ->
                        val relatedSongs = nextResult.items
                            .distinctBy { it.id }
                            .filter { it.id !in currentState.paginationState.shownSongIds }
                            .take(config.batchSize)

                        if (relatedSongs.isNotEmpty()) {
                            val newPaginationState = currentState.paginationState.copy(
                                relatedSongQueue = relatedSongs,
                                lastRequestTime = System.currentTimeMillis()
                            )

                            _suggestionsState.value = currentState.copy(
                                suggestion = PlaylistSuggestion(
                                    query = "Related to added song",
                                    songs = relatedSongs,
                                    source = SuggestionSource.RELATED_SONGS
                                ),
                                paginationState = newPaginationState
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Silently fail for related songs - it's a bonus feature
                }
            }
        }
    }

    /**
     * Refreshes the playlist suggestions.
     * If there are more pages, loads them. Otherwise cycles through existing songs.
     */
    fun refreshSuggestions() {
        viewModelScope.launch {
            val currentState = _suggestionsState.value

            if (currentState is PlaylistSuggestionsState.Success) {
                val paginationState = currentState.paginationState

                // Check if we can load more from API
                if (currentState.canLoadMore &&
                    (paginationState.hasMorePages ||
                            paginationState.continuationToken != null ||
                            paginationState.currentQueryIndex < paginationState.queries.size - 1)) {
                    loadMoreSuggestions()
                } else {
                    // Cycle through existing songs
                    showUnseenSongs(paginationState)
                }
            } else {
                // Reset and start fresh
                val playlistName = playlist.value?.playlist?.name ?: return@launch
                loadPlaylistSuggestions(playlistName, reset = true)
            }
        }
    }

    /**
     * Clears the suggestions state.
     */
    fun clearSuggestions() {
        _suggestionsState.value = PlaylistSuggestionsState.Idle
    }
}

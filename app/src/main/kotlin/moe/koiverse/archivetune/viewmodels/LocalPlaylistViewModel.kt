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
import moe.koiverse.archivetune.models.PlaylistSuggestion
import moe.koiverse.archivetune.models.PlaylistSuggestionsState
import moe.koiverse.archivetune.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

    // Track all songs fetched from search for pagination on refresh
    private var allFetchedSongs: List<moe.koiverse.archivetune.innertube.models.SongItem> = emptyList()
    private var currentQuery: String = ""

    /**
     * Loads song suggestions based on the playlist name using YouTube search.
     *
     * @param playlistName The name of the playlist to use as search query
     * @param resetShownSongs Whether to reset the shown songs tracking (true for new query, false for refresh)
     */
    fun loadPlaylistSuggestions(playlistName: String, resetShownSongs: Boolean = true) {
        viewModelScope.launch {
            if (playlistName.isBlank()) {
                _suggestionsState.value = PlaylistSuggestionsState.Idle
                return@launch
            }

            // If this is a new query or we don't have cached songs, do a fresh search
            if (resetShownSongs || playlistName != currentQuery || allFetchedSongs.isEmpty()) {
                _suggestionsState.value = PlaylistSuggestionsState.Loading
                currentQuery = playlistName

                try {
                    val result = YouTube.search(
                        query = playlistName,
                        filter = YouTube.SearchFilter.FILTER_SONG
                    )

                    result.onSuccess { searchResult ->
                        val songs = searchResult.items
                            .filterIsInstance<moe.koiverse.archivetune.innertube.models.SongItem>()

                        if (songs.isEmpty()) {
                            allFetchedSongs = emptyList()
                            _suggestionsState.value = PlaylistSuggestionsState.Error("No suggestions found")
                        } else {
                            allFetchedSongs = songs
                            // Show first batch of songs (up to 10)
                            val firstBatch = songs.take(10)
                            _suggestionsState.value = PlaylistSuggestionsState.Success(
                                suggestion = PlaylistSuggestion(
                                    query = playlistName,
                                    songs = firstBatch
                                ),
                                shownSongIds = firstBatch.map { it.id }.toSet()
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
                // This is a refresh with the same query - show next batch
                showNextBatch()
            }
        }
    }

    /**
     * Shows the next batch of songs that haven't been shown yet.
     */
    private fun showNextBatch() {
        val currentState = _suggestionsState.value
        if (currentState !is PlaylistSuggestionsState.Success) return

        val shownIds = currentState.shownSongIds
        val remainingSongs = allFetchedSongs.filter { it.id !in shownIds }

        if (remainingSongs.isEmpty()) {
            // All songs shown, start over from the beginning
            val firstBatch = allFetchedSongs.take(10)
            _suggestionsState.value = PlaylistSuggestionsState.Success(
                suggestion = PlaylistSuggestion(
                    query = currentQuery,
                    songs = firstBatch
                ),
                shownSongIds = firstBatch.map { it.id }.toSet()
            )
        } else {
            // Show next batch of remaining songs (up to 10)
            val nextBatch = remainingSongs.take(10)
            _suggestionsState.value = PlaylistSuggestionsState.Success(
                suggestion = PlaylistSuggestion(
                    query = currentQuery,
                    songs = nextBatch
                ),
                shownSongIds = shownIds + nextBatch.map { it.id }.toSet()
            )
        }
    }

    /**
     * Refreshes the playlist suggestions to show the next batch of songs.
     */
    fun refreshSuggestions() {
        viewModelScope.launch {
            if (currentQuery.isNotBlank() && allFetchedSongs.isNotEmpty()) {
                showNextBatch()
            } else {
                val playlistName = playlist.value?.playlist?.name ?: return@launch
                loadPlaylistSuggestions(playlistName, resetShownSongs = true)
            }
        }
    }
}

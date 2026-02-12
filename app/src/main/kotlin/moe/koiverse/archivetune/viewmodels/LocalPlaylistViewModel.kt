/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import moe.koiverse.archivetune.constants.PlaylistSongSortDescendingKey
import moe.koiverse.archivetune.constants.PlaylistSongSortType
import moe.koiverse.archivetune.constants.PlaylistSongSortTypeKey
import moe.koiverse.archivetune.constants.HideExplicitKey
import moe.koiverse.archivetune.constants.HideVideoKey
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.PlaylistSong
import moe.koiverse.archivetune.extensions.reversed
import moe.koiverse.archivetune.extensions.toEnum
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.YTItem
import moe.koiverse.archivetune.innertube.models.filterExplicit
import moe.koiverse.archivetune.innertube.models.filterVideo
import moe.koiverse.archivetune.innertube.pages.SearchResult
import moe.koiverse.archivetune.models.PlaylistSuggestion
import moe.koiverse.archivetune.models.PlaylistSuggestionPage
import moe.koiverse.archivetune.models.PlaylistSuggestionQuery
import moe.koiverse.archivetune.utils.PlaylistSuggestionQueryBuilder
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import moe.koiverse.archivetune.models.toMediaMetadata
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
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
    
    // Playlist Suggestions State
    private val _playlistSuggestions = MutableStateFlow<PlaylistSuggestion?>(null)
    val playlistSuggestions = combine(_playlistSuggestions, playlistSongs) { suggestions, songs ->
        val songIds = songs.map { it.song.id }.toSet()
        val filteredItems = suggestions?.items?.filter { it.id !in songIds } ?: emptyList()
        suggestions?.copy(
            items = filteredItems.take(10)
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions = _isLoadingSuggestions.asStateFlow()
    
    private val suggestionQueries = MutableStateFlow<List<PlaylistSuggestionQuery>>(emptyList())
    private val currentSuggestionQueryIndex = MutableStateFlow(0)
    private val suggestionsCacheTimestamp = MutableStateFlow(0L)
    private val suggestedSongIds = MutableStateFlow<Set<String>>(emptySet())
    
    // Mutex to prevent concurrent suggestion loading
    private val suggestionLoadMutex = Mutex()
    
    // Cache for current suggestion page
    private var currentSuggestionPage: PlaylistSuggestionPage? = null

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
        
        // Auto-load suggestions when playlist or songs change
        viewModelScope.launch {
            combine(playlist, playlistSongs) { playlist, songs ->
                Pair(playlist, songs)
            }.collect { (playlist, songs) ->
                playlist?.let { 
                    loadPlaylistSuggestions()
                }
            }
        }

        // Auto-refresh suggestions when they become empty
        viewModelScope.launch {
            playlistSuggestions.collect { suggestions ->
                if (suggestions != null && suggestions.items.isEmpty() && suggestions.hasMore && !_isLoadingSuggestions.value) {
                    loadMoreSuggestions()
                }
            }
        }
    }
    
    // Playlist Suggestions Functions
    
    fun loadPlaylistSuggestions(forceReset: Boolean = false) {
        viewModelScope.launch {
            suggestionLoadMutex.withLock {
                val currentSongs = playlistSongs.first()
                val currentPlaylist = playlist.first() ?: return@withLock
                
                val shouldRefresh = forceReset || PlaylistSuggestionQueryBuilder.shouldRefreshSuggestions(
                    suggestionsCacheTimestamp.value
                )
                
                if (!shouldRefresh && _playlistSuggestions.value != null) {
                    return@withLock // Use cached suggestions
                }
                
                _isLoadingSuggestions.value = true
                
                // Clear state for refresh
                _playlistSuggestions.value = null
                currentSuggestionQueryIndex.value = 0
                // Keep previously suggested IDs to avoid showing them again on refresh
                // but ensure songs already in playlist are always included in the filter
                suggestedSongIds.value = suggestedSongIds.value + currentSongs.map { it.song.id }.toSet()
                suggestionsCacheTimestamp.value = 0L
                currentSuggestionPage = null
                
                try {
                    // Build suggestion queries
                    val queries = PlaylistSuggestionQueryBuilder.buildSuggestionQueries(
                        playlistName = currentPlaylist.playlist.name,
                        playlistSongs = currentSongs
                    )
                    suggestionQueries.value = queries
                    suggestionsCacheTimestamp.value = System.currentTimeMillis()
                    
                    if (queries.isEmpty()) {
                        _playlistSuggestions.value = PlaylistSuggestion(
                            items = emptyList(),
                            continuation = null,
                            currentQueryIndex = 0,
                            totalQueries = 0,
                            query = "",
                            hasMore = false
                        )
                        return@withLock
                    }
                    
                    // Load first page of suggestions
                    loadNextSuggestionPage()
                    
                } catch (e: Exception) {
                    reportException(e)
                    if (_playlistSuggestions.value == null) {
                        _playlistSuggestions.value = PlaylistSuggestion(
                            items = emptyList(),
                            continuation = null,
                            currentQueryIndex = 0,
                            totalQueries = 0,
                            query = "",
                            hasMore = false
                        )
                    }
                } finally {
                    _isLoadingSuggestions.value = false
                }
            }
        }
    }
    
    fun loadMoreSuggestions() {
        viewModelScope.launch {
            suggestionLoadMutex.withLock {
                if (_isLoadingSuggestions.value) return@withLock
                
                val currentSuggestions = _playlistSuggestions.value ?: return@withLock
                val queries = suggestionQueries.value
                
                try {
                    // If we have a continuation, load more from current query
                    currentSuggestionPage?.continuation?.let { continuation ->
                        _isLoadingSuggestions.value = true
                        loadMoreFromContinuation(continuation)
                        return@withLock
                    }
                    
                    // Otherwise, move to next query
                    val nextIndex = currentSuggestionQueryIndex.value + 1
                    if (nextIndex < queries.size) {
                        _isLoadingSuggestions.value = true
                        currentSuggestionQueryIndex.value = nextIndex
                        loadNextSuggestionPage()
                    } else {
                        // No more queries and no continuation
                        _playlistSuggestions.value = currentSuggestions.copy(hasMore = false)
                    }
                } finally {
                    _isLoadingSuggestions.value = false
                }
            }
        }
    }
    
    fun resetAndLoadPlaylistSuggestions() {
        loadPlaylistSuggestions(forceReset = true)
    }
    
    suspend fun addSongToPlaylist(song: moe.koiverse.archivetune.innertube.models.SongItem, browseId: String?): Boolean {
        return try {
            if (browseId != null) {
                // Add to YouTube playlist
                withContext(Dispatchers.IO) {
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            
            val added = database.withTransaction {
                // Ensure playlist exists in local database (it should, but just in case)
                val p = getPlaylistById(playlistId)
                if (p == null) {
                    // If not found, we can't add to it.
                    // This might happen if it's a special playlist that hasn't been created yet.
                    if (playlistId == moe.koiverse.archivetune.db.entities.PlaylistEntity.LIKED_PLAYLIST_ID) {
                        insert(moe.koiverse.archivetune.db.entities.PlaylistEntity(id = playlistId, name = context.getString(R.string.liked_songs), isEditable = false, bookmarkedAt = java.time.LocalDateTime.now()))
                    } else {
                        return@withTransaction false
                    }
                }

                // First, ensure the song and its artists are in the database
                insert(song.toMediaMetadata())
                
                // Add to local playlist
                val maxPosition = maxPlaylistSongPosition(playlistId)
                val position = (maxPosition ?: -1) + 1
                insert(
                    moe.koiverse.archivetune.db.entities.PlaylistSongMap(
                        songId = song.id,
                        playlistId = playlistId,
                        position = position,
                        setVideoId = null
                    )
                )
                true
            }
            
            if (!added) {
                return false
            }
            
            // Update suggested song IDs to avoid duplicates
            suggestedSongIds.value = suggestedSongIds.value + song.id
            
            true
        } catch (e: Exception) {
            reportException(e)
            false
        }
    }
    
    private suspend fun loadNextSuggestionPage() {
        val queries = suggestionQueries.value
        val currentIndex = currentSuggestionQueryIndex.value
        
        if (currentIndex >= queries.size) return
        
        val currentQuery = queries[currentIndex]
        
        try {
            val searchFilter = YouTube.SearchFilter.FILTER_SONG
            
            val result = YouTube.search(currentQuery.query, searchFilter).getOrNull()
                ?: return
            
            val filteredItems = filterSuggestionItems(result.items).shuffled().take(10)
            
            // If we got no new items after filtering, try to load more if available
            if (filteredItems.isEmpty() && (result.continuation != null || currentIndex < queries.size - 1)) {
                result.continuation?.let { continuationValue ->
                    loadMoreFromContinuation(continuationValue)
                } ?: run {
                    currentSuggestionQueryIndex.value = currentIndex + 1
                    loadNextSuggestionPage()
                }
                return
            }
            
            currentSuggestionPage = PlaylistSuggestionPage(
                items = filteredItems,
                continuation = result.continuation
            )

            // Update suggested song IDs to avoid duplicates
            val newIds = filteredItems.map { item: YTItem -> item.id }.toSet()
            suggestedSongIds.value = suggestedSongIds.value + newIds

            // Update suggestions state
            val currentSuggestions = _playlistSuggestions.value
            val newSuggestions = if (currentSuggestions == null) {
                filteredItems
            } else {
                currentSuggestions.items + filteredItems
            }
            
            _playlistSuggestions.value = PlaylistSuggestion(
                items = newSuggestions,
                continuation = result.continuation,
                currentQueryIndex = currentIndex,
                totalQueries = queries.size,
                query = currentQuery.query,
                hasMore = result.continuation != null || currentIndex < queries.size - 1
            )
            
        } catch (e: Exception) {
            reportException(e)
        }
    }
    
    private suspend fun loadMoreFromContinuation(continuation: String) {
        try {
            val result = YouTube.searchContinuation(continuation).getOrNull()
                ?: return
            
            val filteredItems = filterSuggestionItems(result.items).shuffled().take(10)
            
            // If we got no new items after filtering, try to move to next query if available
            if (filteredItems.isEmpty()) {
                val currentSuggestions = _playlistSuggestions.value
                val queries = suggestionQueries.value
                val currentIndex = currentSuggestionQueryIndex.value
                
                result.continuation?.let { continuationValue ->
                    loadMoreFromContinuation(continuationValue)
                } ?: run {
                    if (currentIndex < queries.size - 1) {
                        currentSuggestionQueryIndex.value = currentIndex + 1
                        loadNextSuggestionPage()
                    } else {
                        // No more items and no more queries
                        if (currentSuggestions != null) {
                            _playlistSuggestions.value = currentSuggestions.copy(hasMore = false)
                        }
                    }
                }
                return
            }
            
            currentSuggestionPage = PlaylistSuggestionPage(
                items = filteredItems,
                continuation = result.continuation
            )
            
            // Update suggested song IDs to avoid duplicates
            val moreIds = filteredItems.map { item: YTItem -> item.id }.toSet()
            suggestedSongIds.value = suggestedSongIds.value + moreIds
            
            // Update suggestions state
            val currentSuggestions = _playlistSuggestions.value
            if (currentSuggestions == null) {
                _playlistSuggestions.value = PlaylistSuggestion(
                    items = filteredItems,
                    continuation = result.continuation,
                    currentQueryIndex = currentSuggestionQueryIndex.value,
                    totalQueries = suggestionQueries.value.size,
                    query = suggestionQueries.value.getOrNull(currentSuggestionQueryIndex.value)?.query ?: "",
                    hasMore = result.continuation != null || currentSuggestionQueryIndex.value < suggestionQueries.value.size - 1
                )
            } else {
                _playlistSuggestions.value = currentSuggestions.copy(
                    items = currentSuggestions.items + filteredItems,
                    continuation = result.continuation,
                    hasMore = result.continuation != null || currentSuggestions.currentQueryIndex < suggestionQueries.value.size - 1
                )
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private suspend fun filterSuggestionItems(items: List<YTItem>): List<YTItem> {
        val hideExplicit = context.dataStore.data.first()[HideExplicitKey] ?: false
        val hideVideos = context.dataStore.data.first()[HideVideoKey] ?: false

        var filteredItems = items.filter { item ->
            item.id !in suggestedSongIds.value
        }

        if (hideExplicit) {
            filteredItems = filteredItems.filterExplicit()
        }

        if (hideVideos) {
            filteredItems = filteredItems.filterVideo()
        }
        return filteredItems
    }
}

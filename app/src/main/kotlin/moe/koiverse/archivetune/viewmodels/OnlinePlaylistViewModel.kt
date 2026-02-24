/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.utils.completed
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class OnlinePlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    database: MusicDatabase
) : ViewModel() {
    private val playlistId = savedStateHandle.get<String>("playlistId")!!

    val playlist = MutableStateFlow<PlaylistItem?>(null)
    val playlistSongs = MutableStateFlow<List<SongItem>>(emptyList())
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()
    
    val dbPlaylist = database.playlistByBrowseId(playlistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _viewCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val viewCounts = _viewCounts.asStateFlow()

    private val viewCountsMutex = Mutex()
    private val viewCountsInFlight = mutableSetOf<String>()
    private val viewCountsSemaphore = Semaphore(permits = 4)

    var continuation: String? = null
        private set

    init {
        load(initial = true)
    }

    fun refresh() {
        load(initial = false)
    }

    fun loadMoreSongs() {
        if (_isLoadingMore.value) return // Prevent multiple concurrent requests
        
        continuation?.let {
            viewModelScope.launch(Dispatchers.IO) {
                _isLoadingMore.value = true
                YouTube.playlistContinuation(it)
                    .onSuccess { playlistContinuationPage ->
                        val currentSongs = playlistSongs.value.toMutableList()
                        currentSongs.addAll(playlistContinuationPage.songs)
                        playlistSongs.value = currentSongs.distinctBy { it.id }
                        continuation = playlistContinuationPage.continuation
                        prefetchViewCounts(playlistContinuationPage.songs.map { song -> song.id })
                        _isLoadingMore.value = false
                    }.onFailure { throwable ->
                        _isLoadingMore.value = false
                        reportException(throwable)
                    }
            }
        }
    }

    fun retry() {
        load(initial = true)
    }

    private fun load(initial: Boolean) {
        if (initial) {
            if (_isLoading.value && playlist.value != null) return
        } else {
            if (_isRefreshing.value || _isLoading.value) return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (initial) {
                _isLoading.value = true
            } else {
                _isRefreshing.value = true
            }

            _error.value = null

            YouTube
                .playlist(playlistId)
                .completed()
                .onSuccess { playlistPage ->
                    playlist.value = playlistPage.playlist
                    playlistSongs.value = playlistPage.songs.distinctBy { it.id }
                    continuation = playlistPage.songsContinuation
                    prefetchViewCounts(playlistPage.songs.map { song -> song.id })
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: "Failed to load playlist"
                    reportException(throwable)
                }

            if (initial) {
                _isLoading.value = false
            } else {
                _isRefreshing.value = false
            }
        }
    }

    private fun prefetchViewCounts(videoIds: List<String>) {
        val uniqueIds = videoIds.distinct().filter { it.isNotBlank() }
        if (uniqueIds.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            coroutineScope {
                uniqueIds.map { videoId ->
                    async {
                        val shouldFetch =
                            viewCountsMutex.withLock {
                                if (_viewCounts.value.containsKey(videoId) || viewCountsInFlight.contains(videoId)) {
                                    false
                                } else {
                                    viewCountsInFlight.add(videoId)
                                    true
                                }
                            }

                        if (!shouldFetch) return@async

                        try {
                            viewCountsSemaphore.withPermit {
                                val count = YouTube.getMediaInfo(videoId).getOrNull()?.viewCount
                                if (count != null && count >= 0) {
                                    _viewCounts.update { current -> current + (videoId to count) }
                                }
                            }
                        } finally {
                            viewCountsMutex.withLock { viewCountsInFlight.remove(videoId) }
                        }
                    }
                }.awaitAll()
            }
        }
    }
}

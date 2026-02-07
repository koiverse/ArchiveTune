/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.LyricsEntity
import moe.koiverse.archivetune.lyrics.LyricsHelper
import moe.koiverse.archivetune.lyrics.LyricsResult
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.utils.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
 
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    private var job: Job? = null
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            networkConnectivity.networkStatus.collect { isConnected ->
                _isNetworkAvailable.value = isConnected
            }
        }
        
        // Set initial state using synchronous check
        _isNetworkAvailable.value = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true // Assume connected as fallback
        }
    }

    fun search(
        mediaId: String,
        title: String,
        artist: String,
        duration: Int,
    ) {
        isLoading.value = true
        results.value = emptyList()
        job?.cancel()
        job =
            viewModelScope.launch(Dispatchers.IO) {
                lyricsHelper.getAllLyrics(mediaId, title, artist, null, duration) { result ->
                    results.update {
                        it + result
                    }
                }
                isLoading.value = false
            }
    }

    fun cancelSearch() {
        job?.cancel()
        job = null
    }

    suspend fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        lyricsEntity: LyricsEntity?,
    ) {
        val lyrics = withContext(Dispatchers.IO) {
            lyricsHelper.getLyrics(mediaMetadata)
        }

        withContext(Dispatchers.IO) {
            database.query {
                lyricsEntity?.let(::delete)
                upsert(LyricsEntity(mediaMetadata.id, lyrics))
            }
        }
    }

    fun updateLyrics(
        mediaMetadata: MediaMetadata,
        lyrics: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            database.query {
                upsert(LyricsEntity(mediaMetadata.id, lyrics))
            }
        }
    }
}

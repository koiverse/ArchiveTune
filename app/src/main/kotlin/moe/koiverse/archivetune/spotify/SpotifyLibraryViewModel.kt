/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.spotify.models.SpotifyPlaylist
import javax.inject.Inject

@HiltViewModel
class SpotifyLibraryViewModel @Inject constructor(
    private val repository: SpotifyLibraryRepository,
) : ViewModel() {
    val playlists: StateFlow<List<SpotifyPlaylist>> =
        repository.playlists.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isRefreshing: StateFlow<Boolean> =
        repository.isRefreshing.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val errorMessage: StateFlow<String?> =
        repository.errorMessage.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun refreshPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshPlaylists()
        }
    }
}

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.AlbumItem
import moe.koiverse.archivetune.innertube.models.AlbumReleaseType
import moe.koiverse.archivetune.innertube.models.filterExplicit
import moe.koiverse.archivetune.innertube.models.filterVideo
import moe.koiverse.archivetune.constants.HideExplicitKey
import moe.koiverse.archivetune.constants.HideVideoKey
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class NewReleaseContent(
    val albums: List<AlbumItem>,
    val singles: List<AlbumItem>,
    val eps: List<AlbumItem>,
) {
    val totalReleases: Int
        get() = albums.size + singles.size + eps.size

    val isEmpty: Boolean
        get() = totalReleases == 0
}

sealed interface NewReleaseUiState {
    data object Loading : NewReleaseUiState
    data class Success(val content: NewReleaseContent) : NewReleaseUiState
    data object Empty : NewReleaseUiState
    data class Error(val throwable: Throwable?) : NewReleaseUiState
}

@HiltViewModel
class NewReleaseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<NewReleaseUiState>(NewReleaseUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = NewReleaseUiState.Loading
            try {
                val albums = YouTube.newReleaseAlbums().getOrThrow()
                val artistRanks: MutableMap<String, Int> = mutableMapOf()
                val favouriteArtistRanks: MutableMap<String, Int> = mutableMapOf()
                database.allArtistsByPlayTime().first().let { list ->
                    var favIndex = 0
                    for ((artistsIndex, artist) in list.withIndex()) {
                        artistRanks[artist.id] = artistsIndex
                        if (artist.artist.bookmarkedAt != null) {
                            favouriteArtistRanks[artist.id] = favIndex
                            favIndex++
                        }
                    }
                }
                val filtered =
                    albums
                        .sortedBy { album ->
                            val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                            val firstArtistKey =
                                artistIds.firstNotNullOfOrNull { artistId ->
                                    favouriteArtistRanks[artistId] ?: artistRanks[artistId]
                                } ?: Int.MAX_VALUE
                            firstArtistKey
                        }
                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                        .filterVideo(context.dataStore.get(HideVideoKey, false))
                        .distinctBy { it.id }
                val content = filtered.toNewReleaseContent()
                _uiState.value =
                    if (content.isEmpty) NewReleaseUiState.Empty
                    else NewReleaseUiState.Success(content)
            } catch (t: Throwable) {
                reportException(t)
                _uiState.value = NewReleaseUiState.Error(t)
            }
        }
    }

    private fun List<AlbumItem>.toNewReleaseContent(): NewReleaseContent {
        return NewReleaseContent(
            albums = filter { it.releaseType == AlbumReleaseType.ALBUM },
            singles = filter { it.releaseType == AlbumReleaseType.SINGLE },
            eps = filter { it.releaseType == AlbumReleaseType.EP },
        )
    }
}

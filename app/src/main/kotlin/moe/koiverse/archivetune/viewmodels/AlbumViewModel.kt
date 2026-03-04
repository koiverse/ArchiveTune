/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.AlbumItem
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.AlbumWithSongs
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AlbumUiState {
    data object Loading : AlbumUiState

    data object Content : AlbumUiState

    data object Empty : AlbumUiState

    data class Error(
        val isNotFound: Boolean = false,
    ) : AlbumUiState
}

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            val album = database.album(albumId).first()
            YouTube
                .album(albumId)
                .onSuccess {
                    runCatching {
                        val hasSongs = it.songs.isNotEmpty()
                        playlistId.value = it.album.playlistId
                        otherVersions.value = it.otherVersions
                        database.withTransaction {
                            if (album == null) {
                                insert(it)
                            } else {
                                update(album.album, it, album.artists)
                            }
                        }
                        _uiState.value = if (hasSongs) AlbumUiState.Content else AlbumUiState.Empty
                    }.onFailure { throwable ->
                        reportException(throwable)
                        updateUiState(database.albumWithSongs(albumId).first())
                    }
                }.onFailure {
                    reportException(it)
                    val isNotFound = it.message?.contains("NOT_FOUND") == true
                    if (isNotFound) {
                        album?.album?.let { albumEntity ->
                            database.withTransaction {
                                delete(albumEntity)
                            }
                        }
                    }
                    val cachedAlbum = database.albumWithSongs(albumId).first()
                    if (cachedAlbum != null) {
                        updateUiState(cachedAlbum)
                    } else {
                        _uiState.value = AlbumUiState.Error(isNotFound = isNotFound)
                    }
                }
        }
    }

    private fun updateUiState(albumWithSongs: AlbumWithSongs?) {
        _uiState.value =
            when {
                albumWithSongs == null -> AlbumUiState.Error()
                albumWithSongs.songs.isEmpty() -> AlbumUiState.Empty
                else -> AlbumUiState.Content
            }
    }
}

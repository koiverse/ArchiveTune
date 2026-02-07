/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.constants.AlbumFilter
import moe.koiverse.archivetune.constants.AlbumFilterKey
import moe.koiverse.archivetune.constants.AlbumSortDescendingKey
import moe.koiverse.archivetune.constants.AlbumSortType
import moe.koiverse.archivetune.constants.AlbumSortTypeKey
import moe.koiverse.archivetune.constants.ArtistFilter
import moe.koiverse.archivetune.constants.ArtistFilterKey
import moe.koiverse.archivetune.constants.ArtistSongSortDescendingKey
import moe.koiverse.archivetune.constants.ArtistSongSortType
import moe.koiverse.archivetune.constants.ArtistSongSortTypeKey
import moe.koiverse.archivetune.constants.ArtistSortDescendingKey
import moe.koiverse.archivetune.constants.ArtistSortType
import moe.koiverse.archivetune.constants.ArtistSortTypeKey
import moe.koiverse.archivetune.constants.HideExplicitKey
import moe.koiverse.archivetune.constants.HideVideoKey
import moe.koiverse.archivetune.constants.LibraryFilter
import moe.koiverse.archivetune.constants.PlaylistSortDescendingKey
import moe.koiverse.archivetune.constants.PlaylistSortType
import moe.koiverse.archivetune.constants.PlaylistSortDescendingKey
import moe.koiverse.archivetune.constants.PlaylistSortTypeKey
import moe.koiverse.archivetune.constants.SongFilter
import moe.koiverse.archivetune.constants.SongFilterKey
import moe.koiverse.archivetune.constants.SongSortDescendingKey
import moe.koiverse.archivetune.constants.SongSortType
import moe.koiverse.archivetune.constants.SongSortTypeKey
import moe.koiverse.archivetune.constants.TopSize
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.extensions.filterExplicit
import moe.koiverse.archivetune.extensions.filterExplicitAlbums
import moe.koiverse.archivetune.extensions.reversed
import moe.koiverse.archivetune.extensions.toEnum
import moe.koiverse.archivetune.playback.DownloadUtil
import moe.koiverse.archivetune.utils.SyncUtils
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LibrarySongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allSongs =
        context.dataStore.data
            .map {
                Triple(
                    Triple(
                        it[SongFilterKey].toEnum(SongFilter.LIKED),
                        it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                        (it[SongSortDescendingKey] ?: true),
                    ),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoKey] ?: false,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filterSort, hideExplicit, hideVideo) ->
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    SongFilter.LIBRARY -> database.songs(sortType, descending, hideVideo).map { it.filterExplicit(hideExplicit) }
                    SongFilter.LIKED -> database.likedSongs(sortType, descending, hideVideo).map { it.filterExplicit(hideExplicit) }
                    SongFilter.DOWNLOADED ->
                        downloadUtil.downloads.flatMapLatest { downloads ->
                            database
                                .allSongs()
                                .flowOn(Dispatchers.IO)
                                .map { songs ->
                                    songs.filter { song: Song ->
                                        downloads[song.id]?.state == Download.STATE_COMPLETED
                                    }
                                }.map { songs ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> songs.sortedBy { song: Song ->
                                            downloads[song.id]?.updateTimeMs ?: 0L
                                        }

                                        SongSortType.NAME -> songs.sortedBy { song: Song -> song.song.title }
                                        SongSortType.ARTIST -> {
                                            val collator =
                                                Collator.getInstance(Locale.getDefault())
                                            collator.strength = Collator.PRIMARY
                                            songs
                                                .sortedWith(
                                                    compareBy(collator) { song: Song ->
                                                        song.artists.joinToString("") { artist -> artist.name }
                                                    },
                                                ).groupBy { it.album?.title }
                                                .flatMap { (_, songsByAlbum) ->
                                                    songsByAlbum.sortedBy { album ->
                                                        album.artists.joinToString(
                                                            "",
                                                        ) { artist -> artist.name }
                                                    }
                                                }
                                        }

                                        SongSortType.PLAY_TIME -> songs.sortedBy { song: Song -> song.song.totalPlayTime }
                                    }.reversed(descending).filterExplicit(hideExplicit)
                                }
                        }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncLibrarySongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLibrarySongs() }
    }
}

@HiltViewModel
class LibraryArtistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allArtists =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistFilterKey].toEnum(ArtistFilter.LIKED),
                    it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                    it[ArtistSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                    ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncArtistsSubscriptions() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryAlbumsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allAlbums =
        context.dataStore.data
            .map {
                Pair(
                    Triple(
                        it[AlbumFilterKey].toEnum(AlbumFilter.LIKED),
                        it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                        it[AlbumSortDescendingKey] ?: true,
                    ),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filterSort, hideExplicit) ->
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    AlbumFilter.DOWNLOADED ->
                        downloadUtil.downloads.flatMapLatest { downloads ->
                            database.allSongs()
                                .flowOn(Dispatchers.IO)
                                .map { songs ->
                                    songs
                                        .filter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }
                                        .mapNotNull { it.song.albumId }
                                        .toSet()
                                }.flatMapLatest { downloadedAlbumIds ->
                                    database.albumsByIds(downloadedAlbumIds, sortType, descending)
                                        .map { albums -> albums.filterExplicitAlbums(hideExplicit) }
                                }
                        }
                    
                        AlbumFilter.DOWNLOADED_FULL ->
                            downloadUtil.downloads.flatMapLatest { downloads ->
                                database.allSongs()
                                    .flowOn(Dispatchers.IO)
                                    .map { songs ->
                                        songs
                                            .filter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }
                                            .mapNotNull { song -> song.song.albumId?.let { albumId -> albumId to song } }
                                            .groupBy({ it.first }, { it.second })
                                            .mapValues { (_, songList) -> songList.size }
                                    }.flatMapLatest { downloadedCountByAlbum ->
                                        database.albumsByIds(downloadedCountByAlbum.keys, sortType, descending)
                                            .map { albums ->
                                                albums.filter { album ->
                                                    val totalSongsInAlbum = album.album.songCount
                                                    val downloadedSongsCount = downloadedCountByAlbum[album.album.id] ?: 0
                                                    totalSongsInAlbum > 0 && downloadedSongsCount >= totalSongsInAlbum
                                                }.filterExplicitAlbums(hideExplicit)
                                            }
                                    }
                            }
                    AlbumFilter.LIBRARY -> database.albums(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
                    AlbumFilter.LIKED -> database.albumsLiked(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedAlbums() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryPlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allPlaylists =
        context.dataStore.data
            .map {
                it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CUSTOM) to (it[PlaylistSortDescendingKey]
                    ?: true)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.playlists(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            syncUtils.syncSavedPlaylists()
            syncUtils.syncAutoSyncPlaylists()
            _isRefreshing.value = false
        }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
}

@HiltViewModel
class ArtistSongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist =
        database
            .artist(artistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs =
        context.dataStore.data
            .map {
                Pair(
                    it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (it[ArtistSongSortDescendingKey]
                        ?: true),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit) ->
                val (sortType, descending) = sortDesc
                database.artistSongs(artistId, sortType, descending).map { it.filterExplicit(hideExplicit) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val syncAllLibrary = {
         viewModelScope.launch(Dispatchers.IO) {
             try {
                 syncUtils.performFullSync()
             } catch (e: Exception) {
                 timber.log.Timber.e(e, "Error during manual sync")
             }
         }
    }
    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
    var artists =
        database
            .artistsBookmarked(
                ArtistSortType.CREATE_DATE,
                true,
            ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var albums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.albumsLiked(AlbumSortType.CREATE_DATE, true).map { it.filterExplicitAlbums(hideExplicit) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var playlists =
        context.dataStore.data
            .map {
                it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CUSTOM) to (it[PlaylistSortDescendingKey] ?: true)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) -> database.playlists(sortType, descending) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                                Duration.between(
                                    it.lastUpdateTime,
                                    LocalDateTime.now(),
                                ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryViewModel
@Inject
constructor() : ViewModel() {
    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen
}

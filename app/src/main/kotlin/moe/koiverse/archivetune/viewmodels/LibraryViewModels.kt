/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





@file:OptIn(ExperimentalCoroutinesApi::class)

package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ai.AiServiceConfig
import moe.koiverse.archivetune.ai.AiTextService
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.constants.AiApiKeyKey
import moe.koiverse.archivetune.constants.AiApiValidationStatus
import moe.koiverse.archivetune.constants.AiApiValidationStatusKey
import moe.koiverse.archivetune.constants.AiCustomEndpointKey
import moe.koiverse.archivetune.constants.AiCustomModelKey
import moe.koiverse.archivetune.constants.AiProvider
import moe.koiverse.archivetune.constants.AiProviderKey
import moe.koiverse.archivetune.constants.AiSelectedModelKey
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
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.PlaylistSongMap
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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
                                            songs.sortedWith(compareBy(collator) { song: Song ->
                                                song.artists.joinToString("") { artist -> artist.name }
                                            })
                                        }

                                        SongSortType.PLAY_TIME -> songs.sortedBy { song: Song -> song.song.totalPlayTime }
                                    }.reversed(descending).filterExplicit(hideExplicit)
                                }
                        }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refresh(filter: SongFilter) {
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                when (filter) {
                    SongFilter.LIKED -> syncUtils.syncLikedSongs()
                    SongFilter.LIBRARY -> syncUtils.syncLibrarySongs()
                    SongFilter.DOWNLOADED -> Unit
                }
            } catch (e: Exception) {
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun syncLikedSongs() {
        refresh(SongFilter.LIKED)
    }

    fun syncLibrarySongs() {
        refresh(SongFilter.LIBRARY)
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
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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

    fun refresh(filter: ArtistFilter) {
        if (filter != ArtistFilter.LIKED) return
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                syncUtils.syncArtistsSubscriptions()
            } catch (e: Exception) {
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun sync() {
        refresh(ArtistFilter.LIKED)
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
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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

    fun refresh(filter: AlbumFilter) {
        if (filter != AlbumFilter.LIKED) return
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                syncUtils.syncLikedAlbums()
            } catch (e: Exception) {
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun sync() {
        refresh(AlbumFilter.LIKED)
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
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    private val _buildYourMixState = MutableStateFlow<BuildYourMixUiState>(BuildYourMixUiState.Idle)
    val buildYourMixState = _buildYourMixState.asStateFlow()

    val isBuildYourMixAvailable =
        context.dataStore.data
            .map { prefs ->
                val provider = prefs[AiProviderKey].toEnum(AiProvider.NONE)
                provider != AiProvider.NONE &&
                    prefs[AiApiKeyKey].orEmpty().isNotBlank() &&
                    (provider != AiProvider.CUSTOM || prefs[AiCustomEndpointKey].orEmpty().isNotBlank()) &&
                    prefs[AiApiValidationStatusKey].toEnum(AiApiValidationStatus.UNKNOWN) != AiApiValidationStatus.FAILED
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun syncAllLibrary() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncUtils.performFullSync()
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error during manual sync")
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun buildYourMix(
        basis: BuildYourMixBasis,
        songCount: Int,
        manualBasis: String,
    ) {
        if (_buildYourMixState.value == BuildYourMixUiState.Loading) return
        viewModelScope.launch(Dispatchers.IO) {
            _buildYourMixState.value = BuildYourMixUiState.Loading
            try {
                val count = songCount.coerceIn(1, 100)
                val normalizedManualBasis = manualBasis.trim()
                if (basis == BuildYourMixBasis.INPUT_MANUALLY && normalizedManualBasis.isBlank()) {
                    _buildYourMixState.value = BuildYourMixUiState.Error(context.getString(R.string.build_your_mix_manual_required))
                    return@launch
                }

                val localCandidates = mixCandidatesFor(
                    basis = basis,
                    songCount = count,
                    manualBasis = normalizedManualBasis,
                )
                val candidates = validateSongsWithYtm(
                    songs = localCandidates,
                    basis = basis,
                    manualBasis = normalizedManualBasis,
                )
                if (candidates.isEmpty()) {
                    _buildYourMixState.value = BuildYourMixUiState.Error(
                        context.getString(
                            if (basis == BuildYourMixBasis.INPUT_MANUALLY) {
                                R.string.build_your_mix_no_manual_match
                            } else {
                                R.string.build_your_mix_empty_library
                            },
                        ),
                    )
                    return@launch
                }

                val aiMix = requestAiMix(
                    config = readAiConfig(),
                    basis = basis,
                    songCount = count,
                    manualBasis = if (basis == BuildYourMixBasis.INPUT_MANUALLY) normalizedManualBasis else "",
                    candidates = candidates,
                )
                val finalSongs = validateFinalMixSongs(
                    aiSongs = aiMix.songs,
                    fallbackCandidates = candidates,
                    basis = basis,
                    manualBasis = normalizedManualBasis,
                    songCount = count,
                )
                if (finalSongs.isEmpty()) {
                    _buildYourMixState.value = BuildYourMixUiState.Error(
                        context.getString(
                            if (basis == BuildYourMixBasis.INPUT_MANUALLY) {
                                R.string.build_your_mix_no_manual_match
                            } else {
                                R.string.build_your_mix_empty_library
                            },
                        ),
                    )
                    return@launch
                }
                val playlistId = createGeneratedPlaylist(
                    title = aiMix.title.ifBlank { basis.defaultTitle() },
                    songs = finalSongs.map { it.song },
                )
                _buildYourMixState.value = BuildYourMixUiState.Success(
                    playlistId = playlistId,
                    playlistTitle = aiMix.title.ifBlank { basis.defaultTitle() },
                )
            } catch (e: Exception) {
                reportException(e)
                _buildYourMixState.value = BuildYourMixUiState.Error(
                    context.getString(R.string.build_your_mix_failed) + ": " + (e.localizedMessage ?: e.toString()),
                )
            }
        }
    }

    fun resetBuildYourMixState() {
        _buildYourMixState.value = BuildYourMixUiState.Idle
    }

    private suspend fun readAiConfig(): AiServiceConfig {
        val prefs = context.dataStore.data.first()
        val provider = prefs[AiProviderKey].toEnum(AiProvider.NONE)
        return AiServiceConfig(
            provider = provider,
            apiKey = prefs[AiApiKeyKey].orEmpty(),
            customEndpoint = prefs[AiCustomEndpointKey].orEmpty(),
            model = if (provider == AiProvider.CUSTOM) {
                prefs[AiCustomModelKey].orEmpty()
            } else {
                prefs[AiSelectedModelKey].orEmpty()
            },
        )
    }

    private suspend fun mixCandidatesFor(
        basis: BuildYourMixBasis,
        songCount: Int,
        manualBasis: String,
    ): List<Song> {
        val requestedPoolSize = (songCount * 2).coerceIn(50, 100)
        val primary = when (basis) {
            BuildYourMixBasis.LISTENING_HISTORY -> database.recentSongs(requestedPoolSize).first()
            BuildYourMixBasis.AVERAGE_LISTENED -> database
                .songs(SongSortType.PLAY_TIME, descending = true)
                .first()
                .filter { it.song.totalPlayTime > 0L }
            BuildYourMixBasis.INPUT_MANUALLY -> database
                .songs(SongSortType.CREATE_DATE, descending = true)
                .first()
                .asSequence()
                .filter { it.matchesManualBasis(manualBasis) }
                .sortedByDescending { it.manualBasisMatchScore(manualBasis) }
                .toList()
        }
        val fallback = if (primary.size < songCount && basis != BuildYourMixBasis.INPUT_MANUALLY) {
            database.songs(SongSortType.CREATE_DATE, descending = true).first()
        } else {
            emptyList()
        }
        return (primary + fallback)
            .distinctBy { it.id }
            .take(requestedPoolSize)
    }

    private suspend fun validateSongsWithYtm(
        songs: List<Song>,
        basis: BuildYourMixBasis,
        manualBasis: String,
    ): List<ValidatedMixSong> =
        buildList {
            songs.distinctBy { it.id }.forEach { song ->
                song.validateWithYtm()
                    ?.takeIf { it.matchesBasis(basis, manualBasis) }
                    ?.let(::add)
            }
        }

    private suspend fun validateFinalMixSongs(
        aiSongs: List<ValidatedMixSong>,
        fallbackCandidates: List<ValidatedMixSong>,
        basis: BuildYourMixBasis,
        manualBasis: String,
        songCount: Int,
    ): List<ValidatedMixSong> {
        val aiValidated = validateSongsWithYtm(
            songs = aiSongs.map { it.song },
            basis = basis,
            manualBasis = manualBasis,
        )
        val selectedIds = aiValidated.mapTo(mutableSetOf()) { it.id }
        val proposedSongs = buildList {
            addAll(aiValidated.map { it.song })
            fallbackCandidates.forEach { candidate ->
                if (size >= songCount) return@forEach
                if (selectedIds.add(candidate.id)) add(candidate.song)
            }
        }
        return validateSongsWithYtm(
            songs = proposedSongs,
            basis = basis,
            manualBasis = manualBasis,
        ).take(songCount)
    }

    private suspend fun requestAiMix(
        config: AiServiceConfig,
        basis: BuildYourMixBasis,
        songCount: Int,
        manualBasis: String,
        candidates: List<ValidatedMixSong>,
    ): GeneratedMix {
        val candidateById = candidates.associateBy { it.id }
        val candidatePayload = JSONArray().apply {
            candidates.forEach { candidate ->
                put(
                    JSONObject()
                        .put("id", candidate.id)
                        .put("title", candidate.ytmTitle)
                        .put("artists", candidate.ytmArtists.joinToString(", "))
                        .put("localTitle", candidate.song.song.title)
                        .put("localArtists", candidate.song.artists.joinToString(", ") { it.name })
                        .put("totalPlayTimeMs", candidate.song.song.totalPlayTime),
                )
            }
        }
        val response = AiTextService.complete(
            config = config,
            systemPrompt = """
                You are a music curator for ArchiveTune.
                Build one personal playlist using only the provided candidate song IDs.
                Return JSON only with this schema: {"title":"short accurate playlist title","songIds":["id"]}.
                The title must reflect the selected basis and the selected songs.
                For manual user input, only choose songs where the provided artist/title text matches the candidate title or artist.
                Select up to $songCount songs, avoid duplicates, and prioritize coherence over variety.
            """.trimIndent(),
            userPrompt = JSONObject()
                .put("basis", basis.promptLabel)
                .put("manualBasis", manualBasis.trim())
                .put("requestedSongCount", songCount)
                .put("candidates", candidatePayload)
                .toString(),
            temperature = 0.35,
            maxTokens = 4096,
        )
        val json = JSONObject(response.substringAfter('{').substringBeforeLast('}').let { "{$it}" })
        val selected = json
            .optJSONArray("songIds")
            ?.toStringList()
            .orEmpty()
            .mapNotNull(candidateById::get)
            .filter { it.matchesBasis(basis, manualBasis) }
            .distinctBy { it.id }
            .toMutableList()
        if (selected.size < songCount) {
            val selectedIds = selected.mapTo(mutableSetOf()) { it.id }
            candidates.forEach { candidate ->
                if (selected.size >= songCount) return@forEach
                if (candidate.matchesBasis(basis, manualBasis) && selectedIds.add(candidate.id)) selected.add(candidate)
            }
        }
        return GeneratedMix(
            title = json.optString("title").sanitizePlaylistTitle().ifBlank { basis.defaultTitle() },
            songs = selected.take(songCount),
        )
    }

    private suspend fun createGeneratedPlaylist(
        title: String,
        songs: List<Song>,
    ): String {
        val playlistId = PlaylistEntity.generatePlaylistId()
        database.withTransaction {
            val playlistEntity = PlaylistEntity(
                id = playlistId,
                name = title,
                bookmarkedAt = LocalDateTime.now(),
                customOrder = (maxPlaylistCustomOrder() ?: -1) + 1,
                isLocal = true,
            )
            insert(playlistEntity)
            songs.forEachIndexed { index, song ->
                insert(
                    PlaylistSongMap(
                        playlistId = playlistId,
                        songId = song.id,
                        position = index,
                    ),
                )
            }
        }
        return playlistId
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

enum class BuildYourMixBasis(
    val promptLabel: String,
) {
    LISTENING_HISTORY("recent listening history"),
    AVERAGE_LISTENED("average listened songs"),
    INPUT_MANUALLY("manual user input"),
}

fun BuildYourMixBasis.defaultTitle(): String =
    when (this) {
        BuildYourMixBasis.LISTENING_HISTORY -> "Listening History Mix"
        BuildYourMixBasis.AVERAGE_LISTENED -> "Average Listened Mix"
        BuildYourMixBasis.INPUT_MANUALLY -> "Custom Mix"
    }

@Immutable
sealed interface BuildYourMixUiState {
    data object Idle : BuildYourMixUiState
    data object Loading : BuildYourMixUiState

    @Immutable
    data class Success(
        val playlistId: String,
        val playlistTitle: String,
    ) : BuildYourMixUiState

    @Immutable
    data class Error(
        val message: String,
    ) : BuildYourMixUiState
}

@Immutable
private data class GeneratedMix(
    val title: String,
    val songs: List<ValidatedMixSong>,
)

@Immutable
private data class ValidatedMixSong(
    val song: Song,
    val ytmTitle: String,
    val ytmArtists: List<String>,
) {
    val id: String
        get() = song.id
}

private suspend fun Song.validateWithYtm(): ValidatedMixSong? {
    val query = ytmValidationQuery()
    if (query.isBlank()) return null
    val songs = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
        .getOrNull()
        ?.items
        .orEmpty()
        .filterIsInstance<SongItem>()
    val match = songs.firstOrNull { it.id == id }
        ?: songs.firstOrNull { it.matchesLocalIdentity(this) }
    return match?.let { songItem ->
        ValidatedMixSong(
            song = this,
            ytmTitle = songItem.title,
            ytmArtists = songItem.artists.map { it.name },
        )
    }
}

private fun Song.ytmValidationQuery(): String =
    buildString {
        append(song.title)
        val artistNames = artists.joinToString(" ") { it.name }
        if (artistNames.isNotBlank()) {
            append(' ')
            append(artistNames)
        }
    }.trim()

private fun SongItem.matchesLocalIdentity(song: Song): Boolean =
    title.matchesComparableTitle(song.song.title) && artists.matchesAnyLocalArtist(song)

private fun String.matchesComparableTitle(other: String): Boolean {
    val self = normalizedForMixMatch()
    val target = other.normalizedForMixMatch()
    return self.isNotBlank() &&
        target.isNotBlank() &&
        (self == target || (self.length > 3 && target.contains(self)) || (target.length > 3 && self.contains(target)))
}

private fun List<moe.koiverse.archivetune.innertube.models.Artist>.matchesAnyLocalArtist(song: Song): Boolean {
    val remoteArtists = map { it.name.normalizedForMixMatch() }.filter { it.isNotBlank() }
    val localArtists = song.artists.map { it.name.normalizedForMixMatch() }.filter { it.isNotBlank() }
    if (remoteArtists.isEmpty() || localArtists.isEmpty()) return false
    return localArtists.any { localArtist ->
        remoteArtists.any { remoteArtist ->
            localArtist == remoteArtist ||
                (localArtist.length > 3 && remoteArtist.contains(localArtist)) ||
                (remoteArtist.length > 3 && localArtist.contains(remoteArtist))
        }
    }
}

private fun ValidatedMixSong.matchesBasis(
    basis: BuildYourMixBasis,
    manualBasis: String,
): Boolean =
    basis != BuildYourMixBasis.INPUT_MANUALLY || matchesManualBasis(manualBasis)

private fun ValidatedMixSong.matchesManualBasis(manualBasis: String): Boolean {
    val searchable = manualSearchableText()
    val phrases = manualBasis.manualSearchPhrases()
    if (phrases.isEmpty()) return false
    if (phrases.any { searchable.contains(it) }) return true
    val tokens = manualBasis.manualSearchTokens()
    return tokens.isNotEmpty() && tokens.all(searchable::contains)
}

private fun ValidatedMixSong.manualSearchableText(): String =
    buildString {
        append(ytmTitle)
        append(' ')
        append(ytmArtists.joinToString(" "))
    }.normalizedForMixMatch()

private fun JSONArray.toStringList(): List<String> =
    List(length()) { index -> optString(index) }.filter { it.isNotBlank() }

private fun String.sanitizePlaylistTitle(): String =
    lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
        .take(80)

private fun Song.matchesManualBasis(manualBasis: String): Boolean {
    val searchable = manualSearchableText()
    val phrases = manualBasis.manualSearchPhrases()
    if (phrases.isEmpty()) return false
    if (phrases.any { searchable.contains(it) }) return true
    val tokens = manualBasis.manualSearchTokens()
    return tokens.isNotEmpty() && tokens.all(searchable::contains)
}

private fun Song.manualBasisMatchScore(manualBasis: String): Int {
    val title = song.title.normalizedForMixMatch()
    val artist = artists.joinToString(" ") { it.name }.normalizedForMixMatch()
    val searchable = "$title $artist".trim()
    val phraseScore = manualBasis.manualSearchPhrases().sumOf { phrase ->
        when {
            artist.contains(phrase) -> 80 + phrase.length
            title.contains(phrase) -> 60 + phrase.length
            searchable.contains(phrase) -> 40 + phrase.length
            else -> 0
        }
    }
    val tokenScore = manualBasis.manualSearchTokens().sumOf { token ->
        (if (artist.contains(token)) 10 else 0) +
            (if (title.contains(token)) 8 else 0)
    }
    return phraseScore + tokenScore
}

private fun Song.manualSearchableText(): String =
    buildString {
        append(song.title)
        append(' ')
        append(artists.joinToString(" ") { it.name })
    }.normalizedForMixMatch()

private fun String.manualSearchPhrases(): List<String> =
    split(Regex("[,;\\n]+"))
        .map { it.normalizedForMixMatch() }
        .filter { it.length > 1 }

private fun String.manualSearchTokens(): List<String> =
    normalizedForMixMatch()
        .split(' ')
        .filter { it.length > 1 }

private fun String.normalizedForMixMatch(): String =
    lowercase(Locale.getDefault())
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

@HiltViewModel
class LibraryViewModel
@Inject
constructor() : ViewModel() {
    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen
}

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.spotifyimport

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.SpotifyAccessTokenExpiresAtKey
import moe.koiverse.archivetune.constants.SpotifyAccessTokenKey
import moe.koiverse.archivetune.constants.SpotifyAccountAvatarUrlKey
import moe.koiverse.archivetune.constants.SpotifyAccountNameKey
import moe.koiverse.archivetune.constants.SpotifySpDcKey
import moe.koiverse.archivetune.constants.SpotifySpKeyKey
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.PlaylistSongMap
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.spotify.Spotify
import moe.koiverse.archivetune.spotify.SpotifyAuth
import moe.koiverse.archivetune.spotify.SpotifyMapper
import moe.koiverse.archivetune.spotify.models.SpotifyPlaylist
import moe.koiverse.archivetune.spotify.models.SpotifyPlaylistTracksRef
import moe.koiverse.archivetune.spotify.models.SpotifyTrack
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.reportException
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class SpotifyImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val mapperMutex = Mutex()

    suspend fun restoreSession(): SpotifyImportSession =
        withContext(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            val token = prefs[SpotifyAccessTokenKey].orEmpty()
            val expiresAt = prefs[SpotifyAccessTokenExpiresAtKey] ?: 0L
            val accountName = prefs[SpotifyAccountNameKey].orEmpty()
            val avatarUrl = prefs[SpotifyAccountAvatarUrlKey]

            if (token.isNotBlank() && expiresAt > System.currentTimeMillis() + TOKEN_EXPIRY_GRACE_MS) {
                Spotify.accessToken = token
                return@withContext SpotifyImportSession(
                    isAuthenticated = true,
                    accountName = accountName,
                    accountAvatarUrl = avatarUrl,
                )
            }

            val spDc = prefs[SpotifySpDcKey].orEmpty()
            if (spDc.isBlank()) {
                return@withContext SpotifyImportSession()
            }

            refreshAccessToken(spDc = spDc, spKey = prefs[SpotifySpKeyKey].orEmpty())
                .fold(
                    onSuccess = {
                        val refreshed = context.dataStore.data.first()
                        SpotifyImportSession(
                            isAuthenticated = true,
                            accountName = refreshed[SpotifyAccountNameKey].orEmpty(),
                            accountAvatarUrl = refreshed[SpotifyAccountAvatarUrlKey],
                        )
                    },
                    onFailure = {
                        if (it is CancellationException) throw it
                        reportException(it)
                        SpotifyImportSession()
                    },
                )
        }

    suspend fun connectWithCookies(
        spDc: String,
        spKey: String,
    ): SpotifyImportSession =
        withContext(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[SpotifySpDcKey] = spDc
                if (spKey.isNotBlank()) {
                    prefs[SpotifySpKeyKey] = spKey
                } else {
                    prefs.remove(SpotifySpKeyKey)
                }
            }
            refreshAccessToken(spDc = spDc, spKey = spKey).getOrThrow()
            val prefs = context.dataStore.data.first()
            SpotifyImportSession(
                isAuthenticated = true,
                accountName = prefs[SpotifyAccountNameKey].orEmpty(),
                accountAvatarUrl = prefs[SpotifyAccountAvatarUrlKey],
            )
        }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs.remove(SpotifySpDcKey)
                prefs.remove(SpotifySpKeyKey)
                prefs.remove(SpotifyAccessTokenKey)
                prefs.remove(SpotifyAccessTokenExpiresAtKey)
                prefs.remove(SpotifyAccountNameKey)
                prefs.remove(SpotifyAccountAvatarUrlKey)
            }
            Spotify.accessToken = null
        }
    }

    suspend fun loadSources(): List<SpotifyImportSource> =
        withContext(Dispatchers.IO) {
            ensureAuthenticated()
            refreshProfile()

            val likedSongs = spotifyCallWithTokenRetry {
                Spotify.likedSongs(limit = 1, offset = 0).getOrThrow()
            }
            val playlists = fetchAllPlaylists()

            buildList {
                add(
                    SpotifyImportSource.LikedSongs(
                        title = context.getString(R.string.spotify_liked_songs),
                        trackCount = likedSongs.total,
                    ),
                )
                playlists.forEach { playlist ->
                    if (playlist.id.isNotBlank()) {
                        add(SpotifyImportSource.Playlist(playlist))
                    }
                }
            }
        }

    suspend fun importSources(
        sources: List<SpotifyImportSource>,
        onProgress: (SpotifyImportProgressUi) -> Unit,
    ): SpotifyImportSummaryUi =
        withContext(Dispatchers.IO) {
            ensureAuthenticated()
            val summaries = ArrayList<SpotifyImportSourceSummaryUi>(sources.size)

            sources.forEachIndexed { sourceIndex, source ->
                onProgress(
                    SpotifyImportProgressUi(
                        sourceTitle = source.title,
                        completedSources = sourceIndex,
                        totalSources = sources.size,
                        matchedTracks = 0,
                        totalTracks = source.trackCount ?: 0,
                        percent = progressPercent(sourceIndex, sources.size, 0, source.trackCount ?: 0),
                    ),
                )

                val tracks = fetchAllTracks(source)
                if (tracks.isEmpty()) {
                    mirrorPlaylist(source, emptyList())
                    summaries += SpotifyImportSourceSummaryUi(
                        title = source.title,
                        totalTracks = 0,
                        importedTracks = 0,
                        failedTracks = 0,
                    )
                    onProgress(
                        SpotifyImportProgressUi(
                            sourceTitle = source.title,
                            completedSources = sourceIndex + 1,
                            totalSources = sources.size,
                            matchedTracks = 0,
                            totalTracks = 0,
                            percent = progressPercent(sourceIndex + 1, sources.size, 0, 0),
                        ),
                    )
                    return@forEachIndexed
                }

                val matched = matchTracks(
                    sourceIndex = sourceIndex,
                    sourceCount = sources.size,
                    sourceTitle = source.title,
                    tracks = tracks,
                    onProgress = onProgress,
                )

                mirrorPlaylist(source, matched.map { it.metadata })
                summaries += SpotifyImportSourceSummaryUi(
                    title = source.title,
                    totalTracks = tracks.size,
                    importedTracks = matched.size,
                    failedTracks = tracks.size - matched.size,
                )
                onProgress(
                    SpotifyImportProgressUi(
                        sourceTitle = source.title,
                        completedSources = sourceIndex + 1,
                        totalSources = sources.size,
                        matchedTracks = matched.size,
                        totalTracks = tracks.size,
                        percent = progressPercent(sourceIndex + 1, sources.size, 0, 0),
                    ),
                )
            }

            SpotifyImportSummaryUi(summaries)
        }

    private suspend fun ensureAuthenticated() {
        val prefs = context.dataStore.data.first()
        val token = prefs[SpotifyAccessTokenKey].orEmpty()
        val expiresAt = prefs[SpotifyAccessTokenExpiresAtKey] ?: 0L
        if (token.isNotBlank() && expiresAt > System.currentTimeMillis() + TOKEN_EXPIRY_GRACE_MS) {
            Spotify.accessToken = token
            return
        }

        val spDc = prefs[SpotifySpDcKey].orEmpty()
        if (spDc.isBlank()) {
            throw IllegalStateException(context.getString(R.string.spotify_not_connected))
        }
        refreshAccessToken(spDc = spDc, spKey = prefs[SpotifySpKeyKey].orEmpty()).getOrThrow()
    }

    private suspend fun refreshAccessToken(
        spDc: String,
        spKey: String,
    ): Result<Unit> =
        SpotifyAuth.fetchAccessToken(spDc = spDc, spKey = spKey)
            .mapCatching { token ->
                Spotify.accessToken = token.accessToken
                context.dataStore.edit { prefs ->
                    prefs[SpotifyAccessTokenKey] = token.accessToken
                    prefs[SpotifyAccessTokenExpiresAtKey] = token.accessTokenExpirationTimestampMs
                }
                refreshProfile()
            }

    private suspend fun refreshProfile() {
        Spotify.me()
            .onSuccess { user ->
                context.dataStore.edit { prefs ->
                    prefs[SpotifyAccountNameKey] = user.displayName.orEmpty()
                    user.images.firstOrNull()?.url?.let { prefs[SpotifyAccountAvatarUrlKey] = it }
                        ?: prefs.remove(SpotifyAccountAvatarUrlKey)
                }
            }
            .onFailure { error ->
                if (error is CancellationException) {
                    throw error
                }
            }
    }

    private suspend fun fetchAllPlaylists(): List<SpotifyPlaylist> {
        val playlists = ArrayList<SpotifyPlaylist>()
        var offset = 0
        val limit = 50

        while (true) {
            val page = spotifyCallWithTokenRetry {
                Spotify.myPlaylists(limit = limit, offset = offset).getOrThrow()
            }
            if (page.items.isEmpty()) break
            playlists += enrichPlaylistTrackCounts(page.items)
            offset += page.items.size
            if (offset >= page.total || page.items.size < limit) break
        }

        return playlists
    }

    private suspend fun enrichPlaylistTrackCounts(playlists: List<SpotifyPlaylist>): List<SpotifyPlaylist> =
        coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT_SPOTIFY_COUNT_REQUESTS)
            playlists.map { playlist ->
                async {
                    if (playlist.tracks?.total != null) {
                        playlist
                    } else {
                        semaphore.withPermit {
                            playlistTrackCount(playlist.id)
                                ?.let { count -> playlist.copy(tracks = SpotifyPlaylistTracksRef(total = count)) }
                                ?: playlist
                        }
                    }
                }
            }.awaitAll()
        }

    private suspend fun playlistTrackCount(playlistId: String): Int? =
        try {
            spotifyCallWithTokenRetry {
                Spotify.playlistTracks(
                    playlistId = playlistId,
                    limit = 1,
                    offset = 0,
                ).getOrThrow()
            }.total
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            reportException(error)
            null
        }

    private suspend fun fetchAllTracks(source: SpotifyImportSource): List<SpotifyTrack> {
        val tracks = ArrayList<SpotifyTrack>()
        var offset = 0
        val limit = 100

        while (true) {
            val page =
                when (source) {
                    is SpotifyImportSource.LikedSongs -> {
                        val paging = spotifyCallWithTokenRetry {
                            Spotify.likedSongs(limit = limit, offset = offset).getOrThrow()
                        }
                        SpotifyTrackPage(
                            items = paging.items.map { it.track },
                            total = paging.total,
                        )
                    }
                    is SpotifyImportSource.Playlist -> {
                        val paging = spotifyCallWithTokenRetry {
                            Spotify.playlistTracks(
                                playlistId = source.spotifyId,
                                limit = limit,
                                offset = offset,
                            ).getOrThrow()
                        }
                        SpotifyTrackPage(
                            items = paging.items.mapNotNull { it.track },
                            total = paging.total,
                        )
                    }
                }

            if (page.items.isEmpty()) break
            tracks += page.items.filter { it.name.isNotBlank() }
            offset += page.items.size
            if (offset >= page.total || page.items.size < limit) break
        }

        return tracks
    }

    private suspend fun <T> spotifyCallWithTokenRetry(block: suspend () -> T): T =
        runCatching { block() }
            .getOrElse { error ->
                if ((error as? Spotify.SpotifyException)?.statusCode != 401) {
                    throw error
                }
                val prefs = context.dataStore.data.first()
                val spDc = prefs[SpotifySpDcKey].orEmpty()
                if (spDc.isBlank()) {
                    throw error
                }
                refreshAccessToken(spDc = spDc, spKey = prefs[SpotifySpKeyKey].orEmpty()).getOrThrow()
                block()
            }

    private suspend fun matchTracks(
        sourceIndex: Int,
        sourceCount: Int,
        sourceTitle: String,
        tracks: List<SpotifyTrack>,
        onProgress: (SpotifyImportProgressUi) -> Unit,
    ): List<MatchedTrack> =
        coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT_MATCHES)
            val completed = AtomicInteger(0)

            tracks.mapIndexed { index, track ->
                async {
                    semaphore.withPermit {
                        val matched =
                            try {
                                matchTrack(track, index)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Throwable) {
                                reportException(error)
                                null
                            }
                        val completedCount = completed.incrementAndGet()
                        onProgress(
                            SpotifyImportProgressUi(
                                sourceTitle = sourceTitle,
                                completedSources = sourceIndex,
                                totalSources = sourceCount,
                                matchedTracks = completedCount,
                                totalTracks = tracks.size,
                                percent = progressPercent(sourceIndex, sourceCount, completedCount, tracks.size),
                            ),
                        )
                        matched
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .sortedBy { it.index }
        }

    private suspend fun matchTrack(
        track: SpotifyTrack,
        index: Int,
    ): MatchedTrack? {
        val searchResult = YouTube.search(
            query = SpotifyMapper.buildSearchQuery(track),
            filter = YouTube.SearchFilter.FILTER_SONG,
        ).getOrElse { error ->
            if (error is CancellationException) {
                throw error
            }
            return null
        }
        val candidates = searchResult.items
            .filterIsInstance<SongItem>()
            .distinctBy { it.id }

        val best = mapperMutex.withLock {
            candidates.maxByOrNull { candidate ->
                SpotifyMapper.matchScore(
                    spotifyTitle = track.name,
                    spotifyArtist = track.artists.joinToString(" ") { it.name },
                    spotifyDurationMs = track.durationMs,
                    candidateTitle = candidate.title,
                    candidateArtist = candidate.artists.joinToString(" ") { it.name },
                    candidateDurationSec = candidate.duration,
                )
            }
        } ?: return null

        return MatchedTrack(index = index, metadata = best.toMediaMetadata())
    }

    private suspend fun mirrorPlaylist(
        source: SpotifyImportSource,
        tracks: List<MediaMetadata>,
    ) {
        database.withTransaction {
            val existing = getPlaylistById(source.localPlaylistId)
            val now = LocalDateTime.now()
            val entity =
                existing?.playlist?.copy(
                    name = source.title,
                    bookmarkedAt = existing.playlist.bookmarkedAt ?: now,
                    lastUpdateTime = now,
                    thumbnailUrl = source.thumbnailUrl,
                    isEditable = true,
                ) ?: PlaylistEntity(
                    id = source.localPlaylistId,
                    name = source.title,
                    bookmarkedAt = now,
                    lastUpdateTime = now,
                    thumbnailUrl = source.thumbnailUrl,
                    isEditable = true,
                )

            if (existing == null) {
                insert(entity)
            } else {
                update(entity)
            }

            tracks.forEach { metadata ->
                insert(metadata)
            }

            clearPlaylist(source.localPlaylistId)
            tracks.forEachIndexed { index, metadata ->
                insert(
                    PlaylistSongMap(
                        playlistId = source.localPlaylistId,
                        songId = metadata.id,
                        position = index,
                        setVideoId = metadata.setVideoId,
                    ),
                )
            }
            update(entity.copy(lastUpdateTime = now))
        }
    }

    private fun progressPercent(
        completedSources: Int,
        totalSources: Int,
        completedTracks: Int,
        totalTracks: Int,
    ): Int {
        if (totalSources <= 0) return 0
        val sourceProgress =
            if (totalTracks <= 0) {
                0f
            } else {
                completedTracks.toFloat() / totalTracks.toFloat()
            }
        return (((completedSources + sourceProgress) / totalSources.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private data class SpotifyTrackPage(
        val items: List<SpotifyTrack>,
        val total: Int,
    )

    private data class MatchedTrack(
        val index: Int,
        val metadata: MediaMetadata,
    )

    companion object {
        private const val MAX_CONCURRENT_MATCHES = 4
        private const val MAX_CONCURRENT_SPOTIFY_COUNT_REQUESTS = 4
        private const val TOKEN_EXPIRY_GRACE_MS = 60_000L
    }
}

data class SpotifyImportSession(
    val isAuthenticated: Boolean = false,
    val accountName: String = "",
    val accountAvatarUrl: String? = null,
)

sealed interface SpotifyImportSource {
    val id: String
    val title: String
    val subtitle: String
    val thumbnailUrl: String?
    val trackCount: Int?
    val localPlaylistId: String
    val type: SpotifyImportSourceType

    data class Playlist(
        val playlist: SpotifyPlaylist,
    ) : SpotifyImportSource {
        val spotifyId: String = playlist.id
        override val id: String = "playlist:${playlist.id}"
        override val title: String = playlist.name
        override val subtitle: String = playlist.owner?.displayName.orEmpty()
        override val thumbnailUrl: String? = SpotifyMapper.getPlaylistThumbnail(playlist)
        override val trackCount: Int? = playlist.tracks?.total
        override val localPlaylistId: String = "SPOTIFY_PLAYLIST_${playlist.id}"
        override val type: SpotifyImportSourceType = SpotifyImportSourceType.PLAYLIST
    }

    data class LikedSongs(
        override val title: String,
        override val trackCount: Int,
    ) : SpotifyImportSource {
        override val id: String = "liked_songs"
        override val subtitle: String = ""
        override val thumbnailUrl: String? = null
        override val localPlaylistId: String = "SPOTIFY_LIKED_SONGS"
        override val type: SpotifyImportSourceType = SpotifyImportSourceType.LIKED_SONGS
    }
}

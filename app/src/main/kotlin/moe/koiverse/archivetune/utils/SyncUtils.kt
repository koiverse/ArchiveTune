package moe.koiverse.archivetune.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.constants.SelectedYtmPlaylistsKey
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.AlbumItem
import moe.koiverse.archivetune.innertube.models.ArtistItem
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.utils.completed
import moe.koiverse.archivetune.innertube.utils.parseCookieString
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.ArtistEntity
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.PlaylistSongMap
import moe.koiverse.archivetune.db.entities.SongEntity
import moe.koiverse.archivetune.models.toMediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUtils @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    
    private val syncMutex = Mutex()
    private val playlistSyncMutex = Mutex()
    private val dbWriteSemaphore = Semaphore(2)
    private val isSyncing = AtomicBoolean(false)
    
    suspend fun performFullSync() = withContext(Dispatchers.IO) {
        if (!isSyncing.compareAndSet(false, true)) {
            Timber.d("Sync already in progress, skipping")
            return@withContext
        }
        
        try {
            syncMutex.withLock {
                if (!isLoggedIn()) {
                    Timber.w("Skipping full sync - user not logged in")
                    return@withLock
                }
                
                supervisorScope {
                    listOf(
                        async { syncLikedSongs() },
                        async { syncLibrarySongs() },
                        async { syncLikedAlbums() },
                        async { syncArtistsSubscriptions() },
                    ).awaitAll()
                    
                    syncSavedPlaylists()
                    syncAutoSyncPlaylists()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during full sync")
        } finally {
            isSyncing.set(false)
        }
    }
    
    suspend fun cleanupDuplicatePlaylists() = withContext(Dispatchers.IO) {
        try {
            val allPlaylists = database.playlistsByNameAsc().first()
            val browseIdGroups = allPlaylists
                .filter { it.playlist.browseId != null }
                .groupBy { it.playlist.browseId }
            
            for ((browseId, playlists) in browseIdGroups) {
                if (playlists.size > 1) {
                    Timber.w("Found ${playlists.size} duplicate playlists for browseId: $browseId")
                    val toKeep = playlists.maxByOrNull { it.songCount }
                        ?: playlists.first()
                    
                    playlists.filter { it.id != toKeep.id }.forEach { duplicate ->
                        Timber.d("Removing duplicate playlist: ${duplicate.playlist.name} (${duplicate.id})")
                        database.clearPlaylist(duplicate.id)
                        database.delete(duplicate.playlist)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up duplicate playlists")
        }
    }
    
    /**
     * Check if user is properly logged in with a valid SAPISID cookie
     */
    private suspend fun isLoggedIn(): Boolean {
        val cookie = context.dataStore.data
            .map { it[InnerTubeCookieKey] }
            .first()
        return cookie?.let { "SAPISID" in parseCookieString(it) } ?: false
    }

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            if (!isLoggedIn()) {
                Timber.w("Skipping likeSong - user not logged in")
                return@launch
            }
            YouTube.likeVideo(s.id, s.liked)
        }
    }

    suspend fun syncLikedSongs() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLikedSongs - user not logged in")
            return@coroutineScope
        }
        YouTube.playlist("LM").completed().onSuccess { page ->
            val remoteSongs = page.songs
            val remoteIds = remoteSongs.map { it.id }
            val localSongs = database.likedSongsByNameAsc().first()

            localSongs.filterNot { it.id in remoteIds }
                .forEach { database.update(it.song.localToggleLike()) }

            val now = LocalDateTime.now()
            remoteSongs.forEachIndexed { index, song ->
                launch {
                    dbWriteSemaphore.withPermit {
                        val dbSong = database.song(song.id).firstOrNull()
                        val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                        database.withTransaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.copy(liked = true, likedDate = timestamp) }
                            } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp) {
                                update(dbSong.song.copy(liked = true, likedDate = timestamp))
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncLibrarySongs() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLibrarySongs - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_liked_videos").completed().onSuccess { page ->
            val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
            val remoteIds = remoteSongs.map { it.id }.toSet()
            val localSongs = database.songsByNameAsc().first()

            localSongs.filterNot { it.id in remoteIds }
                .forEach { database.update(it.song.toggleLibrary()) }

            remoteSongs.forEach { song ->
                launch {
                    dbWriteSemaphore.withPermit {
                        val dbSong = database.song(song.id).firstOrNull()
                        database.withTransaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.toggleLibrary() }
                            } else if (dbSong.song.inLibrary == null) {
                                update(dbSong.song.toggleLibrary())
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncLikedAlbums() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLikedAlbums - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
            val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
            val remoteIds = remoteAlbums.map { it.id }.toSet()
            val localAlbums = database.albumsLikedByNameAsc().first()

            localAlbums.filterNot { it.id in remoteIds }
                .forEach { database.update(it.album.localToggleLike()) }

            remoteAlbums.forEach { album ->
                launch {
                    dbWriteSemaphore.withPermit {
                        val dbAlbum = database.album(album.id).firstOrNull()
                        YouTube.album(album.browseId).onSuccess { albumPage ->
                            if (dbAlbum == null) {
                                database.insert(albumPage)
                                database.album(album.id).firstOrNull()?.let { newDbAlbum ->
                                    database.update(newDbAlbum.album.localToggleLike())
                                }
                            } else if (dbAlbum.album.bookmarkedAt == null) {
                                database.update(dbAlbum.album.localToggleLike())
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncArtistsSubscriptions() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncArtistsSubscriptions - user not logged in")
            return@coroutineScope
        }
        YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
            val remoteArtists = page.items.filterIsInstance<ArtistItem>()
            val remoteIds = remoteArtists.map { it.id }.toSet()
            val localArtists = database.artistsBookmarkedByNameAsc().first()

            localArtists.filterNot { it.id in remoteIds }
                .forEach { database.update(it.artist.localToggleLike()) }

            remoteArtists.forEach { artist ->
                launch {
                    dbWriteSemaphore.withPermit {
                        val dbArtist = database.artist(artist.id).firstOrNull()
                        database.withTransaction {
                            if (dbArtist == null) {
                                insert(
                                    ArtistEntity(
                                        id = artist.id,
                                        name = artist.title,
                                        thumbnailUrl = artist.thumbnail,
                                        channelId = artist.channelId,
                                    )
                                )
                            } else {
                                val existing = dbArtist.artist
                                if (existing.name != artist.title || existing.thumbnailUrl != artist.thumbnail || existing.channelId != artist.channelId) {
                                    update(
                                        existing.copy(
                                            name = artist.title,
                                            thumbnailUrl = artist.thumbnail,
                                            channelId = artist.channelId,
                                            lastUpdateTime = java.time.LocalDateTime.now()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun syncSavedPlaylists() = playlistSyncMutex.withLock {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncSavedPlaylists - user not logged in")
            return@withLock
        }
        
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
            val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" || it.id == "SE" }
                .reversed()

            val selectedCsv = context.dataStore[SelectedYtmPlaylistsKey] ?: ""
            val selectedIds = selectedCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

            val playlistsToSync = if (selectedIds.isNotEmpty()) remotePlaylists.filter { it.id in selectedIds } else remotePlaylists
            val remoteIds = playlistsToSync.map { it.id }.toSet()

            val localPlaylists = database.playlistsByNameAsc().first()
            localPlaylists.filterNot { it.playlist.browseId in remoteIds }
                .filterNot { it.playlist.browseId == null }
                .forEach { database.update(it.playlist.localToggleLike()) }

            for (playlist in playlistsToSync) {
                try {
                    val existingPlaylist = database.playlistByBrowseId(playlist.id).firstOrNull()
                    
                    val playlistEntity: PlaylistEntity
                    if (existingPlaylist == null) {
                        playlistEntity = PlaylistEntity(
                            name = playlist.title,
                            browseId = playlist.id,
                            thumbnailUrl = playlist.thumbnail,
                            isEditable = playlist.isEditable,
                            bookmarkedAt = LocalDateTime.now(),
                            remoteSongCount = playlist.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                            playEndpointParams = playlist.playEndpoint?.params,
                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                            radioEndpointParams = playlist.radioEndpoint?.params
                        )
                        database.insert(playlistEntity)
                        Timber.d("syncSavedPlaylists: Created new playlist ${playlist.title} (${playlist.id})")
                    } else {
                        playlistEntity = existingPlaylist.playlist
                        database.update(playlistEntity, playlist)
                        Timber.d("syncSavedPlaylists: Updated existing playlist ${playlist.title} (${playlist.id})")
                    }
                    
                    syncPlaylist(playlist.id, playlistEntity.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync playlist ${playlist.title}")
                }
            }
        }.onFailure { e ->
            Timber.e(e, "syncSavedPlaylists: Failed to fetch playlists from YouTube")
        }
    }

    suspend fun syncAutoSyncPlaylists() = coroutineScope {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncAutoSyncPlaylists - user not logged in")
            return@coroutineScope
        }
        val autoSyncPlaylists = database.playlistsByNameAsc().first()
            .filter { it.playlist.isAutoSync && it.playlist.browseId != null }

        Timber.d("syncAutoSyncPlaylists: Found ${autoSyncPlaylists.size} playlists to sync")

        autoSyncPlaylists.forEach { playlist ->
            launch {
                try {
                    dbWriteSemaphore.withPermit {
                        syncPlaylist(playlist.playlist.browseId!!, playlist.playlist.id)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync playlist ${playlist.playlist.name}")
                }
            }
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String) = coroutineScope {
        Timber.d("syncPlaylist: Starting sync for browseId=$browseId, playlistId=$playlistId")
        
        YouTube.playlist(browseId).completed().onSuccess { page ->
            val songs = page.songs.map(SongItem::toMediaMetadata)
            Timber.d("syncPlaylist: Fetched ${songs.size} songs from remote")

            if (songs.isEmpty()) {
                Timber.w("syncPlaylist: Remote playlist is empty, skipping sync")
                return@onSuccess
            }

            val remoteIds = songs.map { it.id }
            val localIds = database.playlistSongs(playlistId).first()
                .sortedBy { it.map.position }
                .map { it.song.id }

            if (remoteIds == localIds) {
                Timber.d("syncPlaylist: Local and remote are in sync, no changes needed")
                return@onSuccess
            }

            Timber.d("syncPlaylist: Updating local playlist (remote: ${remoteIds.size}, local: ${localIds.size})")

            database.withTransaction {
                database.clearPlaylist(playlistId)
                songs.forEachIndexed { idx, song ->
                    if (database.song(song.id).firstOrNull() == null) {
                        database.insert(song)
                    }
                    database.insert(
                        PlaylistSongMap(
                            songId = song.id,
                            playlistId = playlistId,
                            position = idx,
                            setVideoId = song.setVideoId
                        )
                    )
                }
            }
            Timber.d("syncPlaylist: Successfully synced playlist")
        }.onFailure { e ->
            Timber.e(e, "syncPlaylist: Failed to fetch playlist from YouTube")
        }
    }
}

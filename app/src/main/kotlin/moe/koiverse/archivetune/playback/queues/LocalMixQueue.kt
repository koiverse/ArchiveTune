package moe.koiverse.archivetune.playback.queues

import androidx.media3.common.MediaItem
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.PlaylistSong
import moe.koiverse.archivetune.db.entities.RelatedSongMap
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class LocalMixQueue(
    private val database: MusicDatabase,
    private val playlistId: String,
    private val maxMixSize: Int = 50,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus(): Queue.Status = withContext(Dispatchers.IO) {
        val playlistSongEntities = database.playlistSongs(playlistId).first()
        val playlistSongIds = playlistSongEntities.map { it.songId }

        val relatedSongs = playlistSongIds.flatMap { songId ->
            database.relatedSongs(songId)
        }
        val uniqueRelated = relatedSongs.filter { it.id !in playlistSongIds }.distinctBy { it.id }
        val recentEvents = database.events(limit = 100)
        val recentlyPlayedIds = recentEvents.map { it.songId }.toSet()
        val finalMix = uniqueRelated.filter { it.id !in recentlyPlayedIds }.take(maxMixSize)

        Queue.Status(
            title = "Mix from Playlist",
            items = finalMix.map { it.toMediaItem() },
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}
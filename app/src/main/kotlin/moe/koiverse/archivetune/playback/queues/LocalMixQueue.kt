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

class LocalMixQueue(
    private val database: MusicDatabase,
    private val playlistId: String,
    private val maxMixSize: Int = 50,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus(): Queue.Status = withContext(Dispatchers.IO) {
        val playlistSongs = database.playlistSongs(playlistId).first().map { it.songId }
        val relatedSongIds = database.run {
            playlistSongs.flatMap { songId ->
                delegate.dao.getRelatedSongIds(songId)
            }
        }.distinct()
        val mixSongIds = relatedSongIds.filterNot { it in playlistSongs }
        val playedSongIds = database.delegate.dao.getRecentlyPlayedSongIds().toSet()
        val finalMixIds = mixSongIds.filterNot { it in playedSongIds }.take(maxMixSize)
        val mixSongs = database.delegate.dao.getSongsByIds(finalMixIds)
        Queue.Status(
            title = "Mix from Playlist",
            items = mixSongs.map { it.toMediaItem() },
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}
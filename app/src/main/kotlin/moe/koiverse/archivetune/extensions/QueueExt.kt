/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.extensions

import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.models.PersistQueue
import moe.koiverse.archivetune.models.QueueData
import moe.koiverse.archivetune.models.QueueType
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.playback.queues.ListQueue
import moe.koiverse.archivetune.playback.queues.LocalAlbumRadio
import moe.koiverse.archivetune.playback.queues.Queue
import moe.koiverse.archivetune.playback.queues.YouTubeAlbumRadio
import moe.koiverse.archivetune.playback.queues.YouTubeQueue

fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long,
): PersistQueue = when (this) {
    is ListQueue -> PersistQueue(
        title = title,
        items = items,
        mediaItemIndex = mediaItemIndex,
        position = position,
        queueType = QueueType.LIST,
    )
    is YouTubeQueue -> PersistQueue(
        title = title,
        items = items,
        mediaItemIndex = mediaItemIndex,
        position = position,
        queueType = QueueType.YOUTUBE,
        queueData = QueueData.YouTubeData(
            videoId = endpoint.videoId,
            playlistId = endpoint.playlistId,
            endpointParams = endpoint.params,
            followAutomixPreview = followAutomixPreview,
        ),
    )
    is YouTubeAlbumRadio -> PersistQueue(
        title = title,
        items = items,
        mediaItemIndex = mediaItemIndex,
        position = position,
        queueType = QueueType.YOUTUBE_ALBUM_RADIO,
        queueData = QueueData.YouTubeAlbumRadioData(
            playlistId = playlistId,
            albumSongCount = albumSongCount,
            continuation = continuation,
            firstTimeLoaded = firstTimeLoaded,
        ),
    )
    is LocalAlbumRadio -> PersistQueue(
        title = title,
        items = items,
        mediaItemIndex = mediaItemIndex,
        position = position,
        queueType = QueueType.LOCAL_ALBUM_RADIO,
        queueData = QueueData.LocalAlbumRadioData(
            albumId = albumWithSongs.album.id,
            startIndex = startIndex,
        ),
    )
    else -> PersistQueue(
        title = title,
        items = items,
        mediaItemIndex = mediaItemIndex,
        position = position,
        queueType = QueueType.LIST,
    )
}

fun PersistQueue.toQueue(): Queue = ListQueue(
    title = title,
    items = items.map { it.toMediaItem() },
    startIndex = mediaItemIndex,
    position = position,
)

fun PersistQueue.toContinuationQueue(): Queue = when (queueType) {
    is QueueType.LIST -> ListQueue(
        title = title,
        items = items.map { it.toMediaItem() },
        startIndex = mediaItemIndex,
        position = position,
    )
    is QueueType.YOUTUBE -> {
        val data = queueData as? QueueData.YouTubeData
            ?: return ListQueue(title, items.map { it.toMediaItem() }, mediaItemIndex, position)
        YouTubeQueue(
            endpoint = WatchEndpoint(
                videoId = data.videoId,
                playlistId = data.playlistId,
                params = data.endpointParams,
            ),
            followAutomixPreview = data.followAutomixPreview,
        )
    }
    is QueueType.YOUTUBE_ALBUM_RADIO -> {
        val data = queueData as? QueueData.YouTubeAlbumRadioData
            ?: return ListQueue(title, items.map { it.toMediaItem() }, mediaItemIndex, position)
        YouTubeAlbumRadio(
            playlistId = data.playlistId,
            albumSongCount = data.albumSongCount,
            continuation = data.continuation,
            firstTimeLoaded = data.firstTimeLoaded,
        )
    }
    is QueueType.LOCAL_ALBUM_RADIO -> ListQueue(
        title = title,
        items = items.map { it.toMediaItem() },
        startIndex = mediaItemIndex,
        position = position,
    )
}

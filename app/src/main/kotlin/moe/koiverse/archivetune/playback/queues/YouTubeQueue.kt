/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package moe.koiverse.archivetune.playback.queues

import androidx.media3.common.MediaItem
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    internal var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
    internal val followAutomixPreview: Boolean = false,
    private val expandToFullQueueWhenAutoLoadMoreDisabled: Boolean = false,
) : Queue {
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult =
            withContext(IO) {
                YouTube.next(
                    endpoint = endpoint,
                    continuation = continuation,
                    followAutomixPreview = followAutomixPreview,
                ).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return Queue.Status(
            title = nextResult.title,
            items = nextResult.items.map { it.toMediaItem() },
            mediaItemIndex = nextResult.currentIndex ?: 0,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override fun shouldExpandToFullQueueWhenAutoLoadMoreDisabled(): Boolean =
        expandToFullQueueWhenAutoLoadMoreDisabled

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult =
            withContext(IO) {
                YouTube.next(
                    endpoint = endpoint,
                    continuation = continuation,
                    followAutomixPreview = followAutomixPreview,
                ).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return nextResult.items.map { it.toMediaItem() }
    }

    companion object {
        fun playlist(
            endpoint: WatchEndpoint,
            preloadItem: MediaMetadata? = null,
        ) = YouTubeQueue(
            endpoint = endpoint,
            preloadItem = preloadItem,
            expandToFullQueueWhenAutoLoadMoreDisabled = true,
        )

        fun radio(song: MediaMetadata) =
            YouTubeQueue(
                endpoint = WatchEndpoint(videoId = song.id),
                preloadItem = song,
                followAutomixPreview = true,
            )
    }
}

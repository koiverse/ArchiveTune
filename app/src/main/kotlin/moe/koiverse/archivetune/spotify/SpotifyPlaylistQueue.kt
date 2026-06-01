/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.spotify

import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.playback.queues.Queue
import moe.koiverse.archivetune.spotify.models.SpotifyTrack

class SpotifyPlaylistQueue(
    private val playlistId: String,
    private val title: String? = null,
    private val initialTracks: List<SpotifyTrack> = emptyList(),
    private val startIndex: Int = 0,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private val allTracks = mutableListOf<SpotifyTrack>()
    private var resolveOffset = 0
    private var apiFetchOffset = 0
    private var apiTotal = 0
    private var apiHasMore = true

    override suspend fun getInitialStatus(): Queue.Status =
        withContext(Dispatchers.IO) {
            if (initialTracks.isNotEmpty()) {
                allTracks += initialTracks
                apiTotal = initialTracks.size
                apiFetchOffset = apiTotal
                apiHasMore = false
            } else {
                fetchNextApiPage()
            }

            while (startIndex >= allTracks.size && apiHasMore) {
                fetchNextApiPage()
            }

            if (allTracks.isEmpty()) {
                return@withContext Queue.Status(title = title, items = emptyList(), mediaItemIndex = 0)
            }

            val targetIndex = startIndex.coerceIn(allTracks.indices)
            val windowEnd = (targetIndex + FAST_START_COUNT).coerceAtMost(allTracks.size)
            val windowTracks = allTracks.subList(targetIndex, windowEnd)
            val resolvedItems = resolveTracks(windowTracks)

            resolveOffset = windowEnd

            Queue.Status(
                title = title,
                items = resolvedItems,
                mediaItemIndex = 0,
            )
        }

    override fun hasNextPage(): Boolean =
        resolveOffset < allTracks.size || apiHasMore

    override suspend fun nextPage(): List<MediaItem> =
        withContext(Dispatchers.IO) {
            if (resolveOffset >= allTracks.size && apiHasMore) {
                fetchNextApiPage()
            }
            if (resolveOffset >= allTracks.size) return@withContext emptyList()

            val end = (resolveOffset + RESOLVE_BATCH_SIZE).coerceAtMost(allTracks.size)
            val batch = allTracks.subList(resolveOffset, end)
            resolveOffset = end
            resolveTracks(batch)
        }

    private suspend fun resolveTracks(tracks: List<SpotifyTrack>): List<MediaItem> =
        coroutineScope {
            tracks.map { track ->
                async { SpotifyPlaybackResolver.resolveToMediaItem(track) }
            }.awaitAll().filterNotNull()
        }

    private suspend fun fetchNextApiPage() {
        if (!apiHasMore) return
        val result = Spotify.playlistTracks(
            playlistId = playlistId,
            limit = SPOTIFY_PAGE_SIZE,
            offset = apiFetchOffset,
        ).getOrThrow()
        apiTotal = result.total
        val fetched = result.items.mapNotNull { it.track?.takeUnless(SpotifyTrack::isLocal) }
        allTracks += fetched
        apiFetchOffset += result.items.size
        apiHasMore = apiFetchOffset < apiTotal
    }

    companion object {
        private const val SPOTIFY_PAGE_SIZE = 50
        private const val RESOLVE_BATCH_SIZE = 20
        private const val FAST_START_COUNT = 3
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.utils

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.constants.RemoteHistoryPendingRangesKey
import moe.koiverse.archivetune.constants.RemoteHistorySyncCursorKey
import moe.koiverse.archivetune.constants.YtmSyncKey
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.innertube.PlaybackAuthState
import moe.koiverse.archivetune.innertube.YouTube

@Immutable
data class RemoteHistorySyncResult(
    val status: RemoteHistorySyncStatus,
    val syncedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val totalCount: Int = 0,
)

enum class RemoteHistorySyncStatus {
    SUCCESS,
    NOTHING_TO_SYNC,
    PARTIAL,
    NOT_LOGGED_IN,
    AUTO_SYNC_DISABLED,
    FAILED,
}

private enum class PlaybackHistoryRegistrationStatus {
    SYNCED,
    SKIPPED,
    FAILED,
}

private data class RemoteHistorySyncState(
    var cursor: Long,
    val pendingRanges: MutableList<LongRange>,
) {
    fun normalize() {
        advanceCursor()
    }

    fun isSettled(eventId: Long): Boolean {
        if (eventId <= cursor) return true
        return pendingRanges.any { eventId in it }
    }

    fun markSettled(eventId: Long) {
        if (eventId <= cursor) return

        var start = eventId
        var end = eventId
        var index = 0

        while (index < pendingRanges.size && pendingRanges[index].last + 1 < start) {
            index++
        }

        while (index < pendingRanges.size && pendingRanges[index].first - 1 <= end) {
            val range = pendingRanges.removeAt(index)
            start = minOf(start, range.first)
            end = maxOf(end, range.last)
        }

        pendingRanges.add(index, start..end)
        advanceCursor()
    }

    private fun advanceCursor() {
        while (pendingRanges.isNotEmpty()) {
            val range = pendingRanges.first()
            if (range.first > cursor + 1) break
            cursor = maxOf(cursor, range.last)
            pendingRanges.removeAt(0)
        }
    }
}

@Singleton
class RemoteHistorySyncManager @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
) {
    private val syncMutex = Mutex()

    suspend fun syncPlaybackEvent(
        mediaId: String,
        eventId: Long? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val authState = YouTube.currentPlaybackAuthState()
            if (!authState.hasLoginCookie) return@withLock false
            if (!context.dataStore.getAsync(YtmSyncKey, true)) return@withLock false

            val syncState = loadSyncState()
            if (eventId != null && syncState.isSettled(eventId)) return@withLock true

            when (registerRemoteHistory(mediaId = mediaId, authState = authState)) {
                PlaybackHistoryRegistrationStatus.SYNCED,
                PlaybackHistoryRegistrationStatus.SKIPPED,
                -> {
                    if (eventId != null) {
                        syncState.markSettled(eventId)
                        saveSyncState(syncState)
                    }
                    true
                }

                PlaybackHistoryRegistrationStatus.FAILED -> false
            }
        }
    }

    suspend fun forceSyncLocalHistory(
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): RemoteHistorySyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val authState = YouTube.currentPlaybackAuthState()
            if (!authState.hasLoginCookie) {
                return@withLock RemoteHistorySyncResult(status = RemoteHistorySyncStatus.NOT_LOGGED_IN)
            }

            val syncState = loadSyncState()
            val events = database.eventsAfter(syncState.cursor)
            val totalCount = events.count { !syncState.isSettled(it.event.id) }
            if (totalCount == 0) {
                return@withLock RemoteHistorySyncResult(status = RemoteHistorySyncStatus.NOTHING_TO_SYNC)
            }

            var syncedCount = 0
            var skippedCount = 0
            var failedCount = 0
            var completedCount = 0
            var dirtySettledCount = 0

            onProgress(0, totalCount)

            for (event in events) {
                val eventId = event.event.id
                if (syncState.isSettled(eventId)) {
                    syncState.markSettled(eventId)
                    continue
                }

                val status = if (event.song.song.isLocal) {
                    PlaybackHistoryRegistrationStatus.SKIPPED
                } else {
                    registerRemoteHistory(mediaId = event.song.id, authState = authState)
                }

                when (status) {
                    PlaybackHistoryRegistrationStatus.SYNCED -> syncedCount++
                    PlaybackHistoryRegistrationStatus.SKIPPED -> skippedCount++
                    PlaybackHistoryRegistrationStatus.FAILED -> {
                        failedCount++
                        completedCount++
                        onProgress(completedCount, totalCount)
                        break
                    }
                }

                syncState.markSettled(eventId)
                dirtySettledCount++
                completedCount++
                onProgress(completedCount, totalCount)

                if (dirtySettledCount >= 20) {
                    saveSyncState(syncState)
                    dirtySettledCount = 0
                }
            }

            if (dirtySettledCount > 0) {
                saveSyncState(syncState)
            }

            val status = when {
                failedCount > 0 && syncedCount == 0 && skippedCount == 0 -> RemoteHistorySyncStatus.FAILED
                failedCount > 0 -> RemoteHistorySyncStatus.PARTIAL
                syncedCount == 0 && skippedCount == 0 -> RemoteHistorySyncStatus.NOTHING_TO_SYNC
                else -> RemoteHistorySyncStatus.SUCCESS
            }

            RemoteHistorySyncResult(
                status = status,
                syncedCount = syncedCount,
                skippedCount = skippedCount,
                failedCount = failedCount,
                totalCount = totalCount,
            )
        }
    }

    private suspend fun registerRemoteHistory(
        mediaId: String,
        authState: PlaybackAuthState,
    ): PlaybackHistoryRegistrationStatus {
        if (database.song(mediaId).first()?.song?.isLocal == true) {
            return PlaybackHistoryRegistrationStatus.SKIPPED
        }

        val attemptedUrls = LinkedHashSet<String>()
        var hadCandidateUrl = false

        suspend fun registerTrackingUrl(url: String): Boolean {
            attemptedUrls += url
            return YouTube.registerPlayback(playbackTracking = url, authState = authState)
                .onFailure {
                    reportException(it)
                }.isSuccess
        }

        val cachedPlaybackUrl = database.format(mediaId).first()?.playbackUrl
        if (!cachedPlaybackUrl.isNullOrBlank()) {
            hadCandidateUrl = true
            if (registerTrackingUrl(cachedPlaybackUrl)) {
                return PlaybackHistoryRegistrationStatus.SYNCED
            }
        }

        val playerResponse = YTPlayerUtils.playerResponseForMetadata(mediaId, null, authState)
            .onFailure {
                reportException(it)
            }.getOrNull()
            ?: return PlaybackHistoryRegistrationStatus.FAILED

        val playbackTracking = playerResponse.playbackTracking
            ?: return PlaybackHistoryRegistrationStatus.SKIPPED

        val trackingUrls = listOfNotNull(
            playbackTracking.videostatsPlaybackUrl?.baseUrl,
            playbackTracking.videostatsWatchtimeUrl?.baseUrl,
        ).filter { it.isNotBlank() && it !in attemptedUrls }

        if (trackingUrls.isEmpty()) {
            return if (hadCandidateUrl) {
                PlaybackHistoryRegistrationStatus.FAILED
            } else {
                PlaybackHistoryRegistrationStatus.SKIPPED
            }
        }

        hadCandidateUrl = true
        for (trackingUrl in trackingUrls) {
            if (registerTrackingUrl(trackingUrl)) {
                return PlaybackHistoryRegistrationStatus.SYNCED
            }
        }

        return if (hadCandidateUrl) {
            PlaybackHistoryRegistrationStatus.FAILED
        } else {
            PlaybackHistoryRegistrationStatus.SKIPPED
        }
    }

    private suspend fun loadSyncState(): RemoteHistorySyncState {
        val cursor = context.dataStore.getAsync(RemoteHistorySyncCursorKey, 0L)
        val pendingRanges = parseRanges(context.dataStore.getAsync(RemoteHistoryPendingRangesKey, ""))
        return RemoteHistorySyncState(cursor = cursor, pendingRanges = pendingRanges).also {
            it.normalize()
        }
    }

    private suspend fun saveSyncState(state: RemoteHistorySyncState) {
        context.dataStore.edit { preferences ->
            preferences[RemoteHistorySyncCursorKey] = state.cursor

            val serializedRanges = serializeRanges(state.pendingRanges)
            if (serializedRanges.isEmpty()) {
                preferences.remove(RemoteHistoryPendingRangesKey)
            } else {
                preferences[RemoteHistoryPendingRangesKey] = serializedRanges
            }
        }
    }

    private fun parseRanges(serialized: String): MutableList<LongRange> {
        if (serialized.isBlank()) return mutableListOf()

        val mergedRanges = mutableListOf<LongRange>()
        serialized.split(',')
            .mapNotNull { token ->
                val parts = token.split('-')
                when (parts.size) {
                    1 -> parts[0].toLongOrNull()?.let { it..it }
                    2 -> {
                        val start = parts[0].toLongOrNull() ?: return@mapNotNull null
                        val end = parts[1].toLongOrNull() ?: return@mapNotNull null
                        minOf(start, end)..maxOf(start, end)
                    }

                    else -> null
                }
            }.sortedBy { it.first }
            .forEach { range ->
                val lastRange = mergedRanges.lastOrNull()
                if (lastRange == null || lastRange.last + 1 < range.first) {
                    mergedRanges += range
                } else {
                    mergedRanges[mergedRanges.lastIndex] = lastRange.first..maxOf(lastRange.last, range.last)
                }
            }

        return mergedRanges
    }

    private fun serializeRanges(ranges: List<LongRange>): String =
        ranges.joinToString(separator = ",") { range ->
            if (range.first == range.last) {
                range.first.toString()
            } else {
                "${range.first}-${range.last}"
            }
        }
}
/*
 * ArchiveTune Project Original (2026)
 * KÃ²i Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.playback

internal object PlaybackResumptionPlanner {
    data class PersistedItems<T>(
        val items: List<T>,
        val mediaItemIndex: Int,
        val positionMs: Long,
    )

    data class Result<T>(
        val items: List<T>,
        val startIndex: Int,
        val startPositionMs: Long,
    )

    fun <T> resolve(
        currentItems: List<T>,
        currentIndex: Int,
        currentPositionMs: Long,
        persistedItems: PersistedItems<T>?,
        isForPlayback: Boolean,
    ): Result<T> {
        if (currentItems.isNotEmpty()) {
            return currentItems.toResult(
                currentIndex = currentIndex,
                positionMs = currentPositionMs,
                isForPlayback = isForPlayback,
            )
        }

        val persisted = persistedItems
        if (persisted != null && persisted.items.isNotEmpty()) {
            return persisted.items.toResult(
                currentIndex = persisted.mediaItemIndex,
                positionMs = persisted.positionMs,
                isForPlayback = isForPlayback,
            )
        }

        return Result(emptyList(), 0, 0L)
    }

    private fun <T> List<T>.toResult(
        currentIndex: Int,
        positionMs: Long,
        isForPlayback: Boolean,
    ): Result<T> {
        val safeIndex = currentIndex.coerceIn(indices)
        val safePosition = positionMs.coerceAtLeast(0L)
        if (isForPlayback) {
            return Result(
                items = this,
                startIndex = safeIndex,
                startPositionMs = safePosition,
            )
        }

        return Result(
            items = listOf(this[safeIndex]),
            startIndex = 0,
            startPositionMs = safePosition,
        )
    }
}

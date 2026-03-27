package moe.koiverse.archivetune.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumptionPlannerTest {
    @Test
    fun playbackResumesFullCurrentQueue() {
        val result =
            PlaybackResumptionPlanner.resolve(
                currentItems = listOf("one", "two", "three"),
                currentIndex = 1,
                currentPositionMs = 42L,
                persistedItems = null,
                isForPlayback = true,
            )

        assertEquals(listOf("one", "two", "three"), result.items)
        assertEquals(1, result.startIndex)
        assertEquals(42L, result.startPositionMs)
    }

    @Test
    fun nonPlaybackRequestsOnlyExposeCurrentItem() {
        val result =
            PlaybackResumptionPlanner.resolve(
                currentItems = listOf("one", "two", "three"),
                currentIndex = 2,
                currentPositionMs = 99L,
                persistedItems = null,
                isForPlayback = false,
            )

        assertEquals(listOf("three"), result.items)
        assertEquals(0, result.startIndex)
        assertEquals(99L, result.startPositionMs)
    }

    @Test
    fun persistedQueueIsUsedWhenPlayerHasNoItems() {
        val result =
            PlaybackResumptionPlanner.resolve(
                currentItems = emptyList(),
                currentIndex = 0,
                currentPositionMs = 0L,
                persistedItems =
                    PlaybackResumptionPlanner.PersistedItems(
                        items = listOf("saved-1", "saved-2"),
                        mediaItemIndex = 1,
                        positionMs = 1234L,
                    ),
                isForPlayback = true,
            )

        assertEquals(listOf("saved-1", "saved-2"), result.items)
        assertEquals(1, result.startIndex)
        assertEquals(1234L, result.startPositionMs)
    }

    @Test
    fun playbackUsesPersistedQueueWhenCurrentQueueLooksLikeSingleItemSnapshot() {
        val result =
            PlaybackResumptionPlanner.resolve(
                currentItems = listOf("saved-2"),
                currentIndex = 0,
                currentPositionMs = 50L,
                persistedItems =
                    PlaybackResumptionPlanner.PersistedItems(
                        items = listOf("saved-1", "saved-2", "saved-3"),
                        mediaItemIndex = 1,
                        positionMs = 1234L,
                    ),
                isForPlayback = true,
            )

        assertEquals(listOf("saved-1", "saved-2", "saved-3"), result.items)
        assertEquals(1, result.startIndex)
        assertEquals(1234L, result.startPositionMs)
    }

    @Test
    fun playbackKeepsCurrentQueueWhenSingleItemDoesNotMatchPersistedCurrentItem() {
        val result =
            PlaybackResumptionPlanner.resolve(
                currentItems = listOf("current-only"),
                currentIndex = 0,
                currentPositionMs = 50L,
                persistedItems =
                    PlaybackResumptionPlanner.PersistedItems(
                        items = listOf("saved-1", "saved-2", "saved-3"),
                        mediaItemIndex = 1,
                        positionMs = 1234L,
                    ),
                isForPlayback = true,
            )

        assertEquals(listOf("current-only"), result.items)
        assertEquals(0, result.startIndex)
        assertEquals(50L, result.startPositionMs)
    }

    @Test
    fun invalidIndicesAndNegativePositionsAreClampedSafely() {
        val result =
            PlaybackResumptionPlanner.resolve(
                currentItems = emptyList(),
                currentIndex = 0,
                currentPositionMs = 0L,
                persistedItems =
                    PlaybackResumptionPlanner.PersistedItems(
                        items = listOf("saved-1", "saved-2"),
                        mediaItemIndex = 99,
                        positionMs = -50L,
                    ),
                isForPlayback = false,
            )

        assertEquals(listOf("saved-2"), result.items)
        assertEquals(0, result.startIndex)
        assertEquals(0L, result.startPositionMs)
    }
}

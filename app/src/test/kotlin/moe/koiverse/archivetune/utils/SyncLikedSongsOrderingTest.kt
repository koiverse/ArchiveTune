package moe.koiverse.archivetune.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SyncLikedSongsOrderingTest {
    @Test
    fun likedSongTimestamp_decrementsBySecondsPerIndex() {
        val base = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
        assertEquals(base, likedSongTimestamp(base, 0))
        assertEquals(base.minusSeconds(1), likedSongTimestamp(base, 1))
        assertEquals(base.minusSeconds(123), likedSongTimestamp(base, 123))
    }
}

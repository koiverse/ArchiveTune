package moe.koiverse.archivetune.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopMusicOnTaskClearTest {
    @Test
    fun shouldStopServiceOnTaskRemoved_stopsWhenSettingEnabled() {
        assertTrue(
            MusicService.shouldStopServiceOnTaskRemoved(
                stopMusicOnTaskClearEnabled = true,
                isHostSessionActive = false,
                isPlaybackInactive = false,
            )
        )
    }

    @Test
    fun shouldStopServiceOnTaskRemoved_stopsWhenHostSessionAndPlaybackInactive() {
        assertTrue(
            MusicService.shouldStopServiceOnTaskRemoved(
                stopMusicOnTaskClearEnabled = false,
                isHostSessionActive = true,
                isPlaybackInactive = true,
            )
        )
    }

    @Test
    fun shouldStopServiceOnTaskRemoved_doesNotStopOtherwise() {
        assertFalse(
            MusicService.shouldStopServiceOnTaskRemoved(
                stopMusicOnTaskClearEnabled = false,
                isHostSessionActive = true,
                isPlaybackInactive = false,
            )
        )
    }
}

package moe.koiverse.archivetune.together

import org.junit.Assert.assertEquals
import org.junit.Test

class TogetherGuestPlaybackPlannerTest {
    @Test
    fun planPlayTrackNow_returnsEmpty_whenControlDisabled() {
        val roomState =
            TogetherRoomState(
                sessionId = "sid",
                hostId = "hid",
                settings = TogetherRoomSettings(allowGuestsToControlPlayback = false, allowGuestsToAddTracks = true),
                queue = listOf(TogetherTrack(id = "a", title = "A")),
                currentIndex = 0,
                isPlaying = true,
            )
        val ops = TogetherGuestPlaybackPlanner.planPlayTrackNow(roomState, TogetherTrack(id = "a", title = "A"), 0L, true)
        assertEquals(emptyList<TogetherGuestOp>(), ops)
    }

    @Test
    fun planPlayTrackNow_seeks_whenTrackExistsInHostQueue() {
        val roomState =
            TogetherRoomState(
                sessionId = "sid",
                hostId = "hid",
                settings = TogetherRoomSettings(allowGuestsToControlPlayback = true, allowGuestsToAddTracks = false),
                queue = listOf(TogetherTrack(id = "a", title = "A"), TogetherTrack(id = "b", title = "B")),
                currentIndex = 0,
                isPlaying = true,
            )
        val ops = TogetherGuestPlaybackPlanner.planPlayTrackNow(roomState, TogetherTrack(id = "b", title = "B"), 123L, true)
        assertEquals(
            listOf(
                TogetherGuestOp.Control(ControlAction.SeekToTrack(trackId = "b", positionMs = 123L)),
            ),
            ops,
        )
    }

    @Test
    fun planPlayTrackNow_addsAndSkips_whenTrackMissingAndAddAllowed() {
        val roomState =
            TogetherRoomState(
                sessionId = "sid",
                hostId = "hid",
                settings = TogetherRoomSettings(allowGuestsToControlPlayback = true, allowGuestsToAddTracks = true),
                queue = listOf(TogetherTrack(id = "a", title = "A")),
                currentIndex = 0,
                isPlaying = true,
            )
        val ops = TogetherGuestPlaybackPlanner.planPlayTrackNow(roomState, TogetherTrack(id = "b", title = "B"), 0L, true)
        assertEquals(
            listOf(
                TogetherGuestOp.AddTrack(TogetherTrack(id = "b", title = "B"), AddTrackMode.PLAY_NEXT),
                TogetherGuestOp.Control(ControlAction.SkipNext),
            ),
            ops,
        )
    }

    @Test
    fun planPlayTrackNow_requestsPlay_whenHostPausedAndPlayWhenReadyTrue() {
        val roomState =
            TogetherRoomState(
                sessionId = "sid",
                hostId = "hid",
                settings = TogetherRoomSettings(allowGuestsToControlPlayback = true, allowGuestsToAddTracks = false),
                queue = listOf(TogetherTrack(id = "a", title = "A")),
                currentIndex = 0,
                isPlaying = false,
            )
        val ops = TogetherGuestPlaybackPlanner.planPlayTrackNow(roomState, TogetherTrack(id = "a", title = "A"), 0L, true)
        assertEquals(
            listOf(
                TogetherGuestOp.Control(ControlAction.SeekToTrack(trackId = "a", positionMs = 0L)),
                TogetherGuestOp.Control(ControlAction.Play),
            ),
            ops,
        )
    }
}

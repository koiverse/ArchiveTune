/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.player

import androidx.media3.common.PlaybackException
import moe.koiverse.archivetune.utils.YTPlayerUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackErrorInfoTest {
    @Test
    fun loginRecoveryExceptionMapsToConfirmationWithTargetUrl() {
        val error =
            PlaybackException(
                "Source error",
                YTPlayerUtils.LoginRequiredForPlaybackException(
                    videoId = "abc123",
                    targetUrl = "https://music.youtube.com/watch?v=abc123",
                    reason = "Login to confirm you are not a bot",
                ),
                PlaybackException.ERROR_CODE_REMOTE_ERROR,
            )

        val info = error.toPlaybackErrorInfo(currentMediaId = "abc123")

        assertEquals(PlaybackErrorKind.ConfirmationRequired, info.kind)
        assertEquals("https://music.youtube.com/watch?v=abc123", info.loginRecoveryUrl)
    }

    @Test
    fun botDetectionFallsBackToCurrentTrackUrl() {
        val error =
            PlaybackException(
                "Sign in to confirm you're not a bot",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR,
            )

        val info = error.toPlaybackErrorInfo(currentMediaId = "track42")

        assertEquals(PlaybackErrorKind.ConfirmationRequired, info.kind)
        assertEquals("https://music.youtube.com/watch?v=track42", info.loginRecoveryUrl)
    }

    @Test
    fun networkErrorsStayClassifiedAsNetworkErrors() {
        val error =
            PlaybackException(
                "No network connection",
                null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            )

        val info = error.toPlaybackErrorInfo(currentMediaId = "track42")

        assertEquals(PlaybackErrorKind.NoInternet, info.kind)
        assertNull(info.loginRecoveryUrl)
    }
}

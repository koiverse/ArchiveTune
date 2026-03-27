package moe.koiverse.archivetune.utils

import moe.koiverse.archivetune.constants.PlayerStreamClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YTPlayerUtilsClientSelectionTest {
    @Test
    fun webRemixIsPreferredWhenLoggedInAndPoTokensAreReady() {
        assertTrue(
            YTPlayerUtils.shouldPreferWebRemixForLoggedInPlayback(
                preferredStreamClient = PlayerStreamClient.ANDROID_VR,
                isLoggedIn = true,
                webClientPoTokenEnabled = true,
                hasPlayerPoToken = true,
                hasGvsPoToken = true,
            ),
        )
    }

    @Test
    fun webRemixIsNotForcedWithoutFullPoTokenPlaybackSupport() {
        assertFalse(
            YTPlayerUtils.shouldPreferWebRemixForLoggedInPlayback(
                preferredStreamClient = PlayerStreamClient.ANDROID_VR,
                isLoggedIn = true,
                webClientPoTokenEnabled = true,
                hasPlayerPoToken = true,
                hasGvsPoToken = false,
            ),
        )
    }

    @Test
    fun cipheredWebCandidatesAreAllowedWhenGvsTokenExists() {
        assertFalse(
            YTPlayerUtils.shouldSkipCipheredWebPlaybackCandidate(
                webClientPoTokenEnabled = true,
                isWebClient = true,
                isCiphered = true,
                hasGvsPoToken = true,
            ),
        )
    }

    @Test
    fun cipheredWebCandidatesAreSkippedWhenGvsTokenIsMissing() {
        assertTrue(
            YTPlayerUtils.shouldSkipCipheredWebPlaybackCandidate(
                webClientPoTokenEnabled = true,
                isWebClient = true,
                isCiphered = true,
                hasGvsPoToken = false,
            ),
        )
    }
}

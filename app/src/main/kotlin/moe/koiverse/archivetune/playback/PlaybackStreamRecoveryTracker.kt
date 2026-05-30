/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.playback

internal class PlaybackStreamRecoveryTracker {
    private var attemptedMediaId: String? = null

    fun registerRetryAttempt(mediaId: String): Boolean {
        if (attemptedMediaId == mediaId) return false
        attemptedMediaId = mediaId
        return true
    }

    fun onPlaybackRecovered(mediaId: String?) {
        if (mediaId != null && attemptedMediaId == mediaId) {
            attemptedMediaId = null
        }
    }

    fun onMediaItemChanged(currentMediaId: String?) {
        if (attemptedMediaId != currentMediaId) {
            attemptedMediaId = null
        }
    }
}

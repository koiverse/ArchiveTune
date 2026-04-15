/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
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

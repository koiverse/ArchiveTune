package moe.koiverse.archivetune.extensions

import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber

fun ExoPlayer.setOffloadEnabled(enabled: Boolean) {
    try {
        // Try to call platform API if present (some ExoPlayer versions expose this)
        val method = this::class.java.getMethod("setOffloadEnabled", Boolean::class.javaPrimitiveType)
        method.invoke(this, enabled)
    } catch (e: NoSuchMethodException) {
        // API not present — ignore silently
    } catch (t: Throwable) {
        Timber.tag("ExoPlayerExtensions").v(t, "setOffloadEnabled reflection failed")
    }
}

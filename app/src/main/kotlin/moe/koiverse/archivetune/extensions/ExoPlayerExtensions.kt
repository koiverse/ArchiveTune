/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.extensions

import androidx.media3.exoplayer.ExoPlayer
import timber.log.Timber

fun ExoPlayer.setOffloadEnabled(enabled: Boolean) {
    try {
        val method = this::class.java.getMethod("setOffloadEnabled", Boolean::class.javaPrimitiveType)
        method.invoke(this, enabled)
    } catch (e: NoSuchMethodException) {
        Timber.tag("ExoPlayerExtensions").v("setOffloadEnabled method not found")
    } catch (t: Throwable) {
        Timber.tag("ExoPlayerExtensions").v(t, "setOffloadEnabled reflection failed")
    }
}

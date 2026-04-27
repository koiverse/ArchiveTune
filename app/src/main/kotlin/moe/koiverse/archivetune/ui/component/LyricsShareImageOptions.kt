/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt
import moe.koiverse.archivetune.R

enum class LyricsShareAspectRatio(
    @StringRes val labelRes: Int,
    val exportWidth: Int,
    val exportHeight: Int,
) {
    Square(
        labelRes = R.string.lyrics_share_layout_square,
        exportWidth = 1080,
        exportHeight = 1080,
    ),
    Portrait(
        labelRes = R.string.lyrics_share_layout_portrait,
        exportWidth = 1080,
        exportHeight = 1350,
    ),
    Story(
        labelRes = R.string.lyrics_share_layout_story,
        exportWidth = 1080,
        exportHeight = 1920,
    ),
    ;

    val previewAspectRatio: Float
        get() = exportWidth.toFloat() / exportHeight.toFloat()
}

@Immutable
data class LyricsShareImageOptions(
    val aspectRatio: LyricsShareAspectRatio = LyricsShareAspectRatio.Square,
    val blurRadius: Float = 24f,
    val dimAmount: Float = 1f,
    val showArtwork: Boolean = true,
) {
    val sanitizedBlurRadius: Float
        get() = blurRadius.coerceIn(0f, 48f)

    val sanitizedDimAmount: Float
        get() = dimAmount.coerceIn(0.6f, 1.6f)

    val previewBlurRadius: Int
        get() = sanitizedBlurRadius.roundToInt().coerceIn(0, 48)
}

@Immutable
data class LyricsSharePayload(
    val lyricsText: String,
    val songTitle: String,
    val artists: String,
)

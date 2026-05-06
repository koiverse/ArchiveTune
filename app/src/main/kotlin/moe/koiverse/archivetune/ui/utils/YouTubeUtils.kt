/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.ui.utils

private const val PlayerArtworkHighResPx = 1080

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}

/**
 * Returns a high-resolution (1080 px) version of the cover-art URL.
 * Falls back to the original URL unchanged for formats that don't support
 * the Google/YouTube resize parameter scheme.
 */
fun String.highRes(): String = resize(PlayerArtworkHighResPx, PlayerArtworkHighResPx)

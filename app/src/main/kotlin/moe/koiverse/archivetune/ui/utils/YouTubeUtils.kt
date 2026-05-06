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

    // Support for various Google CDN domains (lh3-6, yt3, etc.)
    val isGoogleCdn = contains("googleusercontent.com") || contains("ggpht.com")
    val isYtimg = contains("i.ytimg.com")

    if (isGoogleCdn) {
        val w = width ?: height!!
        val h = height ?: width!!

        // Handle wNNN-hNNN pattern often used in path segments
        if (contains(Regex("w\\d+-h\\d+"))) {
            return replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
        }

        // Handle =wNNN-hNNN query-param style
        "=w(\\d+)-h(\\d+)".toRegex().find(this)?.let {
            return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
        }

        // Handle =sNNN (square) style used by yt3.ggpht.com
        if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
            return "${split("=s")[0]}=s${maxOf(w, h)}"
        }

        return this
    }

    if (isYtimg) {
        // Replace any known resolution token with maxresdefault for best quality
        val resTokens = listOf(
            "maxresdefault", "sddefault", "hqdefault", "mqdefault", "default",
            "sd1", "sd2", "sd3", "hq1", "hq2", "hq3", "mq1", "mq2", "mq3",
        )
        for (token in resTokens) {
            if (contains("$token.jpg")) {
                return replace("$token.jpg", "maxresdefault.jpg")
            }
        }
        return this
    }

    return this
}

/**
 * Returns a high-resolution (1080 px) version of the cover-art URL.
 * Falls back to the original URL unchanged for formats that don't support
 * the Google/YouTube resize parameter scheme.
 */
fun String.highRes(): String = resize(PlayerArtworkHighResPx, PlayerArtworkHighResPx)

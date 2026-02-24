/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.utils

import moe.koiverse.archivetune.innertube.models.YouTubeClient

/**
 * Shared utility for resolving the correct User-Agent and Origin/Referer headers
 * based on the `c` (client) query parameter embedded in YouTube stream URLs.
 *
 * This centralizes the logic that was previously duplicated across:
 * - [YTPlayerUtils.validateStatus]
 * - MusicService OkHttp interceptor
 * - DownloadUtil OkHttp interceptor
 */
object StreamClientUtils {

    /**
     * Resolve the correct User-Agent for a YouTube media request based on
     * the `c` query parameter from the stream URL.
     *
     * @param clientParam  the value of the `c` query parameter (e.g. "WEB_REMIX", "IOS", "ANDROID_VR")
     * @return the appropriate User-Agent string
     */
    fun resolveUserAgent(clientParam: String): String {
        val c = clientParam.trim()
        return when {
            c.equals("WEB_REMIX", ignoreCase = true) ||
                c.equals("WEB", ignoreCase = true) ||
                c.equals("WEB_CREATOR", ignoreCase = true) -> YouTubeClient.USER_AGENT_WEB

            c.equals("TVHTML5", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY_EMBEDDED_PLAYER", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY", ignoreCase = true) -> YouTubeClient.TVHTML5.userAgent

            c.startsWith("IOS", ignoreCase = true) -> YouTubeClient.IOS.userAgent

            c.startsWith("ANDROID_VR", ignoreCase = true) -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent

            c.startsWith("ANDROID_CREATOR", ignoreCase = true) -> YouTubeClient.ANDROID_CREATOR.userAgent

            c.startsWith("ANDROID", ignoreCase = true) -> YouTubeClient.MOBILE.userAgent

            c.startsWith("VISIONOS", ignoreCase = true) -> YouTubeClient.VISIONOS.userAgent

            else -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent
        }
    }

    /**
     * Data class holding Origin and Referer header values (nullable when not required).
     */
    data class OriginReferer(val origin: String?, val referer: String?)

    /**
     * Determine the correct Origin and Referer for a YouTube media request.
     * Web-type clients need YouTube Music origin; TV clients need YouTube origin.
     * Other clients (native app clients) do not need these headers.
     *
     * @param clientParam  the value of the `c` query parameter
     * @return [OriginReferer] with appropriate values, or null fields if not needed
     */
    fun resolveOriginReferer(clientParam: String): OriginReferer {
        val c = clientParam.trim()
        return when {
            c.equals("WEB_REMIX", ignoreCase = true) ||
                c.equals("WEB", ignoreCase = true) ||
                c.equals("WEB_CREATOR", ignoreCase = true) ->
                OriginReferer(YouTubeClient.ORIGIN_YOUTUBE_MUSIC, YouTubeClient.REFERER_YOUTUBE_MUSIC)

            c.equals("TVHTML5", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY_EMBEDDED_PLAYER", ignoreCase = true) ||
                c.equals("TVHTML5_SIMPLY", ignoreCase = true) ->
                OriginReferer(YouTubeClient.ORIGIN_YOUTUBE, YouTubeClient.REFERER_YOUTUBE_TV)

            else -> OriginReferer(null, null)
        }
    }

    /**
     * Check whether the given client parameter represents a web-type client
     * that requires poToken for playback requests.
     */
    fun isWebClient(clientParam: String): Boolean {
        val c = clientParam.trim()
        return c.equals("WEB", ignoreCase = true) ||
            c.equals("WEB_REMIX", ignoreCase = true) ||
            c.equals("WEB_CREATOR", ignoreCase = true) ||
            c.equals("MWEB", ignoreCase = true)
    }

    /**
     * Patch the `cver` (client version) parameter in a stream URL to match the actual
     * client version we used, preventing version mismatch 403 errors.
     *
     * @param url           the original stream URL
     * @param clientVersion the client version string that was used for the player request
     * @return the patched URL, or the original URL if no patching was needed
     */
    fun patchClientVersion(url: String, clientVersion: String): String {
        if (!url.contains("cver=")) return url
        return url.replace(Regex("cver=[^&]+"), "cver=$clientVersion")
    }

    /**
     * Append a poToken to a stream URL as the `pot` query parameter.
     *
     * @param url      the stream URL
     * @param poToken  the token to append
     * @return the URL with the `pot` parameter appended
     */
    fun appendPoToken(url: String, poToken: String): String {
        if (url.contains("pot=")) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}pot=$poToken"
    }
}

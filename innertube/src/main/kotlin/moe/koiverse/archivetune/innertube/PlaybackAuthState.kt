/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.innertube

import moe.koiverse.archivetune.innertube.models.YouTubeClient
import moe.koiverse.archivetune.innertube.utils.parseCookieString
import moe.koiverse.archivetune.innertube.utils.sha1
import java.util.Locale

data class PlaybackAuthState(
    val cookie: String? = null,
    val visitorData: String? = null,
    val dataSyncId: String? = null,
    val poToken: String? = null,
    val poTokenGvs: String? = null,
    val poTokenPlayer: String? = null,
    val webClientPoTokenEnabled: Boolean = false,
) {
    val hasLoginCookie: Boolean
        get() {
            val currentCookie = cookie ?: return false
            return "SAPISID" in parseCookieString(currentCookie)
        }

    val hasPlaybackLoginContext: Boolean
        get() = hasLoginCookie && !dataSyncId.isNullOrBlank()

    val sessionId: String?
        get() = if (hasPlaybackLoginContext) dataSyncId else visitorData

    val fingerprint: String
        get() = sha1(
            listOf(
                cookie.orEmpty(),
                visitorData.orEmpty(),
                dataSyncId.orEmpty(),
                poToken.orEmpty(),
                poTokenGvs.orEmpty(),
                poTokenPlayer.orEmpty(),
                webClientPoTokenEnabled.toString(),
            ).joinToString(separator = "\u0000")
        )

    fun normalized(): PlaybackAuthState =
        copy(
            cookie = cookie.normalizeAuthValue(),
            visitorData = visitorData.normalizeAuthValue(),
            dataSyncId = dataSyncId.normalizeDataSyncId(),
            poToken = poToken.normalizeAuthValue(),
            poTokenGvs = poTokenGvs.normalizeAuthValue(),
            poTokenPlayer = poTokenPlayer.normalizeAuthValue(),
        )

    fun resolvePlayerPoToken(
        client: YouTubeClient,
        explicitPoToken: String? = null,
    ): String? {
        val explicit = explicitPoToken.normalizeAuthValue()
        if (explicit != null) return explicit
        if (!webClientPoTokenEnabled) return null
        if (!needsServiceIntegrity(client)) return null
        return poTokenPlayer ?: poToken
    }

    fun resolveGvsPoToken(client: YouTubeClient? = null): String? {
        if (client != null && !needsServiceIntegrity(client)) return null
        if (!webClientPoTokenEnabled) return null
        return poTokenGvs ?: poToken
    }

    companion object {
        val EMPTY = PlaybackAuthState()

        internal fun needsServiceIntegrity(client: YouTubeClient): Boolean {
            val name = client.clientName.uppercase(Locale.US)
            return name == "WEB" ||
                name == "WEB_REMIX" ||
                name == "WEB_CREATOR" ||
                name == "MWEB" ||
                name == "WEB_EMBEDDED_PLAYER" ||
                name == "TVHTML5" ||
                name == "TVHTML5_SIMPLY_EMBEDDED_PLAYER" ||
                name == "TVHTML5_SIMPLY"
        }
    }
}

private fun String?.normalizeAuthValue(): String? {
    val trimmed = this?.trim()
    return trimmed?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
}

private fun String?.normalizeDataSyncId(): String? {
    val normalized = this.normalizeAuthValue() ?: return null
    return normalized.takeIf { !it.contains("||") }
        ?: normalized.takeIf { it.endsWith("||") }?.substringBefore("||")
        ?: normalized.substringAfter("||")
}

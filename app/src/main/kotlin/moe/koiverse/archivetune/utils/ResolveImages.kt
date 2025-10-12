package moe.koiverse.archivetune.utils

import android.content.Context
import com.my.kizzy.repository.KizzyRepository
import kotlinx.coroutines.withTimeoutOrNull
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.constants.*
import timber.log.Timber
import moe.koiverse.archivetune.db.entities.Song
import com.my.kizzy.rpc.RpcImage

private const val TAG = "ResolveImages"

/**
 * Resolve (and persist) large (thumbnail) and small (artist) image external URLs for a song.
 * Returns pair of (largeUrl, smallUrl) where each value is an external HTTP URL or null.
 */
suspend fun resolveAndPersistImages(context: Context, song: Song, isPaused: Boolean): Pair<String?, String?> {
    val repo = KizzyRepository()

    try {
        // Seed repo cache from saved artwork (if any)
        val saved = ArtworkStorage.findBySongId(context, song.song.id)
        if (saved != null) {
            try {
                song.song.thumbnailUrl?.let { original ->
                    if (original.isNotBlank() && !saved.thumbnail.isNullOrBlank()) {
                        repo.putToCache(original, saved.thumbnail)
                    }
                }
                song.artists.firstOrNull()?.thumbnailUrl?.let { originalArtist ->
                    if (originalArtist.isNotBlank() && !saved.artist.isNullOrBlank()) {
                        repo.putToCache(originalArtist, saved.artist)
                    }
                }
            } catch (_: Exception) {
            }
        }

        val largeImageTypePref = context.dataStore[DiscordLargeImageTypeKey] ?: "thumbnail"
        val largeImageCustomPref = context.dataStore[DiscordLargeImageCustomUrlKey] ?: ""
        val smallImageTypePref = context.dataStore[DiscordSmallImageTypeKey] ?: "artist"
        val smallImageCustomPref = context.dataStore[DiscordSmallImageCustomUrlKey] ?: ""

        fun String?.asHttp(): String? {
            if (this == null) return null
            val trimmed = this.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) return trimmed
            return null
        }

        suspend fun resolveUrlCandidate(candidate: String?): String? {
            val url = candidate?.trim()?.takeIf { it.isNotBlank() } ?: return null
            // check repo cache first
            val cached = repo.peekCache(url)
            val resolved = withTimeoutOrNull(4000L) {
                when {
                    !cached.isNullOrBlank() -> cached
                    else -> repo.getImage(url) ?: url
                }
            }
            return when {
                !resolved.isNullOrBlank() -> resolved
                url.startsWith("http://") || url.startsWith("https://") -> url
                else -> null
            }
        }

        // Choose original candidate URLs based on prefs
        val originalLargeCandidate = when (largeImageTypePref.lowercase()) {
            "thumbnail" -> song.song.thumbnailUrl
            "artist" -> song.artists.firstOrNull()?.thumbnailUrl
            "appicon" -> null
            "custom" -> largeImageCustomPref.takeIf { it.isNotBlank() } ?: song.song.thumbnailUrl
            else -> song.song.thumbnailUrl
        }

        val originalSmallCandidate = when {
            smallImageTypePref.lowercase() in listOf("none", "dontshow") -> null
            isPaused -> PAUSE_IMAGE_URL
            smallImageTypePref.lowercase() == "song" -> song.song.thumbnailUrl
            smallImageTypePref.lowercase() == "artist" -> song.artists.firstOrNull()?.thumbnailUrl
            smallImageTypePref.lowercase() == "thumbnail" || smallImageTypePref.lowercase() == "album" -> song.song.thumbnailUrl
            smallImageTypePref.lowercase() == "appicon" || smallImageTypePref.lowercase() == "app" -> null
            smallImageTypePref.lowercase() == "custom" -> smallImageCustomPref.takeIf { it.isNotBlank() } ?: song.artists.firstOrNull()?.thumbnailUrl
            else -> song.artists.firstOrNull()?.thumbnailUrl
        }

        // Try saved values first — but ignore saved values that are the pause image.
        val resolvedLargeFromSaved = saved?.thumbnail?.asHttp()?.takeIf { it != PAUSE_IMAGE_URL }
        val smallPrefLower = smallImageTypePref.lowercase()
        val allowSavedSmall = when {
            smallPrefLower in listOf("none", "dontshow", "appicon", "app") -> false
            isPaused -> false
            smallPrefLower in listOf("song", "artist", "thumbnail", "album", "custom") -> true
            else -> true
        }

        val resolvedSmallFromSaved = if (allowSavedSmall) saved?.artist?.asHttp()?.takeIf { it != PAUSE_IMAGE_URL } else null

        var finalLarge: String? = null
        var finalSmall: String? = null

        if (!resolvedLargeFromSaved.isNullOrBlank()) {
            finalLarge = resolvedLargeFromSaved
        } else {
            val candidate = originalLargeCandidate?.takeIf { it.isNotBlank() }
            finalLarge = resolveUrlCandidate(candidate)
            if (!finalLarge.isNullOrBlank() && (finalLarge.startsWith("http://") || finalLarge.startsWith("https://")) && finalLarge != PAUSE_IMAGE_URL) {
                try {
                    val updated = SavedArtwork(songId = song.song.id, thumbnail = finalLarge, artist = saved?.artist)
                    ArtworkStorage.saveOrUpdate(context, updated)
                    if (!candidate.isNullOrBlank()) repo.putToCache(candidate, finalLarge)
                } catch (e: Exception) {
                    Timber.tag(TAG).v(e, "failed to persist large image")
                }
            }
        }

        if (!resolvedSmallFromSaved.isNullOrBlank()) {
            finalSmall = resolvedSmallFromSaved
        } else {
            val candidate = originalSmallCandidate?.takeIf { it.isNotBlank() }
            finalSmall = resolveUrlCandidate(candidate)
            if (!finalSmall.isNullOrBlank() && (finalSmall.startsWith("http://") || finalSmall.startsWith("https://")) && finalSmall != PAUSE_IMAGE_URL) {
                try {
                    val updated = SavedArtwork(songId = song.song.id, thumbnail = saved?.thumbnail, artist = finalSmall)
                    ArtworkStorage.saveOrUpdate(context, updated)
                    if (!candidate.isNullOrBlank()) repo.putToCache(candidate, finalSmall)
                } catch (e: Exception) {
                    Timber.tag(TAG).v(e, "failed to persist small image")
                }
            }
        }

        Timber.tag(TAG).v("resolved images for %s -> large=%s small=%s", song.song.id, finalLarge, finalSmall)
        return finalLarge to finalSmall
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "resolveAndPersistImages failed")
        return null to null
    }
}

// Pause image constant reused from DiscordRPC companion
private const val PAUSE_IMAGE_URL = "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png"

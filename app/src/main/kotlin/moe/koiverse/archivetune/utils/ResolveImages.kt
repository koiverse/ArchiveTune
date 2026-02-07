/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

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

        // --- small image resolution ---
        val originalSmallCandidate = when {
            // user explicitly disabled small image
            smallImageTypePref.lowercase() in listOf("none", "dontshow") -> null
            // ✅ only override the small image when paused; keep large image untouched
            isPaused -> PAUSE_IMAGE_URL
            // song cover as small image
            smallImageTypePref.lowercase() == "song" -> song.song.thumbnailUrl
            // artist profile picture
            smallImageTypePref.lowercase() == "artist" -> song.artists.firstOrNull()?.thumbnailUrl
            // album cover
            smallImageTypePref.lowercase() == "thumbnail" || smallImageTypePref.lowercase() == "album" -> song.song.thumbnailUrl
            // app icon (handled later as Discord internal asset, so no HTTP here)
            smallImageTypePref.lowercase() == "appicon" || smallImageTypePref.lowercase() == "app" -> null
            // ✅ handle custom URLs correctly
            smallImageTypePref.lowercase() == "custom" ->
                smallImageCustomPref.takeIf { it.isNotBlank() } ?: song.artists.firstOrNull()?.thumbnailUrl
            // fallback: artist image
            else -> song.artists.firstOrNull()?.thumbnailUrl
        }


        // Determine which saved field to use based on what type of source the large image is
        val largePrefLower = largeImageTypePref.lowercase()
        val isLargeFromArtist = largePrefLower == "artist"
        val resolvedLargeFromSaved = when {
            isLargeFromArtist -> saved?.artist?.asHttp()?.takeIf { it != PAUSE_IMAGE_URL }
            else -> saved?.thumbnail?.asHttp()?.takeIf { it != PAUSE_IMAGE_URL }
        }
        
        val smallPrefLower = smallImageTypePref.lowercase()
        val allowSavedSmall = when {
            smallPrefLower in listOf("none", "dontshow", "appicon", "app") -> false
            isPaused -> false
            smallPrefLower in listOf("song", "artist", "thumbnail", "album") -> true
            else -> true
        }

        // Determine which saved field to use based on what type of source the small image is
        val isSmallFromArtist = smallPrefLower == "artist"
        val resolvedSmallFromSaved = if (allowSavedSmall) {
            when {
                isSmallFromArtist -> saved?.artist?.asHttp()?.takeIf { it != PAUSE_IMAGE_URL }
                else -> saved?.thumbnail?.asHttp()?.takeIf { it != PAUSE_IMAGE_URL }
            }
        } else null

        var finalLarge: String? = null
        var finalSmall: String? = null
        
        // Track current saved values to ensure we don't lose data when saving both images
        var currentSavedThumbnail = saved?.thumbnail
        var currentSavedArtist = saved?.artist

        if (!resolvedLargeFromSaved.isNullOrBlank()) {
            finalLarge = resolvedLargeFromSaved
        } else {
            val candidate = originalLargeCandidate?.takeIf { it.isNotBlank() }
            finalLarge = resolveUrlCandidate(candidate)
            if (!finalLarge.isNullOrBlank() && (finalLarge.startsWith("http://") || finalLarge.startsWith("https://")) && finalLarge != PAUSE_IMAGE_URL) {
                try {
                    // Save to the correct field based on image source type and update tracking variables
                    if (isLargeFromArtist) {
                        currentSavedArtist = finalLarge
                        val updated = SavedArtwork(songId = song.song.id, thumbnail = currentSavedThumbnail, artist = finalLarge)
                        ArtworkStorage.saveOrUpdate(context, updated)
                    } else {
                        currentSavedThumbnail = finalLarge
                        val updated = SavedArtwork(songId = song.song.id, thumbnail = finalLarge, artist = currentSavedArtist)
                        ArtworkStorage.saveOrUpdate(context, updated)
                    }
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
                    // Save to the correct field based on image source type using tracked values
                    if (isSmallFromArtist) {
                        currentSavedArtist = finalSmall
                        val updated = SavedArtwork(songId = song.song.id, thumbnail = currentSavedThumbnail, artist = finalSmall)
                        ArtworkStorage.saveOrUpdate(context, updated)
                    } else {
                        currentSavedThumbnail = finalSmall
                        val updated = SavedArtwork(songId = song.song.id, thumbnail = finalSmall, artist = currentSavedArtist)
                        ArtworkStorage.saveOrUpdate(context, updated)
                    }
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

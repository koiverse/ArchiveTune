/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.utils

import android.content.Context
import com.my.kizzy.repository.KizzyRepository
import com.my.kizzy.rpc.RpcImage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import moe.koiverse.archivetune.db.entities.Song
import timber.log.Timber

data class ResolvedDiscordImages(
    val thumbnailOriginalUrl: String?,
    val thumbnailResolvedId: String?,
    val artistOriginalUrl: String?,
    val artistResolvedId: String?
)

object DiscordImageResolver {
    private const val TAG = "DiscordImageResolver"
    private const val RESOLUTION_TIMEOUT_MS = 8000L
    private const val SINGLE_IMAGE_TIMEOUT_MS = 5000L

    private val repository = KizzyRepository()

    private var cachedSongId: String? = null
    private var cachedImages: ResolvedDiscordImages? = null

    @Synchronized
    fun getCachedImages(songId: String): ResolvedDiscordImages? {
        return if (cachedSongId == songId) cachedImages else null
    }

    @Synchronized
    private fun setCachedImages(songId: String, images: ResolvedDiscordImages) {
        cachedSongId = songId
        cachedImages = images
    }

    @Synchronized
    fun clearCache() {
        cachedSongId = null
        cachedImages = null
    }

    suspend fun resolveImagesForSong(
        context: Context,
        song: Song
    ): ResolvedDiscordImages = coroutineScope {
        val songId = song.song.id

        getCachedImages(songId)?.let { cached ->
            Timber.tag(TAG).d("Using cached images for song: %s", songId)
            return@coroutineScope cached
        }

        val thumbnailUrl = song.song.thumbnailUrl?.takeIf { it.isNotBlank() && it.isValidHttpUrl() }
        val artistUrl = song.artists.firstOrNull()?.thumbnailUrl?.takeIf { it.isNotBlank() && it.isValidHttpUrl() }

        Timber.tag(TAG).d("Resolving images for song: %s, thumbnail=%s, artist=%s", songId, thumbnailUrl, artistUrl)

        val savedArtwork = ArtworkStorage.findBySongId(context, songId)
        
        var thumbnailResolved: String? = savedArtwork?.thumbnail?.takeIf { it.isNotBlank() && it.isResolvedId() }
        var artistResolved: String? = savedArtwork?.artist?.takeIf { it.isNotBlank() && it.isResolvedId() }

        if (thumbnailResolved != null) {
            thumbnailUrl?.let { repository.putToCache(it, thumbnailResolved!!) }
        }
        if (artistResolved != null) {
            artistUrl?.let { repository.putToCache(it, artistResolved!!) }
        }

        val result = withTimeoutOrNull(RESOLUTION_TIMEOUT_MS) {
            val thumbnailDeferred = if (thumbnailResolved == null && thumbnailUrl != null) {
                async { resolveImageUrl(thumbnailUrl) }
            } else null

            val artistDeferred = if (artistResolved == null && artistUrl != null) {
                async { resolveImageUrl(artistUrl) }
            } else null

            val newThumbnailResolved = thumbnailDeferred?.await()
            val newArtistResolved = artistDeferred?.await()

            if (newThumbnailResolved != null) {
                thumbnailResolved = newThumbnailResolved
                Timber.tag(TAG).d("Thumbnail resolved: %s -> %s", thumbnailUrl, newThumbnailResolved)
            }
            if (newArtistResolved != null) {
                artistResolved = newArtistResolved
                Timber.tag(TAG).d("Artist resolved: %s -> %s", artistUrl, newArtistResolved)
            }

            ResolvedDiscordImages(
                thumbnailOriginalUrl = thumbnailUrl,
                thumbnailResolvedId = thumbnailResolved,
                artistOriginalUrl = artistUrl,
                artistResolvedId = artistResolved
            )
        } ?: ResolvedDiscordImages(
            thumbnailOriginalUrl = thumbnailUrl,
            thumbnailResolvedId = thumbnailResolved,
            artistOriginalUrl = artistUrl,
            artistResolvedId = artistResolved
        )

        if (result.thumbnailResolvedId != null || result.artistResolvedId != null) {
            try {
                val artwork = SavedArtwork(
                    songId = songId,
                    thumbnail = result.thumbnailResolvedId,
                    artist = result.artistResolvedId
                )
                ArtworkStorage.saveOrUpdate(context, artwork)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to save artwork")
            }
        }

        setCachedImages(songId, result)

        Timber.tag(TAG).i(
            "Final resolved images for %s: thumbnail=%s, artist=%s",
            songId,
            result.thumbnailResolvedId?.take(30),
            result.artistResolvedId?.take(30)
        )

        result
    }

    private suspend fun resolveImageUrl(url: String): String? {
        return withTimeoutOrNull(SINGLE_IMAGE_TIMEOUT_MS) {
            try {
                repository.peekCache(url)?.let { cached ->
                    Timber.tag(TAG).v("Cache hit for %s", url)
                    return@withTimeoutOrNull cached
                }

                val resolved = repository.getImage(url)
                if (resolved != null) {
                    Timber.tag(TAG).v("Resolved %s -> %s", url, resolved)
                    resolved
                } else {
                    Timber.tag(TAG).w("Failed to resolve image: %s", url)
                    null
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error resolving image: %s", url)
                null
            }
        }
    }

    fun buildRpcImage(
        imageType: String,
        customUrl: String?,
        resolvedImages: ResolvedDiscordImages,
        song: Song
    ): RpcImage? {
        return when (imageType.lowercase()) {
            "thumbnail", "song", "album" -> {
                resolvedImages.thumbnailResolvedId?.let { createRpcImageFromId(it) }
                    ?: resolvedImages.thumbnailOriginalUrl?.let { RpcImage.ExternalImage(it) }
                    ?: song.song.thumbnailUrl?.takeIf { it.isValidHttpUrl() }?.let { RpcImage.ExternalImage(it) }
            }
            "artist" -> {
                resolvedImages.artistResolvedId?.let { createRpcImageFromId(it) }
                    ?: resolvedImages.artistOriginalUrl?.let { RpcImage.ExternalImage(it) }
                    ?: song.artists.firstOrNull()?.thumbnailUrl?.takeIf { it.isValidHttpUrl() }?.let { RpcImage.ExternalImage(it) }
            }
            "appicon" -> {
                RpcImage.ExternalImage("https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/icon.png")
            }
            "custom" -> {
                val url = customUrl?.takeIf { it.isNotBlank() && it.isValidHttpUrl() }
                    ?: resolvedImages.thumbnailOriginalUrl
                    ?: song.song.thumbnailUrl?.takeIf { it.isValidHttpUrl() }
                url?.let { RpcImage.ExternalImage(it) }
            }
            "dontshow", "none" -> null
            else -> {
                resolvedImages.thumbnailResolvedId?.let { createRpcImageFromId(it) }
                    ?: resolvedImages.thumbnailOriginalUrl?.let { RpcImage.ExternalImage(it) }
            }
        }
    }
    
    private fun createRpcImageFromId(id: String): RpcImage? {
        if (id.isBlank()) return null
        return when {
            id.startsWith("mp:") -> RpcImage.DiscordImage(id.removePrefix("mp:"))
            id.startsWith("external/") -> RpcImage.DiscordImage(id)
            id.startsWith("attachments/") -> RpcImage.DiscordImage(id)
            id.startsWith("http") -> RpcImage.ExternalImage(id)
            else -> RpcImage.DiscordImage(id)
        }
    }

    private fun String.isValidHttpUrl(): Boolean {
        return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
    }

    private fun String.isResolvedId(): Boolean {
        return startsWith("mp:") || startsWith("b7.") || 
               (startsWith("http") && contains("discord"))
    }
}

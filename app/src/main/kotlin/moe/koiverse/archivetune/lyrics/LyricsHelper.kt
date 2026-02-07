/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import moe.koiverse.archivetune.utils.GlobalLog
import moe.koiverse.archivetune.constants.PreferredLyricsProvider
import moe.koiverse.archivetune.constants.PreferredLyricsProviderKey
import moe.koiverse.archivetune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import moe.koiverse.archivetune.extensions.toEnum
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.reportException
import moe.koiverse.archivetune.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val baseProviders =
        listOf(
            SimpMusicLyricsProvider,
            BetterLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider,
        )

    private var lyricsProviders =
        baseProviders

    val preferred =
        context.dataStore.data
            .map {
                it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
            }.distinctUntilChanged()
            .map {
                val first =
                    when (it) {
                        PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
                        PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
                        PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
                        PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
                    }

                lyricsProviders = listOf(first) + baseProviders.filterNot { provider -> provider == first }
            }

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata): String {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
            return cached.lyrics
        }
        
        GlobalLog.append(Log.DEBUG, "LyricsHelper", "Fetching lyrics for ${mediaMetadata.title} (Artist: ${mediaMetadata.artists.joinToString { it.name }}, Album: ${mediaMetadata.album?.title})")

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }
        
        if (!isNetworkAvailable) {
            GlobalLog.append(Log.WARN, "LyricsHelper", "Network unavailable, aborting lyrics fetch")
            // Still proceed but return not found to avoid hanging
            return LYRICS_NOT_FOUND
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val deferred = scope.async {
            for (provider in lyricsProviders) {
                val enabled = provider.isEnabled(context)
                
                if (enabled) {
                    try {
                        val result = provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.album?.title,
                            mediaMetadata.duration,
                        )
                        result.onSuccess { lyrics ->
                            if (isMeaningfulLyrics(lyrics)) {
                                return@async lyrics
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            return@async LYRICS_NOT_FOUND
        }

        val lyrics = deferred.await()
        scope.cancel()
        return lyrics
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        songAlbum: String?,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            lyricsProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) lyricsCallback@{ lyrics ->
                            if (!isMeaningfulLyrics(lyrics)) return@lyricsCallback
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized =
            lyrics
                .replace("\uFEFF", "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        if (normalized == LYRICS_NOT_FOUND) return false

        val remaining =
            TIMESTAMP_REGEX
                .replace(normalized, "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

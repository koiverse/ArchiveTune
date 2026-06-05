/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import moe.koiverse.archivetune.utils.GlobalLog
import moe.koiverse.archivetune.constants.LyricsProviderOrderKey
import moe.koiverse.archivetune.constants.PreferredLyricsProvider
import moe.koiverse.archivetune.constants.deserializeLyricsProviderOrder
import moe.koiverse.archivetune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.reportException
import moe.koiverse.archivetune.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.selects.select
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val baseProviders =
        listOf(
            BetterLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            SimpMusicLyricsProvider,
            UnisonLyricsProvider,
            PaxsenixAppleMusicLyricsProvider,
            PaxsenixNeteaseLyricsProvider,
            PaxsenixSpotifyLyricsProvider,
            PaxsenixMusixmatchLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider,
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private val singleLyricsCache = LruCache<String, LyricsFetchResult>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): String =
        getLyricsResult(mediaMetadata, preferredProviderOnly).lyrics

    suspend fun getLyricsResult(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): LyricsFetchResult {
        val cacheKey = mediaMetadata.lyricsCacheKey
        singleLyricsCache.get(cacheKey)?.let { result ->
            GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
            return result
        }

        val cached = cache.get(cacheKey)?.firstOrNull()
        if (cached != null) {
            GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
            return LyricsFetchResult(cached.lyrics, cached.providerName)
        }
        
        GlobalLog.append(Log.DEBUG, "LyricsHelper", "Fetching lyrics for ${mediaMetadata.title} (Artist: ${mediaMetadata.artists.joinToString { it.name }}, Album: ${mediaMetadata.album?.title})")

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            GlobalLog.append(Log.WARN, "LyricsHelper", "Network unavailable, aborting lyrics fetch")
            return LyricsFetchResult(LYRICS_NOT_FOUND, null)
        }

        val ordered = orderedProviders().filter { it.isEnabled(context) }
        val providers = if (preferredProviderOnly) ordered.take(1) else ordered
        val result = fetchPriorityLyrics(providers, mediaMetadata)
        if (isMeaningfulLyrics(result.lyrics)) {
            singleLyricsCache.put(cacheKey, result)
        }

        return result
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

        val cacheKey = lyricsCacheKey(songTitle, songArtists)
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
        val providers = orderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
            providers.forEach { provider ->
                if (!provider.isEnabled(context)) return@forEach

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
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    private suspend fun fetchPriorityLyrics(
        providers: List<LyricsProvider>,
        mediaMetadata: MediaMetadata,
    ): LyricsFetchResult {
        if (providers.isEmpty()) return LyricsFetchResult(LYRICS_NOT_FOUND, null)

        val artist = mediaMetadata.artists.joinToString { it.name }
        val priorityProvider = providers.first()
        fetchProviderLyrics(priorityProvider, mediaMetadata, artist)?.let { lyrics ->
            return LyricsFetchResult(lyrics, priorityProvider.name)
        }

        return fetchFirstMeaningfulLyrics(providers.drop(1), mediaMetadata, artist)
    }

    private suspend fun fetchFirstMeaningfulLyrics(
        providers: List<LyricsProvider>,
        mediaMetadata: MediaMetadata,
        artist: String,
    ): LyricsFetchResult = supervisorScope {
        val requests =
            providers
                .map { provider ->
                    async(Dispatchers.IO) {
                        fetchProviderLyrics(provider, mediaMetadata, artist)?.let { lyrics ->
                            LyricsFetchResult(lyrics, provider.name)
                        }
                    }
                }

        if (requests.isEmpty()) return@supervisorScope LyricsFetchResult(LYRICS_NOT_FOUND, null)

        val pending = requests.toMutableSet()
        while (pending.isNotEmpty()) {
            val (request, result) = select<Pair<Deferred<LyricsFetchResult?>, LyricsFetchResult?>> {
                pending.forEach { deferred ->
                    deferred.onAwait { result -> deferred to result }
                }
            }
            pending.remove(request)
            if (result != null) {
                pending.forEach { it.cancel() }
                return@supervisorScope result
            }
        }

        LyricsFetchResult(LYRICS_NOT_FOUND, null)
    }

    private suspend fun fetchProviderLyrics(
        provider: LyricsProvider,
        mediaMetadata: MediaMetadata,
        artist: String,
    ): String? {
        return try {
            provider.getLyrics(
                mediaMetadata.id,
                mediaMetadata.title,
                artist,
                mediaMetadata.album?.title,
                mediaMetadata.duration,
            ).fold(
                onSuccess = { lyrics ->
                    lyrics.takeIf(::isMeaningfulLyrics)
                },
                onFailure = {
                    reportException(it)
                    null
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportException(e)
            null
        }
    }

    private suspend fun orderedProviders(): List<LyricsProvider> {
        val orderStr = context.dataStore.data.first()[LyricsProviderOrderKey]
        val orderedEnums = deserializeLyricsProviderOrder(orderStr)
        val providerMap: Map<PreferredLyricsProvider, LyricsProvider> = mapOf(
            PreferredLyricsProvider.LRCLIB to LrcLibLyricsProvider,
            PreferredLyricsProvider.KUGOU to KuGouLyricsProvider,
            PreferredLyricsProvider.BETTER_LYRICS to BetterLyricsProvider,
            PreferredLyricsProvider.SIMPMUSIC to SimpMusicLyricsProvider,
            PreferredLyricsProvider.PAXSENIX_APPLE_MUSIC to PaxsenixAppleMusicLyricsProvider,
            PreferredLyricsProvider.PAXSENIX_NETEASE to PaxsenixNeteaseLyricsProvider,
            PreferredLyricsProvider.PAXSENIX_SPOTIFY to PaxsenixSpotifyLyricsProvider,
            PreferredLyricsProvider.PAXSENIX_MUSIXMATCH to PaxsenixMusixmatchLyricsProvider,
            PreferredLyricsProvider.UNISON to UnisonLyricsProvider,
        )
        val userOrdered = orderedEnums.mapNotNull { providerMap[it] }
        val rest = baseProviders.filterNot { it in userOrdered }
        return userOrdered + rest
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

    fun clearCache() {
        cache.evictAll()
        singleLyricsCache.evictAll()
    }

    private val MediaMetadata.lyricsCacheKey: String
        get() = lyricsCacheKey(
            title = title,
            artists = artists.joinToString { it.name },
        )

    private fun lyricsCacheKey(
        title: String,
        artists: String,
    ): String = "$artists-$title".replace(" ", "")

    companion object {
        private const val MAX_CACHE_SIZE = 16
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsFetchResult(
    val lyrics: String,
    val providerName: String?,
)

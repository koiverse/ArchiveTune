/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.playback

import android.content.Context
import android.media.MediaCodecList
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import moe.koiverse.archivetune.constants.AudioQuality
import moe.koiverse.archivetune.constants.AudioQualityKey
import moe.koiverse.archivetune.constants.NetworkMeteredKey
import moe.koiverse.archivetune.constants.PlayerStreamClient
import moe.koiverse.archivetune.constants.PlayerStreamClientKey
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.FormatEntity
import moe.koiverse.archivetune.db.entities.SongEntity
import moe.koiverse.archivetune.di.DownloadCache
import moe.koiverse.archivetune.di.PlayerCache
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.utils.AuthScopedCacheValue
import moe.koiverse.archivetune.utils.StreamClientUtils
import moe.koiverse.archivetune.utils.YTPlayerUtils
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.enumPreference
import moe.koiverse.archivetune.utils.get
import moe.koiverse.archivetune.utils.retryWithoutPlaybackLoginContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: Cache,
    @PlayerCache val playerCache: Cache,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val preferredStreamClient by enumPreference(context, PlayerStreamClientKey, PlayerStreamClient.ANDROID_VR)
    private val songUrlCache = ConcurrentHashMap<String, AuthScopedCacheValue>()
    private val downloadExecutor = Executors.newFixedThreadPool(DEFAULT_MAX_PARALLEL_DOWNLOADS)
    private val streamInfoRequestLimiter = Semaphore(MAX_CONCURRENT_STREAM_INFO_REQUESTS)
    private val streamInfoSpacingMutex = Mutex()
    private val consecutiveThrottleSignals = AtomicInteger(0)

    @Volatile
    private var currentMaxParallelDownloads = DEFAULT_MAX_PARALLEL_DOWNLOADS

    @Volatile
    private var cooldownUntilMs = 0L

    @Volatile
    private var lastStreamInfoRequestAtMs = 0L

    private val mediaOkHttpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .proxy(YouTube.streamProxy)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("youtube.com") ||
                        host.endsWith("youtube-nocookie.com") ||
                        host.endsWith("ytimg.com")

                if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

                val clientParam = request.url.queryParameter("c")?.trim().orEmpty()

                val userAgent = StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

                val builder = request.newBuilder().header("User-Agent", userAgent)
                originReferer.origin?.let { builder.header("Origin", it) }
                originReferer.referer?.let { builder.header("Referer", it) }

                chain.proceed(builder.build())
            }.build()
    }

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        mediaOkHttpClient,
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1
            if (playerCache.cacheSpace > 500 * 1024 * 1024L) {
                GlobalScope.launch(Dispatchers.IO) {
                    playerCache.keys.shuffled().take(10).forEach { key ->
                        playerCache.getCachedSpans(key).sumOf { it.length }
                    }
                }
            }
            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }
            val authFingerprint = YouTube.currentPlaybackAuthState().fingerprint
            songUrlCache[mediaId]?.takeIf { it.isValidFor(authFingerprint) }?.let {
                return@Factory dataSpec.withUri(it.url.toUri())
            }
            val playbackData = runBlocking(Dispatchers.IO) {
                streamInfoRequestLimiter.withPermit {
                    awaitStreamInfoCooldown()
                    spaceOutStreamInfoRequests()

                    val networkMeteredPref = context.dataStore.get(NetworkMeteredKey, true)
                    val result =
                        context.retryWithoutPlaybackLoginContext {
                            YTPlayerUtils.playerResponseForPlayback(
                                mediaId,
                                audioQuality = audioQuality,
                                preferredStreamClient = preferredStreamClient,
                                connectivityManager = connectivityManager,
                                networkMetered = networkMeteredPref,
                            )
                        }

                    if (result.isSuccess) {
                        clearThrottleSignal()
                    } else {
                        registerThrottleSignal(result.exceptionOrNull())
                    }

                    result
                }
            }.getOrThrow()
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl,
                    ),
                )

                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(mediaId)?.song

                val updatedSong = if (existing != null) {
                    if (existing.dateDownload == null) existing.copy(dateDownload = now) else existing
                } else {
                    SongEntity(
                        id = mediaId,
                        title = playbackData.videoDetails?.title ?: "Unknown",
                        duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                        dateDownload = now,
                    )
                }

                upsert(updatedSong)
            }

            val streamUrl = playbackData.streamUrl

            songUrlCache[mediaId] =
                AuthScopedCacheValue(
                    url = streamUrl,
                    expiresAtMs = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L),
                    authFingerprint = playbackData.authFingerprint,
                )
            dataSpec.withUri(streamUrl.toUri())
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            downloadExecutor,
        ).apply {
            maxParallelDownloads = currentMaxParallelDownloads
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        if (download.state == Download.STATE_FAILED) {
                            registerThrottleSignal(finalException)
                        } else if (download.state == Download.STATE_COMPLETED) {
                            clearThrottleSignal()
                        }

                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }
                    }
                },
            )
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val result = mutableMapOf<String, Download>()
            val cursor = downloadManager.downloadIndex.getDownloads()
            while (cursor.moveToNext()) {
                result[cursor.download.request.id] = cursor.download
            }
            downloads.value = result
        }
        CoroutineScope(Dispatchers.IO).launch {
            var previousFingerprint: String? = null
            YouTube.authStateFlow
                .map { it.fingerprint }
                .distinctUntilChanged()
                .collect { fingerprint ->
                    if (previousFingerprint != null && previousFingerprint != fingerprint) {
                        songUrlCache.clear()
                    }
                    previousFingerprint = fingerprint
                }
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    private suspend fun awaitStreamInfoCooldown() {
        val remainingMs = cooldownUntilMs - System.currentTimeMillis()
        if (remainingMs > 0) {
            delay(remainingMs)
        }
    }

    private suspend fun spaceOutStreamInfoRequests() {
        streamInfoSpacingMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsedMs = now - lastStreamInfoRequestAtMs
            val waitMs = STREAM_INFO_REQUEST_SPACING_MS - elapsedMs
            if (waitMs > 0) {
                delay(waitMs)
            }
            lastStreamInfoRequestAtMs = System.currentTimeMillis()
        }
    }

    private fun registerThrottleSignal(exception: Throwable?) {
        if (exception is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException && exception.responseCode == 403) {
            val urlStr = exception.dataSpec.uri.toString()
            val videoId = urlStr.toHttpUrlOrNull()?.queryParameter("docid") ?: urlStr.toHttpUrlOrNull()?.queryParameter("id")
            val clientParam = urlStr.toHttpUrlOrNull()?.queryParameter("c")?.trim()
            if (videoId != null && clientParam != null) {
                YTPlayerUtils.markStreamClientFailed(videoId, clientParam, exception.responseCode)
            }
        }
        
        val nextStrikeCount =
            if (exception == null || isProbablyThrottleSignal(exception)) {
                consecutiveThrottleSignals.incrementAndGet()
            } else {
                consecutiveThrottleSignals.updateAndGet { strikes -> maxOf(1, strikes) }
            }

        val reducedParallelDownloads =
            when {
                nextStrikeCount >= 4 -> MIN_PARALLEL_DOWNLOADS
                nextStrikeCount >= 2 -> DEFAULT_MAX_PARALLEL_DOWNLOADS - 1
                else -> currentMaxParallelDownloads
            }.coerceIn(MIN_PARALLEL_DOWNLOADS, DEFAULT_MAX_PARALLEL_DOWNLOADS)

        val cooldownMs =
            when {
                nextStrikeCount >= 4 -> LONG_COOLDOWN_MS
                nextStrikeCount >= 2 -> SHORT_COOLDOWN_MS
                else -> 0L
            }

        if (reducedParallelDownloads != currentMaxParallelDownloads) {
            currentMaxParallelDownloads = reducedParallelDownloads
            downloadManager.maxParallelDownloads = reducedParallelDownloads
        }

        if (cooldownMs > 0) {
            cooldownUntilMs = maxOf(cooldownUntilMs, System.currentTimeMillis() + cooldownMs)
        }
    }

    private fun clearThrottleSignal() {
        val remainingStrikes = consecutiveThrottleSignals.updateAndGet { strikes ->
            if (strikes > 0) strikes - 1 else 0
        }

        if (remainingStrikes == 0 && currentMaxParallelDownloads != DEFAULT_MAX_PARALLEL_DOWNLOADS) {
            currentMaxParallelDownloads = DEFAULT_MAX_PARALLEL_DOWNLOADS
            downloadManager.maxParallelDownloads = DEFAULT_MAX_PARALLEL_DOWNLOADS
        }
    }

    private fun isProbablyThrottleSignal(exception: Throwable): Boolean {
        val message = buildString {
            append(exception.message.orEmpty())
            exception.cause?.message?.let {
                if (isNotBlank()) append(' ')
                append(it)
            }
        }.lowercase()

        return listOf(
            "429",
            "403",
            "quota",
            "rate",
            "too many",
            "temporarily unavailable",
            "timed out",
            "timeout",
            "unavailable",
            "reset by peer",
        ).any(message::contains)
    }

    companion object {
        private const val DEFAULT_MAX_PARALLEL_DOWNLOADS = 4
        private const val MIN_PARALLEL_DOWNLOADS = 2
        private const val MAX_CONCURRENT_STREAM_INFO_REQUESTS = 2
        private const val STREAM_INFO_REQUEST_SPACING_MS = 350L
        private const val SHORT_COOLDOWN_MS = 2_500L
        private const val LONG_COOLDOWN_MS = 8_000L
    }
}

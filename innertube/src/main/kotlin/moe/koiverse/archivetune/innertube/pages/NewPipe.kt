/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.innertube

import moe.koiverse.archivetune.innertube.PlaybackAuthState
import moe.koiverse.archivetune.innertube.models.YouTubeClient
import moe.koiverse.archivetune.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)

        var hasUserAgent = false
        headers.forEach { (headerName, headerValueList) ->
            if (headerName.equals("User-Agent", ignoreCase = true) && headerValueList.isNotEmpty()) {
                hasUserAgent = true
            }
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        if (!hasUserAgent) {
            requestBuilder.header("User-Agent", YouTubeClient.USER_AGENT_WEB)
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body.string()

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }

}

object NewPipeUtils {

    enum class ExternalAudioService {
        BANDCAMP,
        SOUNDCLOUD,
    }

    data class ExternalAudioQuery(
        val title: String,
        val artists: List<String>,
        val durationSeconds: Int?,
    )

    data class ExternalAudioStream(
        val source: ExternalAudioService,
        val streamUrl: String,
        val durationSeconds: Long,
        val mimeType: String,
        val bitrate: Int,
        val averageBitrate: Int,
        val quality: String?,
        val itag: Int,
    )

    init {
        NewPipe.init(NewPipeDownloaderImpl(YouTube.proxy))
    }

    fun getHiResLosslessAudioStream(query: ExternalAudioQuery): Result<ExternalAudioStream> =
        runCatching {
            val normalizedQuery = query.normalized()
            val errors = mutableListOf<Throwable>()
            val services =
                listOf(
                    ServiceList.Bandcamp to ExternalAudioService.BANDCAMP,
                    ServiceList.SoundCloud to ExternalAudioService.SOUNDCLOUD,
                )

            for ((service, source) in services) {
                resolveExternalAudioStream(
                    service = service,
                    source = source,
                    query = normalizedQuery,
                ).onSuccess { return@runCatching it }
                    .onFailure { errors += it }
            }

            throw IllegalStateException(
                "No Bandcamp or SoundCloud stream found for ${normalizedQuery.title}",
                errors.lastOrNull(),
            )
        }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<String> =
        runCatching {
            val directUrl = format.url
            if (directUrl != null) {
                val resolvedDirectUrl =
                    if (directUrl.toHttpUrlOrNull()?.queryParameter("n")?.isNotBlank() == true) {
                        runCatching {
                            retryWithBackoff(
                                maxAttempts = 3,
                                initialDelayMs = 250L,
                                maxDelayMs = 2_000L
                            ) {
                                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, directUrl)
                            }
                        }.getOrElse { directUrl }
                    } else {
                        directUrl
                    }

                return@runCatching YouTube.appendGvsPoToken(
                    url = resolvedDirectUrl,
                    client = client,
                    authState = authState,
                )
            }

            val url = run {
                val cipherString = format.signatureCipher ?: format.cipher
                if (cipherString == null) throw ParsingException("Could not find format url")

                val params = parseQueryString(cipherString)
                val obfuscatedSignature = params["s"]
                    ?: throw ParsingException("Could not parse cipher signature")
                val signatureParam = params["sp"]
                    ?: throw ParsingException("Could not parse cipher signature parameter")
                val url = params["url"]?.let { URLBuilder(it) }
                    ?: throw ParsingException("Could not parse cipher url")
                url.parameters[signatureParam] =
                    YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                        videoId,
                        obfuscatedSignature
                    )
                url.toString()
            }

            val resolvedUrl = runCatching {
                retryWithBackoff(
                    maxAttempts = 3,
                    initialDelayMs = 250L,
                    maxDelayMs = 2_000L
                ) {
                    YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
                }
            }.getOrElse { url }

            YouTube.appendGvsPoToken(
                url = resolvedUrl,
                client = client,
                authState = authState,
            )
        }

    private fun resolveExternalAudioStream(
        service: StreamingService,
        source: ExternalAudioService,
        query: ExternalAudioQuery,
    ): Result<ExternalAudioStream> =
        runCatching {
            val candidates =
                query.searchQueries()
                    .asSequence()
                    .flatMap { searchQuery ->
                        runCatching {
                            service
                                .getSearchExtractor(searchQuery)
                                .apply { fetchPage() }
                                .getInitialPage()
                                .getItems()
                                .asSequence()
                                .filterIsInstance<StreamInfoItem>()
                        }.getOrElse { emptySequence() }
                    }
                    .distinctBy { it.getUrl() }
                    .filter { candidate -> query.matchesDuration(candidate.getDuration()) }
                    .sortedByDescending { candidate -> query.score(candidate) }
                    .take(MAX_EXTERNAL_SEARCH_CANDIDATES)
                    .toList()

            for (candidate in candidates) {
                val streamInfo = runCatching { StreamInfo.getInfo(candidate.getUrl()) }.getOrNull() ?: continue
                val audioStream = selectExternalAudioStream(streamInfo.getAudioStreams()) ?: continue
                val content =
                    audioStream.getContent()
                        .takeIf { audioStream.isUrl() && it.startsWith("http", ignoreCase = true) }
                        ?: continue

                return@runCatching ExternalAudioStream(
                    source = source,
                    streamUrl = content,
                    durationSeconds = streamInfo.getDuration(),
                    mimeType = audioStream.getFormat()?.getMimeType() ?: FALLBACK_EXTERNAL_AUDIO_MIME_TYPE,
                    bitrate = audioStream.getBitrate().takeUnless { it == AudioStream.UNKNOWN_BITRATE } ?: 0,
                    averageBitrate = audioStream.getAverageBitrate().takeUnless { it == AudioStream.UNKNOWN_BITRATE } ?: 0,
                    quality = audioStream.getQuality(),
                    itag = audioStream.getItag(),
                )
            }

            throw IllegalStateException("No playable ${source.name.lowercase(Locale.US)} audio stream found")
        }

    private fun selectExternalAudioStream(streams: List<AudioStream>): AudioStream? =
        streams
            .asSequence()
            .filter { stream -> stream.isUrl() && stream.getContent().startsWith("http", ignoreCase = true) }
            .sortedWith(
                compareByDescending<AudioStream> { it.losslessRank() }
                    .thenByDescending { it.getAverageBitrate().takeUnless { bitrate -> bitrate == AudioStream.UNKNOWN_BITRATE } ?: 0 }
                    .thenByDescending { it.getBitrate().takeUnless { bitrate -> bitrate == AudioStream.UNKNOWN_BITRATE } ?: 0 }
            )
            .firstOrNull()

    private fun AudioStream.losslessRank(): Int {
        val formatName = getFormat()?.getName().orEmpty()
        val mimeType = getFormat()?.getMimeType().orEmpty()
        val codecName = getCodec().orEmpty()
        return when {
            listOf(formatName, mimeType, codecName).any { value ->
                value.contains("flac", ignoreCase = true) ||
                    value.contains("alac", ignoreCase = true) ||
                    value.contains("wav", ignoreCase = true) ||
                    value.contains("aiff", ignoreCase = true)
            } -> 3
            codecName.contains("opus", ignoreCase = true) -> 2
            else -> 1
        }
    }

    private fun ExternalAudioQuery.normalized(): ExternalAudioQuery =
        copy(
            title = title.trim(),
            artists = artists.mapNotNull { it.trim().takeIf(String::isNotEmpty) }.distinct(),
            durationSeconds = durationSeconds?.takeIf { it > 0 },
        )

    private fun ExternalAudioQuery.searchQueries(): List<String> {
        val primaryArtist = artists.firstOrNull().orEmpty()
        return buildList {
            if (primaryArtist.isNotBlank()) add("$primaryArtist $title")
            add(title)
        }.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun ExternalAudioQuery.matchesDuration(candidateDurationSeconds: Long): Boolean {
        val expected = durationSeconds ?: return true
        if (candidateDurationSeconds <= 0L) return true
        val tolerance = maxOf(EXTERNAL_DURATION_MIN_TOLERANCE_SECONDS, (expected * EXTERNAL_DURATION_TOLERANCE_PERCENT) / 100)
        return abs(candidateDurationSeconds - expected) <= tolerance
    }

    private fun ExternalAudioQuery.score(candidate: StreamInfoItem): Int {
        val titleScore = tokenOverlap(title, candidate.getName()) * 4
        val artistScore =
            artists.maxOfOrNull { artist ->
                maxOf(
                    tokenOverlap(artist, candidate.getUploaderName().orEmpty()),
                    tokenOverlap(artist, candidate.getName()),
                )
            } ?: 0
        val durationScore =
            durationSeconds?.let { expected ->
                val actual = candidate.getDuration()
                if (actual <= 0L) 0 else maxOf(0, 20 - abs(actual - expected).toInt())
            } ?: 0
        return titleScore + (artistScore * 3) + durationScore
    }

    private fun tokenOverlap(left: String, right: String): Int {
        val leftTokens = left.normalizedTokens()
        if (leftTokens.isEmpty()) return 0
        val rightTokens = right.normalizedTokens()
        return leftTokens.count { it in rightTokens }
    }

    private fun String.normalizedTokens(): Set<String> =
        lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), " ")
            .split(' ')
            .mapNotNull { it.trim().takeIf { token -> token.length > 1 } }
            .toSet()

    private inline fun <T> retryWithBackoff(
        maxAttempts: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        block: () -> T
    ): T {
        var attempt = 0
        var delayMs = initialDelayMs
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            try {
                return block()
            } catch (e: Throwable) {
                val isRetryable =
                    e is SocketTimeoutException ||
                        e is IOException ||
                        e.cause is SocketTimeoutException ||
                        e.cause is IOException
                if (!isRetryable || attempt == maxAttempts - 1) throw e
                lastError = e
                try {
                    Thread.sleep(delayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
                delayMs = (delayMs * 2).coerceAtMost(maxDelayMs)
                attempt++
            }
        }
        throw lastError ?: IllegalStateException("Retry attempts exhausted")
    }

    private const val MAX_EXTERNAL_SEARCH_CANDIDATES = 8
    private const val EXTERNAL_DURATION_MIN_TOLERANCE_SECONDS = 8
    private const val EXTERNAL_DURATION_TOLERANCE_PERCENT = 12
    private const val FALLBACK_EXTERNAL_AUDIO_MIME_TYPE = "audio/mpeg"

}

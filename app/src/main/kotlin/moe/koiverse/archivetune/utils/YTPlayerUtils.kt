/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import moe.koiverse.archivetune.constants.AudioQuality
import moe.koiverse.archivetune.constants.PlayerStreamClient
import moe.koiverse.archivetune.innertube.NewPipeUtils
import moe.koiverse.archivetune.innertube.PlaybackAuthState
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.YouTubeClient
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.IOS
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB_REMIX
import moe.koiverse.archivetune.innertube.models.response.PlayerResponse
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_TESTSUITE
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_UNPLUGGED
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.IPADOS
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.IOS_MUSIC
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.MOBILE
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.TVHTML5
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.VISIONOS
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val FAILED_CLIENT_BACKOFF_MS = 10 * 60 * 1000L
    private const val DEFAULT_STREAM_EXPIRE_SECONDS = 300

    private fun extractExpireSecondsFromUrl(url: String): Int? {
        val expireTimestamp = url.toHttpUrlOrNull()
            ?.queryParameter("expire")
            ?.toLongOrNull()
            ?: return null
        val remaining = expireTimestamp - (System.currentTimeMillis() / 1000L)
        return remaining.toInt().takeIf { it > 0 }
    }

    private fun resolveExpireSeconds(apiExpire: Int?, streamUrl: String?): Int {
        apiExpire?.let { return it }
        streamUrl?.let { url ->
            extractExpireSecondsFromUrl(url)?.let { fromUrl ->
                Timber.tag(logTag).w("Using expire time extracted from stream URL: ${fromUrl}s")
                return fromUrl
            }
        }
        Timber.tag(logTag).w("No expire time available from API or URL, using default: ${DEFAULT_STREAM_EXPIRE_SECONDS}s")
        return DEFAULT_STREAM_EXPIRE_SECONDS
    }

    class LoginRequiredForPlaybackException(
        val videoId: String,
        val targetUrl: String,
        reason: String?,
    ) : IllegalStateException(reason)

    class InvalidPlaybackLoginContextException(
        val videoId: String,
        val targetUrl: String,
        cause: Throwable,
    ) : IllegalStateException("Invalid YouTube Music playback login context", cause)

    private data class PlaybackGateFailure(
        val clientName: String,
        val status: String,
        val reason: String?,
    )

    @Volatile private var streamClientPair: Pair<java.net.Proxy?, OkHttpClient>? = null

    private fun currentStreamClient(): OkHttpClient {
        val current = YouTube.streamProxy
        streamClientPair?.let { (proxy, client) ->
            if (proxy == current) return client
        }
        val client = OkHttpClient.Builder()
            .proxy(current)
            .build()
        streamClientPair = current to client
        return client
    }
    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [moe.koiverse.archivetune.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX
    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        IOS,
        MOBILE,
        ANDROID_MUSIC,
        IOS_MUSIC,
        ANDROID_VR_NO_AUTH,
        ANDROID_VR_1_61_48,
        ANDROID_VR_1_43_32,
        ANDROID_CREATOR,
        ANDROID_TESTSUITE,
        ANDROID_UNPLUGGED,
        IPADOS,
        VISIONOS,
        TVHTML5,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        WEB,
        WEB_CREATOR,
        WEB_REMIX
    )
    private data class CachedStreamUrl(
        val url: String,
        val expiresAtMs: Long,
        val authFingerprint: String,
    )

    private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()
    private val failedStreamClientsUntil = ConcurrentHashMap<String, Long>()
    @Volatile private var lastSuccessfulClientName: String? = null

    fun clearPlaybackAuthCaches() {
        streamUrlCache.clear()
        failedStreamClientsUntil.clear()
    }

    private suspend fun ensureVisitorDataReady(
        videoId: String,
        authState: PlaybackAuthState,
        forceRefresh: Boolean = false,
        reason: String,
    ): PlaybackAuthState {
        if (!forceRefresh) {
            authState.visitorData
                ?.takeIf { it.isNotBlank() }
                ?.let { return authState }
        }

        val action = if (forceRefresh) "Refreshing" else "Fetching"
        Timber.tag(logTag).i("%s visitorData for %s (%s)", action, videoId, reason)

        val refreshedVisitorData =
            YouTube.visitorData()
                .onFailure {
                    Timber.tag(logTag).e(it, "Failed to refresh visitorData for $videoId")
                    reportException(it)
                }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }

        if (refreshedVisitorData != null) {
            YouTube.visitorData = refreshedVisitorData
            return authState.copy(visitorData = refreshedVisitorData).normalized()
        }

        return authState
    }

    internal fun shouldPreferWebRemixForLoggedInPlayback(
        preferredStreamClient: PlayerStreamClient,
        isLoggedIn: Boolean,
        webClientPoTokenEnabled: Boolean,
        hasPlayerPoToken: Boolean,
        hasGvsPoToken: Boolean,
    ): Boolean {
        return preferredStreamClient == PlayerStreamClient.ANDROID_VR &&
            isLoggedIn &&
            webClientPoTokenEnabled &&
            hasPlayerPoToken &&
            hasGvsPoToken
    }

    internal fun shouldSkipCipheredWebPlaybackCandidate(
        webClientPoTokenEnabled: Boolean,
        isWebClient: Boolean,
        isCiphered: Boolean,
        hasGvsPoToken: Boolean,
    ): Boolean {
        return webClientPoTokenEnabled &&
            isWebClient &&
            isCiphered &&
            !hasGvsPoToken
    }

    internal fun buildStreamCacheKey(videoId: String, itag: Int, authFingerprint: String): String {
        return "$authFingerprint:$videoId:$itag"
    }

    fun invalidateCachedStreamUrls(videoId: String) {
        val marker = ":$videoId:"
        streamUrlCache.keys.removeIf { it.contains(marker) }
    }

    fun markStreamClientFailed(
        videoId: String,
        clientKey: String?,
        httpStatusCode: Int?,
        authFingerprint: String = YouTube.currentPlaybackAuthState().fingerprint,
    ) {
        if (httpStatusCode != 403) return
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return
        failedStreamClientsUntil[buildFailedClientKey(videoId, normalizedClientKey, authFingerprint)] =
            System.currentTimeMillis() + FAILED_CLIENT_BACKOFF_MS
    }

    fun markPreferredClientFailed(
        videoId: String,
        client: PlayerStreamClient,
        httpStatusCode: Int?,
        authFingerprint: String = YouTube.currentPlaybackAuthState().fingerprint,
    ) {
        markStreamClientFailed(videoId, client.name, httpStatusCode, authFingerprint)
    }

    private fun isStreamClientTemporarilyBlocked(
        videoId: String,
        clientKey: String?,
        authFingerprint: String,
    ): Boolean {
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return false
        val key = buildFailedClientKey(videoId, normalizedClientKey, authFingerprint)
        val until = failedStreamClientsUntil[key] ?: return false
        if (until <= System.currentTimeMillis()) {
            failedStreamClientsUntil.remove(key)
            return false
        }
        return true
    }

    private fun normalizeStreamClientKey(clientKey: String?): String {
        return clientKey?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.US).orEmpty()
    }

    internal fun buildFailedClientKey(videoId: String, clientKey: String, authFingerprint: String): String {
        return "$authFingerprint:$videoId:${normalizeStreamClientKey(clientKey)}"
    }

    internal fun resolvePreferredPlaybackClient(
        preferredStreamClient: PlayerStreamClient,
        authState: PlaybackAuthState,
    ): YouTubeClient {
        val hasPlayerPoToken = !authState.resolvePlayerPoToken(WEB_REMIX).isNullOrBlank()
        val hasGvsPoToken = !authState.resolveGvsPoToken(WEB_REMIX).isNullOrBlank()

        if (
            shouldPreferWebRemixForLoggedInPlayback(
                preferredStreamClient = preferredStreamClient,
                isLoggedIn = authState.hasPlaybackLoginContext,
                webClientPoTokenEnabled = authState.webClientPoTokenEnabled,
                hasPlayerPoToken = hasPlayerPoToken,
                hasGvsPoToken = hasGvsPoToken,
            )
        ) {
            return WEB_REMIX
        }

        return when (preferredStreamClient) {
            PlayerStreamClient.ANDROID_VR ->
                if (authState.hasPlaybackLoginContext) ANDROID_MUSIC else ANDROID_VR_NO_AUTH
            PlayerStreamClient.WEB_REMIX -> WEB_REMIX
            PlayerStreamClient.IOS -> IOS
            PlayerStreamClient.TVHTML5 -> TVHTML5
            PlayerStreamClient.ANDROID_MUSIC -> ANDROID_MUSIC
        }
    }

    internal fun buildStreamClientOrder(
        preferredStreamClient: PlayerStreamClient,
        authState: PlaybackAuthState,
    ): List<YouTubeClient> {
        val preferredYouTubeClient = resolvePreferredPlaybackClient(preferredStreamClient, authState)
        val lastSuccessfulClient = lastSuccessfulClientName?.let { name ->
            STREAM_FALLBACK_CLIENTS.find { it.clientName == name }
        }

        val orderedFallbackClients =
            if (authState.hasPlaybackLoginContext) {
                STREAM_FALLBACK_CLIENTS.filter { it.loginSupported } + STREAM_FALLBACK_CLIENTS.filterNot { it.loginSupported }
            } else {
                STREAM_FALLBACK_CLIENTS.toList()
            }

        return buildList {
            lastSuccessfulClient?.let { add(it) }
            add(preferredYouTubeClient)
            addAll(orderedFallbackClients)
            if (preferredYouTubeClient != MAIN_CLIENT) add(MAIN_CLIENT)
            if (preferredStreamClient == PlayerStreamClient.WEB_REMIX) {
                addAll(STREAM_FALLBACK_CLIENTS)
            }
        }.distinct()
    }

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        val authFingerprint: String,
    )
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        preferredStreamClient: PlayerStreamClient = PlayerStreamClient.ANDROID_VR,
        // if provided, this preference overrides ConnectivityManager.isActiveNetworkMetered
        networkMetered: Boolean? = null,
    ): Result<PlaybackData> = runCatching {
        val attempts =
            when (audioQuality) {
                AudioQuality.HIGHEST -> listOf(AudioQuality.HIGHEST, AudioQuality.HIGH)
                AudioQuality.AUTO -> listOf(AudioQuality.AUTO, AudioQuality.HIGH)
                else -> listOf(audioQuality)
            }.distinct()

        var lastError: Throwable? = null
        for (attempt in attempts) {
            val attemptResult =
                runCatching {
                    playerResponseForPlaybackOnce(
                        videoId = videoId,
                        playlistId = playlistId,
                        audioQuality = attempt,
                        connectivityManager = connectivityManager,
                        preferredStreamClient = preferredStreamClient,
                        networkMetered = networkMetered,
                    )
                }
            if (attemptResult.isSuccess) return@runCatching attemptResult.getOrThrow()
            lastError = attemptResult.exceptionOrNull()
        }
        throw lastError ?: IllegalStateException("Failed to resolve stream")
    }

    private suspend fun playerResponseForPlaybackOnce(
        videoId: String,
        playlistId: String?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        preferredStreamClient: PlayerStreamClient,
        networkMetered: Boolean?,
    ): PlaybackData {
        Timber.tag(logTag).i("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).v("Signature timestamp: $signatureTimestamp")

        var authState = YouTube.currentPlaybackAuthState()
        val hasLoginCookie = authState.hasLoginCookie
        val canUseLoggedInPlayback = authState.hasPlaybackLoginContext
        if (!canUseLoggedInPlayback) {
            if (hasLoginCookie) {
                Timber.tag(logTag).w(
                    "Ignoring incomplete login context for %s because dataSyncId is missing; falling back to visitorData playback",
                    videoId,
                )
            }
            authState = ensureVisitorDataReady(
                videoId = videoId,
                authState = authState,
                reason = if (hasLoginCookie) "cookie-only playback fallback" else "anonymous playback bootstrap",
            )
        }
        val sessionId = authState.sessionId
        val authStatus =
            when {
                canUseLoggedInPlayback -> "Logged in"
                hasLoginCookie -> "Cookie-only"
                else -> "Not logged in"
            }
        Timber.tag(logTag).v("Session authentication status: $authStatus (sessionId=${sessionId.orEmpty()})")

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        val preferredYouTubeClient = resolvePreferredPlaybackClient(preferredStreamClient, authState)
        if (
            preferredYouTubeClient == WEB_REMIX &&
            preferredStreamClient == PlayerStreamClient.ANDROID_VR &&
            canUseLoggedInPlayback
        ) {
            Timber.tag(logTag).i(
                "Promoting playback client to WEB_REMIX for %s because login and Web PoToken playback are available",
                videoId,
            )
        }

        val metadataClient = MAIN_CLIENT

        Timber.tag(logTag).i("Fetching metadata response using client: ${metadataClient.clientName}")
        var metadataPlayerResponse =
            YouTube.player(
                videoId = videoId,
                playlistId = playlistId,
                client = metadataClient,
                signatureTimestamp = signatureTimestamp,
                setLogin = canUseLoggedInPlayback,
                authState = authState,
            ).getPlaybackPlayerResponseOrThrow(videoId, authState)
        var expectedDurationMs =
            metadataPlayerResponse.videoDetails?.lengthSeconds
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
                ?.times(1000L)

        val streamClients = buildStreamClientOrder(preferredStreamClient, authState).filterNot { client ->
                val blocked = isStreamClientTemporarilyBlocked(
                    videoId = videoId,
                    clientKey = client.clientName,
                    authFingerprint = authState.fingerprint,
                )
                if (blocked) {
                    Timber.tag(logTag).w("Temporarily blocked stream client for $videoId: ${client.clientName}")
                }
                blocked
            }

        val botDetectedClients = mutableSetOf<String>()
        var gateFailure: PlaybackGateFailure? = null
        var didRefreshVisitorDataAfterBotDetection = false

        for ((index, client) in streamClients.withIndex()) {
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            streamPlayerResponse = null

            Timber.tag(logTag).v(
                "Trying ${if (client == MAIN_CLIENT) "MAIN_CLIENT" else "fallback client"} ${index + 1}/${streamClients.size}: ${client.clientName}"
            )

            if (client != MAIN_CLIENT && client.loginRequired && !canUseLoggedInPlayback) {
                Timber.tag(logTag).w("Skipping client ${client.clientName} - requires login but user is not logged in")
                continue
            }

            streamPlayerResponse =
                if (client == metadataClient) {
                    metadataPlayerResponse
                } else {
                    Timber.tag(logTag).i("Fetching player response for fallback client: ${client.clientName}")
                    YouTube.player(
                        videoId = videoId,
                        playlistId = playlistId,
                        client = client,
                        signatureTimestamp = signatureTimestamp,
                        setLogin = canUseLoggedInPlayback,
                        authState = authState,
                    ).getPlaybackPlayerResponseOrNull(videoId, authState)
                }

            if (streamPlayerResponse == null) continue

            var playabilityStatus = streamPlayerResponse.playabilityStatus
            if (playabilityStatus.status != "OK") {
                var reason = playabilityStatus.reason.orEmpty()
                var isLoginRecovery = isLoginRecoveryError(reason)
                var isBotDetection = isBotDetectionError(reason)

                if (!canUseLoggedInPlayback && isBotDetection && !didRefreshVisitorDataAfterBotDetection) {
                    val refreshedVisitorData =
                        ensureVisitorDataReady(
                            videoId = videoId,
                            authState = authState,
                            forceRefresh = true,
                            reason = "bot-detection recovery on ${client.clientName}",
                        )

                    if (!refreshedVisitorData.visitorData.isNullOrBlank()) {
                        authState = refreshedVisitorData
                        didRefreshVisitorDataAfterBotDetection = true
                        Timber.tag(logTag).i(
                            "Retrying %s for %s after refreshing visitorData",
                            client.clientName,
                            videoId,
                        )
                        streamPlayerResponse =
                            YouTube.player(
                                videoId = videoId,
                                playlistId = playlistId,
                                client = client,
                                signatureTimestamp = signatureTimestamp,
                                setLogin = canUseLoggedInPlayback,
                                authState = authState,
                            ).getPlaybackPlayerResponseOrNull(videoId, authState)

                        if (streamPlayerResponse == null) continue

                        playabilityStatus = streamPlayerResponse.playabilityStatus
                        reason = playabilityStatus.reason.orEmpty()
                        isLoginRecovery = isLoginRecoveryError(reason)
                        isBotDetection = isBotDetectionError(reason)
                    }
                }

                if (playabilityStatus.status == "OK") {
                    if (client == metadataClient) {
                        metadataPlayerResponse = streamPlayerResponse
                        expectedDurationMs =
                            metadataPlayerResponse.videoDetails?.lengthSeconds
                                ?.toLongOrNull()
                                ?.takeIf { it > 0 }
                                ?.times(1000L)
                    }
                    Timber.tag(logTag).i(
                        "Recovered playback with %s after visitorData refresh",
                        client.clientName,
                    )
                } else {
                    Timber.tag(logTag).w(
                        "Player response status not OK: ${playabilityStatus.status}, reason: $reason, loginRecovery: $isLoginRecovery, botDetection: $isBotDetection"
                    )
                    if (isLoginRecovery) {
                        gateFailure = PlaybackGateFailure(
                            clientName = client.clientName,
                            status = playabilityStatus.status,
                            reason = playabilityStatus.reason,
                        )
                    } else if (isBotDetection) {
                        botDetectedClients.add(client.clientName)
                    }
                    continue
                }
            }

            val isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered
            val candidates =
                selectAudioFormatCandidates(
                    streamPlayerResponse,
                    audioQuality,
                    isMetered,
                )

            if (candidates.isEmpty()) continue

            var selectedFormat: PlayerResponse.StreamingData.Format? = null
            var selectedUrl: String? = null

            for (candidate in candidates.asSequence().take(6)) {
                if (canUseLoggedInPlayback && expectedDurationMs != null && isLikelyPreview(candidate, expectedDurationMs)) continue
                if (shouldSkipCipheredWebCandidate(client, candidate, authState)) continue
                val cacheKey = buildStreamCacheKey(videoId, candidate.itag, authState.fingerprint)
                val cached = streamUrlCache[cacheKey]
                val candidateUrl =
                    if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) {
                        cached.url
                    } else {
                        findUrlOrNull(candidate, videoId, client, authState)
                    } ?: continue
                selectedFormat = candidate
                selectedUrl = candidateUrl
                break
            }

            if (selectedFormat == null || selectedUrl == null) continue

            format = selectedFormat
            streamUrl = selectedUrl
            streamExpiresInSeconds = resolveExpireSeconds(
                apiExpire = streamPlayerResponse.streamingData?.expiresInSeconds,
                streamUrl = selectedUrl,
            )

            Timber.tag(logTag).i("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")
            Timber.tag(logTag).v("Stream expires in: $streamExpiresInSeconds seconds")

            val valid = validateStatus(streamUrl, client.userAgent)
            if (valid) {
                Timber.tag(logTag).i("Stream validated successfully with client: ${client.clientName}")
                lastSuccessfulClientName = client.clientName
                break
            }

            Timber.tag(logTag).w("Stream validation failed with client: ${client.clientName}, trying next fallback")
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            streamPlayerResponse = null
        }

        if (streamPlayerResponse == null) {
            gateFailure?.let { failure ->
                Timber.tag(logTag).w(
                    "Playback requires login recovery for $videoId via ${failure.clientName} (${failure.status}): ${failure.reason.orEmpty()}"
                )
                throw LoginRequiredForPlaybackException(
                    videoId = videoId,
                    targetUrl = "https://music.youtube.com/watch?v=$videoId",
                    reason = failure.reason,
                )
            }
            if (botDetectedClients.isNotEmpty()) {
                Timber.tag(logTag).e("Bot detection triggered on clients: $botDetectedClients - all clients failed")
                throw PlaybackException(
                    "Sign in to confirm you're not a bot",
                    null,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            }
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            if (isLoginRecoveryError(errorReason.orEmpty())) {
                Timber.tag(logTag).w("Playback requires login recovery for $videoId: $errorReason")
                throw LoginRequiredForPlaybackException(
                    videoId = videoId,
                    targetUrl = "https://music.youtube.com/watch?v=$videoId",
                    reason = errorReason,
                )
            }
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            streamExpiresInSeconds = resolveExpireSeconds(
                apiExpire = null,
                streamUrl = streamUrl,
            )
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find suitable format for quality: $audioQuality. Available formats from last client: ${streamPlayerResponse.streamingData?.adaptiveFormats?.filter { it.isAudio }?.map { "${it.mimeType} @ ${it.bitrate}bps (itag: ${it.itag})" }}")
            throw Exception("Could not find format for quality: $audioQuality")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url for format: ${format.mimeType}, itag: ${format.itag}")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).i("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")

        streamUrlCache[buildStreamCacheKey(videoId, format.itag, authState.fingerprint)] =
            CachedStreamUrl(
                url = streamUrl,
                expiresAtMs = System.currentTimeMillis() + (streamExpiresInSeconds * 1000L),
                authFingerprint = authState.fingerprint,
            )

        return PlaybackData(
            metadataPlayerResponse.playerConfig?.audioConfig,
            metadataPlayerResponse.videoDetails,
            metadataPlayerResponse.playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
            authState.fingerprint,
        )
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<PlayerResponse> {
        Timber.tag(logTag).i("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(
            videoId = videoId,
            playlistId = playlistId,
            client = MAIN_CLIENT,
            setLogin = authState.hasPlaybackLoginContext,
            authState = authState,
        )
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        // optional override from user preference; if non-null, use this instead of ConnectivityManager
        networkMetered: Boolean? = null,
    ): PlayerResponse.StreamingData.Format? {
        val isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered
        return selectAudioFormatCandidates(
            playerResponse,
            audioQuality,
            isMetered,
        ).firstOrNull()
    }

    private fun selectAudioFormatCandidates(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        networkMetered: Boolean,
    ): List<PlayerResponse.StreamingData.Format> {
        Timber.tag(logTag).i("Finding format with audioQuality: $audioQuality, network metered: $networkMetered")

        val audioFormats =
            playerResponse.streamingData?.adaptiveFormats
                ?.asSequence()
                ?.filter { it.isAudio && it.bitrate > 0 }
                ?.filter { it.url != null || it.signatureCipher != null || it.cipher != null }
                ?.toList()
                .orEmpty()

        if (audioFormats.isEmpty()) return emptyList()

        val effectiveQuality =
            when (audioQuality) {
                AudioQuality.AUTO -> if (networkMetered) AudioQuality.HIGH else AudioQuality.HIGHEST
                else -> audioQuality
            }

        val targetBitrateBps =
            when (effectiveQuality) {
                AudioQuality.LOW -> 70_000
                AudioQuality.HIGH -> 160_000
                AudioQuality.HIGHEST -> 320_000
                AudioQuality.AUTO -> null
            }

        val preferHigher =
            compareByDescending<PlayerResponse.StreamingData.Format> { it.url != null }
                .thenByDescending { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val preferLowerAboveTarget =
            compareByDescending<PlayerResponse.StreamingData.Format> { it.url != null }
                .thenBy { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val candidates =
            if (targetBitrateBps == null) {
                audioFormats.sortedWith(preferHigher)
            } else {
                val belowOrEqual = audioFormats.filter { it.bitrate <= targetBitrateBps }
                if (belowOrEqual.isNotEmpty()) {
                    belowOrEqual.sortedWith(preferHigher)
                } else {
                    val aboveOrEqual = audioFormats.filter { it.bitrate >= targetBitrateBps }
                    if (aboveOrEqual.isNotEmpty()) aboveOrEqual.sortedWith(preferLowerAboveTarget)
                    else audioFormats.sortedWith(preferHigher)
                }
            }

        Timber.tag(logTag)
            .v(
                "Available audio formats: ${
                    candidates.take(12).map {
                        val codec = extractCodec(it.mimeType)
                        val direct = if (it.url != null) "direct" else "cipher"
                        "${it.mimeType} ($direct, codec=${codec ?: "unknown"}) @ ${it.bitrate}bps"
                    }
                }"
            )

        return candidates
    }

    private fun extractCodec(mimeType: String): String? {
        val match = Regex("""codecs="([^"]+)"""").find(mimeType) ?: return null
        return match.groupValues.getOrNull(1)?.split(",")?.firstOrNull()?.trim()
    }

    private fun isCipheredFormat(format: PlayerResponse.StreamingData.Format): Boolean {
        return format.url == null && (format.signatureCipher != null || format.cipher != null)
    }

    private fun shouldSkipCipheredWebCandidate(
        client: YouTubeClient,
        format: PlayerResponse.StreamingData.Format,
        authState: PlaybackAuthState,
    ): Boolean {
        val isWebClient = StreamClientUtils.isWebClient(client.clientName)
        val isCiphered = isCipheredFormat(format)
        val hasGvsPoToken = !authState.resolveGvsPoToken(client).isNullOrBlank()
        if (
            !shouldSkipCipheredWebPlaybackCandidate(
                webClientPoTokenEnabled = authState.webClientPoTokenEnabled,
                isWebClient = isWebClient,
                isCiphered = isCiphered,
                hasGvsPoToken = hasGvsPoToken,
            )
        ) {
            return false
        }

        Timber.tag(logTag).w(
            "Skipping ciphered %s stream candidate because Web PoToken playback is enabled but no GVS token is available",
            client.clientName,
        )
        return true
    }

    private fun codecRank(codec: String?): Int =
        when {
            codec.isNullOrBlank() -> 0
            codec.contains("opus", ignoreCase = true) -> 3
            codec.contains("mp4a", ignoreCase = true) -> 2
            else -> 1
        }
    private fun isLikelyPreview(
        format: PlayerResponse.StreamingData.Format,
        expectedDurationMs: Long,
    ): Boolean {
        val approx = format.approxDurationMs?.toLongOrNull() ?: return false
        if (expectedDurationMs < 90_000L) return false
        return approx in 1L..(minOf(90_000L, (expectedDurationMs * 9L) / 10L))
    }
    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String, userAgent: String): Boolean {
        Timber.tag(logTag).v("Validating stream URL status")
        try {
            val httpUrl = url.toHttpUrlOrNull()
            val clientParam = httpUrl?.queryParameter("c")?.trim().orEmpty()

            val resolvedUserAgent = StreamClientUtils.resolveUserAgent(clientParam).ifEmpty { userAgent }
            val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

            val probeRanges =
                if (StreamClientUtils.isWebClient(clientParam)) {
                    listOf("bytes=0-0", "bytes=262144-262145", "bytes=1048576-1048577")
                } else {
                    listOf("bytes=0-0")
                }

            var sawReadableProbe = false
            for (range in probeRanges) {
                val rangeRequest =
                    okhttp3.Request.Builder()
                        .get()
                        .header("User-Agent", resolvedUserAgent)
                        .header("Range", range)
                        .apply {
                            originReferer.origin?.let { header("Origin", it) }
                            originReferer.referer?.let { header("Referer", it) }
                        }.url(url)
                        .build()

                val probeValid =
                    currentStreamClient().newCall(rangeRequest).execute().use { response ->
                        val code = response.code
                        if (code == 403) return@use false
                        if (code !in 200..399 && code != 416) return@use false
                        if (code == 416) return@use sawReadableProbe

                        val contentType = response.header("Content-Type").orEmpty().lowercase(Locale.US)
                        if (
                            contentType.startsWith("text/html") ||
                            contentType.startsWith("text/plain") ||
                            contentType.startsWith("application/json") ||
                            contentType.startsWith("application/xml") ||
                            contentType.startsWith("text/xml")
                        ) {
                            Timber.tag(logTag).w(
                                "Rejecting stream probe because it returned non-media content-type: %s",
                                contentType,
                            )
                            return@use false
                        }

                        val readable = response.body?.source()?.request(1) == true
                        if (readable) {
                            sawReadableProbe = true
                        }
                        readable
                    }
                if (!probeValid) return false
            }

            return true
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).i("Getting signature timestamp for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).i("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }
    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions.
     * Also patches cver to match the client version.
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: PlaybackAuthState,
    ): String? {
        Timber.tag(logTag).i("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        var url = NewPipeUtils.getStreamUrl(format, videoId, client, authState)
            .onSuccess { Timber.tag(logTag).i("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull() ?: return null

        // Patch cver in the URL to match the client we actually used
        if (client != null) {
            url = StreamClientUtils.patchClientVersion(url, client.clientVersion)
        }

        return url
    }

    private fun Result<PlayerResponse>.getPlaybackPlayerResponseOrThrow(
        videoId: String,
        authState: PlaybackAuthState,
    ): PlayerResponse {
        val failure = exceptionOrNull()
        if (failure != null) {
            throwInvalidPlaybackLoginContextIfNeeded(videoId, authState, failure)
            throw failure
        }
        return getOrThrow()
    }

    private fun Result<PlayerResponse>.getPlaybackPlayerResponseOrNull(
        videoId: String,
        authState: PlaybackAuthState,
    ): PlayerResponse? {
        val failure = exceptionOrNull()
        if (failure != null) {
            throwInvalidPlaybackLoginContextIfNeeded(videoId, authState, failure)
            return null
        }
        return getOrNull()
    }

    private fun throwInvalidPlaybackLoginContextIfNeeded(
        videoId: String,
        authState: PlaybackAuthState,
        failure: Throwable,
    ) {
        if (!authState.hasPlaybackLoginContext) return
        if (!failure.isInvalidPlaybackLoginContextFailure()) return

        Timber.tag(logTag).w(
            failure,
            "Detected invalid logged-in playback context for %s; requiring login refresh",
            videoId,
        )
        throw InvalidPlaybackLoginContextException(
            videoId = videoId,
            targetUrl = "https://music.youtube.com/watch?v=$videoId",
            cause = failure,
        )
    }

    private fun Throwable.isInvalidPlaybackLoginContextFailure(): Boolean {
        val clientError = this as? ClientRequestException ?: return false
        if (clientError.response.status != HttpStatusCode.BadRequest) return false

        val message = clientError.message.orEmpty()
        if (!message.contains("/youtubei/v1/player", ignoreCase = true)) return false

        return message.contains("INVALID_ARGUMENT", ignoreCase = true) ||
            message.contains("invalid argument", ignoreCase = true)
    }

    private fun isBotDetectionError(reason: String): Boolean {
        val lower = reason.lowercase(Locale.US)
        return "bot" in lower ||
            "unusual traffic" in lower ||
            "automated" in lower ||
            "confirm" in lower && "not a" in lower ||
            "not a robot" in lower ||
            "verify" in lower && "human" in lower
    }

    private fun isLoginRecoveryError(reason: String): Boolean {
        val lower = reason.lowercase(Locale.US)
        return "confirm your age" in lower ||
            "age-restricted" in lower ||
            "age restricted" in lower ||
            "inappropriate for some users" in lower ||
            "mature audiences" in lower ||
            "adult" in lower && "sign in" in lower ||
            "allow" in lower && "youtube music" in lower
    }

    fun isBotDetectionException(error: PlaybackException): Boolean {
        val message = error.message.orEmpty()
        if (isBotDetectionError(message)) return true
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (isBotDetectionError(cause.message.orEmpty())) return true
            cause = cause.cause
        }
        return false
    }
}

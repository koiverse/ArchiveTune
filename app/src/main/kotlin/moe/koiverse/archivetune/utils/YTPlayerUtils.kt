package moe.koiverse.archivetune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import moe.koiverse.archivetune.constants.AudioQuality
import moe.koiverse.archivetune.innertube.NewPipeUtils
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.YouTubeClient
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.IOS
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB_REMIX
import moe.koiverse.archivetune.innertube.models.response.PlayerResponse
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.MOBILE
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB
import moe.koiverse.archivetune.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import okhttp3.OkHttpClient
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    
    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()
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
        ANDROID_VR_NO_AUTH,
        MOBILE,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB,
        WEB_CREATOR
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
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
        // if provided, this preference overrides ConnectivityManager.isActiveNetworkMetered
        networkMetered: Boolean? = null,
    ): Result<PlaybackData> = runCatching {
    Timber.tag(logTag).i("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
    Timber.tag(logTag).v("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }
    Timber.tag(logTag).v("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

    Timber.tag(logTag).i("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                // try with streams from main client first
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
                Timber.tag(logTag).v("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag).v("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    Timber.tag(logTag).w("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                Timber.tag(logTag).i("Fetching player response for fallback client: ${client.clientName}")
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).i("Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")

                format =
                    findFormat(
                        streamPlayerResponse,
                        audioQuality,
                        connectivityManager,
                        networkMetered = networkMetered,
                    )

                if (format == null) {
                    Timber.tag(logTag).v("No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).i("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId)
                if (streamUrl == null) {
                    Timber.tag(logTag).w("Stream URL not found for format")
                    continue
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).w("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).v("Stream expires in: $streamExpiresInSeconds seconds")

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    Timber.tag(logTag).i("Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    break
                }

                if (validateStatus(streamUrl)) {
                    // working stream found
                    Timber.tag(logTag).i("Stream validated successfully with client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    break
                } else {
                    Timber.tag(logTag).w("Stream validation failed for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                }
            } else {
                Timber.tag(logTag).w("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
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
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).i("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = MAIN_CLIENT)
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
        Timber.tag(logTag).i("Finding format with audioQuality: $audioQuality, network metered: $isMetered")

        val audioFormats =
            playerResponse.streamingData?.adaptiveFormats?.filter { it.isAudio && it.contentLength != null && it.bitrate > 0 }
        if (audioFormats.isNullOrEmpty()) {
            Timber.tag(logTag).w("No audio formats available")
            return null
        }

        Timber.tag(logTag)
            .v(
                "Available audio formats: ${
                    audioFormats.map {
                        val codec = extractCodec(it.mimeType)
                        "${it.mimeType} (codec=${codec ?: "unknown"}) @ ${it.bitrate}bps"
                    }
                }"
            )

        val effectiveQuality =
            when (audioQuality) {
                AudioQuality.AUTO -> if (isMetered) AudioQuality.HIGH else AudioQuality.HIGHEST
                else -> audioQuality
            }

        val targetBitrateBps =
            when (effectiveQuality) {
                AudioQuality.LOW -> 70_000
                AudioQuality.HIGH -> 160_000
                AudioQuality.VERY_HIGH -> 256_000
                AudioQuality.HIGHEST -> null
                AudioQuality.AUTO -> null
            }

        val format =
            if (targetBitrateBps == null) {
                audioFormats.maxWithOrNull(
                    compareBy<PlayerResponse.StreamingData.Format> { it.bitrate }
                        .thenBy { codecRank(extractCodec(it.mimeType)) }
                        .thenBy { it.audioSampleRate ?: 0 }
                )
            } else {
                val belowOrEqual = audioFormats.filter { it.bitrate <= targetBitrateBps }
                if (belowOrEqual.isNotEmpty()) {
                    belowOrEqual.maxWithOrNull(
                        compareBy<PlayerResponse.StreamingData.Format> { it.bitrate }
                            .thenBy { codecRank(extractCodec(it.mimeType)) }
                            .thenBy { it.audioSampleRate ?: 0 }
                    )
                } else {
                    val above = audioFormats.filter { it.bitrate >= targetBitrateBps }
                    above.minWithOrNull(
                        compareBy<PlayerResponse.StreamingData.Format> { it.bitrate }
                            .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                            .thenByDescending { it.audioSampleRate ?: 0 }
                    )
                }
            }

        if (format != null) {
            Timber.tag(logTag).i("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}bps (itag: ${format.itag})")
        } else {
            Timber.tag(logTag).w("No suitable audio format found")
        }

        return format
    }

    private fun extractCodec(mimeType: String): String? {
        val match = Regex("""codecs="([^"]+)"""").find(mimeType) ?: return null
        return match.groupValues.getOrNull(1)?.split(",")?.firstOrNull()?.trim()
    }

    private fun codecRank(codec: String?): Int =
        when {
            codec.isNullOrBlank() -> 0
            codec.contains("opus", ignoreCase = true) -> 3
            codec.contains("mp4a", ignoreCase = true) -> 2
            else -> 1
        }
    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).v("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).i("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
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
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).i("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).i("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull()
    }
}

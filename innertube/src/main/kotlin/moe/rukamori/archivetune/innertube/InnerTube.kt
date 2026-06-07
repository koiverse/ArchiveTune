/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.innertube

import moe.rukamori.archivetune.innertube.models.Context
import moe.rukamori.archivetune.innertube.proxy.RotatingProxySelector
import moe.rukamori.archivetune.innertube.models.MediaInfo
import moe.rukamori.archivetune.innertube.models.ReturnYouTubeDislikeResponse
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import moe.rukamori.archivetune.innertube.models.body.*
import moe.rukamori.archivetune.innertube.models.response.NextResponse
import moe.rukamori.archivetune.innertube.utils.sha1
import moe.rukamori.archivetune.innertube.utils.youtubeLoginCookieValue
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.Proxy
import java.io.IOException
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import kotlinx.coroutines.delay
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Provide access to InnerTube endpoints.
 * For making HTTP requests, not parsing response.
 */
@OptIn(ExperimentalEncodingApi::class)
class InnerTube {
    private var httpClient = createClient()

    private companion object {
        const val PLAYBACK_TELEMETRY_VER = "2"
    }

    var locale = YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag()
    )
    private val queueLocale = YouTubeLocale(gl = "US", hl = "en")

    @Volatile
    private var authState: PlaybackAuthState = PlaybackAuthState.EMPTY

    var visitorData: String?
        get() = authState.visitorData
        set(value) {
            authState = authState.copy(visitorData = value).normalized()
        }
    var dataSyncId: String?
        get() = authState.dataSyncId
        set(value) {
            authState = authState.copy(dataSyncId = value).normalized()
        }
    var poToken: String?
        get() = authState.poToken
        set(value) {
            authState = authState.copy(poToken = value).normalized()
        }
    var cookie: String?
        get() = authState.cookie
        set(value) {
            authState = authState.copy(cookie = value).normalized()
        }

    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var proxyUsername: String? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var proxyPassword: String? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    internal var proxySelector: RotatingProxySelector? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var dns: Dns = Dns.SYSTEM
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var useLoginForBrowse: Boolean = false

    fun currentAuthState(): PlaybackAuthState = authState

    fun applyAuthState(value: PlaybackAuthState) {
        authState = value.normalized()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }

        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }

        engine {
            config {
                dns(this@InnerTube.dns)
                val sel = this@InnerTube.proxySelector
                if (sel != null) {
                    proxySelector(sel)
                } else if (this@InnerTube.proxy != null && !proxyUsername.isNullOrBlank() && !proxyPassword.isNullOrBlank()) {
                    proxyAuthenticator { _, response ->
                        val credential = okhttp3.Credentials.basic(proxyUsername!!, proxyPassword!!)
                        response.request.newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build()
                    }
                }
            }
            if (this@InnerTube.proxySelector == null && this@InnerTube.proxy != null) {
                proxy = this@InnerTube.proxy
            }
        }

        defaultRequest {
            url(YouTubeClient.API_URL_YOUTUBE_MUSIC)
        }
    }

    private fun HttpRequestBuilder.ytClient(
        client: YouTubeClient,
        setLogin: Boolean = false,
        authState: PlaybackAuthState = currentAuthState(),
        includeVisitorData: Boolean = true,
    ) {
        val requestOrigin = client.requestOrigin()
        val requestReferer = client.requestReferer()
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", requestOrigin)
            append("Referer", requestReferer)
            if (includeVisitorData) {
                authState.visitorData?.let { append("X-Goog-Visitor-Id", it) }
            }
            if (setLogin && client.loginSupported) {
                authState.cookie?.let { cookie ->
                    append("cookie", cookie)
                    val loginCookieValue = youtubeLoginCookieValue(cookie) ?: return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime $loginCookieValue $requestOrigin")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    private fun HttpRequestBuilder.ytPlaybackTrackingClient(
        client: YouTubeClient,
        authState: PlaybackAuthState = currentAuthState(),
    ) {
        val requestOrigin = client.requestOrigin()
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", requestOrigin)
            append("Referer", client.requestReferer())
            authState.visitorData?.let { append("X-Goog-Visitor-Id", it) }
            if (client.loginSupported) {
                authState.cookie?.let { cookie ->
                    append("cookie", cookie)
                    val loginCookieValue = youtubeLoginCookieValue(cookie) ?: return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime $loginCookieValue $requestOrigin")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        val resolvedMaxAttempts = proxySelector?.activeCount()?.coerceIn(maxAttempts, 6) ?: maxAttempts
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Throwable) {
                if (e is CancellationException || !e.isTransientNetworkFailure()) throw e
                attempt++
                proxySelector?.markLastSelectedFailed()
                proxySelector?.rotate()
                if (attempt >= resolvedMaxAttempts) throw e
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    private fun Throwable.isTransientNetworkFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException || current is HttpRequestTimeoutException) return true
            if (current.message?.contains("Request timeout has expired", ignoreCase = true) == true) return true
            current = current.cause
        }
        return false
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
        useAccountContext: Boolean = true,
    ) = withRetry {
        httpClient.post("search") {
            ytClient(
                client = client,
                setLogin = useAccountContext && useLoginForBrowse,
                includeVisitorData = useAccountContext,
            )
            setBody(
                SearchBody(
                    context = client.toContext(
                        locale,
                        if (useAccountContext) visitorData else null,
                        if (useAccountContext && useLoginForBrowse) dataSyncId else null
                    ),
                    query = query,
                    params = params
                )
            )
            parameter("continuation", continuation)
            parameter("ctoken", continuation)
        }
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String? = null,
        setLogin: Boolean = true,
        authState: PlaybackAuthState = currentAuthState(),
    ) = withRetry {
        val includeDataSyncId = setLogin && client.loginSupported && !authState.dataSyncId.isNullOrBlank()
        try {
            executePlayerRequest(
                client = client,
                videoId = videoId,
                playlistId = playlistId,
                signatureTimestamp = signatureTimestamp,
                poToken = poToken,
                setLogin = setLogin,
                authState = authState,
                includeDataSyncId = includeDataSyncId,
            )
        } catch (failure: Throwable) {
            if (!shouldRetryPlayerRequestWithoutDataSyncId(failure, includeDataSyncId)) throw failure
            executePlayerRequest(
                client = client,
                videoId = videoId,
                playlistId = playlistId,
                signatureTimestamp = signatureTimestamp,
                poToken = poToken,
                setLogin = false,
                authState = authState,
                includeDataSyncId = false,
            )
        }
    }

    private suspend fun executePlayerRequest(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String?,
        setLogin: Boolean,
        authState: PlaybackAuthState,
        includeDataSyncId: Boolean,
    ) = httpClient.post("player") {
        ytClient(client, setLogin = setLogin, authState = authState)
        setBody(
            PlayerBody(
                context = client.toContext(
                    locale = locale,
                    visitorData = authState.visitorData,
                    dataSyncId = if (includeDataSyncId) authState.dataSyncId else null,
                ).let {
                    if (client.isEmbedded) {
                        it.copy(
                            thirdParty = Context.ThirdParty(
                                embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                            )
                        )
                    } else it
                },
                videoId = videoId,
                playlistId = playlistId,
                playbackContext = if (client.useSignatureTimestamp && signatureTimestamp != null) {
                    PlayerBody.PlaybackContext(
                        PlayerBody.PlaybackContext.ContentPlaybackContext(
                            signatureTimestamp
                        )
                    )
                } else null,
                serviceIntegrityDimensions = poToken?.let {
                    PlayerBody.ServiceIntegrityDimensions(poToken = it)
                },
            )
        )
    }

    private fun shouldRetryPlayerRequestWithoutDataSyncId(
        failure: Throwable,
        includeDataSyncId: Boolean,
    ): Boolean {
        if (!includeDataSyncId) return false
        val clientError = failure as? ClientRequestException ?: return false
        if (clientError.response.status != HttpStatusCode.BadRequest) return false
        val message = clientError.message.orEmpty()
        if (!message.contains("/youtubei/v1/player", ignoreCase = true)) return false
        return message.contains("INVALID_ARGUMENT", ignoreCase = true) ||
            message.contains("invalid argument", ignoreCase = true)
    }

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String?,
        elapsedSeconds: Double? = null,
        state: String? = null,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
        authState: PlaybackAuthState = currentAuthState(),
    ) = withRetry {
        httpClient.get(url) {
            ytPlaybackTrackingClient(client, authState = authState)
            parameterIfMissing(url, "ver", "2")
            parameterIfMissing(url, "c", client.clientName)
            parameterIfMissing(url, "cpn", cpn)

            if (elapsedSeconds != null) {
                val seconds = elapsedSeconds.coerceAtLeast(0.0)
                val formattedSeconds = seconds.toStatsParameter()
                parameterIfMissing(url, "st", "0")
                parameterIfMissing(url, "et", formattedSeconds)
                parameterIfMissing(url, "rt", formattedSeconds)
                parameterIfMissing(url, "rti", formattedSeconds)
            }

            if (!state.isNullOrBlank()) {
                parameterIfMissing(url, "state", state)
            }

            if (playlistId != null) {
                parameterIfMissing(url, "list", playlistId)
                parameterIfMissing(url, "referrer", "https://music.youtube.com/playlist?list=$playlistId")
            }
        }
    }

    private fun HttpRequestBuilder.parameterIfMissing(url: String, name: String, value: String) {
        if (!url.hasQueryParameter(name)) {
            parameter(name, value)
        }
    }

    private fun Double.toStatsParameter(): String =
        String.format(Locale.US, "%.3f", this).trimEnd('0').trimEnd('.').ifBlank { "0" }

    private fun String.hasQueryParameter(name: String): Boolean =
        runCatching { toHttpUrl().queryParameter(name) != null }.getOrDefault(false)

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ) = withRetry {
        httpClient.post("browse") {
            ytClient(client, setLogin = setLogin || useLoginForBrowse)
            setBody(
                BrowseBody(
                    context = client.toContext(
                        locale,
                        visitorData,
                        if (setLogin || useLoginForBrowse) dataSyncId else null
                    ),
                    browseId = browseId,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = withRetry {
        httpClient.post("next") {
            ytClient(client, setLogin = true)
            setBody(
                NextBody(
                    context = client.toContext(queueLocale, visitorData, dataSyncId),
                    videoId = videoId,
                    playlistId = playlistId,
                    playlistSetVideoId = playlistSetVideoId,
                    index = index,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = withRetry {
        httpClient.post("music/get_search_suggestions") {
            ytClient(client)
            setBody(
                GetSearchSuggestionsBody(
                    context = client.toContext(locale, visitorData, null),
                    input = input
                )
            )
        }
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = withRetry {
        httpClient.post("music/get_queue") {
            ytClient(client)
            setBody(
                GetQueueBody(
                    context = client.toContext(locale, visitorData, null),
                    videoIds = videoIds,
                    playlistId = playlistId
                )
            )
        }
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
            parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
            headers {
                append("Content-Type", "application/json")
            }
            setBody(
                GetTranscriptBody(
                    context = client.toContext(locale, null, null),
                    params = Base64.Default.encode(
                        "\n${11.toChar()}$videoId".encodeToByteArray()
                    )
                )
            )
        }
    }

    suspend fun getSwJsData() = withRetry { httpClient.get("https://music.youtube.com/sw.js_data") }


    suspend fun accountMenu(client: YouTubeClient) = withRetry {
        httpClient.post("account/account_menu") {
            ytClient(client, setLogin = true)
            setBody(AccountMenuBody(client.toContext(locale, visitorData, dataSyncId)))
        }
    }

    suspend fun accountChannels(client: YouTubeClient) = withRetry {
        httpClient.post("account/accounts_list") {
            ytClient(client, setLogin = true)
            setBody(AccountsListBody(client.toContext(locale, visitorData, dataSyncId)))
        }
    }

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.VideoTarget(videoId)
                )
            )
        }
    }

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("like/removelike") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.VideoTarget(videoId)
                )
            )
        }
    }

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
    ) = withRetry {
        httpClient.post("subscription/subscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId)
                )
            )
        }
    }

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
    ) = withRetry {
        httpClient.post("subscription/unsubscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId)
                )
            )
        }
    }

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        httpClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.PlaylistTarget(playlistId)
                )
            )
        }
    }

    suspend fun unlikePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        httpClient.post("like/removelike") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.PlaylistTarget(playlistId)
                )
            )
        }
    }

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddVideoAction(addedVideoId = videoId)
                    )
                )
            )
        }
    }

    suspend fun addSongsToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoIds: List<String>,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = videoIds.map { videoId ->
                        Action.AddVideoAction(addedVideoId = videoId)
                    },
                )
            )
        }
    }

    suspend fun addPlaylistToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        addPlaylistId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddPlaylistAction(addedFullListId = addPlaylistId)
                    )
                )
            )
        }
    }

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.RemoveVideoAction(
                            removedVideoId = videoId,
                            setVideoId = setVideoId,
                        )
                    )
                )
            )
        }
    }

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String?,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.MoveVideoAction(
                            movedSetVideoIdSuccessor = successorSetVideoId,
                            setVideoId = setVideoId,
                        )
                    )

                )
            )
        }
    }

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
        videoIds: List<String> = emptyList(),
    ) = withRetry {
        httpClient.post("playlist/create") {
            ytClient(client, true)
            setBody(
                CreatePlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    title = title,
                    videoIds = videoIds,
                )
            )
        }
    }

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.RenamePlaylistAction(
                            playlistName = name
                        )
                    )
                )
            )
        }
    }

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        httpClient.post("playlist/delete") {
            println("deleting $playlistId")
            ytClient(client, setLogin = true)
            setBody(
                PlaylistDeleteBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId
                )
            )
        }
    }

    private suspend fun returnYouTubeDislike(videoId: String) = withRetry {
        httpClient.get("https://returnyoutubedislikeapi.com/Votes?videoId=$videoId") {
            contentType(ContentType.Application.Json)
        }
    }


    suspend fun getMediaInfo(videoId: String): Result<MediaInfo> =
        runCatching {
            val response = next(client = YouTubeClient.WEB, videoId, null, null, null, null, null).body<NextResponse>()

            val baseForInfo =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoSecondaryInfoRenderer != null
                    }?.videoSecondaryInfoRenderer

            val baseForTitle =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoPrimaryInfoRenderer != null
                    }?.videoPrimaryInfoRenderer

            val returnYouTubeDislikeResponse =
                returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()

            return@runCatching MediaInfo(
                videoId = videoId,
                title = baseForTitle
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                author = baseForInfo
                    ?.owner
                    ?.videoOwnerRenderer
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                authorId =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId,
                authorThumbnail =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.find {
                            it.height == 48
                        }?.url
                        ?.replace("s48", "s960"),
                description = baseForInfo?.attributedDescription?.content,
                subscribers =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.subscriberCountText
                        ?.simpleText?.split(" ")?.firstOrNull(),
                uploadDate = baseForTitle?.dateText?.simpleText,
                viewCount = returnYouTubeDislikeResponse.viewCount,
                like = returnYouTubeDislikeResponse.likes,
                dislike = returnYouTubeDislikeResponse.dislikes,
            )

        }


}

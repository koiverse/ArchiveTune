package moe.koiverse.archivetune.lastfm

import moe.koiverse.archivetune.lastfm.models.Authentication
import moe.koiverse.archivetune.lastfm.models.Token
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object LastFM {
    var sessionKey: String? = null

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
            defaultRequest { url("https://ws.audioscrobbler.com/2.0/") }
            expectSuccess = true
        }
    }

    private fun Map<String, String>.apiSig(secret: String): String {
        val sorted = toSortedMap()
        val toHash = sorted.entries.joinToString("") { it.key + it.value } + secret
        val digest = MessageDigest.getInstance("MD5").digest(toHash.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun HttpRequestBuilder.lastfmParams(
        method: String,
        apiKey: String,
        secret: String,
        sessionKey: String? = null,
        extra: Map<String, String> = emptyMap(),
        format: String = "json"
    ) {
        contentType(ContentType.Application.FormUrlEncoded)
        userAgent("ArchiveTune (https://github.com/koiverse/ArchiveTune)")
        
        val paramsForSig = mutableMapOf(
            "api_key" to apiKey,
            "method" to method
        )
        sessionKey?.let { paramsForSig["sk"] = it }
        paramsForSig.putAll(extra)
        
        val apiSig = paramsForSig.apiSig(secret)
        
        setBody(FormDataContent(Parameters.build {
            append("method", method)
            append("api_key", apiKey)
            sessionKey?.let { append("sk", it) }
            extra.forEach { (k, v) -> append(k, v) }
            append("api_sig", apiSig)
            append("format", format)
        }))
    }

    /**
     * Step 1 of OAuth flow: Get an authentication token
     */
    suspend fun getToken() = runCatching {
        client.post {
            lastfmParams(
                method = "auth.getToken",
                apiKey = API_KEY,
                secret = SECRET
            )
        }.body<Token>()
    }

    /**
     * Step 2 of OAuth flow: Generate the authorization URL for the user to approve the token
     * User needs to visit this URL in a browser to authorize the application
     */
    fun getAuthorizationUrl(token: String): String {
        return "http://www.last.fm/api/auth/?api_key=$API_KEY&token=$token"
    }

    /**
     * Step 3 of OAuth flow: Exchange the authorized token for a session key
     * Call this after the user has authorized the token in their browser
     */
    suspend fun getSession(token: String) = runCatching {
        client.post {
            lastfmParams(
                method = "auth.getSession",
                apiKey = API_KEY,
                secret = SECRET,
                extra = mapOf("token" to token)
            )
        }.body<Authentication>()
    }

    /**
     * Legacy mobile session authentication (deprecated by Last.fm)
     * Use OAuth flow (getToken -> authorize -> getSession) instead
     * @deprecated Use OAuth flow methods instead
     */
    @Deprecated("Use OAuth flow (getToken, getAuthorizationUrl, getSession) instead")
    suspend fun getMobileSession(username: String, password: String) = runCatching {
        client.post {
            lastfmParams(
                method = "auth.getMobileSession",
                apiKey = API_KEY,
                secret = SECRET,
                extra = mapOf("username" to username, "password" to password)
            )
        }.body<Authentication>()
    }

    suspend fun updateNowPlaying(
        artist: String, track: String,
        album: String? = null, trackNumber: Int? = null, duration: Int? = null
    ) = runCatching {
        client.post {
            lastfmParams(
                method = "track.updateNowPlaying",
                apiKey = API_KEY,
                secret = SECRET,
                sessionKey = sessionKey!!,
                extra = buildMap {
                    put("artist", artist)
                    put("track", track)
                    album?.let { put("album", it) }
                    trackNumber?.let { put("trackNumber", it.toString()) }
                    duration?.let { put("duration", it.toString()) }
                }
            )
        }
    }

    suspend fun scrobble(
        artist: String, track: String, timestamp: Long,
        album: String? = null, trackNumber: Int? = null, duration: Int? = null
    ) = runCatching {
        client.post {
            lastfmParams(
                method = "track.scrobble",
                apiKey = API_KEY,
                secret = SECRET,
                sessionKey = sessionKey!!,
                extra = buildMap {
                    put("artist[0]", artist)
                    put("track[0]", track)
                    put("timestamp[0]", timestamp.toString())
                    album?.let { put("album[0]", it) }
                    trackNumber?.let { put("trackNumber[0]", it.toString()) }
                    duration?.let { put("duration[0]", it.toString()) }
                }
            )
        }
    }

    // (loaded from BuildConfig/GitHub Secrets)
    private var API_KEY = ""
    private var SECRET = ""

    fun initialize(apiKey: String, secret: String) {
        API_KEY = apiKey
        SECRET = secret
    }

    const val DEFAULT_SCROBBLE_DELAY_PERCENT = 0.5f
    const val DEFAULT_SCROBBLE_MIN_SONG_DURATION = 30
    const val DEFAULT_SCROBBLE_DELAY_SECONDS = 180
}

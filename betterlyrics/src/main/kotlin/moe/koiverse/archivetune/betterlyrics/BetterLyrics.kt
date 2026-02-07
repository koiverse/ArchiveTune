/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.betterlyrics

import moe.koiverse.archivetune.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BetterLyrics {
    private const val API_BASE_URL = "https://lyrics-api.boidu.dev/"
    private const val GET_LYRICS_PATH = "/getLyrics"
    private val jsonFormat by lazy {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
    
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(jsonFormat)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 20000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 20000
            }

            defaultRequest {
                url("https://lyrics-api.boidu.dev/")
            }
            
            // Don't throw on non-2xx responses, handle them gracefully
            expectSuccess = false
        }
    }

    var logger: ((String) -> Unit)? = null

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        album: String?,
        durationSeconds: Int,
    ): String? {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        val cleanAlbum = album?.trim().orEmpty()

        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return null

        logger?.invoke(
            buildString {
                append("Sending Request to: ")
                append(API_BASE_URL.trimEnd('/'))
                append(GET_LYRICS_PATH)
                append(" (s=")
                append(cleanTitle)
                append(", a=")
                append(cleanArtist)
                if (cleanAlbum.isNotBlank()) {
                    append(", al=")
                    append(cleanAlbum)
                }
                if (durationSeconds > 0) {
                    append(", d=")
                    append(durationSeconds)
                }
                append(")")
            }
        )
        
        return try {
            val response: HttpResponse = client.get(GET_LYRICS_PATH) {
                parameter("s", cleanTitle)
                parameter("a", cleanArtist)
                if (cleanAlbum.isNotBlank()) parameter("al", cleanAlbum)
                if (durationSeconds > 0) parameter("d", durationSeconds)
            }
            
            logger?.invoke("Response Status: ${response.status}")
    
            val responseText = response.bodyAsText()
            logger?.invoke("Raw Response: $responseText")

            if (!response.status.isSuccess()) {
                logger?.invoke("Request failed with status: ${response.status}")
                return null
            }

            val ttmlResponse = try {
                jsonFormat.decodeFromString<TTMLResponse>(responseText)
            } catch (e: Exception) {
                logger?.invoke("JSON Parse Error: ${e.message}")
                TTMLResponse("")
            }
            val ttml = ttmlResponse.ttml
            
            logger?.invoke("Parsed TTML - isBlank: ${ttml.isBlank()}, length: ${ttml.length}, first 50 chars: ${ttml.take(50)}")
            
            if (ttml.isNotBlank()) {
                logger?.invoke("Received TTML (length: ${ttml.length}): ${ttml.take(100)}...")
            } else {
                 logger?.invoke("Received empty TTML")
            }
            
            ttml.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logger?.invoke("Error fetching lyrics: ${e.stackTraceToString()}")
            null
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int = -1,
    ) = runCatching {
        require(title.isNotBlank() && artist.isNotBlank()) { "Song title and artist are required" }
        val ttml = fetchTTML(
            artist = artist,
            title = title,
            album = album,
            durationSeconds = durationSeconds,
        )
            ?: throw IllegalStateException("Lyrics unavailable")
        ttml
    }


    suspend fun getAllLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int = -1,
        callback: (String) -> Unit,
    ) {
        val result = getLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
        )
        result.onSuccess { ttml ->
            callback(ttml)
        }
    }
}

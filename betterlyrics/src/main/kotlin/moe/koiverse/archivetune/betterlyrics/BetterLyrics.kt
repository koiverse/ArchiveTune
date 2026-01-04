package moe.koiverse.archivetune.betterlyrics

import moe.koiverse.archivetune.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BetterLyrics {
    private const val API_BASE_URL = "https://lyrics-api.boidu.dev/"
    
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 20000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 20000
            }

            defaultRequest {
                url(API_BASE_URL)
            }
            
            // Don't throw on non-2xx responses, handle them gracefully
            expectSuccess = false
        }
    }

    var logger: ((String) -> Unit)? = null

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        album: String? = null,
        duration: Int = -1,

    ): String? {
        val urlBuilder = StringBuilder("/ttml/getLyrics")
        urlBuilder.append("?s=$title&a=$artist")
        if (album != null) urlBuilder.append("&al=$album")
        if (duration != -1) urlBuilder.append("&d=$duration")
        
        logger?.invoke("Sending Request to: $API_BASE_URL${urlBuilder.toString().trimStart('/')}")
        
        return try {
            val response: HttpResponse = client.get("/ttml/getLyrics") {
                parameter("s", title)
                parameter("a", artist)
                album?.let { parameter("al", it) }
                if (duration != -1) {
                    parameter("d", duration)
                }
            }
            
            logger?.invoke("Response Status: ${response.status}")
    
            if (!response.status.isSuccess()) {
                logger?.invoke("Request failed with status: ${response.status}")
                return null
            }
            
            val ttmlResponse = response.body<TTMLResponse>()
            val ttml = ttmlResponse.ttml
            
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
        album: String?,
        duration: Int,
    ) = runCatching {
        val ttml = fetchTTML(artist, title, album, duration)
            ?: throw IllegalStateException("Lyrics unavailable")
        ttml
    }


    suspend fun getAllLyrics(
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        val result = getLyrics(title, artist, album, duration)
        result.onSuccess { ttml ->
            callback(ttml)
        }
    }
}

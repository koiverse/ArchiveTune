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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BetterLyrics {
    private const val API_BASE_URL = "https://lyrics-api.boidu.dev/"
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
    ): String? {
        val urlBuilder = StringBuilder("/ttml/getLyrics")
        urlBuilder.append("?s=$title&a=$artist")
        
        logger?.invoke("Sending Request to: $API_BASE_URL${urlBuilder.toString().trimStart('/')}")
        
        return try {
            val response: HttpResponse = client.get("/ttml/getLyrics") {
                parameter("s", title)
                parameter("a", artist)
            }
            
            logger?.invoke("Response Status: ${response.status}")
    
            if (!response.status.isSuccess()) {
                logger?.invoke("Request failed with status: ${response.status}")
                return null
            }
            
            val responseText = response.bodyAsText()
            logger?.invoke("Raw Response: $responseText")

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
    ) = runCatching {
        val ttml = fetchTTML(artist, title)
            ?: throw IllegalStateException("Lyrics unavailable")
        ttml
    }


    suspend fun getAllLyrics(
        title: String,
        artist: String,
        callback: (String) -> Unit,
    ) {
        val result = getLyrics(title, artist)
        result.onSuccess { ttml ->
            callback(ttml)
        }
    }
}

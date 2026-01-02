package moe.koiverse.archivetune.betterlyrics

import moe.koiverse.archivetune.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
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
        HttpClient(CIO) {
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

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        duration: Int = -1,
    ): String? = runCatching {
        val response: HttpResponse = client.get("/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            if (duration != -1) {
                parameter("d", duration)
            }
        }
        
        if (!response.status.isSuccess()) {
            return@runCatching null
        }
        
        val ttmlResponse = response.body<TTMLResponse>()
        ttmlResponse.ttml.takeIf { it.isNotBlank() }
    }.getOrNull()

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
    ) = runCatching {
        val ttml = fetchTTML(artist, title, duration)
            ?: throw IllegalStateException("Lyrics unavailable")
        ttml
    }


    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        val result = getLyrics(title, artist, duration)
        result.onSuccess { ttml ->
            callback(ttml)
        }
    }
}

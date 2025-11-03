package moe.koiverse.archivetune.betterlyrics

import moe.koiverse.archivetune.betterlyrics.models.SearchResponse
import moe.koiverse.archivetune.betterlyrics.models.Track
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.math.abs

object BetterLyrics {
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
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url("https://lyrics-api-go-better-lyrics-api-pr-12.up.railway.app")
            }

            expectSuccess = true
        }
    }

    private suspend fun searchLyrics(
        artist: String,
        title: String,
        duration: Int = -1,
    ): List<Track> = runCatching {
        val url = buildString {
            append("/getLyrics?s=")
            append(title)
            append("&a=")
            append(artist)
            if (duration != -1) {
                append("&d=")
                append(duration)
            }
        }
        client.get(url).body<SearchResponse>().results
    }.getOrElse { emptyList() }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
    ) = runCatching {
        val tracks = searchLyrics(artist, title, duration)
        
        if (tracks.isEmpty()) {
            throw IllegalStateException("Lyrics unavailable")
        }

        // Find best matching track by duration
        val bestMatch = if (duration == -1) {
            tracks.firstOrNull { it.lyrics != null }
        } else {
            tracks
                .filter { it.lyrics != null }
                .minByOrNull { abs(it.duration - duration) }
        }

        val lyrics = bestMatch?.lyrics 
            ?: throw IllegalStateException("Lyrics unavailable")

        // Convert to LRC format with word timestamps as comments
        buildLrcString(lyrics)
    }

    private fun buildLrcString(lyrics: moe.koiverse.archivetune.betterlyrics.models.Lyrics): String {
        return buildString {
            lyrics.lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10
                
                appendLine(String.format("[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, line.text))
                
                // Add word-level timestamps as special comments if available
                if (!line.words.isNullOrEmpty()) {
                    val wordsData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordsData>")
                }
            }
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        val tracks = searchLyrics(artist, title, duration)
        
        tracks
            .filter { it.lyrics != null }
            .take(5)
            .forEach { track ->
                val lrcString = buildLrcString(track.lyrics!!)
                callback(lrcString)
            }
    }
}

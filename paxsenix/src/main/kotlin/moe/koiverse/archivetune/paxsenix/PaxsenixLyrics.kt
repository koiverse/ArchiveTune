/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.paxsenix

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import moe.koiverse.archivetune.paxsenix.models.*
import kotlin.math.abs

import java.util.Locale

object PaxsenixLyrics {
    private const val BASE_URL = "https://lyrics.paxsenix.org/"

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url(BASE_URL)
                header(HttpHeaders.UserAgent, "ArchiveTune-Lyrics-Fetcher/1.0 (https://github.com/koiverse/archivetune)")
                header(HttpHeaders.Accept, "application/json, text/plain, */*")
                header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
            }

            expectSuccess = false
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        val durationMs = if (durationSeconds > 0) durationSeconds * 1000L else 0L
        System.err.println("PaxsenixLyrics: Querying for [$title] by [$artist] (Duration: $durationSeconds s)")
        
        // Try NetEase first as it returns LRC directly
        try {
            val query = "$title $artist"
            val neteaseSearch = client.get("netease/search") {
                parameter("q", query)
            }
            
            if (neteaseSearch.status == HttpStatusCode.OK) {
                val searchResponse = neteaseSearch.body<NeteaseSearchResponse>()
                val songs = searchResponse.result?.songs ?: emptyList()
                
                val bestMatch = if (durationMs > 0) {
                    songs.minByOrNull { abs(it.duration - durationMs) }
                } else {
                    songs.firstOrNull()
                }
                
                if (bestMatch != null) {
                    System.err.println("PaxsenixLyrics: Best NetEase match: ${bestMatch.name} (ID: ${bestMatch.id}, Duration: ${bestMatch.duration})")
                    if (durationMs <= 0 || (abs(bestMatch.duration - durationMs) < 10000)) {
                        val lyricsResponse = client.get("netease/lyrics") {
                            parameter("id", bestMatch.id)
                        }.body<NeteaseLyricsResponse>()
                        
                        lyricsResponse.lrc?.lyric?.let { 
                            if (it.isNotBlank()) {
                                System.err.println("PaxsenixLyrics: SUCCESS from NetEase")
                                return@runCatching it 
                            }
                        }
                    } else {
                        System.err.println("PaxsenixLyrics: NetEase match duration mismatch (diff: ${abs(bestMatch.duration - durationMs)})")
                    }
                }
            } else {
                System.err.println("PaxsenixLyrics: NetEase search failed with status: ${neteaseSearch.status}")
            }
        } catch (e: Exception) {
            System.err.println("PaxsenixLyrics: NetEase error: ${e.message}")
        }

        // Try Apple Music
        try {
            val query = "$title $artist"
            val amSearch = client.get("apple-music/search") {
                parameter("q", query)
            }
            
            if (amSearch.status == HttpStatusCode.OK) {
                val items = amSearch.body<List<AppleMusicSearchItem>>()
                val bestMatch = if (durationMs > 0) {
                    items.minByOrNull { abs(it.duration - durationMs) }
                } else {
                    items.firstOrNull()
                }
                
                if (bestMatch != null) {
                    System.err.println("PaxsenixLyrics: Best Apple Music match: ${bestMatch.songName} (ID: ${bestMatch.id}, Duration: ${bestMatch.duration})")
                    if (durationMs <= 0 || (abs(bestMatch.duration - durationMs) < 10000)) {
                        val lyricsResponse = client.get("apple-music/lyrics") {
                            parameter("id", bestMatch.id)
                        }.body<AppleMusicLyricsResponse>()
                        
                        if (lyricsResponse.content.isNotEmpty()) {
                            System.err.println("PaxsenixLyrics: SUCCESS from Apple Music")
                            return@runCatching convertAppleMusicToLrc(lyricsResponse)
                        }
                    } else {
                        System.err.println("PaxsenixLyrics: Apple Music match duration mismatch (diff: ${abs(bestMatch.duration - durationMs)})")
                    }
                }
            } else {
                System.err.println("PaxsenixLyrics: Apple Music search failed with status: ${amSearch.status}")
            }
        } catch (e: Exception) {
            System.err.println("PaxsenixLyrics: Apple Music error: ${e.message}")
        }
        
        throw IllegalStateException("Lyrics unavailable from Paxsenix for $title")
    }

    private fun convertAppleMusicToLrc(response: AppleMusicLyricsResponse): String {
        return response.content.joinToString("\n") { line ->
            val minutes = line.timestamp / 1000 / 60
            val seconds = (line.timestamp / 1000) % 60
            val hundredths = (line.timestamp % 1000) / 10
            val time = String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, hundredths)
            val text = line.text.joinToString("") { it.text }
            "$time$text"
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration).onSuccess(callback)
    }
}

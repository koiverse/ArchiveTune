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
import kotlinx.serialization.json.*
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

    private fun resolveDurationMs(duration: Int): Long = when {
        duration <= 0 -> 0L
        duration > 360000 -> duration.toLong() // 1h+ is likely ms (360k ms = 6 min)
        else -> duration * 1000L // Likely seconds
    }

    private fun cleanJsonLyrics(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            runCatching { Json.decodeFromString<String>(trimmed) }.getOrDefault(trimmed)
        } else trimmed
    }

    suspend fun getAppleMusicLyrics(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        val durationMs = resolveDurationMs(durationSeconds)
        val query = "$title $artist"
        val amSearch = client.get("apple-music/search") {
            parameter("q", query)
        }

        if (amSearch.status == HttpStatusCode.OK) {
            val items = amSearch.body<List<AppleMusicSearchItem>>()
            val bestMatch = if (durationMs > 0) {
                items.minByOrNull { abs(it.duration.toLong() - durationMs) }
            } else {
                items.firstOrNull()
            }

            if (bestMatch != null) {
                val diff = abs(bestMatch.duration.toLong() - durationMs)
                System.err.println("PaxsenixLyrics: Best Apple Music match: ${bestMatch.songName} (ID: ${bestMatch.id}, Duration: ${bestMatch.duration}, Diff: $diff)")
                if (durationMs <= 0 || (diff < 10000)) {
                    val lyricsResponse = client.get("apple-music/lyrics") {
                        parameter("id", bestMatch.id)
                        parameter("ttml", "true")
                    }

                    System.err.println("PaxsenixLyrics: Apple Music lyrics (TTML) status: ${lyricsResponse.status}")
                    if (lyricsResponse.status == HttpStatusCode.OK) {
                        try {
                            val rawBody = lyricsResponse.body<String>().trim()
                            
                            // Case 1: Direct XML response
                            if (rawBody.startsWith("<tt") || rawBody.startsWith("<?xml")) {
                                System.err.println("PaxsenixLyrics: SUCCESS from Apple Music (Direct TTML)")
                                return@runCatching rawBody
                            }

                            // Case 2: JSON-wrapped XML response
                            val data = Json.decodeFromString<JsonObject>(rawBody)
                            val content = data["content"]?.jsonPrimitive?.content
                            if (content != null && (content.contains("<tt") || content.contains("<?xml"))) {
                                System.err.println("PaxsenixLyrics: SUCCESS from Apple Music (JSON-wrapped TTML, Length: ${content.length})")
                                return@runCatching content
                            } else {
                                System.err.println("PaxsenixLyrics: Apple Music TTML content was null or invalid. Type: ${data["type"]}")
                            }
                        } catch (e: Exception) {
                            System.err.println("PaxsenixLyrics: Error parsing Apple Music TTML: ${e.message}")
                        }
                    }

                    val jsonResponse = client.get("apple-music/lyrics") {
                        parameter("id", bestMatch.id)
                    }
                    System.err.println("PaxsenixLyrics: Apple Music lyrics (JSON) status: ${jsonResponse.status}")
                    if (jsonResponse.status == HttpStatusCode.OK) {
                        val lyricsData = jsonResponse.body<AppleMusicLyricsResponse>()
                        if (lyricsData.content.isNotEmpty()) {
                            System.err.println("PaxsenixLyrics: SUCCESS from Apple Music (LRC Fallback)")
                            return@runCatching convertAppleMusicToLrc(lyricsData)
                        }
                    }
                }
            }
        }
        throw IllegalStateException("Apple Music lyrics unavailable")
    }

    suspend fun getNeteaseLyrics(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        val durationMs = resolveDurationMs(durationSeconds)
        val query = "$title $artist"
        val neteaseSearch = client.get("netease/search") {
            parameter("q", query)
        }

        if (neteaseSearch.status == HttpStatusCode.OK) {
            val searchResponse = neteaseSearch.body<NeteaseSearchResponse>()
            val songs = searchResponse.result?.songs ?: emptyList()

            val bestMatch = if (durationMs > 0) {
                songs.minByOrNull { abs(it.duration.toLong() - durationMs) }
            } else {
                songs.firstOrNull()
            }

            if (bestMatch != null) {
                val diff = abs(bestMatch.duration.toLong() - durationMs)
                System.err.println("PaxsenixLyrics: Best NetEase match: ${bestMatch.name} (ID: ${bestMatch.id}, Duration: ${bestMatch.duration}, Diff: $diff)")
                if (durationMs <= 0 || (diff < 10000)) {
                    val lyricsResponse = client.get("netease/lyrics") {
                        parameter("id", bestMatch.id)
                        parameter("word", "true")
                    }

                    System.err.println("PaxsenixLyrics: NetEase lyrics status: ${lyricsResponse.status}")
                    if (lyricsResponse.status == HttpStatusCode.OK) {
                        val lyricsData = lyricsResponse.body<JsonObject>()
                        
                        // Try to get word-by-word (klyric) first
                        val klyric = lyricsData["klyric"]?.jsonObject?.get("lyric")?.jsonPrimitive?.content
                        if (!klyric.isNullOrBlank()) {
                            System.err.println("PaxsenixLyrics: SUCCESS from NetEase (Karaoke)")
                            return@runCatching klyric
                        }

                        // Fallback to normal lyric (lrc)
                        val lrc = lyricsData["lrc"]?.jsonObject?.get("lyric")?.jsonPrimitive?.content
                        if (!lrc.isNullOrBlank()) {
                            System.err.println("PaxsenixLyrics: SUCCESS from NetEase (LRC)")
                            return@runCatching lrc
                        }
                    }
                }
            }
        }
        throw IllegalStateException("NetEase lyrics unavailable")
    }

    suspend fun getSpotifyLyrics(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        val durationMs = resolveDurationMs(durationSeconds)
        val query = "$title $artist"
        val spotifySearch = client.get("spotify/search") {
            parameter("q", query)
        }
        if (spotifySearch.status == HttpStatusCode.OK) {
            val items = spotifySearch.body<List<PaxsenixSearchItem>>()
            val bestMatch = if (durationMs > 0) {
                items.minByOrNull { abs(it.durationMs - durationMs) }
            } else {
                items.firstOrNull()
            }

            if (bestMatch != null) {
                val diff = abs(bestMatch.durationMs - durationMs)
                System.err.println("PaxsenixLyrics: Best Spotify match: ${bestMatch.name ?: bestMatch.title} (ID: ${bestMatch.realId}, Duration: ${bestMatch.durationMs}, Diff: $diff)")
                if (durationMs <= 0 || (diff < 10000)) {
                    val lyricsResponse = client.get("spotify/lyrics") {
                        parameter("id", bestMatch.realId)
                    }
                    System.err.println("PaxsenixLyrics: Spotify lyrics status: ${lyricsResponse.status}")
                    if (lyricsResponse.status == HttpStatusCode.OK) {
                        val data = cleanJsonLyrics(lyricsResponse.body<String>())
                        if (data.isNotBlank()) {
                            System.err.println("PaxsenixLyrics: SUCCESS from Spotify")
                            return@runCatching data
                        }
                    }
                }
            }
        }
        throw IllegalStateException("Spotify lyrics unavailable")
    }

    suspend fun getMusixmatchLyrics(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        val query = "$title $artist"
        System.err.println("PaxsenixLyrics: Requesting Musixmatch lyrics for: $query (Duration: $durationSeconds)")

        // Try word-by-word first
        val mxmWord = client.get("musixmatch/lyrics") {
            parameter("q", query)
            parameter("t", title)
            parameter("a", artist)
            parameter("duration", durationSeconds.toString())
            parameter("type", "word")
        }
        if (mxmWord.status == HttpStatusCode.OK) {
            val data = cleanJsonLyrics(mxmWord.body<String>())
            if (data.isNotBlank() && !data.contains("\"error\"")) {
                System.err.println("PaxsenixLyrics: SUCCESS from Musixmatch (Word)")
                return@runCatching data
            }
        }

        // Fallback to default
        val mxmLyrics = client.get("musixmatch/lyrics") {
            parameter("q", query)
            parameter("t", title)
            parameter("a", artist)
            parameter("duration", durationSeconds.toString())
        }
        System.err.println("PaxsenixLyrics: Musixmatch lyrics status: ${mxmLyrics.status}")
        if (mxmLyrics.status == HttpStatusCode.OK) {
            val data = cleanJsonLyrics(mxmLyrics.body<String>())
            if (data.isNotBlank() && !data.contains("\"error\"")) {
                System.err.println("PaxsenixLyrics: SUCCESS from Musixmatch")
                return@runCatching data
            }
        }
        throw IllegalStateException("Musixmatch lyrics unavailable")
    }

    suspend fun getKugouLyrics(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        val durationMs = resolveDurationMs(durationSeconds)
        val query = "$title $artist"
        val kugouSearch = client.get("kugou/search") {
            parameter("q", query)
        }
        if (kugouSearch.status == HttpStatusCode.OK) {
            val items = kugouSearch.body<List<PaxsenixSearchItem>>()
            val bestMatch = if (durationMs > 0) {
                items.minByOrNull { abs(it.durationMs - durationMs) }
            } else {
                items.firstOrNull()
            }

            if (bestMatch != null) {
                val diff = abs(bestMatch.durationMs - durationMs)
                if (durationMs <= 0 || (diff < 10000)) {
                    val lyricsResponse = client.get("kugou/lyrics") {
                        parameter("id", bestMatch.id ?: "")
                        parameter("word", "true")
                    }
                    if (lyricsResponse.status == HttpStatusCode.OK) {
                        val data = lyricsResponse.body<JsonObject>()
                        val lyrics = data["lyrics"]?.jsonPrimitive?.content
                        if (!lyrics.isNullOrBlank()) {
                            return@runCatching lyrics
                        }
                    }
                }
            }
        }
        throw IllegalStateException("KuGou lyrics unavailable")
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<String> = runCatching {
        System.err.println("PaxsenixLyrics: --- Starting search for [$title] by [$artist] ---")
        
        getAppleMusicLyrics(title, artist, durationSeconds).getOrNull()?.let { 
            System.err.println("PaxsenixLyrics: Search FINISHED (Apple Music)")
            return@runCatching it 
        }
        
        getNeteaseLyrics(title, artist, durationSeconds).getOrNull()?.let { 
            System.err.println("PaxsenixLyrics: Search FINISHED (NetEase)")
            return@runCatching it 
        }
        
        getSpotifyLyrics(title, artist, durationSeconds).getOrNull()?.let { 
            System.err.println("PaxsenixLyrics: Search FINISHED (Spotify)")
            return@runCatching it 
        }
        
        getMusixmatchLyrics(title, artist, durationSeconds).getOrNull()?.let { 
            System.err.println("PaxsenixLyrics: Search FINISHED (Musixmatch)")
            return@runCatching it 
        }

        getKugouLyrics(title, artist, durationSeconds).getOrNull()?.let { 
            System.err.println("PaxsenixLyrics: Search FINISHED (KuGou)")
            return@runCatching it 
        }
        
        System.err.println("PaxsenixLyrics: Search FAILED - No providers found lyrics")
        throw IllegalStateException("Lyrics unavailable from Paxsenix for $title")
    }

    private fun convertAppleMusicToLrc(response: AppleMusicLyricsResponse): String {
        return response.content.joinToString("\n") { line ->
            val minutes = line.timestamp / 1000 / 60
            val seconds = (line.timestamp / 1000) % 60
            val hundredths = (line.timestamp % 1000) / 10
            val time = String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, hundredths)
            val text = line.text.joinToString(" ") { it.text.trim() }
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

    suspend fun getStats(): Result<PaxsenixStats> = runCatching {
        client.get("api/stats").body<PaxsenixStats>()
    }
}

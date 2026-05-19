/*
 * ArchiveTune (2026)
 * Â© Chartreux Westia â€” github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.ai

import java.util.Locale
import kotlinx.coroutines.flow.first
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.Album
import moe.koiverse.archivetune.db.entities.Artist
import moe.koiverse.archivetune.db.entities.Song

class AiUserMixGenerator(
    private val database: MusicDatabase,
) {
    suspend fun generate(
        config: AiServiceConfig,
        count: Int,
    ): List<AiUserMix> {
        val snapshot = buildSnapshot()
        if (snapshot.isBlank()) {
            throw AiServiceException("Not enough listening history to build an AI mix")
        }
        val requestedCount = count.coerceIn(1, 10)
        val response = AiTextService.complete(
            config = config,
            systemPrompt = """
                You create concise YouTube Music search mixes from listening history.
                Return only a JSON array with exactly $requestedCount objects.
                Each object must contain "title" and "query".
                Titles must be short, personal, and under 32 characters.
                Queries must be specific enough for YouTube Music search and should combine artist, mood, genre, era, or song cues.
                Do not include explanations, markdown, or duplicate ideas.
            """.trimIndent(),
            userPrompt = snapshot,
            temperature = 0.35,
            maxTokens = 2048,
        )
        val array = extractJsonArray(response)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                val query = item.optString("query").trim()
                if (title.isBlank() || query.isBlank()) continue
                add(AiUserMixJson.fromAiResult(title = title, query = query, index = index))
                if (size == requestedCount) break
            }
        }.also {
            if (it.isEmpty()) throw AiServiceException("AI did not return usable mix suggestions")
        }
    }

    private suspend fun buildSnapshot(): String {
        val now = System.currentTimeMillis()
        val since = now - 1000L * 60L * 60L * 24L * 90L
        val topSongs = database.mostPlayedSongs(fromTimeStamp = since, limit = 20).first()
        val topArtists = database.mostPlayedArtists(fromTimeStamp = since, limit = 12).first()
        val topAlbums = database.mostPlayedAlbums(fromTimeStamp = since, limit = 10).first()
        val recentSongs = database.events().first().take(30).map { it.song }
        val likedSongs = database.likedSongsByPlayTimeAsc().first().takeLast(20).asReversed()
        return buildString {
            appendSection("Top songs", topSongs.map(::songLine))
            appendSection("Recent songs", recentSongs.map(::songLine).distinct())
            appendSection("Liked songs", likedSongs.map(::songLine).distinct())
            appendSection("Top artists", topArtists.map(::artistLine))
            appendSection("Top albums", topAlbums.map(::albumLine))
        }.trim()
    }

    private fun StringBuilder.appendSection(
        title: String,
        items: List<String>,
    ) {
        val cleanItems = items.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(24)
        if (cleanItems.isEmpty()) return
        appendLine(title)
        cleanItems.forEachIndexed { index, item ->
            appendLine("${index + 1}. $item")
        }
        appendLine()
    }

    private fun songLine(song: Song): String {
        val artists = song.artists.joinToString { it.name }.ifBlank { "Unknown artist" }
        val album = song.album?.title?.takeIf { it.isNotBlank() }
        return listOfNotNull(song.title, artists, album).joinToString(" - ")
    }

    private fun artistLine(artist: Artist): String =
        artist.title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private fun albumLine(album: Album): String {
        val artists = album.artists.joinToString { it.name }.ifBlank { "Unknown artist" }
        return "${album.title} - $artists"
    }
}

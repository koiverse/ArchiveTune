/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.utils

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class SavedArtwork(
    val songId: String,
    val thumbnail: String? = null,
    val artist: String? = null
)

object ArtworkStorage {
    private const val FILENAME = "archivetune_saved_artworks.json"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun fileFor(context: Context): File = File(context.filesDir, FILENAME)

    fun loadAll(context: Context): List<SavedArtwork> {
        try {
            val f = fileFor(context)
            if (!f.exists()) return emptyList()
            val text = f.readText()
            if (text.isBlank()) return emptyList()
            return json.decodeFromString(text)
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun findBySongId(context: Context, songId: String): SavedArtwork? = loadAll(context).firstOrNull { it.songId == songId }

    fun saveOrUpdate(context: Context, artwork: SavedArtwork) {
        try {
            val list = loadAll(context).toMutableList()
            val idx = list.indexOfFirst { it.songId == artwork.songId }
            if (idx >= 0) list[idx] = artwork else list.add(artwork)
            fileFor(context).writeText(json.encodeToString(list))
        } catch (_: Exception) {
            // ignore write errors
        }
    }

    fun clear(context: Context) {
        try {
            val f = fileFor(context)
            if (f.exists()) f.writeText("[]")
        } catch (_: Exception) {
        }
    }

    fun removeBySongId(context: Context, songId: String) {
        try {
            val list = loadAll(context).toMutableList()
            val idx = list.indexOfFirst { it.songId == songId }
            if (idx >= 0) {
                list.removeAt(idx)
                fileFor(context).writeText(json.encodeToString(list))
            }
        } catch (_: Exception) {
        }
    }
}

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.lyrics

import android.content.Context
import moe.koiverse.archivetune.constants.EnablePaxsenixMusixmatchLyricsKey
import moe.koiverse.archivetune.paxsenix.PaxsenixLyrics
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get

object PaxsenixMusixmatchLyricsProvider : LyricsProvider {
    override val name = "Paxsenix: Musixmatch"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixMusixmatchLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = PaxsenixLyrics.getMusixmatchLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getLyrics(id, title, artist, album, duration).onSuccess(callback)
    }
}

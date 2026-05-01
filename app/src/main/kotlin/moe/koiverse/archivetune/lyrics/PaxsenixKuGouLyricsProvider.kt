/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.lyrics

import android.content.Context
import moe.koiverse.archivetune.constants.EnablePaxsenixKuGouLyricsKey
import moe.koiverse.archivetune.paxsenix.PaxsenixLyrics
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get

object PaxsenixKuGouLyricsProvider : LyricsProvider {
    override val name = "Paxsenix: KuGou"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixKuGouLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = PaxsenixLyrics.getKugouLyrics(title, artist, duration)

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

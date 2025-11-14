package moe.koiverse.archivetune.lyrics

import android.content.Context
import moe.koiverse.archivetune.betterlyrics.BetterLyrics
import moe.koiverse.archivetune.constants.EnableBetterLyricsKey
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(title, artist, duration, callback)
    }
}

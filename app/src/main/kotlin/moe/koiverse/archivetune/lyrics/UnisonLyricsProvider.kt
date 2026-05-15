/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */


package moe.koiverse.archivetune.lyrics

import android.content.Context
import android.util.Log
import moe.koiverse.archivetune.constants.EnableUnisonLyricsKey
import moe.koiverse.archivetune.unison.Unison
import moe.koiverse.archivetune.utils.GlobalLog
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get

object UnisonLyricsProvider : LyricsProvider {
    init {
        Unison.logger = { message ->
            GlobalLog.append(Log.INFO, "Unison", message)
        }
    }

    override val name = "Unison"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableUnisonLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = Unison.getLyrics(
        videoId = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = duration,
    )

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        Unison.getAllLyrics(
            videoId = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
            callback = callback,
        )
    }
}

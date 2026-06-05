/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.LyricsEntity
import moe.koiverse.archivetune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND

@Composable
internal fun lyricsSourceLabel(
    lyricsEntity: LyricsEntity?,
    lyrics: String?,
): String? {
    val sourceName = lyricsEntity?.source?.trim()?.takeIf { it.isNotEmpty() }?.let { source ->
        when (source) {
            LyricsEntity.Source.REMOTE.value,
            LyricsEntity.Source.USER_SELECTION.value -> null
            LyricsEntity.Source.USER_EDIT.value -> stringResource(R.string.lyrics_source_user_edit)
            LyricsEntity.Source.AI_TRANSLATION.value -> stringResource(R.string.lyrics_source_ai_translation)
            else -> source
        }
    }
    return if (lyrics != null && lyrics != LYRICS_NOT_FOUND && sourceName != null) {
        stringResource(R.string.lyrics_source_format, sourceName)
    } else {
        null
    }
}

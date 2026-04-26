/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.paxsenix.models

import kotlinx.serialization.Serializable

@Serializable
data class AppleMusicSearchItem(
    val id: String,
    val songName: String,
    val artistName: String,
    val duration: Int
)

@Serializable
data class AppleMusicLyricsResponse(
    val type: String? = null,
    val content: List<AppleMusicLine> = emptyList()
)

@Serializable
data class AppleMusicLine(
    val timestamp: Long,
    val text: List<AppleMusicWord> = emptyList()
)

@Serializable
data class AppleMusicWord(
    val text: String
)

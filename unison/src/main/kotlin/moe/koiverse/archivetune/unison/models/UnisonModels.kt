/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.unison.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnisonEntry(
    val id: Long,
    @SerialName("videoId") val videoId: String? = null,
    val song: String,
    val artist: String,
    val lyrics: String,
    val format: String,
    val syncType: String,
    val score: Double = 0.0,
    val effectiveScore: Double = 0.0,
    val voteCount: Int = 0,
    val confidence: String = "low",
    val language: String? = null,
)

@Serializable
data class UnisonResponse(
    val success: Boolean,
    val data: UnisonEntry? = null,
)

@Serializable
data class UnisonSearchResponse(
    val success: Boolean,
    val data: List<UnisonEntry>? = null,
)

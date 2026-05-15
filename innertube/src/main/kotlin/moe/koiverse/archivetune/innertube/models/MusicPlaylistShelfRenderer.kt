/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package moe.koiverse.archivetune.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicPlaylistShelfRenderer(
    val playlistId: String?,
    val contents: List<MusicShelfRenderer.Content> = emptyList(),
    val collapsedItemCount: Int? = null,
    val continuations: List<Continuation>? = null,
)

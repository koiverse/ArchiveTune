/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package moe.koiverse.archivetune.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class TwoColumnBrowseResultsRenderer(
    val secondaryContents: SecondaryContents?,
    val tabs: List<Tabs.Tab>?
) {
    @Serializable
    data class SecondaryContents(
        val sectionListRenderer: SectionListRenderer?
    )

    @Serializable
    data class SectionListRenderer(
        val contents: List<Content>?,
        val continuations: List<Continuation>?,
    ) {
        @Serializable
        data class Content(
            val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?,
            val musicShelfRenderer: MusicShelfRenderer?
        )
    }
}

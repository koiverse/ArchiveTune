/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.innertube.models.response

import moe.koiverse.archivetune.innertube.models.Continuation
import moe.koiverse.archivetune.innertube.models.ContinuationItemRenderer
import moe.koiverse.archivetune.innertube.models.MusicResponsiveListItemRenderer
import moe.koiverse.archivetune.innertube.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?,
    )

    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfContinuation,
    ) {
        @Serializable
        data class MusicShelfContinuation(
            val contents: List<Content>,
            val continuations: List<Continuation>?,
        ) {
            @Serializable
            data class Content(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
                val continuationItemRenderer: ContinuationItemRenderer? = null,
            )
        }
    }
}

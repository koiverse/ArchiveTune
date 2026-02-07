/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class AddItemYouTubePlaylistResponse(
    val status: String,
    val playlistEditResults: List<PlaylistEditResult>
) {
    @Serializable
    data class PlaylistEditResult(
        val playlistEditVideoAddedResultData: PlaylistEditVideoAddedResultData,
    ) {
        @Serializable
        data class PlaylistEditVideoAddedResultData(
            val setVideoId: String,
            val videoId: String
        )
    }
}

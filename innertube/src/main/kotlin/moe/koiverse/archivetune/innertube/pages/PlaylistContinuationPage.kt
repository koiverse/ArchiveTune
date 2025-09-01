package moe.koiverse.archivetune.innertube.pages

import moe.koiverse.archivetune.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)

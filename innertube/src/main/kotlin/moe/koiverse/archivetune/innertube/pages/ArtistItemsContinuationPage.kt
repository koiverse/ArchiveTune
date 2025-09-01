package moe.koiverse.archivetune.innertube.pages

import moe.koiverse.archivetune.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)

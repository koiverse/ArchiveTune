package moe.koiverse.archivetune.innertube.pages

import moe.koiverse.archivetune.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)

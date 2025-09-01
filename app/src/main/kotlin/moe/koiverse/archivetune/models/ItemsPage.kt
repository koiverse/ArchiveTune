package moe.koiverse.archivetune.models

import moe.koiverse.archivetune.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)

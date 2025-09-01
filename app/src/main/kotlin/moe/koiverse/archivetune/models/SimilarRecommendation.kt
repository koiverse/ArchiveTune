package moe.koiverse.archivetune.models

import moe.koiverse.archivetune.innertube.models.YTItem
import moe.koiverse.archivetune.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)

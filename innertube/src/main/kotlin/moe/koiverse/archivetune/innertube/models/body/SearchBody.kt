package moe.koiverse.archivetune.innertube.models.body

import moe.koiverse.archivetune.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)

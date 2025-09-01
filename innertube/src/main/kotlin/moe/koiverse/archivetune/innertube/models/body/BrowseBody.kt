package moe.koiverse.archivetune.innertube.models.body

import moe.koiverse.archivetune.innertube.models.Context
import moe.koiverse.archivetune.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)

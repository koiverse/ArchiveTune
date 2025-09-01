package moe.koiverse.archivetune.innertube.models.body

import moe.koiverse.archivetune.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeBody(
    val channelIds: List<String>,
    val context: Context,
)

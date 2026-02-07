/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ContinuationItemRenderer(
    val continuationEndpoint: ContinuationEndpoint?,
) {
    @Serializable
    data class ContinuationEndpoint(
        val continuationCommand: ContinuationCommand?,
    ) {
        @Serializable
        data class ContinuationCommand(
            val token: String?,
        )
    }
}
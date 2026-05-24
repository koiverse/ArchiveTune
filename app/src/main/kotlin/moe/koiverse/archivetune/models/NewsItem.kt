/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class NewsItem(
    @SerialName("Title") val title: String,
    @SerialName("Description") val description: String,
    @SerialName("ImageURL") val imageUrl: String? = null,
    @SerialName("Important") val important: Boolean = false,
    @SerialName("Author") val author: String,
    @SerialName("Date") val date: String,
)

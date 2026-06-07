/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPaging<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val href: String? = null,
)

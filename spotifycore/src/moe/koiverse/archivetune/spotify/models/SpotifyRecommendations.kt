/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyRecommendations(
    val tracks: List<SpotifyTrack> = emptyList(),
    val seeds: List<SpotifyRecommendationSeed> = emptyList(),
)

@Serializable
data class SpotifyRecommendationSeed(
    val id: String? = null,
    val type: String? = null,
    val href: String? = null,
)

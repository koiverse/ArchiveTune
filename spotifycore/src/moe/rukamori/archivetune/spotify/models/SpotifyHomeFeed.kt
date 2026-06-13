/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.spotify.models

/**
 * Personalized Spotify home feed returned by the `home` GQL operation.
 * Mirrors what open.spotify.com shows on its landing page: Daily Mix,
 * Discover Weekly, Release Radar, Jump back in, recently played, etc.
 */
data class SpotifyHomeFeed(
    val greeting: String?,
    val sections: List<SpotifyHomeFeedSection>,
)

data class SpotifyHomeFeedSection(
    val sectionUri: String,
    val title: String?,
    val typename: String,
    val totalCount: Int,
    val items: List<SpotifyHomeFeedItem>,
)

sealed class SpotifyHomeFeedItem {
    abstract val uri: String

    data class Playlist(
        override val uri: String,
        val id: String,
        val name: String,
        val description: String?,
        val format: String?,
        val totalCount: Int,
        val imageUrl: String?,
        val extractedColorHex: String?,
        val ownerName: String?,
        val madeForUsername: String?,
    ) : SpotifyHomeFeedItem()

    data class Album(
        override val uri: String,
        val id: String,
        val name: String,
        val albumType: String?,
        val artists: List<SpotifySimpleArtist>,
        val imageUrl: String?,
    ) : SpotifyHomeFeedItem()

    data class Artist(
        override val uri: String,
        val id: String,
        val name: String,
        val imageUrl: String?,
    ) : SpotifyHomeFeedItem()
}

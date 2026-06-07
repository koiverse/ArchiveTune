/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTrack(
    val id: String = "",
    val name: String = "",
    val artists: List<SpotifySimpleArtist> = emptyList(),
    val album: SpotifySimpleAlbum? = null,
    @SerialName("duration_ms") val durationMs: Int = 0,
    val explicit: Boolean = false,
    @SerialName("is_local") val isLocal: Boolean = false,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("track_number") val trackNumber: Int? = null,
    val uri: String? = null,
    val popularity: Int? = null,
)

@Serializable
data class SpotifySimpleArtist(
    val id: String? = null,
    val name: String = "",
    val uri: String? = null,
)

@Serializable
data class SpotifySimpleAlbum(
    val id: String = "",
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("album_type") val albumType: String? = null,
    val artists: List<SpotifySimpleArtist> = emptyList(),
    val uri: String? = null,
)

/**
 * Wrapper for the /me/tracks endpoint which returns SavedTrack objects
 */
@Serializable
data class SpotifySavedTrack(
    @SerialName("added_at") val addedAt: String? = null,
    val track: SpotifyTrack,
)

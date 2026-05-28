package moe.koiverse.archivetune.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyAlbum(
    val id: String = "",
    val name: String = "",
    @SerialName("album_type") val albumType: String? = null,
    val artists: List<SpotifySimpleArtist> = emptyList(),
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("total_tracks") val totalTracks: Int = 0,
    val tracks: SpotifyPaging<SpotifyTrack>? = null,
    val uri: String? = null,
    val popularity: Int? = null,
    val genres: List<String> = emptyList(),
)

@Serializable
data class SpotifySavedAlbum(
    @SerialName("added_at") val addedAt: String? = null,
    val album: SpotifyAlbum,
)

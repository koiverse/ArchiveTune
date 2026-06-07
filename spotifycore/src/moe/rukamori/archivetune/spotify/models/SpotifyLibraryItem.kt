/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.spotify.models

import kotlinx.serialization.Serializable

/**
 * A node in the user's Spotify library tree as exposed by the libraryV3 GraphQL
 * operation. The user can organize playlists into folders and nest folders inside
 * folders; libraryV3 returns each level as a list of [SpotifyLibraryItem] entries.
 *
 * The library API exposes both the flat list of playlists ([SpotifyPlaylist]) used
 * for places that don't care about hierarchy (Android Auto, picker dialogs, search)
 * and this hierarchical view used by the library UI to faithfully render the user's
 * folder organization.
 */
@Serializable
sealed class SpotifyLibraryItem {
    abstract val uri: String

    @Serializable
    data class Playlist(
        val playlist: SpotifyPlaylist,
    ) : SpotifyLibraryItem() {
        override val uri: String get() = playlist.uri ?: "spotify:playlist:${playlist.id}"
    }

    @Serializable
    data class Folder(
        val folder: SpotifyLibraryFolder,
    ) : SpotifyLibraryItem() {
        override val uri: String get() = folder.uri
    }
}

/**
 * A user-created folder in the Spotify library. Folders are containers only —
 * they have a name and a child count, but no playable content of their own. To
 * load the children pass the folder's [uri] back into the library API.
 */
@Serializable
data class SpotifyLibraryFolder(
    val uri: String,
    val name: String,
    val totalChildren: Int = 0,
)

package moe.koiverse.archivetune.ui.menu

import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AddToPlaylistDialogFilterTest {
    @Test
    fun playlistsForAddToPlaylist_includesEditableLocalPlaylists() {
        val editableLocal =
            Playlist(
                playlist = PlaylistEntity(id = "p1", name = "Local", browseId = null, isEditable = true),
                songCount = 0,
                songThumbnails = emptyList(),
            )
        val result = playlistsForAddToPlaylist(listOf(editableLocal))
        assertEquals(listOf(editableLocal), result)
    }

    @Test
    fun playlistsForAddToPlaylist_includesSyncedPlaylistsEvenIfNotEditable() {
        val syncedNotEditable =
            Playlist(
                playlist =
                PlaylistEntity(
                    id = "p2",
                    name = "Synced",
                    browseId = "PL123",
                    isEditable = false,
                ),
                songCount = 0,
                songThumbnails = emptyList(),
            )
        val result = playlistsForAddToPlaylist(listOf(syncedNotEditable))
        assertEquals(listOf(syncedNotEditable), result)
    }

    @Test
    fun playlistsForAddToPlaylist_excludesNonEditableNonSyncedPlaylists() {
        val notEditableNotSynced =
            Playlist(
                playlist =
                PlaylistEntity(
                    id = "p3",
                    name = "Hidden",
                    browseId = null,
                    isEditable = false,
                ),
                songCount = 0,
                songThumbnails = emptyList(),
            )
        val result = playlistsForAddToPlaylist(listOf(notEditableNotSynced))
        assertEquals(emptyList<Playlist>(), result)
    }

    @Test
    fun playlistsForAddToPlaylist_preservesOrder() {
        val a =
            Playlist(
                playlist = PlaylistEntity(id = "a", name = "A", browseId = null, isEditable = true),
                songCount = 0,
                songThumbnails = emptyList(),
            )
        val b =
            Playlist(
                playlist = PlaylistEntity(id = "b", name = "B", browseId = "PL_B", isEditable = false),
                songCount = 0,
                songThumbnails = emptyList(),
            )
        val c =
            Playlist(
                playlist = PlaylistEntity(id = "c", name = "C", browseId = null, isEditable = false),
                songCount = 0,
                songThumbnails = emptyList(),
            )

        val result = playlistsForAddToPlaylist(listOf(a, b, c))
        assertEquals(listOf(a, b), result)
    }
}


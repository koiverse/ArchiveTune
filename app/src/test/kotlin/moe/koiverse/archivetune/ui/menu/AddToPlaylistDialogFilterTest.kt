package moe.koiverse.archivetune.ui.menu

import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

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

    @Test
    fun visiblePlaylistsForAddToPlaylist_filtersByName_caseInsensitive() {
        val alpha =
            Playlist(
                playlist = PlaylistEntity(id = "a", name = "Road Trip", isEditable = true),
                songCount = 0,
                songThumbnails = emptyList(),
            )
        val beta =
            Playlist(
                playlist = PlaylistEntity(id = "b", name = "Focus", isEditable = true),
                songCount = 0,
                songThumbnails = emptyList(),
            )

        val result =
            visiblePlaylistsForAddToPlaylist(
                playlists = listOf(alpha, beta),
                sortOption = AddToPlaylistSortOption.RECENTLY_CREATED,
                query = "road",
            )

        assertEquals(listOf(alpha), result)
    }

    @Test
    fun visiblePlaylistsForAddToPlaylist_sortsByRecentlyModified_descending() {
        val older =
            playlist(
                id = "older",
                name = "Older",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-05T00:00:00"),
            )
        val newer =
            playlist(
                id = "newer",
                name = "Newer",
                createdAt = LocalDateTime.parse("2026-01-02T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-10T00:00:00"),
            )

        val result =
            visiblePlaylistsForAddToPlaylist(
                playlists = listOf(older, newer),
                sortOption = AddToPlaylistSortOption.RECENTLY_MODIFIED,
                query = "",
            )

        assertEquals(listOf(newer, older), result)
    }

    @Test
    fun visiblePlaylistsForAddToPlaylist_sortsByRecentlyCreated_descending() {
        val older =
            playlist(
                id = "older",
                name = "Older",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-05T00:00:00"),
            )
        val newer =
            playlist(
                id = "newer",
                name = "Newer",
                createdAt = LocalDateTime.parse("2026-01-10T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-10T00:00:00"),
            )

        val result =
            visiblePlaylistsForAddToPlaylist(
                playlists = listOf(older, newer),
                sortOption = AddToPlaylistSortOption.RECENTLY_CREATED,
                query = "",
            )

        assertEquals(listOf(newer, older), result)
    }

    @Test
    fun visiblePlaylistsForAddToPlaylist_sortsByMostPlayed_descending() {
        val lowPlays =
            playlist(
                id = "low",
                name = "Low",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-03T00:00:00"),
            )
        val highPlays =
            playlist(
                id = "high",
                name = "High",
                createdAt = LocalDateTime.parse("2026-01-02T00:00:00"),
                updatedAt = LocalDateTime.parse("2026-01-04T00:00:00"),
            )

        val result =
            visiblePlaylistsForAddToPlaylist(
                playlists = listOf(lowPlays, highPlays),
                sortOption = AddToPlaylistSortOption.MOST_PLAYED,
                query = "",
                playlistPlayCounts = mapOf(
                    lowPlays.id to 2L,
                    highPlays.id to 20L,
                ),
            )

        assertEquals(listOf(highPlays, lowPlays), result)
    }

    private fun playlist(
        id: String,
        name: String,
        createdAt: LocalDateTime,
        updatedAt: LocalDateTime,
    ) = Playlist(
        playlist =
            PlaylistEntity(
                id = id,
                name = name,
                browseId = null,
                isEditable = true,
                createdAt = createdAt,
                lastUpdateTime = updatedAt,
            ),
        songCount = 0,
        songThumbnails = emptyList(),
    )
}


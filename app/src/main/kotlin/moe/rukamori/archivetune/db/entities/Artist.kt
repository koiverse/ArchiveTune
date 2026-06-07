/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

@Immutable
data class Artist(
    @Embedded
    val artist: ArtistEntity,
    val songCount: Int,
    val timeListened: Int? = 0,
) : LocalItem() {
    override val id: String
        get() = artist.id
    override val title: String
        get() = artist.name
    override val thumbnailUrl: String?
        get() = artist.thumbnailUrl
}

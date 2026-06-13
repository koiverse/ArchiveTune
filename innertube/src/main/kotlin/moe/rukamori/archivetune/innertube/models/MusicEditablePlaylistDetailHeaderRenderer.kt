/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicEditablePlaylistDetailHeaderRenderer(
    val header: Header,
    val editHeader: EditHeader
) {
    @Serializable
    data class Header(
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer?
    )

    @Serializable
    data class EditHeader(
        val musicPlaylistEditHeaderRenderer: MusicPlaylistEditHeaderRenderer?
    )
}

@Serializable
data class MusicDetailHeaderRenderer(
    val title: Runs,
    val subtitle: Runs,
    val secondSubtitle: Runs,
    val description: Runs?,
    val thumbnail: ThumbnailRenderer,
    val menu: Menu,
)

@Serializable
data class MusicPlaylistEditHeaderRenderer(
    val editTitle: Runs?
)

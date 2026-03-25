package moe.koiverse.archivetune.ui.menu

import moe.koiverse.archivetune.db.entities.PlaylistSongMap
import moe.koiverse.archivetune.innertube.YouTube

suspend fun removeSongFromRemotePlaylist(
    playlistBrowseId: String,
    playlistSongMap: PlaylistSongMap,
) {
    val setVideoIds =
        playlistSongMap.setVideoId?.let(::listOf)
            ?: YouTube.playlistEntrySetVideoIds(playlistBrowseId, playlistSongMap.songId)
                .getOrDefault(emptyList())

    setVideoIds
        .distinct()
        .forEach { setVideoId ->
            YouTube.removeFromPlaylist(playlistBrowseId, playlistSongMap.songId, setVideoId)
        }
}

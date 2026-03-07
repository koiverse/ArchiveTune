/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import moe.koiverse.archivetune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.models.toMediaMetadata

const val ExtraIsMusicVideo = "moe.koiverse.archivetune.extra.IS_MUSIC_VIDEO"

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

fun Song.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(song.id)
        .setUri(song.id)
        .setCustomCacheKey(song.id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(song.title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(song.thumbnailUrl?.toUri())
                .setAlbumTitle(song.albumName)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, false) })
                .build(),
        ).build()

fun SongItem.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(thumbnail.toUri())
                .setAlbumTitle(album?.name)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, isMusicVideo()) })
                .build(),
        ).build()

fun MediaMetadata.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(thumbnailUrl?.toUri())
                .setAlbumTitle(album?.title)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, false) })
                .build(),
        ).build()

private fun SongItem.isMusicVideo(): Boolean {
    val musicVideoType = endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC
}

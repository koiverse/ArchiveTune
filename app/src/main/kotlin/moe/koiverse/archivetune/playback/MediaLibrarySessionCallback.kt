/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.offline.Download
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.MediaSessionConstants
import moe.koiverse.archivetune.constants.SongSortType
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.extensions.toMediaItem
import moe.koiverse.archivetune.extensions.toggleRepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus
import javax.inject.Inject
import kotlin.math.min

class MediaLibrarySessionCallback
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : MediaLibrarySession.Callback {
    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    var toggleLike: () -> Unit = {}
    var toggleStartRadio: () -> Unit = {}
    var toggleLibrary: () -> Unit = {}

    private fun browsableExtras(
        browsableHint: Int = CONTENT_STYLE_GRID_ITEM,
        playableHint: Int = CONTENT_STYLE_LIST_ITEM,
    ) = Bundle().apply {
        putBoolean(EXTRA_CONTENT_STYLE_SUPPORTED, true)
        putInt(EXTRA_CONTENT_STYLE_BROWSABLE_HINT, browsableHint)
        putInt(EXTRA_CONTENT_STYLE_PLAYABLE_HINT, playableHint)
    }

    private fun playableExtras(
        playableHint: Int = CONTENT_STYLE_LIST_ITEM,
    ) = Bundle().apply {
        putBoolean(EXTRA_CONTENT_STYLE_SUPPORTED, true)
        putInt(EXTRA_CONTENT_STYLE_PLAYABLE_HINT, playableHint)
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        return MediaSession.ConnectionResult.accept(
            connectionResult.availableSessionCommands
                .buildUpon()
                .add(MediaSessionConstants.CommandToggleLike)
                .add(MediaSessionConstants.CommandToggleStartRadio)
                .add(MediaSessionConstants.CommandToggleLibrary)
                .add(MediaSessionConstants.CommandToggleShuffle)
                .add(MediaSessionConstants.CommandToggleRepeatMode)
                .build(),
            connectionResult.availablePlayerCommands,
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleLike()
            MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> toggleStartRadio()
            MediaSessionConstants.ACTION_TOGGLE_LIBRARY -> toggleLibrary()
            MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> session.player.shuffleModeEnabled =
                !session.player.shuffleModeEnabled

            MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> session.player.toggleRepeatMode()
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem
                    .Builder()
                    .setMediaId(MusicService.ROOT)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setExtras(browsableExtras())
                            .build(),
                    ).build(),
                params,
            ),
        )

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> =
        Futures.immediateFuture(LibraryResult.ofVoid(params))

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank() || pageSize <= 0 || page < 0) {
                return@future LibraryResult.ofItemList(emptyList(), params)
            }

            val requested = (page + 1) * pageSize
            val items = ArrayList<MediaItem>(min(requested, 200))

            val songs = database.searchSongs(q, previewSize = requested).first()
            items += songs.map { it.toMediaItem(MusicService.SONG) }

            if (items.size < requested) {
                val remaining = requested - items.size

                val artists = database.searchArtists(q, previewSize = remaining).first()
                items +=
                    artists.map { artist ->
                        browsableMediaItem(
                            "${MusicService.ARTIST}/${artist.id}",
                            artist.title,
                            context.resources.getQuantityString(
                                R.plurals.n_song,
                                artist.songCount,
                                artist.songCount,
                            ),
                            artist.thumbnailUrl?.toUri(),
                            MediaMetadata.MEDIA_TYPE_ARTIST,
                        )
                    }
            }

            if (items.size < requested) {
                val remaining = requested - items.size

                val albums = database.searchAlbums(q, previewSize = remaining).first()
                items +=
                    albums.map { album ->
                        browsableMediaItem(
                            "${MusicService.ALBUM}/${album.id}",
                            album.title,
                            album.artists.joinToString { it.name },
                            album.thumbnailUrl?.toUri(),
                            MediaMetadata.MEDIA_TYPE_ALBUM,
                        )
                    }
            }

            if (items.size < requested) {
                val remaining = requested - items.size

                val playlists = database.searchPlaylists(q, previewSize = remaining).first()
                items +=
                    playlists.map { playlist ->
                        browsableMediaItem(
                            "${MusicService.PLAYLIST}/${playlist.id}",
                            playlist.title,
                            context.resources.getQuantityString(
                                R.plurals.n_song,
                                playlist.songCount,
                                playlist.songCount,
                            ),
                            playlist.thumbnails.firstOrNull()?.toUri(),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        )
                    }
            }

            val from = page * pageSize
            if (from >= items.size) return@future LibraryResult.ofItemList(emptyList(), params)
            val to = min(from + pageSize, items.size)

            LibraryResult.ofItemList(items.subList(from, to), params)
        }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            LibraryResult.ofItemList(
                when (parentId) {
                    MusicService.ROOT ->
                        listOf(
                            browsableMediaItem(
                                MusicService.SONG,
                                context.getString(R.string.songs),
                                null,
                                drawableUri(R.drawable.music_note),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            ),
                            browsableMediaItem(
                                MusicService.ARTIST,
                                context.getString(R.string.artists),
                                null,
                                drawableUri(R.drawable.artist),
                                MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                            ),
                            browsableMediaItem(
                                MusicService.ALBUM,
                                context.getString(R.string.albums),
                                null,
                                drawableUri(R.drawable.album),
                                MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                            ),
                            browsableMediaItem(
                                MusicService.PLAYLIST,
                                context.getString(R.string.playlists),
                                null,
                                drawableUri(R.drawable.queue_music),
                                MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                            ),
                        )

                    MusicService.SONG -> database.songsByCreateDateAsc().first()
                        .map { it.toMediaItem(parentId) }

                    MusicService.ARTIST ->
                        database.artistsByCreateDateAsc().first().map { artist ->
                            browsableMediaItem(
                                "${MusicService.ARTIST}/${artist.id}",
                                artist.artist.name,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    artist.songCount,
                                    artist.songCount
                                ),
                                artist.artist.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ARTIST,
                            )
                        }

                    MusicService.ALBUM ->
                        database.albumsByCreateDateAsc().first().map { album ->
                            browsableMediaItem(
                                "${MusicService.ALBUM}/${album.id}",
                                album.album.title,
                                album.artists.joinToString {
                                    it.name
                                },
                                album.album.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ALBUM,
                            )
                        }

                    MusicService.PLAYLIST -> {
                        val likedSongCount = database.likedSongsCount().first()
                        val downloadedSongCount = downloadUtil.downloads.value.size
                        listOf(
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}",
                                context.getString(R.string.liked_songs),
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    likedSongCount,
                                    likedSongCount
                                ),
                                drawableUri(R.drawable.favorite),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            ),
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}",
                                context.getString(R.string.downloaded_songs),
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    downloadedSongCount,
                                    downloadedSongCount
                                ),
                                drawableUri(R.drawable.download),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            ),
                        ) +
                                database.playlistsByCreateDateAsc().first().map { playlist ->
                                    browsableMediaItem(
                                        "${MusicService.PLAYLIST}/${playlist.id}",
                                        playlist.playlist.name,
                                        context.resources.getQuantityString(
                                            R.plurals.n_song,
                                            playlist.songCount,
                                            playlist.songCount
                                        ),
                                        playlist.thumbnails.firstOrNull()?.toUri(),
                                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                    )
                                }
                    }

                    else ->
                        when {
                            parentId.startsWith("${MusicService.ARTIST}/") ->
                                database.artistSongsByCreateDateAsc(parentId.removePrefix("${MusicService.ARTIST}/"))
                                    .first().map {
                                    it.toMediaItem(parentId)
                                }

                            parentId.startsWith("${MusicService.ALBUM}/") ->
                                database.albumSongs(parentId.removePrefix("${MusicService.ALBUM}/"))
                                    .first().map {
                                    it.toMediaItem(parentId)
                                }

                            parentId.startsWith("${MusicService.PLAYLIST}/") ->
                                when (val playlistId =
                                    parentId.removePrefix("${MusicService.PLAYLIST}/")) {
                                    PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(
                                        SongSortType.CREATE_DATE,
                                        true
                                    )

                                    PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                        val downloads = downloadUtil.downloads.value
                                        database
                                            .allSongs()
                                            .flowOn(Dispatchers.IO)
                                            .map { songs ->
                                                songs.filter {
                                                    downloads[it.id]?.state == Download.STATE_COMPLETED
                                                }
                                            }.map { songs ->
                                                songs
                                                    .map { it to downloads[it.id] }
                                                    .sortedBy { it.second?.updateTimeMs ?: 0L }
                                                    .map { it.first }
                                            }
                                    }

                                    else ->
                                        database.playlistSongs(playlistId).map { list ->
                                            list.map { it.song }
                                        }
                                }.first().map {
                                    it.toMediaItem(parentId)
                                }

                            else -> emptyList()
                        }
                },
                params,
            )
        }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        scope.future(Dispatchers.IO) {
            when {
                mediaId == MusicService.ROOT ->
                    LibraryResult.ofItem(
                        MediaItem
                            .Builder()
                            .setMediaId(MusicService.ROOT)
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setIsPlayable(false)
                                    .setIsBrowsable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                    .setExtras(browsableExtras())
                                    .build(),
                            ).build(),
                        null,
                    )

                mediaId == MusicService.SONG ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.SONG,
                            context.getString(R.string.songs),
                            null,
                            drawableUri(R.drawable.music_note),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.ARTIST ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.ARTIST,
                            context.getString(R.string.artists),
                            null,
                            drawableUri(R.drawable.artist),
                            MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                        ),
                        null,
                    )

                mediaId == MusicService.ALBUM ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.ALBUM,
                            context.getString(R.string.albums),
                            null,
                            drawableUri(R.drawable.album),
                            MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                        ),
                        null,
                    )

                mediaId == MusicService.PLAYLIST ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.PLAYLIST,
                            context.getString(R.string.playlists),
                            null,
                            drawableUri(R.drawable.queue_music),
                            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                        ),
                        null,
                    )

                mediaId.startsWith("${MusicService.SONG}/") ->
                    database.song(mediaId.removePrefix("${MusicService.SONG}/")).first()?.let {
                        LibraryResult.ofItem(it.toMediaItem(MusicService.SONG), null)
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.ARTIST}/") ->
                    database.artist(mediaId.removePrefix("${MusicService.ARTIST}/")).first()?.let { artist ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.ARTIST}/${artist.id}",
                                artist.title,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    artist.songCount,
                                    artist.songCount,
                                ),
                                artist.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ARTIST,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.ALBUM}/") ->
                    database.album(mediaId.removePrefix("${MusicService.ALBUM}/")).first()?.let { album ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.ALBUM}/${album.id}",
                                album.title,
                                album.artists.joinToString { it.name },
                                album.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ALBUM,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.PLAYLIST}/") ->
                    database.playlist(mediaId.removePrefix("${MusicService.PLAYLIST}/")).first()?.let { playlist ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${playlist.id}",
                                playlist.title,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    playlist.songCount,
                                    playlist.songCount,
                                ),
                                playlist.thumbnails.firstOrNull()?.toUri(),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                else ->
                    database.song(mediaId).first()?.toMediaItem()?.let {
                        LibraryResult.ofItem(it, null)
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
            }
        }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
        scope.future {
            // Play from Android Auto
            val defaultResult =
                MediaSession.MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
            val firstItem = mediaItems.firstOrNull() ?: return@future defaultResult
            val path = firstItem.mediaId.split("/").filter { it.isNotBlank() }
            when (path.firstOrNull()) {
                MusicService.SONG -> {
                    val songId = path.getOrNull(1) ?: return@future defaultResult
                    val allSongs = database.songsByCreateDateAsc().first()
                    MediaSession.MediaItemsWithStartPosition(
                        allSongs.map { it.toMediaItem() },
                        allSongs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs,
                    )
                }

                MusicService.ARTIST -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val artistId = path.getOrNull(1) ?: return@future defaultResult
                    val songs = database.artistSongsByCreateDateAsc(artistId).first()
                    MediaSession.MediaItemsWithStartPosition(
                        songs.map { it.toMediaItem() },
                        songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs,
                    )
                }

                MusicService.ALBUM -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val albumId = path.getOrNull(1) ?: return@future defaultResult
                    val albumWithSongs =
                        database.albumWithSongs(albumId).first() ?: return@future defaultResult
                    MediaSession.MediaItemsWithStartPosition(
                        albumWithSongs.songs.map { it.toMediaItem() },
                        albumWithSongs.songs.indexOfFirst { it.id == songId }.takeIf { it != -1 }
                            ?: 0,
                        startPositionMs,
                    )
                }

                MusicService.PLAYLIST -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val playlistId = path.getOrNull(1) ?: return@future defaultResult
                    val songs =
                        when (playlistId) {
                            PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(
                                SongSortType.CREATE_DATE,
                                descending = true
                            )

                            PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                val downloads = downloadUtil.downloads.value
                                database
                                    .allSongs()
                                    .flowOn(Dispatchers.IO)
                                    .map { songs ->
                                        songs.filter {
                                            downloads[it.id]?.state == Download.STATE_COMPLETED
                                        }
                                    }.map { songs ->
                                        songs
                                            .map { it to downloads[it.id] }
                                            .sortedBy { it.second?.updateTimeMs ?: 0L }
                                            .map { it.first }
                                    }
                            }

                            else ->
                                database.playlistSongs(playlistId).map { list ->
                                    list.map { it.song }
                                }
                        }.first()
                    MediaSession.MediaItemsWithStartPosition(
                        songs.map { it.toMediaItem() },
                        songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs,
                    )
                }

                else -> {
                    val query = firstItem.requestMetadata.searchQuery?.trim().orEmpty()
                    if (query.isBlank()) return@future defaultResult

                    val matchedSongs = database.searchSongs(query, previewSize = 50).first()
                    val songId = matchedSongs.firstOrNull()?.id ?: return@future defaultResult
                    val allSongs = database.songsByCreateDateAsc().first()
                    MediaSession.MediaItemsWithStartPosition(
                        allSongs.map { it.toMediaItem() },
                        allSongs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs,
                    )
                }
            }
        }

    private fun drawableUri(
        @DrawableRes id: Int,
    ) = Uri
        .Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.resources.getResourcePackageName(id))
        .appendPath(context.resources.getResourceTypeName(id))
        .appendPath(context.resources.getResourceEntryName(id))
        .build()

    private fun browsableMediaItem(
        id: String,
        title: String,
        subtitle: String?,
        iconUri: Uri?,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
    ) = MediaItem
        .Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(subtitle)
                .setArtworkUri(iconUri)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(mediaType)
                .setExtras(browsableExtras())
                .build(),
        ).build()

    private fun Song.toMediaItem(path: String) =
        MediaItem
            .Builder()
            .setMediaId("$path/$id")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(song.title)
                    .setSubtitle(artists.joinToString { it.name })
                    .setArtist(artists.joinToString { it.name })
                    .setArtworkUri(song.thumbnailUrl?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(playableExtras())
                    .build(),
            ).build()

    companion object {
        private const val EXTRA_CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val EXTRA_CONTENT_STYLE_BROWSABLE_HINT =
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val EXTRA_CONTENT_STYLE_PLAYABLE_HINT =
            "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"

        private const val CONTENT_STYLE_LIST_ITEM = 1
        private const val CONTENT_STYLE_GRID_ITEM = 2
    }
}

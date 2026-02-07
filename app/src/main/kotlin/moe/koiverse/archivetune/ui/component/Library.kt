/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.db.entities.Album
import moe.koiverse.archivetune.db.entities.Artist
import moe.koiverse.archivetune.db.entities.Playlist
import moe.koiverse.archivetune.ui.menu.AlbumMenu
import moe.koiverse.archivetune.ui.menu.ArtistMenu
import moe.koiverse.archivetune.ui.menu.PlaylistMenu
import moe.koiverse.archivetune.ui.menu.YouTubePlaylistMenu
import kotlinx.coroutines.CoroutineScope

@Composable
fun LibraryArtistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    modifier: Modifier = Modifier
) = ArtistListItem(
    artist = artist,
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    ArtistMenu(
                        originalArtist = artist,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            navController.navigate("artist/${artist.id}")
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryArtistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    modifier: Modifier = Modifier
) = ArtistGridItem(
    artist = artist,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                navController.navigate("artist/${artist.id}")
            },
            onLongClick = {
                menuState.show {
                    ArtistMenu(
                        originalArtist = artist,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        )
)

@Composable
fun LibraryAlbumListItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false
) = AlbumListItem(
    album = album,
    isActive = isActive,
    isPlaying = isPlaying,
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    AlbumMenu(
                        originalAlbum = album,
                        navController = navController,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            navController.navigate("album/${album.id}")
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryAlbumGridItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false
) = AlbumGridItem(
    album = album,
    isActive = isActive,
    isPlaying = isPlaying,
    coroutineScope = coroutineScope,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                navController.navigate("album/${album.id}")
            },
            onLongClick = {
                menuState.show {
                    AlbumMenu(
                        originalAlbum = album,
                        navController = navController,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        )
)

@Composable
fun LibraryPlaylistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    useNewDesign: Boolean,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val trailing: @Composable RowScope.() -> Unit = {
        androidx.compose.material3.IconButton(
            onClick = {
                database.transaction {
                    val newPinned = !playlist.playlist.isPinned
                    val newOrder =
                        if (newPinned) {
                            (minPinnedCustomOrder() ?: 0) - 1
                        } else {
                            (maxUnpinnedCustomOrder() ?: (maxPlaylistCustomOrder() ?: 0)) + 1
                        }
                    setPlaylistPinned(playlist.id, newPinned)
                    setPlaylistCustomOrder(playlist.id, newOrder)
                }
            },
        ) {
            Icon(
                painter =
                painterResource(
                    if (playlist.playlist.isPinned) R.drawable.unpin else R.drawable.pin
                ),
                contentDescription = null,
            )
        }
        if (showDragHandle) {
            androidx.compose.material3.IconButton(
                onClick = { },
                modifier = dragHandleModifier,
            ) {
                Icon(
                    painter = painterResource(R.drawable.drag_handle),
                    contentDescription = null,
                )
            }
        }
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            YouTubePlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null
            )
        }
    }

    val baseMod = modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp)

    val openPlaylist: () -> Unit = {
        if (
            !playlist.playlist.isEditable &&
            playlist.songCount == 0 &&
            playlist.playlist.remoteSongCount != 0
        ) {
            navController.navigate("online_playlist/${playlist.playlist.browseId}")
        } else {
            navController.navigate("local_playlist/${playlist.id}")
        }
    }

    if (useNewDesign) {
        OverlayPlaylistListItem(
            playlist = playlist,
            trailingContent = trailing,
            modifier = baseMod,
            onClick = openPlaylist
        )
    } else {
        PlaylistListItem(
            playlist = playlist,
            trailingContent = trailing,
            modifier = baseMod.clickable(onClick = openPlaylist)
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier
) = PlaylistGridItem(
    playlist = playlist,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0)
                    navController.navigate("online_playlist/${playlist.playlist.browseId}")
                else
                    navController.navigate("local_playlist/${playlist.id}")
            },
            onLongClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            YouTubePlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        )
)

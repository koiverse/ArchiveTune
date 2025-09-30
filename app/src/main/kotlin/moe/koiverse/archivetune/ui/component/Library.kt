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
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.constants.UseNewLibraryDesignKey
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.R
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
    navController: NavController? = null,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    autoPlaylist: Boolean = false,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    // keep the MutableState so Compose observes changes and recomposes
    // Use delegated state so reads are Compose-friendly and recompose when changed
    val useNewDesign by rememberPreference(UseNewLibraryDesignKey, defaultValue = true)

    val trailing: @Composable RowScope.() -> Unit = trailingContent ?: {
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
        .padding(horizontal = 12.dp)
        .padding(bottom = 8.dp)

    val defaultNavigate = {
        if (navController == null) return@let
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

    val actualOnClick = onClick ?: defaultNavigate

    val clickableMod = baseMod.clickable {
        actualOnClick()
    }

    if (useNewDesign) {
        OverlayPlaylistListItem(
            playlist = playlist,
            trailingContent = trailing,
            autoPlaylist = autoPlaylist,
            modifier = baseMod,
            onClick = actualOnClick
        )
    } else {
        PlaylistListItem(
            playlist = playlist,
            autoPlaylist = autoPlaylist,
            trailingContent = trailing,
            modifier = clickableMod
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistGridItem(
    navController: NavController? = null,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    autoPlaylist: Boolean = false,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val useNewDesign by rememberPreference(UseNewLibraryDesignKey, defaultValue = true)

    val defaultNavigate = {
        if (navController == null) return@let
        if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0)
            navController.navigate("online_playlist/${playlist.playlist.browseId}")
        else
            navController.navigate("local_playlist/${playlist.id}")
    }

    val actualOnClick = onClick ?: defaultNavigate

    val trailing = trailingContent ?: run {
        @Composable {
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
    }

    if (useNewDesign) {
        OverlayPlaylistListItem(
            playlist = playlist,
            trailingContent = trailing,
            autoPlaylist = autoPlaylist,
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        actualOnClick()
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
    } else {
        PlaylistGridItem(
            playlist = playlist,
            autoPlaylist = autoPlaylist,
            fillMaxWidth = true,
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        actualOnClick()
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
    }
}

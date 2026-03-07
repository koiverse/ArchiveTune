/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.DarkModeKey
import moe.koiverse.archivetune.constants.PureBlackKey
import moe.koiverse.archivetune.ui.component.BottomSheet
import moe.koiverse.archivetune.ui.component.BottomSheetMenu
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.rememberBottomSheetState
import moe.koiverse.archivetune.ui.screens.BrowseScreen
import moe.koiverse.archivetune.ui.screens.artist.ArtistAlbumsScreen
import moe.koiverse.archivetune.ui.screens.artist.ArtistItemsScreen
import moe.koiverse.archivetune.ui.screens.artist.ArtistScreen
import moe.koiverse.archivetune.ui.screens.artist.ArtistSongsScreen
import moe.koiverse.archivetune.ui.screens.library.LibraryScreen
import moe.koiverse.archivetune.ui.screens.playlist.AutoPlaylistScreen
import moe.koiverse.archivetune.ui.screens.playlist.LocalPlaylistScreen
import moe.koiverse.archivetune.ui.screens.playlist.OnlinePlaylistScreen
import moe.koiverse.archivetune.ui.screens.playlist.TopPlaylistScreen
import moe.koiverse.archivetune.ui.screens.playlist.CachePlaylistScreen
import moe.koiverse.archivetune.ui.screens.search.OnlineSearchResult
import moe.koiverse.archivetune.ui.screens.settings.AboutScreen
import moe.koiverse.archivetune.ui.screens.settings.AccountSettings
import moe.koiverse.archivetune.ui.screens.settings.AppearanceSettings
import moe.koiverse.archivetune.ui.screens.settings.CustomizeBackground
import moe.koiverse.archivetune.ui.screens.settings.BackupAndRestore
import moe.koiverse.archivetune.ui.screens.settings.ChangelogScreen
import moe.koiverse.archivetune.ui.screens.settings.ContentSettings
import moe.koiverse.archivetune.ui.screens.settings.DarkMode
import moe.koiverse.archivetune.ui.screens.settings.DiscordLoginScreen
import moe.koiverse.archivetune.ui.screens.settings.DiscordSettings
import moe.koiverse.archivetune.ui.screens.settings.DebugSettings
import moe.koiverse.archivetune.ui.screens.settings.IntegrationScreen
import moe.koiverse.archivetune.ui.screens.settings.LastFMSettings
import moe.koiverse.archivetune.ui.screens.settings.MusicTogetherScreen
import moe.koiverse.archivetune.ui.screens.settings.PalettePickerScreen
import moe.koiverse.archivetune.ui.screens.settings.PlayerSettings
import moe.koiverse.archivetune.ui.screens.settings.PoTokenScreen
import moe.koiverse.archivetune.ui.screens.settings.PrivacySettings
import moe.koiverse.archivetune.ui.screens.settings.SettingsScreen
import moe.koiverse.archivetune.ui.screens.settings.StorageSettings
 import moe.koiverse.archivetune.ui.screens.settings.ThemeCreatorScreen
 import moe.koiverse.archivetune.ui.screens.settings.UpdateScreen
 import moe.koiverse.archivetune.ui.utils.ShowMediaInfo
 import moe.koiverse.archivetune.utils.rememberEnumPreference
 import moe.koiverse.archivetune.utils.rememberPreference
 import moe.koiverse.archivetune.ui.screens.NewUpdateAvailableScreen

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("year_in_music") {
        YearInMusicScreen(navController)
    }
    composable("mood_and_genres") {
        MoodAndGenresScreen(navController, scrollBehavior)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("charts_screen") {
       ChartsScreen(navController)
    }
    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId")
        )
    }
    composable(
        route = "search/{query}",
        arguments =
        listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
        listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
        listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
        listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
        listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    composable("settings") {
        SettingsScreen(navController, scrollBehavior, latestVersionName)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
    }
    composable("settings/appearance/palette_picker") {
        PalettePickerScreen(navController)
    }
    composable("settings/appearance/theme_creator") {
        ThemeCreatorScreen(navController)
    }
    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/integration") {
        IntegrationScreen(navController, scrollBehavior)
    }
    composable("settings/music_together") {
        MusicTogetherScreen(navController, scrollBehavior)
    }
    composable("settings/lastfm") {
        LastFMSettings(navController, scrollBehavior)
    }
    composable("settings/discord/experimental") {
        moe.koiverse.archivetune.ui.screens.settings.DiscordExperimental(navController)
    }
    composable("settings/misc") {
        DebugSettings(navController)
    }
    composable("settings/update") {
        UpdateScreen(navController, scrollBehavior)
    }
    composable("settings/changelog") {
        ChangelogScreen(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
     composable("settings/about") {
         AboutScreen(navController, scrollBehavior)
     }
     composable("settings/po_token") {
         PoTokenScreen(navController, scrollBehavior)
     }
     composable("customize_background") {
         CustomizeBackground(navController)
     }
      composable("login") {
          LoginScreen(navController)
      }
       composable("new_update_available") {
           NewUpdateAvailableScreen(navController)
       }
}

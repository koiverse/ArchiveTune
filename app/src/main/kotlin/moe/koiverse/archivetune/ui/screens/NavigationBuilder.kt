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
import moe.koiverse.archivetune.ui.screens.settings.ContentSettings
import moe.koiverse.archivetune.ui.screens.settings.DarkMode
import moe.koiverse.archivetune.ui.screens.settings.DiscordLoginScreen
import moe.koiverse.archivetune.ui.screens.settings.DiscordSettings
import moe.koiverse.archivetune.ui.screens.settings.DebugSettings
import moe.koiverse.archivetune.ui.screens.settings.IntegrationScreen
import moe.koiverse.archivetune.ui.screens.settings.LastFMSettings
import moe.koiverse.archivetune.ui.screens.settings.PalettePickerScreen
import moe.koiverse.archivetune.ui.screens.settings.PlayerSettings
import moe.koiverse.archivetune.ui.screens.settings.PrivacySettings
import moe.koiverse.archivetune.ui.screens.settings.SettingsScreen
import moe.koiverse.archivetune.ui.screens.settings.StorageSettings
import moe.koiverse.archivetune.ui.screens.settings.UpdateScreen
import moe.koiverse.archivetune.ui.screens.settings.ExtensionsScreen
import moe.koiverse.archivetune.ui.screens.settings.ExtensionSettingsScreen
import moe.koiverse.archivetune.ui.screens.settings.CreateExtensionScreen
import moe.koiverse.archivetune.ui.utils.ShowMediaInfo
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.extensions.ui.ExtensionsUiContainer

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    composable(Screens.Home.route) {
        ExtensionsUiContainer("home") { HomeScreen(navController) }
    }
    composable(
        Screens.Library.route,
    ) {
        ExtensionsUiContainer("library") { LibraryScreen(navController) }
    }
    composable("history") {
        ExtensionsUiContainer("history") { HistoryScreen(navController) }
    }
    composable("stats") {
        ExtensionsUiContainer("stats") { StatsScreen(navController) }
    }
    composable("mood_and_genres") {
        ExtensionsUiContainer("mood_and_genres") { MoodAndGenresScreen(navController, scrollBehavior) }
    }
    composable("account") {
        ExtensionsUiContainer("account") { AccountScreen(navController, scrollBehavior) }
    }
    composable("new_release") {
        ExtensionsUiContainer("new_release") { NewReleaseScreen(navController, scrollBehavior) }
    }
    composable("charts_screen") {
       ExtensionsUiContainer("charts_screen") { ChartsScreen(navController) }
    }
    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        ExtensionsUiContainer("browse") {
            BrowseScreen(
                navController,
                scrollBehavior,
                it.arguments?.getString("browseId")
            )
        }
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
        ExtensionsUiContainer("search") { OnlineSearchResult(navController) }
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
        ExtensionsUiContainer("album") { AlbumScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("artist") { ArtistScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("artist_songs") { ArtistSongsScreen(navController, scrollBehavior) }
    }
    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ExtensionsUiContainer("artist_albums") { ArtistAlbumsScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("artist_items") { ArtistItemsScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("online_playlist") { OnlinePlaylistScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("local_playlist") { LocalPlaylistScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("auto_playlist") { AutoPlaylistScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("cache_playlist") { CachePlaylistScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("top_playlist") { TopPlaylistScreen(navController, scrollBehavior) }
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
        ExtensionsUiContainer("youtube_browse") { YouTubeBrowseScreen(navController) }
    }
    composable("settings") {
        ExtensionsUiContainer("settings") { SettingsScreen(navController, scrollBehavior, latestVersionName) }
    }
    composable("settings/appearance") {
        ExtensionsUiContainer("settings_appearance") { AppearanceSettings(navController, scrollBehavior) }
    }
    composable("settings/appearance/palette_picker") {
        ExtensionsUiContainer("settings_palette_picker") { PalettePickerScreen(navController) }
    }
    composable("settings/content") {
        ExtensionsUiContainer("settings_content") { ContentSettings(navController, scrollBehavior) }
    }
    composable("settings/player") {
        ExtensionsUiContainer("settings_player") { PlayerSettings(navController, scrollBehavior) }
    }
    composable("settings/storage") {
        ExtensionsUiContainer("settings_storage") { StorageSettings(navController, scrollBehavior) }
    }
    composable("settings/privacy") {
        ExtensionsUiContainer("settings_privacy") { PrivacySettings(navController, scrollBehavior) }
    }
    composable("settings/backup_restore") {
        ExtensionsUiContainer("settings_backup_restore") { BackupAndRestore(navController, scrollBehavior) }
    }
    composable("settings/discord") {
        ExtensionsUiContainer("settings_discord") { DiscordSettings(navController, scrollBehavior) }
    }
    composable("settings/integration") {
        ExtensionsUiContainer("settings_integration") { IntegrationScreen(navController, scrollBehavior) }
    }
    composable("settings/lastfm") {
        ExtensionsUiContainer("settings_lastfm") { LastFMSettings(navController, scrollBehavior) }
    }
    composable("settings/discord/experimental") {
        ExtensionsUiContainer("settings_discord_experimental") { moe.koiverse.archivetune.ui.screens.settings.DiscordExperimental(navController) }
    }
    composable("settings/misc") {
        ExtensionsUiContainer("settings_misc") { DebugSettings(navController) }
    }
    composable("settings/update") {
        ExtensionsUiContainer("settings_update") { UpdateScreen(navController, scrollBehavior) }
    }
    composable("settings/extensions") {
        ExtensionsScreen(navController, scrollBehavior)
    }
    composable("settings/extensions/create") {
        CreateExtensionScreen(navController, scrollBehavior)
    }
    composable(
        route = "settings/extension/{id}",
        arguments = listOf(
            navArgument("id") { type = NavType.StringType }
        )
    ) {
        ExtensionSettingsScreen(navController, scrollBehavior, it)
    }
    composable("settings/discord/login") {
        ExtensionsUiContainer("settings_discord_login") { DiscordLoginScreen(navController) }
    }
    composable("settings/about") {
        ExtensionsUiContainer("settings_about") { AboutScreen(navController, scrollBehavior) }
    }
    composable("customize_background") {
        ExtensionsUiContainer("customize_background") { CustomizeBackground(navController) }
    }
    composable("login") {
        ExtensionsUiContainer("login") { LoginScreen(navController) }
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.R

@Composable
fun buildQuickActions(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsQuickAction> =
    listOf(
        SettingsQuickAction(
            icon = painterResource(R.drawable.palette),
            label = stringResource(R.string.appearance),
            onClick = { resetSearch(); navController.navigate("settings/appearance") },
            accentColor = MaterialTheme.colorScheme.primary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.play),
            label = stringResource(R.string.player_and_audio),
            onClick = { resetSearch(); navController.navigate("settings/player") },
            accentColor = MaterialTheme.colorScheme.tertiary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.storage),
            label = stringResource(R.string.storage),
            onClick = { resetSearch(); navController.navigate("settings/storage") },
            accentColor = MaterialTheme.colorScheme.secondary,
        ),
        SettingsQuickAction(
            icon = painterResource(R.drawable.security),
            label = stringResource(R.string.privacy),
            onClick = { resetSearch(); navController.navigate("settings/privacy") },
            accentColor = MaterialTheme.colorScheme.error,
        ),
    )

@Composable
fun buildIntegrationActions(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsIntegrationAction> =
    listOf(
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.discord),
            label = stringResource(R.string.discord),
            onClick = { resetSearch(); navController.navigate("settings/discord") },
            accentColor = Color(0xFF5865F2),
        ),
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.integration),
            label = stringResource(R.string.integration),
            onClick = { resetSearch(); navController.navigate("settings/integration") },
            accentColor = MaterialTheme.colorScheme.secondary,
        ),
        SettingsIntegrationAction(
            icon = painterResource(R.drawable.fire),
            label = stringResource(R.string.music_together),
            onClick = { resetSearch(); navController.navigate("settings/music_together") },
            accentColor = MaterialTheme.colorScheme.tertiary,
        ),
    )

@Composable
fun buildSettingsGroups(
    navController: NavController,
    isAndroid12OrLater: Boolean,
    hasUpdate: Boolean,
    context: Context,
    resetSearch: () -> Unit,
): List<SettingsGroup> =
    buildList {
        add(
            SettingsGroup(
                title = stringResource(R.string.settings_section_ui),
                items = listOf(
                    SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = stringResource(R.string.appearance),
                        subtitle = stringResource(R.string.dark_theme),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("theme", "palette", "material you", "dynamic color", "font", "ui"),
                        onClick = { resetSearch(); navController.navigate("settings/appearance") },
                    ),
                ),
            ),
        )

        add(
            SettingsGroup(
                title = stringResource(R.string.settings_section_player_content),
                items = listOf(
                    SettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = stringResource(R.string.player_and_audio),
                        subtitle = stringResource(R.string.audio_quality),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("audio", "playback", "volume", "quality", "equalizer", "crossfade"),
                        onClick = { resetSearch(); navController.navigate("settings/player") },
                    ),
                    SettingsItem(
                        icon = painterResource(R.drawable.language),
                        title = stringResource(R.string.content),
                        subtitle = stringResource(R.string.content_language),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf("language", "content", "lyrics", "translation", "region"),
                        onClick = { resetSearch(); navController.navigate("settings/content") },
                    ),
                    SettingsItem(
                        icon = painterResource(R.drawable.token),
                        title = stringResource(R.string.po_token_generation),
                        subtitle = stringResource(R.string.po_token_generation_subtitle),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("po token", "token", "web client", "visitor data", "gvs", "player"),
                        onClick = { resetSearch(); navController.navigate("settings/po_token") },
                    ),
                ),
            ),
        )

        add(
            SettingsGroup(
                title = stringResource(R.string.settings_section_privacy),
                items = listOf(
                    SettingsItem(
                        icon = painterResource(R.drawable.security),
                        title = stringResource(R.string.privacy),
                        subtitle = stringResource(R.string.pause_listen_history),
                        accentColor = MaterialTheme.colorScheme.error,
                        keywords = listOf("privacy", "history", "tracking", "security", "permissions"),
                        onClick = { resetSearch(); navController.navigate("settings/privacy") },
                    ),
                ),
            ),
        )

        add(
            SettingsGroup(
                title = stringResource(R.string.settings_section_storage),
                items = listOf(
                    SettingsItem(
                        icon = painterResource(R.drawable.storage),
                        title = stringResource(R.string.storage),
                        subtitle = stringResource(R.string.cache),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf("storage", "cache", "offline", "downloads", "cleanup"),
                        onClick = { resetSearch(); navController.navigate("settings/storage") },
                    ),
                    SettingsItem(
                        icon = painterResource(R.drawable.restore),
                        title = stringResource(R.string.backup_restore),
                        subtitle = stringResource(R.string.action_backup),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("backup", "restore", "import", "export", "migration"),
                        onClick = { resetSearch(); navController.navigate("settings/backup_restore") },
                    ),
                    SettingsItem(
                        icon = painterResource(R.drawable.export),
                        title = stringResource(R.string.export_songs),
                        subtitle = stringResource(R.string.export_songs_subtitle),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("export", "songs", "download", "cache", "save", "storage", "music"),
                        onClick = { resetSearch(); navController.navigate("settings/export_songs") },
                    ),
                ),
            ),
        )

        add(
            SettingsGroup(
                title = stringResource(R.string.settings_section_system),
                items = buildList {
                    if (isAndroid12OrLater) {
                        add(
                            SettingsItem(
                                icon = painterResource(R.drawable.link),
                                title = stringResource(R.string.default_links),
                                subtitle = stringResource(R.string.open_supported_links),
                                accentColor = MaterialTheme.colorScheme.primary,
                                keywords = listOf("links", "deeplink", "default", "supported links"),
                                onClick = {
                                    resetSearch()
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        when (e) {
                                            is ActivityNotFoundException,
                                            is SecurityException,
                                            -> {
                                                Toast.makeText(
                                                    context,
                                                    R.string.open_app_settings_error,
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                            else -> {
                                                Toast.makeText(
                                                    context,
                                                    R.string.open_app_settings_error,
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            }
                                        }
                                    }
                                },
                            ),
                        )
                    }
                    add(
                        SettingsItem(
                            icon = painterResource(R.drawable.experiment),
                            title = stringResource(R.string.experiment_settings),
                            subtitle = stringResource(R.string.misc),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            keywords = listOf("experimental", "debug", "developer", "labs", "internal"),
                            onClick = { resetSearch(); navController.navigate("settings/misc") },
                        ),
                    )
                    add(
                        SettingsItem(
                            icon = painterResource(R.drawable.update),
                            title = stringResource(R.string.updates),
                            subtitle = if (hasUpdate) {
                                stringResource(R.string.new_version_available)
                            } else {
                                BuildConfig.VERSION_NAME
                            },
                            showUpdateIndicator = hasUpdate,
                            accentColor = if (hasUpdate) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            keywords = listOf("update", "version", "release", "changelog"),
                            onClick = { resetSearch(); navController.navigate("settings/update") },
                        ),
                    )
                    add(
                        SettingsItem(
                            icon = painterResource(R.drawable.info),
                            title = stringResource(R.string.about),
                            subtitle = "ArchiveTune",
                            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            keywords = listOf("about", "app info", "license", "contributors"),
                            onClick = { resetSearch(); navController.navigate("settings/about") },
                        ),
                    )
                },
            ),
        )
    }

@Composable
fun buildInternalItems(
    navController: NavController,
    resetSearch: () -> Unit,
): List<SettingsItem> =
    listOf(
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.theme_creator_title),
            subtitle = stringResource(R.string.theme_creator_subtitle),
            accentColor = MaterialTheme.colorScheme.primary,
            keywords = listOf("theme", "creator", "seed", "material", "palette", "import", "export"),
            onClick = { resetSearch(); navController.navigate("settings/appearance/theme_creator") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.palette),
            title = stringResource(R.string.customize_colors),
            subtitle = stringResource(R.string.appearance),
            accentColor = MaterialTheme.colorScheme.primary,
            keywords = listOf("palette", "color", "accent", "tone", "dynamic color"),
            onClick = { resetSearch(); navController.navigate("settings/appearance/palette_picker") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.image),
            title = stringResource(R.string.customize_background_title),
            subtitle = stringResource(R.string.appearance),
            accentColor = MaterialTheme.colorScheme.secondary,
            keywords = listOf("background", "wallpaper", "image", "blur", "gradient"),
            onClick = { resetSearch(); navController.navigate("customize_background") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.discord),
            title = stringResource(R.string.discord_integration),
            subtitle = stringResource(R.string.integration),
            accentColor = Color(0xFF5865F2),
            keywords = listOf("discord", "rpc", "rich presence", "status", "activity"),
            onClick = { resetSearch(); navController.navigate("settings/discord") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.security),
            title = stringResource(R.string.advanced_login),
            subtitle = stringResource(R.string.discord),
            accentColor = Color(0xFF5865F2),
            keywords = listOf("token", "login", "authentication", "discord login"),
            onClick = { resetSearch(); navController.navigate("settings/discord/login") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.experiment),
            title = stringResource(R.string.experimental_features),
            subtitle = stringResource(R.string.experimental_features_description),
            accentColor = MaterialTheme.colorScheme.tertiary,
            keywords = listOf("experimental", "labs", "advanced", "discord experimental", "internal"),
            onClick = { resetSearch(); navController.navigate("settings/discord/experimental") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.integration),
            title = stringResource(R.string.lastfm_integration),
            subtitle = stringResource(R.string.integration),
            accentColor = MaterialTheme.colorScheme.secondary,
            keywords = listOf("lastfm", "last.fm", "scrobble", "listening history"),
            onClick = { resetSearch(); navController.navigate("settings/lastfm") },
        ),
        SettingsItem(
            icon = painterResource(R.drawable.fire),
            title = stringResource(R.string.music_together),
            subtitle = stringResource(R.string.integration),
            accentColor = MaterialTheme.colorScheme.tertiary,
            keywords = listOf("together", "session", "sync", "party", "join", "host"),
            onClick = { resetSearch(); navController.navigate("settings/music_together") },
        ),
    )

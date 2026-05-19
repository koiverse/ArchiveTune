/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */



package moe.koiverse.archivetune.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.R



@Composable
fun buildSettingsGroups(
    navController: NavController,
    isAndroid12OrLater: Boolean,
    hasUpdate: Boolean,
    context: Context,
): List<SettingsGroup> =
    buildList {
        add(
            SettingsGroup(
                title = stringResource(R.string.settings),
                items = listOf(
                    SettingsItem(
                        key = "account",
                        icon = painterResource(R.drawable.account),
                        title = stringResource(R.string.account),
                        subtitle = stringResource(R.string.settings_account_subtitle),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("account", "profile", "login", "authentication"),
                        onClick = { navController.navigate("settings/account") },
                    ),
                    SettingsItem(
                        key = "music_management",
                        icon = painterResource(R.drawable.library_music),
                        title = stringResource(R.string.local_history),
                        subtitle = stringResource(R.string.local_songs_ready_desc),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("music", "library", "folders", "scan", "refresh", "local"),
                        onClick = { navController.navigate("local_songs") },
                    ),
                    SettingsItem(
                        key = "appearance",
                        icon = painterResource(R.drawable.palette),
                        title = stringResource(R.string.appearance),
                        subtitle = stringResource(R.string.settings_appearance_subtitle),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf("theme", "palette", "material you", "dynamic color", "font", "ui"),
                        onClick = { navController.navigate("settings/appearance") },
                    ),
                    SettingsItem(
                        key = "playback",
                        icon = painterResource(R.drawable.music_note),
                        title = stringResource(R.string.settings_playback_title),
                        subtitle = stringResource(R.string.settings_playback_subtitle),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("audio", "playback", "volume", "quality", "equalizer", "crossfade"),
                        onClick = { navController.navigate("settings/player") },
                    ),
                    SettingsItem(
                        key = "behavior",
                        icon = painterResource(R.drawable.swipe),
                        title = stringResource(R.string.settings_behavior_title),
                        subtitle = stringResource(R.string.settings_behavior_subtitle),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("behavior", "history", "privacy", "tracking", "security", "permissions"),
                        onClick = { navController.navigate("settings/privacy") },
                    ),
                    SettingsItem(
                        key = "integration",
                        icon = painterResource(R.drawable.auto_awesome),
                        title = stringResource(R.string.integration),
                        subtitle = stringResource(R.string.settings_integration_subtitle),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf("integration", "discord", "lastfm", "listenbrainz", "scrobbling", "ai", "translation", "mix"),
                        onClick = { navController.navigate("settings/integration") },
                    ),
                    SettingsItem(
                        key = "ai_integration",
                        icon = painterResource(R.drawable.auto_awesome),
                        title = stringResource(R.string.ai_integration),
                        subtitle = stringResource(R.string.ai_integration_desc),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        keywords = listOf("ai", "translation", "mix", "gpt", "openai", "azure", "custom endpoint"),
                        onClick = { navController.navigate("settings/ai_integration") },
                    )
                    SettingsItem(
                        key = "backup_restore",
                        icon = painterResource(R.drawable.backup),
                        title = stringResource(R.string.backup_restore),
                        subtitle = stringResource(R.string.settings_backup_restore_subtitle),
                        accentColor = MaterialTheme.colorScheme.primary,
                        keywords = listOf("backup", "restore", "import", "export", "migration"),
                        onClick = { navController.navigate("settings/backup_restore") },
                    ),
                    SettingsItem(
                        key = "developer_options",
                        icon = painterResource(R.drawable.experiment),
                        title = stringResource(R.string.settings_developer_options_title),
                        subtitle = stringResource(R.string.settings_developer_options_subtitle),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        keywords = listOf("experimental", "debug", "developer", "labs", "internal"),
                        onClick = { navController.navigate("settings/misc") },
                    ),
                ),
            ),
        )

        add(
            SettingsGroup(
                title = stringResource(R.string.settings_section_player_content),
                items = buildList {
                    add(
                        SettingsItem(
                            key = "lyrics",
                            icon = painterResource(R.drawable.lyrics),
                            title = stringResource(R.string.lyrics),
                            subtitle = stringResource(R.string.settings_lyrics_subtitle),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            keywords = listOf("lyrics", "providers", "lrclib", "kugou", "paxsenix", "romanize"),
                            onClick = { navController.navigate("settings/lyrics") },
                        ),
                    )
                    add(
                        SettingsItem(
                            key = "content",
                            icon = painterResource(R.drawable.language),
                            title = stringResource(R.string.content),
                            subtitle = stringResource(R.string.settings_content_subtitle),
                            accentColor = MaterialTheme.colorScheme.primary,
                            keywords = listOf("language", "content", "translation", "region"),
                            onClick = { navController.navigate("settings/content") },
                        ),
                    )
                    add(
                        SettingsItem(
                            key = "internet",
                            icon = painterResource(R.drawable.wifi_proxy),
                            title = stringResource(R.string.internet),
                            subtitle = stringResource(R.string.settings_internet_subtitle),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            keywords = listOf("internet", "proxy", "dns", "doh", "network"),
                            onClick = { navController.navigate("settings/internet") },
                        ),
                    )
                    add(
                        SettingsItem(
                            key = "po_token",
                            icon = painterResource(R.drawable.token),
                            title = stringResource(R.string.po_token_generation),
                            subtitle = stringResource(R.string.settings_po_token_subtitle),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            keywords = listOf("po token", "token", "web client", "visitor data", "gvs", "player"),
                            onClick = { navController.navigate("settings/po_token") },
                        ),
                    )
                    add(
                        SettingsItem(
                            key = "storage",
                            icon = painterResource(R.drawable.storage),
                            title = stringResource(R.string.storage),
                            subtitle = stringResource(R.string.settings_storage_subtitle),
                            accentColor = MaterialTheme.colorScheme.primary,
                            keywords = listOf("storage", "cache", "offline", "downloads", "cleanup"),
                            onClick = { navController.navigate("settings/storage") },
                        ),
                    )
                    if (isAndroid12OrLater) {
                        add(
                            SettingsItem(
                                key = "default_links",
                                icon = painterResource(R.drawable.link),
                                title = stringResource(R.string.default_links),
                                subtitle = stringResource(R.string.open_supported_links),
                                accentColor = MaterialTheme.colorScheme.secondary,
                                keywords = listOf("links", "deeplink", "default", "supported links"),
                                onClick = {
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
                            key = "updates",
                            icon = painterResource(R.drawable.update),
                            title = stringResource(R.string.updates),
                            subtitle = if (hasUpdate) {
                                stringResource(R.string.new_version_available)
                            } else {
                                stringResource(R.string.settings_updates_subtitle)
                            },
                            showUpdateIndicator = hasUpdate,
                            accentColor = if (hasUpdate) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            badge = if (hasUpdate) "v${BuildConfig.VERSION_NAME}" else BuildConfig.VERSION_NAME,
                            keywords = listOf("update", "version", "release", "changelog"),
                            onClick = { navController.navigate("settings/update") },
                        ),
                    )
                    add(
                        SettingsItem(
                            key = "about",
                            icon = painterResource(R.drawable.info),
                            title = stringResource(R.string.about),
                            subtitle = stringResource(R.string.settings_about_subtitle),
                            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            keywords = listOf("about", "app info", "license", "contributors"),
                            onClick = { navController.navigate("settings/about") },
                        ),
                    )
                },
            ),
        )
    }

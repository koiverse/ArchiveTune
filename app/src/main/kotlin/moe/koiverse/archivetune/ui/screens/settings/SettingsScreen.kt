package moe.koiverse.archivetune.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.Updater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== UI SECTION =====
            SettingsItem(
                icon = R.drawable.palette,
                title = stringResource(R.string.appearance),
                subtitle = stringResource(R.string.appearance_description),
                onClick = { navController.navigate("settings/appearance") }
            )

            // ===== PLAYER & CONTENT =====
            SettingsItem(
                icon = R.drawable.play,
                title = stringResource(R.string.player_and_audio),
                subtitle = stringResource(R.string.player_and_audio_description),
                onClick = { navController.navigate("settings/player") }
            )
            SettingsItem(
                icon = R.drawable.language,
                title = stringResource(R.string.content),
                subtitle = stringResource(R.string.content_description),
                onClick = { navController.navigate("settings/content") }
            )

            // ===== PRIVACY =====
            SettingsItem(
                icon = R.drawable.security,
                title = stringResource(R.string.privacy),
                subtitle = stringResource(R.string.privacy_description),
                onClick = { navController.navigate("settings/privacy") }
            )

            // ===== STORAGE & BACKUP =====
            SettingsItem(
                icon = R.drawable.storage,
                title = stringResource(R.string.storage),
                subtitle = stringResource(R.string.storage_description),
                onClick = { navController.navigate("settings/storage") }
            )
            SettingsItem(
                icon = R.drawable.restore,
                title = stringResource(R.string.backup_restore),
                subtitle = stringResource(R.string.backup_restore_description),
                onClick = { navController.navigate("settings/backup_restore") }
            )

            // ===== SYSTEM =====
            if (isAndroid12OrLater) {
                SettingsItem(
                    icon = R.drawable.link,
                    title = stringResource(R.string.default_links),
                    subtitle = stringResource(R.string.default_links_description),
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
                                is ActivityNotFoundException, is SecurityException -> {
                                    Toast.makeText(
                                        context,
                                        R.string.open_app_settings_error,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                else -> {
                                    Toast.makeText(
                                        context,
                                        R.string.open_app_settings_error,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }

            SettingsItem(
                icon = R.drawable.experiment,
                title = stringResource(R.string.experiment_settings),
                subtitle = stringResource(R.string.experiment_settings_description),
                onClick = { navController.navigate("settings/misc") }
            )

            SettingsItem(
                icon = R.drawable.info,
                title = stringResource(R.string.about),
                subtitle = stringResource(R.string.about_description),
                onClick = { navController.navigate("settings/about") }
            )

            if (latestVersionName != BuildConfig.VERSION_NAME) {
                SettingsItem(
                    icon = R.drawable.update,
                    title = stringResource(R.string.new_version_available),
                    subtitle = latestVersionName,
                    showBadge = true,
                    onClick = { uriHandler.openUri(Updater.getLatestDownloadUrl()) }
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showBadge: Boolean = false
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    if (showBadge) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge()
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

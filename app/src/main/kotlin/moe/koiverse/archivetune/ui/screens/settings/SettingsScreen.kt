package moe.koiverse.archivetune.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain

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
    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                WelcomeCard()
                
                SettingsSection(
                    title = stringResource(R.string.settings_section_ui),
                    icon = painterResource(R.drawable.palette),
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.appearance),
                            icon = painterResource(R.drawable.palette),
                            onClick = { navController.navigate("settings/appearance") }
                        )
                    )
                )
                
                SettingsSection(
                    title = stringResource(R.string.settings_section_player_content),
                    icon = painterResource(R.drawable.play),
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.player_and_audio),
                            icon = painterResource(R.drawable.play),
                            onClick = { navController.navigate("settings/player") }
                        ),
                        SettingItem(
                            title = stringResource(R.string.content),
                            icon = painterResource(R.drawable.language),
                            onClick = { navController.navigate("settings/content") }
                        )
                    )
                )
                
                SettingsSection(
                    title = stringResource(R.string.settings_section_privacy),
                    icon = painterResource(R.drawable.security),
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.privacy),
                            icon = painterResource(R.drawable.security),
                            onClick = { navController.navigate("settings/privacy") }
                        )
                    )
                )
                
                SettingsSection(
                    title = stringResource(R.string.settings_section_storage),
                    icon = painterResource(R.drawable.storage),
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.storage),
                            icon = painterResource(R.drawable.storage),
                            onClick = { navController.navigate("settings/storage") }
                        ),
                        SettingItem(
                            title = stringResource(R.string.backup_restore),
                            icon = painterResource(R.drawable.restore),
                            onClick = { navController.navigate("settings/backup_restore") }
                        )
                    )
                )
                
                SettingsSection(
                    title = stringResource(R.string.settings_section_system),
                    icon = painterResource(R.drawable.integration),
                    items = buildList {
                        add(
                            SettingItem(
                                title = stringResource(R.string.extensions),
                                icon = painterResource(R.drawable.integration),
                                onClick = { navController.navigate("settings/extensions") },
                                onLongClick = { navController.navigate("settings/extensions/create") }
                            )
                        )
                        if (isAndroid12OrLater) {
                            add(
                                SettingItem(
                                    title = stringResource(R.string.default_links),
                                    icon = painterResource(R.drawable.link),
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
                                                is ActivityNotFoundException -> {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.open_app_settings_error,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                
                                                is SecurityException -> {
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
                            )
                        }
                        add(
                            SettingItem(
                                title = stringResource(R.string.experiment_settings),
                                icon = painterResource(R.drawable.experiment),
                                onClick = { navController.navigate("settings/misc") }
                            )
                        )
                        add(
                            SettingItem(
                                title = stringResource(R.string.updates),
                                icon = painterResource(R.drawable.update),
                                badgeVisible = latestVersionName != BuildConfig.VERSION_NAME,
                                onClick = { navController.navigate("settings/update") },
                                trailingContent = if (latestVersionName != BuildConfig.VERSION_NAME) {
                                    {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(
                                                text = stringResource(R.string.new_version_available),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                } else null
                            )
                        )
                        add(
                            SettingItem(
                                title = stringResource(R.string.about),
                                icon = painterResource(R.drawable.info),
                                onClick = { navController.navigate("settings/about") }
                            )
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    )
}

@Composable
private fun WelcomeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.archive),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.painter.Painter,
    items: List<SettingItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 8.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .clickable { item.onClick() }
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = item.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            if (item.trailingContent != null) {
                                item.trailingContent.invoke()
                            } else if (item.badgeVisible) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        text = "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

private data class SettingItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.painter.Painter,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val badgeVisible: Boolean = false,
    val trailingContent: @Composable (() -> Unit)? = null
)

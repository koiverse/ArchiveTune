/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.EnableUpdateNotificationKey
import moe.koiverse.archivetune.constants.UpdateChannel
import moe.koiverse.archivetune.constants.UpdateChannelKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.GitCommit
import moe.koiverse.archivetune.utils.UpdateNotificationManager
import moe.koiverse.archivetune.utils.Updater
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val canaryInstallUrl = "https://nightly.link/koiverse/ArchiveTune/workflows/build/main/app-mobile-universal-release"
    val nightlyInstallUrl = "https://nightly.link/sang765/ArchiveTune-Nightly/workflows/build/dev/app-mobile-universal-nightly"

    val (enableUpdateNotification, onEnableUpdateNotificationChange) = rememberPreference(
        EnableUpdateNotificationKey,
        defaultValue = false
    )
    val (updateChannel, onUpdateChannelChange) = rememberEnumPreference(
        UpdateChannelKey,
        defaultValue = UpdateChannel.STABLE
    )

    var commits by remember { mutableStateOf<List<GitCommit>>(emptyList()) }
    var isLoadingCommits by remember { mutableStateOf(true) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var showCanaryChannelConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showNightlyChannelConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showEnableUpdateNotificationConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val isCanaryChannel = updateChannel == UpdateChannel.CANARY
    val isNightlyChannel = updateChannel == UpdateChannel.NIGHTLY
    val isUpdateAvailable by remember(latestVersion) {
        derivedStateOf {
            latestVersion?.let { !Updater.isSameVersion(it, BuildConfig.VERSION_NAME) } ?: false
        }
    }
    val latestCommit by remember(commits) {
        derivedStateOf { commits.firstOrNull() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            onEnableUpdateNotificationChange(true)
            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
        }
    }

    if (showEnableUpdateNotificationConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEnableUpdateNotificationConfirmDialog = false },
            title = { Text(stringResource(R.string.enable_update_notification)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ArchiveTune provides two download channels for builds:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "• Stable builds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Distributed via official GitHub Releases.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "These versions are tested and recommended for most users.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "• Canary builds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically generated development builds from the main branch.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "• Nightly builds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Experimental builds from the dev branch with a distinct identity.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "Canary builds are provided for testing and early access only.\nStability, compatibility, and functionality are not guaranteed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "By continuing, you acknowledge that canary builds may be unstable and use them at your own risk.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableUpdateNotificationConfirmDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onEnableUpdateNotificationChange(true)
                            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableUpdateNotificationConfirmDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showNightlyChannelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showNightlyChannelConfirmDialog = false },
            title = { Text(stringResource(R.string.channel_nightly)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Switching to the Nightly channel will allow you to download experimental builds from the dev branch.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Nightly builds use a different package name and icon, and are provided for testing only.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNightlyChannelConfirmDialog = false
                        onUpdateChannelChange(UpdateChannel.NIGHTLY)
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNightlyChannelConfirmDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showCanaryChannelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCanaryChannelConfirmDialog = false },
            title = { Text(stringResource(R.string.channel_canary)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ArchiveTune provides two download channels for builds:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "• Stable builds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Distributed via official GitHub Releases.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "These versions are tested and recommended for most users.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "• Canary builds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically generated development builds from the main branch.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "• Nightly builds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Experimental builds from the dev branch with a distinct identity.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "Canary builds are provided for testing and early access only.\nStability, compatibility, and functionality are not guaranteed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "By continuing, you acknowledge that canary builds may be unstable and use them at your own risk.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCanaryChannelConfirmDialog = false
                        onUpdateChannelChange(UpdateChannel.CANARY)
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCanaryChannelConfirmDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(updateChannel) {
        isLoadingCommits = true
        
        val (apkOwner, apkRepo, branch) = when (updateChannel) {
            UpdateChannel.NIGHTLY -> Triple("sang765", "ArchiveTune-Nightly", "dev")
            else -> Triple("koiverse", "ArchiveTune", "main")
        }

        Updater.getLatestVersionName(owner = apkOwner, repo = apkRepo).onSuccess {
            latestVersion = it
        }
        
        Updater.getCommitHistory(count = 30, branch = branch, owner = "koiverse", repo = "ArchiveTune").onSuccess {
            commits = it
        }.onFailure {
            commits = emptyList()
        }
        isLoadingCommits = false
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )
    val topBarSubtitle = when (updateChannel) {
        UpdateChannel.STABLE -> stringResource(R.string.updates_subtitle_stable)
        UpdateChannel.CANARY -> stringResource(R.string.updates_subtitle_canary)
        UpdateChannel.NIGHTLY -> stringResource(R.string.updates_subtitle_nightly)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.updates),
                        fontWeight = FontWeight.Bold,
                    )
                },
                subtitle = {
                    Text(
                        text = topBarSubtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {},
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                UpdateSummaryCard(
                    currentVersion = BuildConfig.VERSION_NAME,
                    latestVersion = latestVersion,
                    updateChannel = updateChannel,
                    isUpdateAvailable = isUpdateAvailable,
                    onOpenChangelog = { navController.navigate("settings/changelog") }
                )
            }

            item {
                PreferenceGroupTitle(title = stringResource(R.string.notification_settings))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(R.string.enable_update_notification))
                        },
                        supportingContent = {
                            Text(text = stringResource(R.string.enable_update_notification_desc))
                        },
                        leadingContent = {
                            FeatureIcon(
                                iconRes = R.drawable.new_release,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enableUpdateNotification,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showEnableUpdateNotificationConfirmDialog = true
                                    } else {
                                        onEnableUpdateNotificationChange(false)
                                        UpdateNotificationManager.cancelPeriodicUpdateCheck(context)
                                    }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                PreferenceGroupTitle(title = stringResource(R.string.commit_history))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(R.string.update_channel))
                            },
                            supportingContent = {
                                Text(text = stringResource(R.string.update_channel_desc))
                            },
                            leadingContent = {
                                FeatureIcon(
                                    iconRes = R.drawable.tune,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = when (updateChannel) {
                                        UpdateChannel.STABLE -> stringResource(R.string.channel_stable)
                                        UpdateChannel.CANARY -> stringResource(R.string.channel_canary)
                                        UpdateChannel.NIGHTLY -> stringResource(R.string.channel_nightly)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            SegmentedButton(
                                selected = updateChannel == UpdateChannel.STABLE,
                                onClick = { onUpdateChannelChange(UpdateChannel.STABLE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                icon = {},
                            ) {
                                Text(text = stringResource(R.string.channel_stable))
                            }
                            SegmentedButton(
                                selected = updateChannel == UpdateChannel.CANARY,
                                onClick = {
                                    if (updateChannel != UpdateChannel.CANARY) {
                                        showCanaryChannelConfirmDialog = true
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                icon = {},
                            ) {
                                Text(text = stringResource(R.string.channel_canary))
                            }
                            SegmentedButton(
                                selected = updateChannel == UpdateChannel.NIGHTLY,
                                onClick = {
                                    if (updateChannel != UpdateChannel.NIGHTLY) {
                                        showNightlyChannelConfirmDialog = true
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                icon = {},
                            ) {
                                Text(text = stringResource(R.string.channel_nightly))
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = isCanaryChannel) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ListItem(
                                overlineContent = {
                                    Text(text = stringResource(R.string.channel_canary))
                                },
                                headlineContent = {
                                    Text(
                                        text = stringResource(R.string.updates_canary_title),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = stringResource(R.string.updates_canary_description))
                                        Text(
                                            text = stringResource(
                                                R.string.updates_latest_commit,
                                                latestCommit?.sha ?: "-"
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                leadingContent = {
                                    FeatureIcon(
                                        iconRes = R.drawable.download,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            OutlinedButton(
                                onClick = { uriHandler.openUri(canaryInstallUrl) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.download))
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = isNightlyChannel) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ListItem(
                                overlineContent = {
                                    Text(text = stringResource(R.string.channel_nightly))
                                },
                                headlineContent = {
                                    Text(
                                        text = stringResource(R.string.updates_nightly_title),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = stringResource(R.string.updates_nightly_description))
                                        Text(
                                            text = stringResource(
                                                R.string.updates_latest_commit,
                                                latestCommit?.sha ?: "-"
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                leadingContent = {
                                    FeatureIcon(
                                        iconRes = R.drawable.download,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            OutlinedButton(
                                onClick = { uriHandler.openUri(nightlyInstallUrl) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.download))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Column {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.recent_commits),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = when {
                                        isLoadingCommits -> stringResource(R.string.updates_loading_commits)
                                        commits.isEmpty() -> stringResource(R.string.updates_no_commits)
                                        else -> stringResource(
                                            R.string.updates_recent_commits_count,
                                            commits.size,
                                        )
                                    }
                                )
                            },
                            leadingContent = {
                                FeatureIcon(
                                    iconRes = R.drawable.history,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            },
                            trailingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.expand_more),
                                    contentDescription = null,
                                    modifier = Modifier.rotate(rotationAngle),
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )

                                when {
                                    isLoadingCommits -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                LoadingIndicator(modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = stringResource(R.string.updates_loading_commits),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }

                                    commits.isEmpty() -> {
                                        Text(
                                            text = stringResource(R.string.updates_no_commits),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp),
                                        )
                                    }

                                    else -> {
                                        Text(
                                            text = stringResource(
                                                R.string.updates_recent_commits_count,
                                                commits.size,
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isExpanded && !isLoadingCommits) {
                items(
                    items = commits,
                    key = { it.sha },
                    contentType = { "commit" },
                ) { commit ->
                    CommitItem(
                        commit = commit,
                        onClick = { uriHandler.openUri(commit.url) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun UpdateSummaryCard(
    currentVersion: String,
    latestVersion: String?,
    updateChannel: UpdateChannel,
    isUpdateAvailable: Boolean,
    onOpenChangelog: () -> Unit,
) {
    val channelLabel = when (updateChannel) {
        UpdateChannel.STABLE -> stringResource(R.string.channel_stable)
        UpdateChannel.CANARY -> stringResource(R.string.channel_canary)
        UpdateChannel.NIGHTLY -> stringResource(R.string.channel_nightly)
    }
    val supportingText = when {
        latestVersion == null -> stringResource(R.string.updates_status_checking)
        isUpdateAvailable -> stringResource(R.string.latest_version_format, latestVersion)
        else -> stringResource(R.string.updates_status_current)
    }
    val channelContainerColor = when (updateChannel) {
        UpdateChannel.NIGHTLY -> MaterialTheme.colorScheme.tertiaryContainer
        UpdateChannel.CANARY -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val channelContentColor = when (updateChannel) {
        UpdateChannel.NIGHTLY -> MaterialTheme.colorScheme.onTertiaryContainer
        UpdateChannel.CANARY -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ListItem(
                overlineContent = {
                    Text(text = stringResource(R.string.current_version))
                },
                headlineContent = {
                    Text(
                        text = currentVersion,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                supportingContent = {
                    Text(text = supportingText)
                },
                leadingContent = {
                    FeatureIcon(
                        iconRes = R.drawable.update,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                trailingContent = {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = channelContainerColor,
                    ) {
                        Text(
                            text = channelLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = channelContentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            FilledTonalButton(
                onClick = onOpenChangelog,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.view_changelog))
            }
        }
    }
}

@Composable
private fun FeatureIcon(
    @DrawableRes iconRes: Int,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .padding(12.dp)
                .size(22.dp),
        )
    }
}

@Composable
private fun CommitItem(
    commit: GitCommit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        onClick = onClick
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = commit.sha,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (commit.date.isNotEmpty()) {
                            commit.author + " - " + formatCommitDate(commit.date)
                        } else {
                            commit.author
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingContent = {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(10.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {}
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

private fun formatCommitDate(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoDate)
        val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        outputFormat.format(date!!)
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

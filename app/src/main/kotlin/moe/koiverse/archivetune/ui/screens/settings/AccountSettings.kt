@file:OptIn(ExperimentalMaterial3Api::class)

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.App.Companion.forgetAccount
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AccountChannelHandleKey
import moe.koiverse.archivetune.constants.AccountEmailKey
import moe.koiverse.archivetune.constants.AccountNameKey
import moe.koiverse.archivetune.constants.DataSyncIdKey
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.constants.SelectedYtmPlaylistsKey
import moe.koiverse.archivetune.constants.UseLoginForBrowse
import moe.koiverse.archivetune.constants.VisitorDataKey
import moe.koiverse.archivetune.constants.YtmSyncKey
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.PlaylistItem
import moe.koiverse.archivetune.innertube.utils.completed
import moe.koiverse.archivetune.innertube.utils.parseCookieString
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.InfoLabel
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.ui.screens.buildLoginRoute
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.PreferenceStore
import moe.koiverse.archivetune.utils.Updater
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.putLegacyPoToken
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.HomeViewModel

@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val accountLabel = stringResource(R.string.account)
    val generalLabel = stringResource(R.string.general)
    val integrationLabel = stringResource(R.string.integration)
    val miscLabel = stringResource(R.string.misc)
    val loginLabel = stringResource(R.string.login)
    val notLoggedInLabel = stringResource(R.string.not_logged_in)
    val tokenDescription = stringResource(R.string.token_adv_login_description)

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val onLegacyPoTokenChange: (String) -> Unit = { value ->
        PreferenceStore.launchEdit(context.dataStore) {
            putLegacyPoToken(value)
        }
    }

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    LaunchedEffect(useLoginForBrowse) {
        YouTube.useLoginForBrowse = useLoginForBrowse
    }

    val viewModel: HomeViewModel = hiltViewModel()
    val accountNameFromViewModel by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()

    val displayName = when {
        accountNameFromViewModel.isNotBlank() -> accountNameFromViewModel
        accountNamePref.isNotBlank() -> accountNamePref
        isLoggedIn -> accountLabel
        else -> loginLabel
    }
    val topBarSubtitle = when {
        isLoggedIn && accountEmail.isNotBlank() -> accountEmail
        isLoggedIn && accountChannelHandle.isNotBlank() -> accountChannelHandle
        isLoggedIn -> displayName
        else -> notLoggedInLabel
    }

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            showToken = false
        }
    }

    val hasUpdate = !Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)
    val tokenActionTitle = when {
        !isLoggedIn -> stringResource(R.string.advanced_login)
        showToken -> stringResource(R.string.token_shown)
        else -> stringResource(R.string.token_hidden)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = accountLabel,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { showTokenEditor = true }) {
                        Icon(
                            painter = painterResource(R.drawable.token),
                            contentDescription = null,
                        )
                    }

                    if (hasUpdate) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error)
                            },
                        ) {
                            FilledTonalIconButton(
                                onClick = { uriHandler.openUri(Updater.getLatestDownloadUrl()) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.update),
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AccountHeroCard(
                    isLoggedIn = isLoggedIn,
                    accountName = displayName,
                    accountEmail = accountEmail,
                    accountHandle = accountChannelHandle,
                    accountImageUrl = accountImageUrl,
                    onPrimaryAction = {
                        if (isLoggedIn) {
                            navController.navigate("account")
                        } else {
                            navController.navigate(buildLoginRoute())
                        }
                    },
                    onSecondaryAction = {
                        if (isLoggedIn) {
                            showToken = false
                            onInnerTubeCookieChange("")
                            forgetAccount(context)
                        } else {
                            showTokenEditor = true
                        }
                    },
                )
            }

            item {
                AccountShortcutChips(
                    isLoggedIn = isLoggedIn,
                    statusLabel = if (isLoggedIn) displayName else loginLabel,
                    accountHandle = accountChannelHandle,
                    onStatusClick = {
                        if (isLoggedIn) {
                            navController.navigate("account")
                        } else {
                            navController.navigate(buildLoginRoute())
                        }
                    },
                    onPlaylistClick = { showPlaylistDialog = true },
                    onIntegrationClick = { navController.navigate("settings/integration") },
                    onAdvancedLoginClick = { showTokenEditor = true },
                )
            }

            item {
                AnimatedVisibility(
                    visible = showToken && hasVisibleSecureDetails(
                        innerTubeCookie = innerTubeCookie,
                        visitorData = visitorData,
                        dataSyncId = dataSyncId,
                        poToken = YouTube.poToken.orEmpty(),
                    ),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    TokenPreviewCard(
                        description = tokenDescription,
                        innerTubeCookie = innerTubeCookie,
                        visitorData = visitorData,
                        dataSyncId = dataSyncId,
                        poToken = YouTube.poToken.orEmpty(),
                        onEdit = { showTokenEditor = true },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = isLoggedIn,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    AccountSectionCard(title = generalLabel) {
                        AccountSwitchRow(
                            icon = painterResource(R.drawable.add_circle),
                            title = stringResource(R.string.more_content),
                            subtitle = stringResource(R.string.use_login_for_browse_desc),
                            checked = useLoginForBrowse,
                            onCheckedChange = onUseLoginForBrowseChange,
                        )

                        SectionDivider()

                        AccountSwitchRow(
                            icon = painterResource(R.drawable.cached),
                            title = stringResource(R.string.yt_sync),
                            checked = ytmSync,
                            onCheckedChange = onYtmSyncChange,
                        )
                    }
                }
            }

            item {
                AccountSectionCard(title = integrationLabel) {
                    AccountActionRow(
                        icon = painterResource(R.drawable.playlist_add),
                        title = stringResource(R.string.select_playlist_to_sync),
                        onClick = { showPlaylistDialog = true },
                    )

                    SectionDivider()

                    AccountActionRow(
                        icon = painterResource(R.drawable.integration),
                        title = integrationLabel,
                        subtitle = "Discord, Last.fm, ListenBrainz",
                        onClick = { navController.navigate("settings/integration") },
                    )

                    SectionDivider()

                    AccountActionRow(
                        icon = painterResource(R.drawable.fire),
                        title = stringResource(R.string.music_together),
                        onClick = { navController.navigate("settings/music_together") },
                    )
                }
            }

            item {
                AccountSectionCard(title = miscLabel) {
                    AccountActionRow(
                        icon = painterResource(R.drawable.token),
                        title = tokenActionTitle,
                        subtitle = tokenDescription,
                        showBadge = isLoggedIn && showToken,
                        highlight = isLoggedIn && showToken,
                        onClick = {
                            if (!isLoggedIn) {
                                showTokenEditor = true
                            } else if (!showToken) {
                                showToken = true
                            } else {
                                showTokenEditor = true
                            }
                        },
                    )
                }
            }

            if (hasUpdate) {
                item {
                    UpdateAvailableCard(
                        latestVersion = latestVersionName,
                        onClick = { uriHandler.openUri(Updater.getLatestDownloadUrl()) },
                    )
                }
            }

            item {
                AccountVersionFooter()
            }
        }
    }

    if (showTokenEditor) {
        TokenEditorDialog(
            innerTubeCookie = innerTubeCookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            accountNamePref = accountNamePref,
            accountEmail = accountEmail,
            accountChannelHandle = accountChannelHandle,
            onInnerTubeCookieChange = onInnerTubeCookieChange,
            onPoTokenChange = onLegacyPoTokenChange,
            onVisitorDataChange = onVisitorDataChange,
            onDataSyncIdChange = onDataSyncIdChange,
            onAccountNameChange = onAccountNameChange,
            onAccountEmailChange = onAccountEmailChange,
            onAccountChannelHandleChange = onAccountChannelHandleChange,
            onDismiss = { showTokenEditor = false },
        )
    }

    if (showPlaylistDialog) {
        PlaylistSelectionDialog(
            onDismiss = { showPlaylistDialog = false },
        )
    }
}

@Composable
private fun AccountHeroCard(
    isLoggedIn: Boolean,
    accountName: String,
    accountEmail: String,
    accountHandle: String,
    accountImageUrl: String?,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                )
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isLoggedIn) R.drawable.account else R.drawable.login,
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.account),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (accountEmail.isNotBlank()) {
                            Text(
                                text = accountEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.not_logged_in),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (accountHandle.isNotBlank()) {
                            Text(
                                text = accountHandle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = onPrimaryAction,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLoggedIn) R.drawable.account else R.drawable.login,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isLoggedIn) stringResource(R.string.account) else stringResource(R.string.login),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    OutlinedButton(
                        onClick = onSecondaryAction,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (isLoggedIn) {
                            Text(
                                text = stringResource(R.string.action_logout),
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.advanced_login),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountShortcutChips(
    isLoggedIn: Boolean,
    statusLabel: String,
    accountHandle: String,
    onStatusClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onIntegrationClick: () -> Unit,
    onAdvancedLoginClick: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterChip(
            selected = isLoggedIn,
            onClick = onStatusClick,
            label = {
                Text(
                    text = statusLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(
                        if (isLoggedIn) R.drawable.account else R.drawable.login,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        if (accountHandle.isNotBlank()) {
            AssistChip(
                onClick = onStatusClick,
                label = {
                    Text(
                        text = accountHandle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.account),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }

        AssistChip(
            onClick = onPlaylistClick,
            label = { Text(stringResource(R.string.select_playlist_to_sync)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )

        AssistChip(
            onClick = onIntegrationClick,
            label = { Text(stringResource(R.string.integration)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.integration),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )

        AssistChip(
            onClick = onAdvancedLoginClick,
            label = { Text(stringResource(R.string.advanced_login)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.token),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
    }
}

@Composable
private fun AccountSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun AccountActionRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    showBadge: Boolean = false,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    val containerColor = if (highlight) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = subtitle?.let {
                {
                    Text(
                        text = subtitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = {
                RowIcon(
                    icon = icon,
                    emphasized = highlight,
                )
            },
            trailingContent = {
                if (showBadge) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = MaterialTheme.colorScheme.error)
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun AccountSwitchRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else Color.Transparent,
            )
            .clickable { onCheckedChange(!checked) },
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = subtitle?.let {
                {
                    Text(
                        text = subtitle,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = {
                RowIcon(
                    icon = icon,
                    emphasized = checked,
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun RowIcon(
    icon: Painter,
    emphasized: Boolean,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (emphasized) {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                        ),
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        ),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
}

@Composable
private fun TokenPreviewCard(
    description: String,
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    poToken: String,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.advanced_login),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(12.dp))

                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.advanced_login),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            SecureValueItem(label = "INNERTUBE COOKIE", value = innerTubeCookie)

            if (visitorData.isNotBlank()) {
                SecureValueItem(label = "VISITOR DATA", value = visitorData)
            }

            if (dataSyncId.isNotBlank()) {
                SecureValueItem(label = "DATASYNC ID", value = dataSyncId)
            }

            if (poToken.isNotBlank()) {
                SecureValueItem(label = "PO TOKEN", value = poToken)
            }
        }
    }
}

@Composable
private fun SecureValueItem(
    label: String,
    value: String,
) {
    ListItem(
        overlineContent = {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = {
            Text(
                text = previewSecureValue(value),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.62f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.clip(RoundedCornerShape(22.dp)),
    )
}

@Composable
private fun UpdateAvailableCard(
    latestVersion: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = MaterialTheme.colorScheme.error)
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.update),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_version_available),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = latestVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_text),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AccountVersionFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
        )
    }
}

@Composable
private fun TokenEditorDialog(
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    accountNamePref: String,
    accountEmail: String,
    accountChannelHandle: String,
    onInnerTubeCookieChange: (String) -> Unit,
    onPoTokenChange: (String) -> Unit,
    onVisitorDataChange: (String) -> Unit,
    onDataSyncIdChange: (String) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onAccountEmailChange: (String) -> Unit,
    onAccountChannelHandleChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val text = """
        ***INNERTUBE COOKIE*** =$innerTubeCookie
        ***VISITOR DATA*** =$visitorData
        ***DATASYNC ID*** =$dataSyncId
        ***PO TOKEN*** =${YouTube.poToken.orEmpty()}
        ***ACCOUNT NAME*** =$accountNamePref
        ***ACCOUNT EMAIL*** =$accountEmail
        ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
    """.trimIndent()

    TextFieldDialog(
        initialTextFieldValue = TextFieldValue(text),
        onDone = { data ->
            data.split("\n").forEach {
                when {
                    it.startsWith("***INNERTUBE COOKIE*** =") -> onInnerTubeCookieChange(it.substringAfter("="))
                    it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                    it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                    it.startsWith("***PO TOKEN*** =") -> onPoTokenChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> onAccountChannelHandleChange(it.substringAfter("="))
                }
            }
        },
        onDismiss = onDismiss,
        singleLine = false,
        maxLines = 20,
        isInputValid = {
            it.isNotEmpty() && "SAPISID" in parseCookieString(it)
        },
        extraContent = {
            InfoLabel(text = stringResource(R.string.token_adv_login_description))
        },
    )
}

@Composable
private fun PlaylistSelectionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val (initialSelected, _) = rememberPreference(SelectedYtmPlaylistsKey, "")
    val selectedList = remember { mutableStateListOf<String>() }

    LaunchedEffect(initialSelected) {
        selectedList.clear()
        if (initialSelected.isNotEmpty()) {
            selectedList.addAll(
                initialSelected.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
            )
        }
    }

    var loading by remember { mutableStateOf(true) }
    val playlists = remember { mutableStateListOf<PlaylistItem>() }

    LaunchedEffect(Unit) {
        loading = true
        YouTube
            .library("FEmusic_liked_playlists")
            .completed()
            .onSuccess { page ->
                playlists.clear()
                playlists.addAll(
                    page.items
                        .filterIsInstance<PlaylistItem>()
                        .filterNot { it.id == "LM" || it.id == "SE" }
                        .reversed(),
                )
            }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        confirmButton = {
            TextButton(
                onClick = {
                    PreferenceStore.launchEdit(context.dataStore) {
                        this[SelectedYtmPlaylistsKey] = selectedList.joinToString(",")
                    }
                    onDismiss()
                },
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel_button))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.select_playlist_to_sync),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(46.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = playlists,
                        key = { it.id },
                    ) { playlist ->
                        PlaylistSelectionRow(
                            playlist = playlist,
                            isSelected = selectedList.contains(playlist.id),
                            onSelectedChange = { isSelected ->
                                selectedList.setSelected(playlist.id, isSelected)
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PlaylistSelectionRow(
    playlist: PlaylistItem,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f)
        },
        label = "playlistSelectionBackground",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onSelectedChange(!isSelected) },
        color = backgroundColor,
        shape = RoundedCornerShape(22.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = playlist.title,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = playlist.songCountText?.takeIf { it.isNotBlank() }?.let {
                {
                    Text(
                        text = playlist.songCountText.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leadingContent = {
                AsyncImage(
                    model = playlist.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            },
            trailingContent = {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked -> onSelectedChange(checked) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

private fun hasVisibleSecureDetails(
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    poToken: String,
): Boolean {
    return innerTubeCookie.isNotBlank() || visitorData.isNotBlank() || dataSyncId.isNotBlank() || poToken.isNotBlank()
}

private fun previewSecureValue(value: String): String {
    val normalized = value.replace("\n", " ").replace("\r", " ").trim()
    if (normalized.length <= 76) {
        return normalized
    }
    return normalized.take(52) + "..." + normalized.takeLast(18)
}

private fun SnapshotStateList<String>.setSelected(id: String, selected: Boolean) {
    if (selected) {
        if (!contains(id)) {
            add(id)
        }
    } else {
        remove(id)
    }
}


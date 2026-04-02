@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

private val CardShape = RoundedCornerShape(28.dp)
private val InnerTileShape = RoundedCornerShape(22.dp)
private val AvatarSize = 88.dp
private val QuickTileIconSize = 48.dp
private val RowIconSize = 42.dp
private const val PressScale = 0.96f

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
                    Column {
                        Text(
                            text = accountLabel,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
                    OutlinedIconButton(
                        onClick = { showTokenEditor = true },
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = null,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.token),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    if (hasUpdate) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error)
                            },
                        ) {
                            OutlinedIconButton(
                                onClick = { uriHandler.openUri(Updater.getLatestDownloadUrl()) },
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                                border = null,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.update),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ProfileIdentityCard(
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

            if (hasUpdate) {
                item {
                    UpdateBannerStrip(
                        latestVersion = latestVersionName,
                        onClick = { uriHandler.openUri(Updater.getLatestDownloadUrl()) },
                    )
                }
            }

            item {
                QuickAccessGrid(
                    isLoggedIn = isLoggedIn,
                    onPlaylistClick = { showPlaylistDialog = true },
                    onIntegrationClick = { navController.navigate("settings/integration") },
                    onMusicTogetherClick = { navController.navigate("settings/music_together") },
                    onTokenClick = {
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

            item {
                AnimatedVisibility(
                    visible = showToken && hasVisibleSecureDetails(
                        innerTubeCookie = innerTubeCookie,
                        visitorData = visitorData,
                        dataSyncId = dataSyncId,
                        poToken = YouTube.poToken.orEmpty(),
                    ),
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + expandVertically(
                        spring(stiffness = Spring.StiffnessLow),
                    ),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    TokenRevealCard(
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
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + expandVertically(
                        spring(stiffness = Spring.StiffnessLow),
                    ),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    ExpressiveSectionCard(title = generalLabel) {
                        ExpressiveSwitchRow(
                            icon = painterResource(R.drawable.add_circle),
                            title = stringResource(R.string.more_content),
                            subtitle = stringResource(R.string.use_login_for_browse_desc),
                            checked = useLoginForBrowse,
                            onCheckedChange = onUseLoginForBrowseChange,
                        )

                        ExpressiveDivider()

                        ExpressiveSwitchRow(
                            icon = painterResource(R.drawable.cached),
                            title = stringResource(R.string.yt_sync),
                            checked = ytmSync,
                            onCheckedChange = onYtmSyncChange,
                        )
                    }
                }
            }

            item {
                ExpressiveSectionCard(title = integrationLabel) {
                    ExpressiveActionRow(
                        icon = painterResource(R.drawable.playlist_add),
                        title = stringResource(R.string.select_playlist_to_sync),
                        onClick = { showPlaylistDialog = true },
                    )

                    ExpressiveDivider()

                    ExpressiveActionRow(
                        icon = painterResource(R.drawable.integration),
                        title = integrationLabel,
                        subtitle = "Discord, Last.fm, ListenBrainz",
                        onClick = { navController.navigate("settings/integration") },
                    )

                    ExpressiveDivider()

                    ExpressiveActionRow(
                        icon = painterResource(R.drawable.fire),
                        title = stringResource(R.string.music_together),
                        onClick = { navController.navigate("settings/music_together") },
                    )
                }
            }

            item {
                ExpressiveSectionCard(title = miscLabel) {
                    ExpressiveActionRow(
                        icon = painterResource(R.drawable.token),
                        title = tokenActionTitle,
                        subtitle = tokenDescription,
                        accent = if (isLoggedIn && showToken) MaterialTheme.colorScheme.tertiary else null,
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

            item {
                VersionStamp()
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
private fun ProfileIdentityCard(
    isLoggedIn: Boolean,
    accountName: String,
    accountEmail: String,
    accountHandle: String,
    accountImageUrl: String?,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PressScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "heroScale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onPrimaryAction,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0f),
                        ),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                ),
                            ),
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f),
                                ),
                            ),
                            shape = CircleShape,
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
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isLoggedIn,
                    enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)),
                    exit = scaleOut(),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AnimatedContent(
                    targetState = accountName,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessLow)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessHigh)))
                    },
                    label = "nameTransition",
                ) { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (accountEmail.isNotBlank()) {
                    Text(
                        text = accountEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (accountHandle.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text(
                            text = accountHandle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!isLoggedIn) {
                    Text(
                        text = stringResource(R.string.not_logged_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                ElevatedButton(
                    onClick = onPrimaryAction,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 0.dp,
                    ),
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
                    Text(
                        text = if (isLoggedIn) stringResource(R.string.action_logout) else stringResource(R.string.advanced_login),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateBannerStrip(
    latestVersion: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PressScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "updateScale",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CardShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgedBox(
                badge = { Badge(containerColor = MaterialTheme.colorScheme.error) },
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.10f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.update),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_version_available),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = latestVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                )
            }

            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
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
private fun QuickAccessGrid(
    isLoggedIn: Boolean,
    onPlaylistClick: () -> Unit,
    onIntegrationClick: () -> Unit,
    onMusicTogetherClick: () -> Unit,
    onTokenClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAccessTile(
                modifier = Modifier.weight(1f),
                icon = painterResource(R.drawable.playlist_add),
                label = stringResource(R.string.select_playlist_to_sync),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onPlaylistClick,
            )
            QuickAccessTile(
                modifier = Modifier.weight(1f),
                icon = painterResource(R.drawable.integration),
                label = stringResource(R.string.integration),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onIntegrationClick,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAccessTile(
                modifier = Modifier.weight(1f),
                icon = painterResource(R.drawable.fire),
                label = stringResource(R.string.music_together),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onMusicTogetherClick,
            )
            QuickAccessTile(
                modifier = Modifier.weight(1f),
                icon = painterResource(R.drawable.token),
                label = stringResource(R.string.advanced_login),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = onTokenClick,
            )
        }
    }
}

@Composable
private fun QuickAccessTile(
    modifier: Modifier = Modifier,
    icon: Painter,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileElevation",
    )

    Surface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = InnerTileShape,
        color = containerColor,
        tonalElevation = elevation,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = contentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(QuickTileIconSize),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpressiveSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ExpressiveActionRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    accent: Color? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rowScale",
    )
    val tint = accent ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(InnerTileShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExpressiveRowIcon(icon = icon, tint = tint)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun ExpressiveSwitchRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "switchRowBg",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(InnerTileShape)
            .background(containerColor)
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExpressiveRowIcon(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary,
                emphasized = checked,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                ),
            )
        }
    }
}

@Composable
private fun ExpressiveRowIcon(
    icon: Painter,
    tint: Color,
    emphasized: Boolean = false,
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (emphasized) 0.20f else 0.10f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconBgAlpha",
    )

    Surface(
        modifier = Modifier.size(RowIconSize),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = bgAlpha),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = tint,
            )
        }
    }
}

@Composable
private fun ExpressiveDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp, end = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}

@Composable
private fun TokenRevealCard(
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    poToken: String,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.advanced_login),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.advanced_login),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            TokenValueChip(label = "INNERTUBE COOKIE", value = innerTubeCookie)

            if (visitorData.isNotBlank()) {
                TokenValueChip(label = "VISITOR DATA", value = visitorData)
            }

            if (dataSyncId.isNotBlank()) {
                TokenValueChip(label = "DATASYNC ID", value = dataSyncId)
            }

            if (poToken.isNotBlank()) {
                TokenValueChip(label = "PO TOKEN", value = poToken)
            }
        }
    }
}

@Composable
private fun TokenValueChip(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            )
            Text(
                text = previewSecureValue(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VersionStamp() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
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
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    PreferenceStore.launchEdit(context.dataStore) {
                        this[SelectedYtmPlaylistsKey] = selectedList.joinToString(",")
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.not_logged_in),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.30f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "playlistBg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "playlistBorder",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable { onSelectedChange(!isSelected) },
        color = backgroundColor,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = playlist.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                playlist.songCountText?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectedChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                ),
            )
        }
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
    return normalized.take(52) + "\u2025" + normalized.takeLast(18)
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


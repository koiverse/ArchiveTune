/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */



@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import androidx.datastore.preferences.core.stringPreferencesKey
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.discord.DiscordAuthCoordinator
import moe.koiverse.archivetune.discord.DiscordOAuthRepository
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceGroup
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.component.ListItem
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.makeTimeString
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.utils.TranslatorLanguages
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import timber.log.Timber
import moe.koiverse.archivetune.utils.getPresenceIntervalMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import moe.koiverse.archivetune.utils.ArtworkStorage

enum class ActivitySource { ARTIST, ALBUM, SONG, APP }

private enum class DiscordAuthorizationUiMode { Idle, Waiting, Success, Failure }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val song by playerConnection.currentSong.collectAsState(null)
    val playbackState by playerConnection.playbackState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var discordAvatarUrl by rememberPreference(DiscordAvatarUrlKey, "")
    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, false)
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }
    var authorizationSession by remember {
        mutableStateOf(DiscordOAuthRepository.createAuthorizationSession())
    }
    var authorizationUiModeName by rememberSaveable {
        mutableStateOf(DiscordAuthorizationUiMode.Idle.name)
    }
    var authorizationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val authorizationUiMode = remember(authorizationUiModeName) {
        DiscordAuthorizationUiMode.valueOf(authorizationUiModeName)
    }

    val imageOptions = remember { listOf("thumbnail", "artist", "appicon", "custom") }
    val smallImageOptions = remember {
        listOf("thumbnail", "artist", "appicon", "custom", "dontshow")
    }
    val activityStatusOptions = remember { listOf("online", "dnd", "idle", "streaming") }
    val intervalOptions = remember { listOf("20s", "50s", "1m", "5m", "Custom", "Disabled") }
    val platformOptions = remember { listOf("desktop", "xbox", "samsung", "ios", "android", "embedded", "ps4", "ps5") }
    val activityOptions = remember {
        listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "COMPETING")
    }
    val largeTextOptions = remember {
        listOf("song", "artist", "album", "app", "custom", "dontshow")
    }

    LaunchedEffect(discordToken) {
        val token = discordToken
        if (token.isNotBlank()) {
            runCatching {
                DiscordOAuthRepository.fetchAccount(token)
            }.onSuccess {
                discordUsername = it.username
                discordName = it.displayName
                discordAvatarUrl = it.avatarUrl.orEmpty()
            }.onFailure {
                Timber.tag("DiscordSettings").w(it, "Discord account lookup failed")
            }
        }
    }

    val (discordRPC, onDiscordRPCChange) = rememberPreference(
        key = EnableDiscordRPCKey,
        defaultValue = true
    )

    LaunchedEffect(discordToken, discordRPC) {
        if (discordRPC && discordToken.isNotBlank()) {
            Timber.tag("DiscordSettings").d("Discord Rich Presence enabled, MusicService will handle start")
        } else {
            Timber.tag("DiscordSettings").d("Discord Rich Presence disabled or not authorized, stopping manager")
            DiscordPresenceManager.stop()
        }
    }

    val isLoggedIn = remember(discordToken) { discordToken.isNotEmpty() }
    val accountDisplayName = remember(isLoggedIn, discordName, discordUsername, context) {
        when {
            discordName.isNotBlank() -> discordName
            discordUsername.isNotBlank() -> discordUsername
            isLoggedIn -> context.getString(R.string.account)
            else -> context.getString(R.string.not_logged_in)
        }
    }

    val launchAuthorization: () -> Unit = {
        val session = DiscordOAuthRepository.createAuthorizationSession()
        authorizationSession = session
        authorizationMessage = null
        authorizationUiModeName = DiscordAuthorizationUiMode.Waiting.name

        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, session.authorizationUri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                },
            )
        }.onFailure {
            authorizationUiModeName = DiscordAuthorizationUiMode.Failure.name
            authorizationMessage = it.message ?: context.getString(R.string.discord_authorization_failed)
        }
    }

    LaunchedEffect(authorizationSession.state, authorizationUiMode) {
        if (authorizationUiMode != DiscordAuthorizationUiMode.Waiting) {
            return@LaunchedEffect
        }

        DiscordAuthCoordinator.redirects.collectLatest { redirect ->
            if (redirect.getQueryParameter("state") != authorizationSession.state) {
                return@collectLatest
            }

            DiscordOAuthRepository.completeAuthorization(
                context = context,
                session = authorizationSession,
                redirect = redirect,
            ).onSuccess { session ->
                discordUsername = session.account?.username.orEmpty()
                discordName = session.account?.displayName.orEmpty()
                discordAvatarUrl = session.account?.avatarUrl.orEmpty()
                authorizationMessage = context.getString(R.string.discord_authorization_success)
                authorizationUiModeName = DiscordAuthorizationUiMode.Success.name
                authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
            }.onFailure {
                authorizationMessage = it.message ?: context.getString(R.string.discord_authorization_failed)
                authorizationUiModeName = DiscordAuthorizationUiMode.Failure.name
                authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
            }
        }
    }

    LaunchedEffect(authorizationUiMode) {
        if (authorizationUiMode == DiscordAuthorizationUiMode.Success ||
            authorizationUiMode == DiscordAuthorizationUiMode.Failure
        ) {
            delay(2600)
            if (authorizationUiModeName == authorizationUiMode.name) {
                authorizationUiModeName = DiscordAuthorizationUiMode.Idle.name
                authorizationMessage = null
            }
        }
    }

    BackHandler(enabled = authorizationUiMode == DiscordAuthorizationUiMode.Waiting) {
        authorizationUiModeName = DiscordAuthorizationUiMode.Idle.name
        authorizationMessage = null
        authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
    }

    val (largeImageType, onLargeImageTypeChange) = rememberPreference(
        key = DiscordLargeImageTypeKey,
        defaultValue = "thumbnail"
    )
    val (largeImageCustomUrl, onLargeImageCustomUrlChange) = rememberPreference(
        key = DiscordLargeImageCustomUrlKey,
        defaultValue = ""
    )
    val (smallImageType, onSmallImageTypeChange) = rememberPreference(
        key = DiscordSmallImageTypeKey,
        defaultValue = "artist"
    )
    val (smallImageCustomUrl, onSmallImageCustomUrlChange) = rememberPreference(
        key = DiscordSmallImageCustomUrlKey,
        defaultValue = ""
    )
    var isRefreshing by remember { mutableStateOf(false) }

    val (activityStatusSelection, onActivityStatusSelectionChange) = rememberPreference(
        key = DiscordPresenceStatusKey,
        defaultValue = "online"
    )
    var activityStatusExpanded by remember { mutableStateOf(false) }

    val (intervalSelection, onIntervalSelectionChange) = rememberPreference(
        key = stringPreferencesKey("discordPresenceIntervalPreset"),
        defaultValue = "20s"
    )
    var intervalExpanded by remember { mutableStateOf(false) }

    val (platformSelection, onPlatformSelectionChange) = rememberPreference(
        key = DiscordActivityPlatformKey,
        defaultValue = "android"
    )
    var platformExpanded by remember { mutableStateOf(false) }

    val (nameSource, onNameSourceChange) = rememberEnumPreference(
        key = DiscordActivityNameKey,
        defaultValue = ActivitySource.APP
    )
    val (detailsSource, onDetailsSourceChange) = rememberEnumPreference(
        key = DiscordActivityDetailsKey,
        defaultValue = ActivitySource.SONG
    )
    val (stateSource, onStateSourceChange) = rememberEnumPreference(
        key = DiscordActivityStateKey,
        defaultValue = ActivitySource.ARTIST
    )

    val (button1Label, onButton1LabelChange) = rememberPreference(
        key = DiscordActivityButton1LabelKey,
        defaultValue = "Listen on YouTube Music"
    )
    val (button1Enabled, onButton1EnabledChange) = rememberPreference(
        key = DiscordActivityButton1EnabledKey,
        defaultValue = true
    )
    val (button2Label, onButton2LabelChange) = rememberPreference(
        key = DiscordActivityButton2LabelKey,
        defaultValue = "Go to ArchiveTune"
    )
    val (button2Enabled, onButton2EnabledChange) = rememberPreference(
        key = DiscordActivityButton2EnabledKey,
        defaultValue = true
    )

    val (activityType, onActivityTypeChange) = rememberPreference(
        key = DiscordActivityTypeKey,
        defaultValue = "LISTENING"
    )
    var showWhenPaused by rememberPreference(
        key = DiscordShowWhenPausedKey,
        defaultValue = false
    )
    var activityExpanded by remember { mutableStateOf(false) }

    val (largeTextSource, onLargeTextSourceChange) = rememberPreference(
        key = DiscordLargeTextSourceKey,
        defaultValue = "album"
    )
    val (largeTextCustom, onLargeTextCustomChange) = rememberPreference(
        key = DiscordLargeTextCustomKey,
        defaultValue = ""
    )
    var largeImageExpanded by remember { mutableStateOf(false) }
    var largeTextExpanded by remember { mutableStateOf(false) }
    var smallImageExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(largeImageType, smallImageType) {
        ArtworkStorage.removeBySongId(context, song?.song?.id ?: return@LaunchedEffect)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.discord_integration),
                        fontWeight = FontWeight.Bold,
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
                    var threeDotMenuExpanded by remember { mutableStateOf(false) }

                    IconButton(onClick = { threeDotMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                        )
                    }

                    DropdownMenu(
                        expanded = threeDotMenuExpanded,
                        onDismissRequest = { threeDotMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.experiment_settings)) },
                            onClick = {
                                threeDotMenuExpanded = false
                                navController.navigate("settings/discord/experimental")
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.experiment),
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                },
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
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreferenceGroup(title = stringResource(R.string.account)) {
                    item {
                        DiscordAccountGroupCard(
                            displayName = accountDisplayName,
                            username = discordUsername,
                            avatarUrl = discordAvatarUrl.takeIf { it.isNotBlank() },
                            isLoggedIn = isLoggedIn,
                            authorizationUiMode = authorizationUiMode,
                            authorizationMessage = authorizationMessage,
                            discordRpcEnabled = discordRPC,
                            onDiscordRpcEnabledChange = onDiscordRPCChange,
                            onPrimaryAction = {
                                if (isLoggedIn) {
                                    showLogoutConfirm = true
                                } else {
                                    launchAuthorization()
                                }
                            },
                            primaryActionEnabled = authorizationUiMode != DiscordAuthorizationUiMode.Waiting,
                        )
                    }
                }
            }

            item {
                DiscordSettingsPanel(title = stringResource(R.string.options)) {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.refresh)) },
                        description = stringResource(R.string.description_refresh),
                        icon = { Icon(painterResource(R.drawable.update), null) },
                        isEnabled = discordRPC && isLoggedIn,
                        trailingContent = {
                            if (isRefreshing) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                )
                            } else {
                                OutlinedButton(
                                    enabled = discordRPC && isLoggedIn,
                                    onClick = {
                                        coroutineScope.launch {
                                            isRefreshing = true
                                            val success = DiscordPresenceManager.updatePresence(
                                                context = context,
                                                token = discordToken,
                                                song = song,
                                                positionMs = playerConnection.player.currentPosition,
                                                isPaused = !playerConnection.player.isPlaying,
                                            )
                                            isRefreshing = false
                                            snackbarHostState.showSnackbar(
                                                message = if (success) {
                                                    context.getString(R.string.discord_refresh_success)
                                                } else {
                                                    context.getString(R.string.discord_refresh_failed)
                                                },
                                            )
                                        }
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                ) {
                                    Text(stringResource(R.string.refresh))
                                }
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                DiscordSettingsPanel(title = stringResource(R.string.discord_connection_settings)) {
                    ExposedDropdownMenuBox(
                        expanded = activityStatusExpanded,
                        onExpandedChange = { activityStatusExpanded = it },
                    ) {
                        TextField(
                            value = when (activityStatusSelection) {
                                "online" -> "Online"
                                "dnd" -> "Do Not Disturb"
                                "idle" -> "Idle"
                                "streaming" -> "Streaming"
                                else -> "Online"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.platform_status)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityStatusExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .padding(horizontal = 13.dp, vertical = 16.dp)
                                .pointerInput(Unit) { detectTapGestures { activityStatusExpanded = true } },
                            leadingIcon = { Icon(painterResource(R.drawable.desktop_windows), null) },
                        )
                        ExposedDropdownMenu(
                            expanded = activityStatusExpanded,
                            onDismissRequest = { activityStatusExpanded = false },
                        ) {
                            activityStatusOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            opt.replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase() else it.toString()
                                            },
                                        )
                                    },
                                    onClick = {
                                        onActivityStatusSelectionChange(opt)
                                        activityStatusExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = intervalExpanded,
                        onExpandedChange = { intervalExpanded = it },
                    ) {
                        TextField(
                            value = intervalSelection,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.update_interval)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .padding(horizontal = 13.dp, vertical = 16.dp)
                                .pointerInput(Unit) { detectTapGestures { intervalExpanded = true } },
                            leadingIcon = { Icon(painterResource(R.drawable.timer), null) },
                        )
                        ExposedDropdownMenu(
                            expanded = intervalExpanded,
                            onDismissRequest = { intervalExpanded = false },
                        ) {
                            intervalOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        onIntervalSelectionChange(opt)
                                        intervalExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    if (intervalSelection == "Custom") {
                        val (customValue, onCustomValueChange) = rememberPreference(
                            key = DiscordPresenceIntervalValueKey,
                            defaultValue = 30,
                        )
                        val (customUnit, onCustomUnitChange) = rememberPreference(
                            key = DiscordPresenceIntervalUnitKey,
                            defaultValue = "S",
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = customValue.toString(),
                                onValueChange = { text ->
                                    val number = text.toIntOrNull()
                                    if (number != null) {
                                        if (customUnit == "S" && number < 30) {
                                            onCustomValueChange(30)
                                        } else {
                                            onCustomValueChange(number)
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.discord_custom_interval_value)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                singleLine = true,
                            )

                            var unitExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = unitExpanded,
                                onExpandedChange = { unitExpanded = it },
                            ) {
                                TextField(
                                    value = when (customUnit) {
                                        "S" -> "Seconds"
                                        "M" -> "Minutes"
                                        "H" -> "Hours"
                                        else -> "Seconds"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.discord_custom_interval_unit)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .weight(1f)
                                        .pointerInput(Unit) { detectTapGestures { unitExpanded = true } },
                                )
                                ExposedDropdownMenu(
                                    expanded = unitExpanded,
                                    onDismissRequest = { unitExpanded = false },
                                ) {
                                    listOf("S" to "Seconds", "M" to "Minutes", "H" to "Hours").forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                if (code == "S" && customValue < 30) {
                                                    onCustomValueChange(30)
                                                }
                                                onCustomUnitChange(code)
                                                unitExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = platformExpanded,
                        onExpandedChange = { platformExpanded = it },
                    ) {
                        TextField(
                            value = platformSelection.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.platform_status)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .padding(horizontal = 13.dp, vertical = 16.dp)
                                .pointerInput(Unit) { detectTapGestures { platformExpanded = true } },
                            leadingIcon = { Icon(painterResource(R.drawable.desktop_windows), null) },
                        )
                        ExposedDropdownMenu(
                            expanded = platformExpanded,
                            onDismissRequest = { platformExpanded = false },
                        ) {
                            platformOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            opt.replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase() else it.toString()
                                            },
                                        )
                                    },
                                    onClick = {
                                        onPlatformSelectionChange(opt)
                                        platformExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                DiscordSettingsPanel(title = stringResource(R.string.discord_activity_content)) {
                    ActivitySourceDropdown(
                        title = stringResource(R.string.discord_activity_name),
                        iconRes = R.drawable.text_fields,
                        selected = nameSource,
                        onChange = onNameSourceChange,
                    )
                    ActivitySourceDropdown(
                        title = stringResource(R.string.discord_activity_details),
                        iconRes = R.drawable.text_fields,
                        selected = detailsSource,
                        onChange = onDetailsSourceChange,
                    )
                    ActivitySourceDropdown(
                        title = stringResource(R.string.discord_activity_state),
                        iconRes = R.drawable.text_fields,
                        selected = stateSource,
                        onChange = onStateSourceChange,
                    )

                    SwitchPreference(
                        title = { Text(stringResource(R.string.discord_show_when_paused)) },
                        description = stringResource(R.string.discord_show_when_paused_desc),
                        icon = { Icon(painterResource(R.drawable.ic_pause_white), null) },
                        checked = showWhenPaused,
                        onCheckedChange = { showWhenPaused = it },
                    )

                    ExposedDropdownMenuBox(
                        expanded = activityExpanded,
                        onExpandedChange = { activityExpanded = it },
                    ) {
                        TextField(
                            value = activityType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.discord_activity_type)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .pointerInput(Unit) { detectTapGestures { activityExpanded = true } }
                                .padding(horizontal = 13.dp, vertical = 16.dp),
                            leadingIcon = { Icon(painterResource(R.drawable.discord), null) },
                        )
                        ExposedDropdownMenu(
                            expanded = activityExpanded,
                            onDismissRequest = { activityExpanded = false },
                        ) {
                            activityOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        onActivityTypeChange(opt)
                                        activityExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                DiscordSettingsPanel(title = stringResource(R.string.discord_image_options)) {
                    ExposedDropdownMenuBox(
                        expanded = largeImageExpanded,
                        onExpandedChange = { largeImageExpanded = it },
                    ) {
                        TextField(
                            value = largeImageType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.large_image)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = largeImageExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .pointerInput(Unit) { detectTapGestures { largeImageExpanded = true } }
                                .padding(horizontal = 13.dp, vertical = 16.dp),
                            leadingIcon = { Icon(painterResource(R.drawable.image), null) },
                        )
                        ExposedDropdownMenu(
                            expanded = largeImageExpanded,
                            onDismissRequest = { largeImageExpanded = false },
                        ) {
                            imageOptions.forEach { opt ->
                                val display = when (opt) {
                                    "appicon" -> "App Icon"
                                    else -> opt.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase() else it.toString()
                                    }
                                }
                                DropdownMenuItem(
                                    text = { Text(display) },
                                    onClick = {
                                        onLargeImageTypeChange(opt)
                                        largeImageExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    if (largeImageType == "custom") {
                        EditablePreference(
                            title = stringResource(R.string.large_image_custom_url),
                            iconRes = R.drawable.link,
                            value = largeImageCustomUrl,
                            defaultValue = "",
                            onValueChange = onLargeImageCustomUrlChange,
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = largeTextExpanded,
                        onExpandedChange = { largeTextExpanded = it },
                    ) {
                        TextField(
                            value = largeTextSource,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.large_text)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = largeTextExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .pointerInput(Unit) { detectTapGestures { largeTextExpanded = true } }
                                .padding(horizontal = 13.dp, vertical = 16.dp),
                            leadingIcon = { Icon(painterResource(R.drawable.text_fields), null) },
                        )
                        ExposedDropdownMenu(
                            expanded = largeTextExpanded,
                            onDismissRequest = { largeTextExpanded = false },
                        ) {
                            largeTextOptions.forEach { opt ->
                                val display = when (opt) {
                                    "song" -> "Song name"
                                    "artist" -> "Artist name"
                                    "album" -> "Album name"
                                    "app" -> "App name"
                                    "custom" -> "Custom text"
                                    "dontshow" -> "Don't show"
                                    else -> opt
                                }
                                DropdownMenuItem(
                                    text = { Text(display) },
                                    onClick = {
                                        onLargeTextSourceChange(opt)
                                        largeTextExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    if (largeTextSource == "custom") {
                        EditablePreference(
                            title = stringResource(R.string.custom_large_text),
                            iconRes = R.drawable.text_fields,
                            value = largeTextCustom,
                            defaultValue = "",
                            onValueChange = onLargeTextCustomChange,
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = smallImageExpanded,
                        onExpandedChange = { smallImageExpanded = it },
                    ) {
                        TextField(
                            value = smallImageType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.small_image)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = smallImageExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .pointerInput(Unit) { detectTapGestures { smallImageExpanded = true } }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            leadingIcon = { Icon(painterResource(R.drawable.image), null) },
                        )
                        ExposedDropdownMenu(
                            expanded = smallImageExpanded,
                            onDismissRequest = { smallImageExpanded = false },
                        ) {
                            smallImageOptions.forEach { opt ->
                                val display = when (opt) {
                                    "appicon" -> "App Icon"
                                    "dontshow" -> "Don't show"
                                    else -> opt.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase() else it.toString()
                                    }
                                }
                                DropdownMenuItem(
                                    text = { Text(display) },
                                    onClick = {
                                        onSmallImageTypeChange(opt)
                                        smallImageExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    if (smallImageType == "custom") {
                        EditablePreference(
                            title = stringResource(R.string.small_image_custom_url),
                            iconRes = R.drawable.link,
                            value = smallImageCustomUrl,
                            defaultValue = "",
                            onValueChange = onSmallImageCustomUrlChange,
                        )
                    }
                }
            }

            item {
                RichPresence(
                    song = song,
                    currentPlaybackTimeMillis = playerConnection.player.currentPosition,
                    nameSource = nameSource,
                    detailsSource = detailsSource,
                    stateSource = stateSource,
                    activityType = activityType,
                    largeImageType = largeImageType,
                    largeImageCustomUrl = largeImageCustomUrl,
                    smallImageType = smallImageType,
                    smallImageCustomUrl = smallImageCustomUrl,
                    button1Enabled = button1Enabled,
                    button2Enabled = button2Enabled,
                    isPlaying = playerConnection.player.isPlaying,
                )
            }
        }

        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = { Text(stringResource(R.string.logout_confirm_title)) },
                text = { Text(stringResource(R.string.logout_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                DiscordOAuthRepository.clearSession(context)
                            }
                            DiscordPresenceManager.stop()
                            authorizationUiModeName = DiscordAuthorizationUiMode.Idle.name
                            authorizationMessage = null
                            authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
                            showLogoutConfirm = false
                        },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(stringResource(R.string.logout_confirm_yes))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutConfirm = false },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(stringResource(R.string.logout_confirm_no))
                    }
                },
            )
        }
    }
}

@Composable
private fun DiscordAccountGroupCard(
    displayName: String,
    username: String,
    avatarUrl: String?,
    isLoggedIn: Boolean,
    authorizationUiMode: DiscordAuthorizationUiMode,
    authorizationMessage: String?,
    discordRpcEnabled: Boolean,
    onDiscordRpcEnabledChange: (Boolean) -> Unit,
    onPrimaryAction: () -> Unit,
    primaryActionEnabled: Boolean,
) {
    val sessionSummary = when (authorizationUiMode) {
        DiscordAuthorizationUiMode.Waiting -> stringResource(R.string.discord_waiting_for_authorization)
        DiscordAuthorizationUiMode.Success -> authorizationMessage ?: stringResource(R.string.discord_authorization_success)
        DiscordAuthorizationUiMode.Failure -> authorizationMessage ?: stringResource(R.string.discord_authorization_failed)
        DiscordAuthorizationUiMode.Idle -> {
            if (isLoggedIn) {
                stringResource(R.string.discord_account_ready)
            } else {
                stringResource(R.string.discord_login_description)
            }
        }
    }

    val sessionContainerColor = when (authorizationUiMode) {
        DiscordAuthorizationUiMode.Waiting -> MaterialTheme.colorScheme.secondaryContainer
        DiscordAuthorizationUiMode.Success -> MaterialTheme.colorScheme.primaryContainer
        DiscordAuthorizationUiMode.Failure -> MaterialTheme.colorScheme.errorContainer
        DiscordAuthorizationUiMode.Idle -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val sessionContentColor = when (authorizationUiMode) {
        DiscordAuthorizationUiMode.Waiting -> MaterialTheme.colorScheme.onSecondaryContainer
        DiscordAuthorizationUiMode.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        DiscordAuthorizationUiMode.Failure -> MaterialTheme.colorScheme.onErrorContainer
        DiscordAuthorizationUiMode.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.discord),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (username.isNotBlank()) {
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Text(
                        text = if (isLoggedIn) {
                            stringResource(R.string.discord_account_connected)
                        } else {
                            stringResource(R.string.discord_authorization_idle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = authorizationUiMode != DiscordAuthorizationUiMode.Idle || isLoggedIn,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = sessionContainerColor,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (authorizationUiMode == DiscordAuthorizationUiMode.Waiting) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = sessionContentColor,
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    when (authorizationUiMode) {
                                        DiscordAuthorizationUiMode.Success -> R.drawable.check
                                        DiscordAuthorizationUiMode.Failure -> R.drawable.close
                                        DiscordAuthorizationUiMode.Idle -> R.drawable.discord
                                        DiscordAuthorizationUiMode.Waiting -> R.drawable.discord
                                    },
                                ),
                                contentDescription = null,
                                tint = sessionContentColor,
                            )
                        }

                        Text(
                            text = sessionSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = sessionContentColor,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = if (discordRpcEnabled && isLoggedIn) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.status),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.enable_discord_rpc),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.discord_rpc_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Switch(
                        checked = discordRpcEnabled,
                        onCheckedChange = onDiscordRpcEnabledChange,
                        enabled = isLoggedIn,
                    )
                }
            }

            if (isLoggedIn) {
                OutlinedButton(
                    onClick = onPrimaryAction,
                    enabled = primaryActionEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.action_logout))
                }
            } else {
                Button(
                    onClick = onPrimaryAction,
                    enabled = primaryActionEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.discord_open_authorization))
                }
            }
        }
    }
}

@Composable
private fun DiscordSettingsPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                Column(content = content)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySourceDropdown(
    title: String,
    iconRes: Int,
    selected: ActivitySource,
    onChange: (ActivitySource) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
    ) {
        TextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(painterResource(iconRes), null) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ActivitySource.values().forEach { source ->
                DropdownMenuItem(
                    text = { Text(source.name) },
                    onClick = {
                        onChange(source)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun EditablePreference(
    title: String,
    iconRes: Int,
    value: String,
    defaultValue: String,
    onValueChange: (String) -> Unit,
    description: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    PreferenceEntry(
        title = { Text(title) },
        description = description ?: if (value.isEmpty()) defaultValue else value,
        icon = { Icon(painterResource(iconRes), null) },
        trailingContent = {
            TextButton(onClick = { showDialog = true }, shapes = ButtonDefaults.shapes()) { Text("Edit") }
        }
    )
    if (showDialog) {
        var text by remember { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(if (text.isBlank()) "" else text)
                    showDialog = false
                }, shapes = ButtonDefaults.shapes()) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
            },
            title = { Text("Edit $title") },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(defaultValue) },
                    singleLine = true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        )
    }
}

@Composable
fun RichPresence(
    song: Song?,
    currentPlaybackTimeMillis: Long = 0L,
    nameSource: ActivitySource = ActivitySource.APP,
    detailsSource: ActivitySource = ActivitySource.SONG,
    stateSource: ActivitySource = ActivitySource.ARTIST,
    activityType: String = "LISTENING",
    largeImageType: String = "thumbnail",
    largeImageCustomUrl: String = "",
    smallImageType: String = "artist",
    smallImageCustomUrl: String = "",
    button1Enabled: Boolean = true,
    button2Enabled: Boolean = true,
    isPlaying: Boolean = false,
) {
    val context = LocalContext.current

    fun resolveUrl(source: String, song: Song?, custom: String): String? {
    return when (source.lowercase()) {
        "songurl" -> song?.id?.let { "https://music.youtube.com/watch?v=$it" }
        "artisturl" -> song?.artists?.firstOrNull()?.id?.let { "https://music.youtube.com/channel/$it" }
        "albumurl" -> song?.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
        "custom" -> if (custom.isNotBlank()) custom else null
        else -> null
    }
   }

   val (button1Label) = rememberPreference(DiscordActivityButton1LabelKey, "Listen on YouTube Music")
   val (button1Enabled) = rememberPreference(DiscordActivityButton1EnabledKey, true)

   val (button2Label) = rememberPreference(DiscordActivityButton2LabelKey, "Go to ArchiveTune")
   val (button2Enabled) = rememberPreference(DiscordActivityButton2EnabledKey, true)

// Button URL sources + custom
   val (button1UrlSource) = rememberPreference(DiscordActivityButton1UrlSourceKey, "songurl")
   val (button1CustomUrl) = rememberPreference(DiscordActivityButton1CustomUrlKey, "")

   val (button2UrlSource) = rememberPreference(DiscordActivityButton2UrlSourceKey, "custom")
   val (button2CustomUrl) = rememberPreference(DiscordActivityButton2CustomUrlKey, "https://github.com/koiverse/ArchiveTune")

// Large text source + custom
   val (largeTextSource) = rememberPreference(DiscordLargeTextSourceKey, "album")
   val (largeTextCustom) = rememberPreference(DiscordLargeTextCustomKey, "")

    val previewLargeText = when (largeTextSource) {
    "song" -> song?.song?.title ?: "Song name"
    "artist" -> song?.artists?.firstOrNull()?.name ?: "Artist"
    "album" -> song?.song?.albumName ?: song?.album?.title ?: "Album"
    "app" -> stringResource(R.string.app_name)
    "custom" -> largeTextCustom.ifBlank { "Custom text" }
    "dontshow" -> null
    else -> song?.song?.albumName ?: song?.album?.title
    }
    val resolvedButton1Url = resolveUrl(button1UrlSource, song, button1CustomUrl)
    val resolvedButton2Url = resolveUrl(button2UrlSource, song, button2CustomUrl)
    val activityVerb = when (activityType.uppercase()) {
    "PLAYING" -> "Playing"
    "LISTENING" -> "Listening to"
    "WATCHING" -> "Watching"
    "STREAMING" -> "Streaming"
    "COMPETING" -> "Competing in"
    else -> activityType.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
       }
    }

    val previewTitle = when (nameSource) {
    ActivitySource.ARTIST -> "$activityVerb ${song?.artists?.firstOrNull()?.name ?: "Artist"}"
    ActivitySource.ALBUM -> "$activityVerb ${song?.album?.title ?: song?.song?.albumName ?: "Album"}"
    ActivitySource.SONG -> "$activityVerb ${song?.song?.title ?: "Song"}"
    ActivitySource.APP -> "$activityVerb ArchiveTune"
   }


    PreferenceEntry(
        title = {
            Text(
            text = stringResource(R.string.preview),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
            )
        },
        content = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = previewTitle,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(108.dp)) {
                            AsyncImage(
                                model = when (largeImageType) {
                                    "thumbnail" -> song?.song?.thumbnailUrl
                                    "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl
                                    "appicon" -> "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/icon.png"
                                    "custom" -> largeImageCustomUrl.ifBlank { song?.song?.thumbnailUrl }
                                    else -> song?.song?.thumbnailUrl
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .align(Alignment.TopStart)
                                    .run {
                                        if (song == null) border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            RoundedCornerShape(12.dp)
                                        ) else this
                                    },
                            )
                            val songThumb = song?.song?.thumbnailUrl
                            val artistThumb = song?.artists?.firstOrNull()?.thumbnailUrl

                            // Fix: Don't fallback from artist to song thumbnail - each source should be independent
                            val smallModel = when (smallImageType.lowercase()) {
                                "thumbnail" -> songThumb  // Only show song thumbnail, no fallback
                                "artist" -> artistThumb   // Only show artist thumbnail, no fallback to song
                                "appicon" -> "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/icon.png"
                                "custom" -> smallImageCustomUrl.takeIf { it.isNotBlank() } ?: songThumb  // Custom with fallback to song only
                                "dontshow", "none" -> null
                                else -> artistThumb  // Default to artist without fallback
                            }
                            smallModel?.let {
                                Box(
                                    modifier = Modifier
                                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                        .padding(2.dp)
                                        .align(Alignment.BottomEnd),
                                ) {
                                    AsyncImage(
                                        model = it,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(CircleShape),
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        ) {
                            Text(
                                text = song?.song?.title ?: "Song Title",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            // Compute a preview for the "state" line according to the selected stateSource
                            val previewState = when (stateSource) {
                                ActivitySource.ARTIST -> song?.artists?.joinToString { it.name } ?: "Artist"
                                ActivitySource.ALBUM -> song?.song?.albumName ?: song?.album?.title ?: song?.song?.title ?: "Unknown Album"
                                ActivitySource.SONG -> song?.song?.title ?: "Song"
                                ActivitySource.APP -> stringResource(R.string.app_name)
                            }

                            Text(
                                text = previewState,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            previewLargeText?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (song != null) {
                                SongProgressBar(
                                    currentTimeMillis = currentPlaybackTimeMillis,
                                    durationMillis = song.song.duration * 1000L,
                                    isPlaying = isPlaying,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = button1Enabled && button1Label.isNotBlank()) {
                        Button(
                            enabled = !resolvedButton1Url.isNullOrBlank(),
                            onClick = {
                              resolvedButton1Url?.let {
                              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                         }
                     },
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(button1Label.ifBlank { "Listen on YouTube Music" })
                        }
                    }

                    AnimatedVisibility(visible = button2Enabled && button2Label.isNotBlank()) {
                        Button(
                            enabled = !resolvedButton2Url.isNullOrBlank(),
                            onClick = {
                              resolvedButton2Url?.let {
                              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                        }
                     },
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(button2Label.ifBlank { "View Album" })
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SongProgressBar(
    currentTimeMillis: Long,
    durationMillis: Long,
    isPlaying: Boolean = false
) {
    var displayedTime by remember { mutableStateOf(currentTimeMillis) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(500)
                displayedTime += 500
                if (displayedTime >= durationMillis) {
                    displayedTime = durationMillis
                    break
                }
            }
        }
    }

    val progress = if (durationMillis > 0) {
        displayedTime.toFloat() / durationMillis
    } else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearWavyProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = makeTimeString(displayedTime),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = 12.sp
            )
            Text(
                text = makeTimeString(durationMillis),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontSize = 12.sp
            )
        }
    }
}

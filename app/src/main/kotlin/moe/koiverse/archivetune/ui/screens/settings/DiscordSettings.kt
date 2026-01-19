package moe.koiverse.archivetune.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
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
import com.my.kizzy.rpc.KizzyRPC
import timber.log.Timber
import moe.koiverse.archivetune.utils.DiscordRPC
import moe.koiverse.archivetune.utils.getPresenceIntervalMillis
import kotlinx.coroutines.*
import moe.koiverse.archivetune.utils.ArtworkStorage

enum class ActivitySource { ARTIST, ALBUM, SONG, APP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val song by playerConnection.currentSong.collectAsState(null)
    val playbackState by playerConnection.playbackState.collectAsState()
    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    // Track last RPC timestamps to detect when RPC progress bar reaches the end.
    // These are now owned by DiscordPresenceManager; read their current values here.
    val lastRpcStartTime = DiscordPresenceManager.lastRpcStartTime
    val lastRpcEndTime = DiscordPresenceManager.lastRpcEndTime
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, false)

    LaunchedEffect(discordToken) {
        val token = discordToken
        if (token.isNotEmpty()) {
            // Run the network call inside this LaunchedEffect coroutine so it is
            // cancelled automatically if the composable leaves the composition.
            try {
                withContext(Dispatchers.IO) {
                    // KizzyRPC.getUserInfo may throw network/socket exceptions when the
                    // app is backgrounded or network drops; catch them to avoid crashing.
                    KizzyRPC.getUserInfo(token)
                }.onSuccess {
                    discordUsername = it.username
                    discordName = it.name
                }
            } catch (e: Exception) {
                // Log and ignore network errors (e.g. SocketException on resume).
                Timber.tag("DiscordSettings").w(e, "getUserInfo failed")
            }
        }
    }

    val (discordRPC, onDiscordRPCChange) = rememberPreference(
        key = EnableDiscordRPCKey,
        defaultValue = true
    )

    LaunchedEffect(discordToken, discordRPC) {
        if (discordRPC && discordToken.isNotBlank()) {
            Timber.tag("DiscordSettings").d("RPC enabled with token, MusicService will handle start")
            // DiscordPresenceManager.start(
            //     context = context,
            //     token = discordToken,
            //     songProvider = { song },
            //     positionProvider = { playerConnection.player.currentPosition },
            //     isPausedProvider = {
            //         val isPlaying = playerConnection.player.playWhenReady &&
            //                 playerConnection.player.playbackState == STATE_READY
            //         !isPlaying
            //     },
            //     intervalProvider = { getPresenceIntervalMillis(context) }
            // )
        } else {
            // user disabled RPC or cleared token -> ensure manager is stopped
            Timber.tag("DiscordSettings").d("RPC disabled or no token, stopping manager")
            DiscordPresenceManager.stop()
        }
    }

    val isLoggedIn = remember(discordToken) { discordToken.isNotEmpty() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            Modifier
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
        ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

    // Developer debug moved to DebugSettings (Settings -> Misc)

        AnimatedVisibility(visible = !infoDismissed) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text = stringResource(R.string.discord_information),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                TextButton(
                    onClick = { infoDismissed = true },
                    modifier = Modifier.align(Alignment.End).padding(16.dp),
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }

        Text(
            text = stringResource(R.string.account),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        )

    var showLogoutConfirm by remember { mutableStateOf(false) }

    PreferenceEntry(
            title = {
                Text(
                    text = if (isLoggedIn) discordName else stringResource(R.string.not_logged_in),
                    modifier = Modifier.alpha(if (isLoggedIn) 1f else 0.5f),
                )
            },
            description = if (discordUsername.isNotEmpty()) "@$discordUsername" else null,
            icon = { Icon(painterResource(R.drawable.discord), null) },
            trailingContent = {
                if (isLoggedIn) {
                        OutlinedButton(onClick = { showLogoutConfirm = true }) { Text(stringResource(R.string.action_logout)) }
                    } else {
                    OutlinedButton(onClick = {
                        navController.navigate("settings/discord/login")
                    }) { Text(stringResource(R.string.action_login)) }
                }
            },
        )

            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    title = { Text(stringResource(R.string.logout_confirm_title)) },
                    text = { Text(stringResource(R.string.logout_confirm_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            discordName = ""
                            discordToken = ""
                            discordUsername = ""
                            showLogoutConfirm = false
                        }) { Text(stringResource(R.string.logout_confirm_yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) { Text(stringResource(R.string.logout_confirm_no)) }
                    }
                )
            }

        Text(
            text = stringResource(R.string.options),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_discord_rpc)) },
            checked = discordRPC,
            onCheckedChange = onDiscordRPCChange,
            isEnabled = isLoggedIn,
        )

        // Add a refresh action to manually re-update Discord RPC
        // PreferenceEntry(
        //     title = { Text(stringResource(R.string.refresh)) },
        //     description = stringResource(R.string.description_refresh),
        //     icon = { Icon(painterResource(R.drawable.refresh), null) },
        //     trailingContent = {
        //         IconButton(onClick = {
        //             // trigger update in background
        //             coroutineScope.launch(Dispatchers.IO) {
        //                 val token = discordToken
        //                 if (token.isNotBlank()) {
        //                     try {
        //                         val rpc = DiscordRPC(context, token)
        //                         song?.let { rpc.updateSong(it, position) }
        //                     } catch (_: Exception) {
        //                         // ignore
        //                     }
        //                 }
        //             }
        //         }) {
        //             Icon(painterResource(R.drawable.update), contentDescription = null)
        //         }
        //     }
        // )
        
        // Discord presence image preferences (hoisted so refresh action can read them)
        val imageOptions = listOf("thumbnail", "artist", "appicon", "custom")
        val smallImageOptions = listOf("thumbnail", "artist", "appicon", "custom", "dontshow")

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

        // When large/small image selection changes, clear any stored artwork for the current song
        LaunchedEffect(largeImageType, smallImageType) {
            ArtworkStorage.removeBySongId(context, song?.song?.id ?: return@LaunchedEffect)
        }

        var isRefreshing by remember { mutableStateOf(false) }

        PreferenceEntry(
        title = { Text(stringResource(R.string.refresh)) },
        description = stringResource(R.string.description_refresh),
        icon = { Icon(painterResource(R.drawable.update), null) },
        isEnabled = discordRPC,
        trailingContent = {
           if (isRefreshing) {
                CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp
            )
        } else {
            OutlinedButton(
                enabled = discordRPC,
                onClick = {
                    coroutineScope.launch {
                       isRefreshing = true
                       val start = System.currentTimeMillis()

                       // Resolve large image from current Compose state (respect user selection)
                       val success = DiscordPresenceManager.updatePresence(
                           context = context,
                           token = discordToken,
                           song = song,
                           positionMs = playerConnection.player.currentPosition,
                           isPaused = !playerConnection.player.isPlaying,
                       )
                       isRefreshing = false
                        // Show snackbar on main thread
                        withContext(Dispatchers.Main) {
                            if (success) {
                                snackbarHostState.showSnackbar("Refreshed!")
                            } else {
                                snackbarHostState.showSnackbar("Refresh failed")
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.refresh))
            }
        }
    }
)

        // Status discord
        val activityStatus = listOf("online", "dnd", "idle", "streaming")
        val (activityStatusSelection, onActivityStatusSelectionChange) = rememberPreference(
            key = DiscordPresenceStatusKey,
            defaultValue = "online"
        )

        var activityStatusExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = activityStatusExpanded, onExpandedChange = { activityStatusExpanded = it }) {
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
                label = { Text(stringResource(R.string.activity_status)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityStatusExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(horizontal = 13.dp, vertical = 16.dp)
                    .pointerInput(Unit) { detectTapGestures { activityStatusExpanded = true } },
                leadingIcon = { Icon(painterResource(R.drawable.status), null) }
            )
            ExposedDropdownMenu(expanded = activityStatusExpanded, onDismissRequest = { activityStatusExpanded = false }) {
                activityStatus.forEach { opt ->
                    val display = when (opt) {
                        "online" -> "Online"
                        "dnd" -> "Do Not Disturb"
                        "idle" -> "Idle"
                        "streaming" -> "Streaming"
                        else -> opt
                    }
                    DropdownMenuItem(text = { Text(display) }, onClick = {
                        onActivityStatusSelectionChange(opt)
                        activityStatusExpanded = false
                    })
                }
            }
        }

        // Platform selector (client platform displayed on Discord)
        val platformOptions = listOf("android", "desktop", "web")
        val (platformSelection, onPlatformSelectionChange) = rememberPreference(
            key = DiscordActivityPlatformKey,
            defaultValue = "desktop"
        )

        var platformExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = platformExpanded, onExpandedChange = { platformExpanded = it }) {
            TextField(
                value = platformSelection.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.platform_status)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(horizontal = 13.dp, vertical = 16.dp)
                    .pointerInput(Unit) { detectTapGestures { platformExpanded = true } },
                leadingIcon = { Icon(painterResource(R.drawable.desktop_windows), null) }
            )
            ExposedDropdownMenu(expanded = platformExpanded, onDismissRequest = { platformExpanded = false }) {
                platformOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }, onClick = {
                        onPlatformSelectionChange(opt)
                        platformExpanded = false
                    })
                }
            }
        }

        // Interval selection
       val intervalOptions = listOf("20s", "50s", "1m", "5m", "Custom", "Disabled")
       val (intervalSelection, onIntervalSelectionChange) = rememberPreference(
           key = stringPreferencesKey("discordPresenceIntervalPreset"),
           defaultValue = "20s"
        )

        var intervalExpanded by remember { mutableStateOf(false) }

ExposedDropdownMenuBox(expanded = intervalExpanded, onExpandedChange = { intervalExpanded = it }) {
    TextField(
        value = intervalSelection,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.update_interval)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .padding(horizontal = 13.dp, vertical = 16.dp)
            .pointerInput(Unit) { detectTapGestures { intervalExpanded = true } },
        leadingIcon = { Icon(painterResource(R.drawable.timer), null) }
    )
    ExposedDropdownMenu(expanded = intervalExpanded, onDismissRequest = { intervalExpanded = false }) {
        intervalOptions.forEach { opt ->
            DropdownMenuItem(text = { Text(opt) }, onClick = {
                onIntervalSelectionChange(opt)
                intervalExpanded = false
            })
        }
    }
}

if (intervalSelection == "Custom") {
    val (customValue, onCustomValueChange) = rememberPreference(
        key = DiscordPresenceIntervalValueKey,
        defaultValue = 30
    )
    val (customUnit, onCustomUnitChange) = rememberPreference(
        key = DiscordPresenceIntervalUnitKey,
        defaultValue = "S"
    )

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = customValue.toString(),
            onValueChange = { text ->
                val number = text.toIntOrNull()
                if (number != null) {
                    // Validation: if seconds, enforce >= 30
                    if (customUnit == "S" && number < 30) {
                        onCustomValueChange(30)
                    } else {
                        onCustomValueChange(number)
                    }
                }
            },
            label = { Text("Value") },
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            singleLine = true
        )

        var unitExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
            TextField(
                value = when (customUnit) {
                    "S" -> "Seconds"
                    "M" -> "Minutes"
                    "H" -> "Hours"
                    else -> "Seconds"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .weight(1f)
                    .pointerInput(Unit) { detectTapGestures { unitExpanded = true } }
            )
            ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                listOf("S" to "Seconds", "M" to "Minutes", "H" to "Hours").forEach { (code, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        // Enforce minimum when switching to seconds
                        if (code == "S" && customValue < 30) {
                            onCustomValueChange(30)
                        }
                        onCustomUnitChange(code)
                        unitExpanded = false
                    })
                }
            }
        }
    }
}

        // PREVIEW HEADING
        Text(
            text = stringResource(R.string.preview),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val (nameSource, onNameSourceChange) = rememberEnumPreference(
            key = DiscordActivityNameKey, defaultValue = ActivitySource.APP
        )
        val (detailsSource, onDetailsSourceChange) = rememberEnumPreference(
            key = DiscordActivityDetailsKey, defaultValue = ActivitySource.SONG
        )
        val (stateSource, onStateSourceChange) = rememberEnumPreference(
            key = DiscordActivityStateKey, defaultValue = ActivitySource.ARTIST
        )

        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_name),
            iconRes = R.drawable.text_fields,
            selected = nameSource,
            onChange = onNameSourceChange
        )
        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_details),
            iconRes = R.drawable.text_fields,
            selected = detailsSource,
            onChange = onDetailsSourceChange
        )
        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_state),
            iconRes = R.drawable.text_fields,
            selected = stateSource,
            onChange = onStateSourceChange
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


    // Activity type selection
        val (activityType, onActivityTypeChange) = rememberPreference(
            key = DiscordActivityTypeKey,
            defaultValue = "LISTENING"
        )
        val activityOptions = listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "COMPETING")

        var showWhenPaused by rememberPreference(
        key = DiscordShowWhenPausedKey,
        defaultValue = false
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.discord_show_when_paused)) },
            description = stringResource(R.string.discord_show_when_paused_desc),
            icon = { Icon(painterResource(R.drawable.ic_pause_white), null) },
            checked = showWhenPaused,
            onCheckedChange = { showWhenPaused = it }
        )

        // Activity type selector - OutlinedTextField anchored dropdown
        var activityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
            TextField(
                value = activityType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.discord_activity_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .pointerInput(Unit) { detectTapGestures { activityExpanded = true } }
                    .padding(horizontal = 13.dp, vertical = 16.dp),
                leadingIcon = { Icon(painterResource(R.drawable.discord), null) }
            )
            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                activityOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        onActivityTypeChange(opt)
                        activityExpanded = false
                    })
                }
            }
        }

    // Group button related preferences
    Text(
        text = stringResource(R.string.discord_image_options),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )

        val largeTextOptions = listOf("song", "artist", "album", "app", "custom", "dontshow")

        val (largeTextSource, onLargeTextSourceChange) = rememberPreference(
            key = DiscordLargeTextSourceKey,
            defaultValue = "album"
     )
        val (largeTextCustom, onLargeTextCustomChange) = rememberPreference(
            key = DiscordLargeTextCustomKey,
            defaultValue = ""
     )

var largeImageExpanded by remember { mutableStateOf(false) }
ExposedDropdownMenuBox(expanded = largeImageExpanded, onExpandedChange = { largeImageExpanded = it }) {
    TextField(
        value = largeImageType,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.large_image)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = largeImageExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .pointerInput(Unit) { detectTapGestures { largeImageExpanded = true } }
            .padding(horizontal = 13.dp, vertical = 16.dp),
        leadingIcon = { Icon(painterResource(R.drawable.image), null) }
    )
    ExposedDropdownMenu(expanded = largeImageExpanded, onDismissRequest = { largeImageExpanded = false }) {
        imageOptions.forEach { opt ->
            val display = when (opt) {
                "appicon" -> "App Icon"
                else -> opt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            DropdownMenuItem(text = { Text(display) }, onClick = {
                onLargeImageTypeChange(opt)
                largeImageExpanded = false
            })
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

var largeTextExpanded by remember { mutableStateOf(false) }
ExposedDropdownMenuBox(expanded = largeTextExpanded, onExpandedChange = { largeTextExpanded = it }) {
    TextField(
        value = largeTextSource,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.large_text)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = largeTextExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .pointerInput(Unit) { detectTapGestures { largeTextExpanded = true } }
            .padding(horizontal = 13.dp, vertical = 16.dp),
        leadingIcon = { Icon(painterResource(R.drawable.text_fields), null) }
    )
    ExposedDropdownMenu(expanded = largeTextExpanded, onDismissRequest = { largeTextExpanded = false }) {
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
                }
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
        onValueChange = onLargeTextCustomChange
    )
}

var smallImageExpanded by remember { mutableStateOf(false) }
ExposedDropdownMenuBox(expanded = smallImageExpanded, onExpandedChange = { smallImageExpanded = it }) {
    TextField(
        value = smallImageType,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.small_image)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = smallImageExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
            .pointerInput(Unit) { detectTapGestures { smallImageExpanded = true } }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        leadingIcon = { Icon(painterResource(R.drawable.image), null) }
    )
    ExposedDropdownMenu(expanded = smallImageExpanded, onDismissRequest = { smallImageExpanded = false }) {
        smallImageOptions.forEach { opt ->
            val display = when (opt) {
                "appicon" -> "App Icon"
                "dontshow" -> "Don't show"
                else -> opt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            DropdownMenuItem(text = { Text(display) }, onClick = {
                onSmallImageTypeChange(opt)
                smallImageExpanded = false
            })
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

    // Compute whether the player is currently playing so the preview progress can run.
    val playerIsPlayingForPreview = playerConnection.player.playWhenReady && playbackState == STATE_READY

    RichPresence(
        song,
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
        isPlaying = playerConnection.player.isPlaying
    )
}

    TopAppBar(
        title = { Text(stringResource(R.string.discord_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        actions = {
            var threeDotMenuExpanded by remember { mutableStateOf(false) }

            IconButton(onClick = { threeDotMenuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = threeDotMenuExpanded,
                onDismissRequest = { threeDotMenuExpanded = false }
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
                            contentDescription = null
                        )
                    }
                )
            }
        }
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
            TextButton(onClick = { showDialog = true }) { Text("Edit") }
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
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
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
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
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

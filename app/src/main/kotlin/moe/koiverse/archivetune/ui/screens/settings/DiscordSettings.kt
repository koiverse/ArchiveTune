package moe.koiverse.archivetune.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
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
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.makeTimeString
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import com.my.kizzy.rpc.KizzyRPC
import moe.koiverse.archivetune.utils.DiscordRPC
import kotlinx.coroutines.*

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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, false)

    LaunchedEffect(discordToken) {
        val token = discordToken
        if (token.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                KizzyRPC.getUserInfo(token).onSuccess {
                    discordUsername = it.username
                    discordName = it.name
                }
            }
        }
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
            }
        }
    }

    val (discordRPC, onDiscordRPCChange) = rememberPreference(
        key = EnableDiscordRPCKey,
        defaultValue = true
    )

    val isLoggedIn = remember(discordToken) { discordToken.isNotEmpty() }

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

        PreferenceGroupTitle(title = stringResource(R.string.account))

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
                    OutlinedButton(onClick = {
                        discordName = ""
                        discordToken = ""
                        discordUsername = ""
                    }) { Text(stringResource(R.string.action_logout)) }
                } else {
                    OutlinedButton(onClick = {
                        navController.navigate("settings/discord/login")
                    }) { Text(stringResource(R.string.action_login)) }
                }
            },
        )

        PreferenceGroupTitle(title = stringResource(R.string.options))

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
        
        PreferenceEntry(
            title = { Text(stringResource(R.string.refresh)) },
            description = stringResource(R.string.description_refresh),
            icon = { Icon(painterResource(R.drawable.update), null) },
            trailingContent = {
                IconButton(onClick = {
                    // trigger update in background
                    coroutineScope.launch(Dispatchers.IO) {
                        val token = discordToken
                        if (token.isNotBlank()) {
                            try {
                                val rpc = DiscordRPC(context, token)
                                song?.let { rpc.updateSong(it, position) }
                                coroutineScope.launch(Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, "Discord RPC refreshed!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                // ignore
                            }
                        }
                    }
                }) {
                    Icon(painterResource(R.drawable.update), contentDescription = null)
                }
            }
        )


        PreferenceGroupTitle(title = stringResource(R.string.preview))

        val (nameSource, onNameSourceChange) = rememberEnumPreference(
            key = DiscordActivityNameKey, defaultValue = ActivitySource.APP
        )
        val (detailsSource, onDetailsSourceChange) = rememberEnumPreference(
            key = DiscordActivityDetailsKey, defaultValue = ActivitySource.SONG
        )
        val (stateSource, onStateSourceChange) = rememberEnumPreference(
            key = DiscordActivityStateKey, defaultValue = ActivitySource.ARTIST
        )
        val (buttonUrlSource, onButtonUrlSourceChange) = rememberEnumPreference(
            key = DiscordActivityButtonUrlKey, defaultValue = ActivitySource.SONG
        )

        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_name),
            iconRes = R.drawable.discord,
            selected = nameSource,
            onChange = onNameSourceChange
        )
        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_details),
            iconRes = R.drawable.info,
            selected = detailsSource,
            onChange = onDetailsSourceChange
        )
        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_state),
            iconRes = R.drawable.info,
            selected = stateSource,
            onChange = onStateSourceChange
        )
        ActivitySourceDropdown(
            title = stringResource(R.string.discord_activity_button_url),
            iconRes = R.drawable.link,
            selected = buttonUrlSource,
            onChange = onButtonUrlSourceChange
        )

        val (button1Label, onButton1LabelChange) = rememberPreference(
            key = DiscordActivityButton1LabelKey,
            defaultValue = "Listen on YouTube Music"
        )
        val (button1Url, onButton1UrlChange) = rememberPreference(
            key = DiscordActivityButton1UrlKey,
            defaultValue = ""
        )
        val (button1Enabled, onButton1EnabledChange) = rememberPreference(
            key = DiscordActivityButton1EnabledKey,
            defaultValue = true
        )
        val (button2Label, onButton2LabelChange) = rememberPreference(
            key = DiscordActivityButton2LabelKey,
            defaultValue = "View Album"
        )
        val (button2Url, onButton2UrlChange) = rememberPreference(
            key = DiscordActivityButton2UrlKey,
            defaultValue = ""
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

        PreferenceGroupTitle(title = stringResource(R.string.discord_button_options))

        EditablePreference(
            title = stringResource(R.string.discord_activity_button1_label),
            iconRes = R.drawable.play,
            value = button1Label,
            defaultValue = "Listen on YouTube Music",
            onValueChange = onButton1LabelChange
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.show_button)) },
            description = stringResource(R.string.show_button1_description),
            icon = { Icon(painterResource(R.drawable.play), null) },
            trailingContent = {
                Switch(checked = button1Enabled, onCheckedChange = onButton1EnabledChange)
            }
        )
        if (button1Enabled) {
            EditablePreference(
                title = stringResource(R.string.discord_activity_button1_url),
                iconRes = R.drawable.link,
                value = button1Url,
                defaultValue = "",
                onValueChange = onButton1UrlChange
            )
        }
        EditablePreference(
            title = stringResource(R.string.discord_activity_button2_label),
            iconRes = R.drawable.info,
            value = button2Label,
            defaultValue = "View Album",
            onValueChange = onButton2LabelChange
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.show_button)) },
            description = stringResource(R.string.show_button2_description),
            icon = { Icon(painterResource(R.drawable.info), null) },
            trailingContent = {
                Switch(checked = button2Enabled, onCheckedChange = onButton2EnabledChange)
            }
        )
        if (button2Enabled) {
            EditablePreference(
                title = stringResource(R.string.discord_activity_button2_url),
                iconRes = R.drawable.link,
                value = button2Url,
                defaultValue = "",
                onValueChange = onButton2UrlChange
            )
        }

        // Activity type selector - OutlinedTextField anchored dropdown
        var activityExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = activityExpanded, onExpandedChange = { activityExpanded = it }) {
            OutlinedTextField(
                value = activityType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.discord_activity_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                modifier = Modifier.fillMaxWidth().clickable { activityExpanded = true },
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
    PreferenceGroupTitle(title = stringResource(R.string.discord_image_options))

    // Discord presence image selection
        val imageOptions = listOf("thumbnail", "artist", "appicon", "custom")
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

        var largeImageExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = largeImageExpanded, onExpandedChange = { largeImageExpanded = it }) {
            OutlinedTextField(
                value = largeImageType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Large image") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = largeImageExpanded) },
                modifier = Modifier.fillMaxWidth().clickable { largeImageExpanded = true },
                leadingIcon = { Icon(painterResource(R.drawable.info), null) }
            )
            ExposedDropdownMenu(expanded = largeImageExpanded, onDismissRequest = { largeImageExpanded = false }) {
                imageOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        onLargeImageTypeChange(opt)
                        largeImageExpanded = false
                    })
                }
            }
        }
        if (largeImageType == "custom") {
            EditablePreference(
                title = "Large image custom URL",
                iconRes = R.drawable.link,
                value = largeImageCustomUrl,
                defaultValue = "",
                onValueChange = onLargeImageCustomUrlChange
            )
        }

        var smallImageExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = smallImageExpanded, onExpandedChange = { smallImageExpanded = it }) {
            OutlinedTextField(
                value = smallImageType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Small image") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = smallImageExpanded) },
                modifier = Modifier.fillMaxWidth().clickable { smallImageExpanded = true },
                leadingIcon = { Icon(painterResource(R.drawable.info), null) }
            )
            ExposedDropdownMenu(expanded = smallImageExpanded, onDismissRequest = { smallImageExpanded = false }) {
                imageOptions.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        onSmallImageTypeChange(opt)
                        smallImageExpanded = false
                    })
                }
            }
        }
        if (smallImageType == "custom") {
            EditablePreference(
                title = "Small image custom URL",
                iconRes = R.drawable.link,
                value = smallImageCustomUrl,
                defaultValue = "",
                onValueChange = onSmallImageCustomUrlChange
            )
        }

        RichPresence(
            song,
            position,
            nameSource,
            detailsSource,
            stateSource,
            buttonUrlSource,
            activityType,
            largeImageType,
            largeImageCustomUrl,
            smallImageType,
            smallImageCustomUrl
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.discord_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) { Icon(painterResource(R.drawable.arrow_back), contentDescription = null) }
        }
    )
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
    val options = ActivitySource.values().map { it.name }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            leadingIcon = { Icon(painterResource(iconRes), null) }
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
    onValueChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    PreferenceEntry(
        title = { Text(title) },
        description = if (value.isEmpty()) defaultValue else value,
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
                    singleLine = true
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
    buttonUrlSource: ActivitySource = ActivitySource.SONG,
    activityType: String = "LISTENING",
    largeImageType: String = "thumbnail",
    largeImageCustomUrl: String = "",
    smallImageType: String = "artist",
    smallImageCustomUrl: String = "",
) {
    val context = LocalContext.current

    val (button1Label) = rememberPreference(
        key = DiscordActivityButton1LabelKey,
        defaultValue = "Listen on YouTube Music"
    )
    val (button1Url) = rememberPreference(
        key = DiscordActivityButton1UrlKey,
        defaultValue = ""
    )
    val (button2Label) = rememberPreference(
        key = DiscordActivityButton2LabelKey,
        defaultValue = "View Album"
    )
    val (button2Url) = rememberPreference(
        key = DiscordActivityButton2UrlKey,
        defaultValue = ""
    )

    val defaultButton1Url = song?.id?.let { "https://music.youtube.com/watch?v=$it" }
    val resolvedButton1Url = if (button1Url.isNotEmpty()) button1Url else defaultButton1Url

    val defaultButton2Url = when (buttonUrlSource) {
        ActivitySource.ALBUM -> song?.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
        ActivitySource.ARTIST -> song?.id?.let { "https://music.youtube.com/watch?v=$it" }
        ActivitySource.SONG, ActivitySource.APP -> song?.id?.let { "https://music.youtube.com/watch?v=$it" }
    }
    val resolvedButton2Url = if (button2Url.isNotEmpty()) button2Url else defaultButton2Url

    PreferenceEntry(
        title = {
            Text(text = stringResource(R.string.preview), style = MaterialTheme.typography.titleMedium)
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
                        text = when (nameSource) {
                            ActivitySource.ARTIST -> song?.artists?.firstOrNull()?.name ?: "ArchiveTune"
                            ActivitySource.ALBUM -> song?.album?.title ?: "ArchiveTune"
                            ActivitySource.SONG -> song?.song?.title ?: "ArchiveTune"
                            ActivitySource.APP -> when (activityType.uppercase()) {
                                "PLAYING" -> "Playing on ArchiveTune"
                                "LISTENING" -> "Listening to ArchiveTune"
                                else -> activityType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " on ArchiveTune"
                            }
                        },
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
                                    "appicon" -> null
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
                            val smallModel = when (smallImageType) {
                                "thumbnail" -> song?.song?.thumbnailUrl
                                "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl
                                "appicon" -> null
                                "custom" -> smallImageCustomUrl.ifBlank { song?.artists?.firstOrNull()?.thumbnailUrl }
                                else -> song?.artists?.firstOrNull()?.thumbnailUrl
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
                            Text(
                                text = song?.artists?.joinToString { it.name } ?: "Artist",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            song?.album?.title?.let {
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
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (resolvedButton1Url != null) {
                        OutlinedButton(
                            enabled = song != null,
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolvedButton1Url)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(button1Label.ifBlank { "Listen on YouTube Music" })
                        }
                    }

                    if (resolvedButton2Url != null) {
                        OutlinedButton(
                            enabled = song != null,
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolvedButton2Url)))
                            },
                            modifier = Modifier.fillMaxWidth(),
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
fun SongProgressBar(currentTimeMillis: Long, durationMillis: Long) {
    val progress = if (durationMillis > 0) currentTimeMillis.toFloat() / durationMillis else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = makeTimeString(currentTimeMillis),
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

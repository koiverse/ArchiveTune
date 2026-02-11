/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.menu

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.getSystemService
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.TogetherAllowGuestsToAddTracksKey
import moe.koiverse.archivetune.constants.TogetherAllowGuestsToControlPlaybackKey
import moe.koiverse.archivetune.constants.TogetherDefaultPortKey
import moe.koiverse.archivetune.constants.TogetherDisplayNameKey
import moe.koiverse.archivetune.constants.TogetherLastJoinLinkKey
import moe.koiverse.archivetune.constants.TogetherRequireHostApprovalToJoinKey
import moe.koiverse.archivetune.together.AddTrackMode
import moe.koiverse.archivetune.together.ControlAction
import moe.koiverse.archivetune.together.TogetherLink
import moe.koiverse.archivetune.together.TogetherRole
import moe.koiverse.archivetune.together.TogetherRoomSettings
import moe.koiverse.archivetune.together.TogetherSessionState
import moe.koiverse.archivetune.together.TogetherTrack
import moe.koiverse.archivetune.ui.component.MenuSurfaceSection
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTogetherDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val sessionState by playerConnection.service.togetherSessionState.collectAsState()

    val (displayName, setDisplayName) =
        rememberPreference(
            TogetherDisplayNameKey,
            defaultValue = Build.MODEL?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name),
        )
    val (port, setPort) = rememberPreference(TogetherDefaultPortKey, defaultValue = 42117)

    val (allowAddTracks, setAllowAddTracks) = rememberPreference(TogetherAllowGuestsToAddTracksKey, defaultValue = true)
    val (allowControlPlayback, setAllowControlPlayback) = rememberPreference(TogetherAllowGuestsToControlPlaybackKey, defaultValue = false)
    val (requireApproval, setRequireApproval) = rememberPreference(TogetherRequireHostApprovalToJoinKey, defaultValue = false)
    val (lastJoinLink, setLastJoinLink) = rememberPreference(TogetherLastJoinLinkKey, defaultValue = "")

    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var showPortDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }

    var joinInput by rememberSaveable { mutableStateOf(lastJoinLink) }

    if (showNameDialog) {
        TextFieldDialog(
            title = { Text(text = stringResource(R.string.together_display_name)) },
            placeholder = { Text(text = stringResource(R.string.together_display_name_placeholder)) },
            isInputValid = { it.trim().isNotBlank() },
            onDone = { setDisplayName(it.trim()) },
            onDismiss = { showNameDialog = false },
        )
    }

    if (showPortDialog) {
        TextFieldDialog(
            title = { Text(text = stringResource(R.string.together_port)) },
            placeholder = { Text(text = "42117") },
            isInputValid = { it.trim().toIntOrNull() in 1..65535 },
            onDone = { setPort(it.trim().toInt()) },
            onDismiss = { showPortDialog = false },
        )
    }

    if (showJoinDialog) {
        TextFieldDialog(
            title = { Text(text = stringResource(R.string.join_session)) },
            placeholder = { Text(text = stringResource(R.string.together_join_link_hint)) },
            singleLine = false,
            maxLines = 6,
            isInputValid = { TogetherLink.decode(it) != null },
            onDone = {
                val trimmed = it.trim()
                joinInput = trimmed
                setLastJoinLink(trimmed)
                playerConnection.service.joinTogether(trimmed, displayName)
            },
            onDismiss = { showJoinDialog = false },
        )
    }

    val canJoin = remember(joinInput) { TogetherLink.decode(joinInput) != null }

    val currentTrack = playerConnection.mediaMetadata.collectAsState().value
    val currentTogetherTrack =
        remember(currentTrack) {
            currentTrack?.let {
                TogetherTrack(
                    id = it.id,
                    title = it.title,
                    artists = it.artists.map { a -> a.name },
                    durationSec = it.duration,
                    thumbnailUrl = it.thumbnailUrl,
                )
            }
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.music_together)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        when (sessionState) {
                            is TogetherSessionState.Hosting,
                            is TogetherSessionState.Joining,
                            is TogetherSessionState.Joined,
                                -> {
                                TextButton(
                                    onClick = {
                                        playerConnection.service.leaveTogether()
                                    },
                                ) {
                                    Text(text = stringResource(R.string.leave))
                                }
                            }

                            else -> Unit
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(
                                bottom =
                                    16.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                            ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(8.dp))

                    when (val state = sessionState) {
                        TogetherSessionState.Idle -> {
                            MenuSurfaceSection {
                                Column {
                                    ListItem(
                                        headlineContent = { Text(text = stringResource(R.string.together_display_name)) },
                                        supportingContent = {
                                            Text(
                                                text = displayName,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.person),
                                                contentDescription = null,
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 6.dp),
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                    TextButton(
                                        onClick = { showNameDialog = true },
                                        modifier = Modifier.padding(start = 56.dp, bottom = 8.dp),
                                    ) {
                                        Text(text = stringResource(R.string.edit))
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 56.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )

                                    ListItem(
                                        headlineContent = { Text(text = stringResource(R.string.together_port)) },
                                        supportingContent = { Text(text = port.toString()) },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.link),
                                                contentDescription = null,
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 6.dp),
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                    TextButton(
                                        onClick = { showPortDialog = true },
                                        modifier = Modifier.padding(start = 56.dp, bottom = 8.dp),
                                    ) {
                                        Text(text = stringResource(R.string.edit))
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 56.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )

                                    SettingToggleRow(
                                        title = stringResource(R.string.together_allow_guests_add),
                                        checked = allowAddTracks,
                                        onCheckedChange = setAllowAddTracks,
                                    )
                                    SettingToggleRow(
                                        title = stringResource(R.string.together_allow_guests_control),
                                        checked = allowControlPlayback,
                                        onCheckedChange = setAllowControlPlayback,
                                    )
                                    SettingToggleRow(
                                        title = stringResource(R.string.together_require_approval),
                                        checked = requireApproval,
                                        onCheckedChange = setRequireApproval,
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    playerConnection.service.startTogetherHost(
                                        port = port,
                                        displayName = displayName,
                                        settings =
                                            TogetherRoomSettings(
                                                allowGuestsToAddTracks = allowAddTracks,
                                                allowGuestsToControlPlayback = allowControlPlayback,
                                                requireHostApprovalToJoin = requireApproval,
                                            ),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text(text = stringResource(R.string.start_session))
                            }

                            MenuSurfaceSection {
                                Column {
                                    ListItem(
                                        headlineContent = { Text(text = stringResource(R.string.join_session)) },
                                        supportingContent = {
                                            Text(
                                                text =
                                                    joinInput.trim().ifBlank { stringResource(R.string.together_join_link_hint) },
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.input),
                                                contentDescription = null,
                                            )
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(start = 56.dp, bottom = 12.dp),
                                    ) {
                                        TextButton(onClick = { showJoinDialog = true }) {
                                            Text(text = stringResource(R.string.edit))
                                        }
                                        TextButton(
                                            enabled = canJoin,
                                            onClick = {
                                                val trimmed = joinInput.trim()
                                                setLastJoinLink(trimmed)
                                                playerConnection.service.joinTogether(trimmed, displayName)
                                            },
                                        ) {
                                            Text(text = stringResource(R.string.join))
                                        }
                                    }
                                }
                            }
                        }

                        is TogetherSessionState.Hosting -> {
                            val link = state.joinLink
                            MenuSurfaceSection {
                                Column {
                                    ListItem(
                                        headlineContent = { Text(text = stringResource(R.string.session_link)) },
                                        supportingContent = {
                                            Text(
                                                text = link,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.link),
                                                contentDescription = null,
                                            )
                                        },
                                        trailingContent = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        val clipboard =
                                                            context.getSystemService<android.content.ClipboardManager>()
                                                                ?: return@IconButton
                                                        clipboard.setPrimaryClip(
                                                            android.content.ClipData.newPlainText(
                                                                context.getString(R.string.session_link),
                                                                link,
                                                            ),
                                                        )
                                                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.link),
                                                        contentDescription = null,
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val share =
                                                            Intent(Intent.ACTION_SEND).apply {
                                                                type = "text/plain"
                                                                putExtra(Intent.EXTRA_TEXT, link)
                                                            }
                                                        context.startActivity(Intent.createChooser(share, null))
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.share),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                }
                            }

                            MenuSurfaceSection {
                                Column {
                                    SettingToggleRow(
                                        title = stringResource(R.string.together_allow_guests_add),
                                        checked = state.settings.allowGuestsToAddTracks,
                                        onCheckedChange = {
                                            val updated = state.settings.copy(allowGuestsToAddTracks = it)
                                            setAllowAddTracks(it)
                                            playerConnection.service.updateTogetherSettings(updated)
                                        },
                                    )
                                    SettingToggleRow(
                                        title = stringResource(R.string.together_allow_guests_control),
                                        checked = state.settings.allowGuestsToControlPlayback,
                                        onCheckedChange = {
                                            val updated = state.settings.copy(allowGuestsToControlPlayback = it)
                                            setAllowControlPlayback(it)
                                            playerConnection.service.updateTogetherSettings(updated)
                                        },
                                    )
                                    SettingToggleRow(
                                        title = stringResource(R.string.together_require_approval),
                                        checked = state.settings.requireHostApprovalToJoin,
                                        onCheckedChange = {
                                            val updated = state.settings.copy(requireHostApprovalToJoin = it)
                                            setRequireApproval(it)
                                            playerConnection.service.updateTogetherSettings(updated)
                                        },
                                    )
                                }
                            }

                            val room = state.roomState
                            if (room != null) {
                                val pending = room.participants.filter { it.isPending && !it.isHost }
                                if (pending.isNotEmpty()) {
                                    MenuSurfaceSection {
                                        Column {
                                            ListItem(
                                                headlineContent = { Text(text = stringResource(R.string.pending_requests)) },
                                                leadingContent = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.notifications_unread),
                                                        contentDescription = null,
                                                    )
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            )
                                            pending.forEach { p ->
                                                PendingRequestRow(
                                                    name = p.name,
                                                    onApprove = { playerConnection.service.approveTogetherParticipant(p.id, true) },
                                                    onDeny = { playerConnection.service.approveTogetherParticipant(p.id, false) },
                                                )
                                            }
                                        }
                                    }
                                }

                                MenuSurfaceSection {
                                    Column {
                                        ListItem(
                                            headlineContent = { Text(text = stringResource(R.string.participants)) },
                                            supportingContent = { Text(text = room.participants.size.toString()) },
                                            leadingContent = {
                                                Icon(
                                                    painter = painterResource(R.drawable.account),
                                                    contentDescription = null,
                                                )
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        )
                                        room.participants.forEach { p ->
                                            ListItem(
                                                headlineContent = {
                                                    Text(
                                                        text = p.name,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                },
                                                supportingContent = {
                                                    if (p.isHost) Text(text = stringResource(R.string.host))
                                                    else if (p.isPending) Text(text = stringResource(R.string.waiting_for_approval))
                                                },
                                                leadingContent = {
                                                    Icon(
                                                        painter = painterResource(if (p.isHost) R.drawable.star else R.drawable.person),
                                                        contentDescription = null,
                                                    )
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            )
                                        }
                                    }
                                }
                            } else {
                                MenuSurfaceSection {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                        Text(
                                            text = stringResource(R.string.loading),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { playerConnection.service.leaveTogether() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text(text = stringResource(R.string.end_session))
                            }
                        }

                        is TogetherSessionState.Joining -> {
                            MenuSurfaceSection {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = stringResource(R.string.connecting),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        is TogetherSessionState.Joined -> {
                            val room = state.roomState
                            val isGuest = state.role is TogetherRole.Guest

                            MenuSurfaceSection {
                                Column {
                                    ListItem(
                                        headlineContent = { Text(text = stringResource(R.string.participants)) },
                                        supportingContent = { Text(text = room.participants.size.toString()) },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.account),
                                                contentDescription = null,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                    room.participants.forEach { p ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    text = p.name,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            },
                                            supportingContent = { if (p.isHost) Text(text = stringResource(R.string.host)) },
                                            leadingContent = {
                                                Icon(
                                                    painter = painterResource(if (p.isHost) R.drawable.star else R.drawable.person),
                                                    contentDescription = null,
                                                )
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        )
                                    }
                                }
                            }

                            if (isGuest) {
                                MenuSurfaceSection {
                                    Column {
                                        ListItem(
                                            headlineContent = { Text(text = stringResource(R.string.session_settings)) },
                                            leadingContent = {
                                                Icon(
                                                    painter = painterResource(R.drawable.settings),
                                                    contentDescription = null,
                                                )
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        )
                                        ReadOnlySettingRow(
                                            title = stringResource(R.string.together_allow_guests_add),
                                            enabled = room.settings.allowGuestsToAddTracks,
                                        )
                                        ReadOnlySettingRow(
                                            title = stringResource(R.string.together_allow_guests_control),
                                            enabled = room.settings.allowGuestsToControlPlayback,
                                        )
                                    }
                                }

                                if (room.settings.allowGuestsToAddTracks && currentTogetherTrack != null) {
                                    MenuSurfaceSection {
                                        Column {
                                            ListItem(
                                                headlineContent = { Text(text = stringResource(R.string.add_current_song)) },
                                                supportingContent = {
                                                    Text(
                                                        text = currentTogetherTrack.title,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                },
                                                leadingContent = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.add),
                                                        contentDescription = null,
                                                    )
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            )
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Button(
                                                    onClick = {
                                                        playerConnection.service.requestTogetherAddTrack(
                                                            currentTogetherTrack,
                                                            AddTrackMode.PLAY_NEXT,
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(20.dp),
                                                ) {
                                                    Text(text = stringResource(R.string.play_next))
                                                }
                                                Button(
                                                    onClick = {
                                                        playerConnection.service.requestTogetherAddTrack(
                                                            currentTogetherTrack,
                                                            AddTrackMode.ADD_TO_QUEUE,
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(20.dp),
                                                ) {
                                                    Text(text = stringResource(R.string.add_to_queue))
                                                }
                                            }
                                        }
                                    }
                                }

                                if (room.settings.allowGuestsToControlPlayback) {
                                    MenuSurfaceSection {
                                        Column {
                                            ListItem(
                                                headlineContent = { Text(text = stringResource(R.string.playback_controls)) },
                                                leadingContent = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.play),
                                                        contentDescription = null,
                                                    )
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            )
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                IconButton(
                                                    onClick = { playerConnection.service.requestTogetherControl(ControlAction.SkipPrevious) },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.arrow_back),
                                                        contentDescription = null,
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        playerConnection.service.requestTogetherControl(
                                                            if (room.isPlaying) ControlAction.Pause else ControlAction.Play,
                                                        )
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(if (room.isPlaying) R.drawable.pause else R.drawable.play),
                                                        contentDescription = null,
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { playerConnection.service.requestTogetherControl(ControlAction.SkipNext) },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.arrow_forward),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        is TogetherSessionState.Error -> {
                            MenuSurfaceSection {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.error),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Button(
                                onClick = { playerConnection.service.leaveTogether() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text(text = stringResource(R.string.dismiss))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ReadOnlySettingRow(
    title: String,
    enabled: Boolean,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = if (enabled) stringResource(R.string.enabled) else stringResource(R.string.disabled)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun PendingRequestRow(
    name: String,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        TextButton(onClick = onDeny) {
            Text(text = stringResource(R.string.deny))
        }
        Button(onClick = onApprove, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            Text(text = stringResource(R.string.approve))
        }
    }
}

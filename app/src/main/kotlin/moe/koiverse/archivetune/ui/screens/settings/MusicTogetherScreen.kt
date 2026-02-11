/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.Switch
import kotlinx.coroutines.flow.MutableStateFlow
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.TogetherAllowGuestsToAddTracksKey
import moe.koiverse.archivetune.constants.TogetherAllowGuestsToControlPlaybackKey
import moe.koiverse.archivetune.constants.TogetherDefaultPortKey
import moe.koiverse.archivetune.constants.TogetherDisplayNameKey
import moe.koiverse.archivetune.constants.TogetherLastJoinLinkKey
import moe.koiverse.archivetune.constants.TogetherRequireHostApprovalToJoinKey
import moe.koiverse.archivetune.constants.TogetherWelcomeShownKey
import moe.koiverse.archivetune.together.TogetherLink
import moe.koiverse.archivetune.together.TogetherRoomSettings
import moe.koiverse.archivetune.together.TogetherSessionState
import moe.koiverse.archivetune.ui.component.IconButton as AtIconButton
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTogetherScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    val (welcomeShown, setWelcomeShown) = rememberPreference(TogetherWelcomeShownKey, false)
    var showWelcome by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(welcomeShown) {
        if (!welcomeShown) {
            showWelcome = true
        }
    }

    if (showWelcome) {
        AlertDialog(
            onDismissRequest = {
                showWelcome = false
                setWelcomeShown(true)
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.together_welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.together_welcome_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            InstructionRow(
                                icon = R.drawable.fire,
                                title = stringResource(R.string.together_welcome_host_title),
                                body = stringResource(R.string.together_welcome_host_body),
                            )
                            InstructionRow(
                                icon = R.drawable.link,
                                title = stringResource(R.string.together_welcome_join_title),
                                body = stringResource(R.string.together_welcome_join_body),
                            )
                            InstructionRow(
                                icon = R.drawable.lock,
                                title = stringResource(R.string.together_welcome_permissions_title),
                                body = stringResource(R.string.together_welcome_permissions_body),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWelcome = false
                        setWelcomeShown(true)
                    },
                ) {
                    Text(text = stringResource(R.string.got_it))
                }
            },
        )
    }

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

    val sessionStateFlow =
        remember(playerConnection) {
            playerConnection?.service?.togetherSessionState ?: MutableStateFlow(TogetherSessionState.Idle)
        }
    val sessionState by sessionStateFlow.collectAsState()

    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var showPortDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }

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

    var joinInput by rememberSaveable { mutableStateOf(lastJoinLink) }
    val canJoin = remember(joinInput) { TogetherLink.decode(joinInput) != null }

    if (showJoinDialog) {
        TextFieldDialog(
            title = { Text(text = stringResource(R.string.join_session)) },
            placeholder = { Text(text = stringResource(R.string.together_join_link_hint)) },
            singleLine = false,
            maxLines = 8,
            isInputValid = { TogetherLink.decode(it) != null },
            onDone = { raw ->
                val trimmed = raw.trim()
                joinInput = trimmed
                setLastJoinLink(trimmed)
                playerConnection?.service?.joinTogether(trimmed, displayName)
            },
            onDismiss = { showJoinDialog = false },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )

        PreferenceGroupTitle(title = stringResource(R.string.music_together))

        StatusCard(
            state = sessionState,
            onCopyLink = { link ->
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(
                    android.content.ClipData.newPlainText(context.getString(R.string.session_link), link),
                )
                Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
            },
            onShareLink = { link ->
                val share =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                context.startActivity(Intent.createChooser(share, null))
            },
            onLeave = { playerConnection?.service?.leaveTogether() },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.size(12.dp))

        PreferenceGroupTitle(title = stringResource(R.string.together_host_section))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.together_display_name)) },
                    supportingContent = {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = { Icon(painterResource(R.drawable.person), null) },
                    trailingContent = {
                        TextButton(onClick = { showNameDialog = true }) {
                            Text(text = stringResource(R.string.edit))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.together_port)) },
                    supportingContent = { Text(port.toString()) },
                    leadingContent = { Icon(painterResource(R.drawable.link), null) },
                    trailingContent = {
                        TextButton(onClick = { showPortDialog = true }) {
                            Text(text = stringResource(R.string.edit))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)

                ToggleRow(
                    title = stringResource(R.string.together_allow_guests_add),
                    checked = allowAddTracks,
                    onCheckedChange = setAllowAddTracks,
                )
                ToggleRow(
                    title = stringResource(R.string.together_allow_guests_control),
                    checked = allowControlPlayback,
                    onCheckedChange = setAllowControlPlayback,
                )
                ToggleRow(
                    title = stringResource(R.string.together_require_approval),
                    checked = requireApproval,
                    onCheckedChange = setRequireApproval,
                )

                Spacer(Modifier.size(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            playerConnection?.service?.startTogetherHost(
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(text = stringResource(R.string.start_session))
                    }
                }
            }
        }

        Spacer(Modifier.size(12.dp))

        PreferenceGroupTitle(title = stringResource(R.string.together_join_section))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.join_session)) },
                    supportingContent = {
                        Text(
                            text = joinInput.trim().ifBlank { stringResource(R.string.together_join_link_hint) },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = { Icon(painterResource(R.drawable.input), null) },
                    trailingContent = {
                        TextButton(onClick = { showJoinDialog = true }) {
                            Text(text = stringResource(R.string.edit))
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        enabled = canJoin,
                        onClick = {
                            val trimmed = joinInput.trim()
                            setLastJoinLink(trimmed)
                            playerConnection?.service?.joinTogether(trimmed, displayName)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(text = stringResource(R.string.join))
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.music_together)) },
        navigationIcon = {
            AtIconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), null)
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun InstructionRow(
    icon: Int,
    title: String,
    body: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(10.dp).size(18.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(
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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun StatusCard(
    state: TogetherSessionState,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fire),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp).size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.together_status),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text =
                            when (state) {
                                TogetherSessionState.Idle -> stringResource(R.string.together_idle)
                                is TogetherSessionState.Hosting -> stringResource(R.string.together_hosting)
                                is TogetherSessionState.Joining -> stringResource(R.string.together_joining)
                                is TogetherSessionState.Joined -> stringResource(R.string.together_connected)
                                is TogetherSessionState.Error -> stringResource(R.string.together_error_state)
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (state !is TogetherSessionState.Idle) {
                    TextButton(onClick = onLeave) {
                        Text(text = stringResource(R.string.leave))
                    }
                }
            }

            when (state) {
                is TogetherSessionState.Hosting -> {
                    SessionLinkRow(state.joinLink, onCopyLink, onShareLink)
                }

                is TogetherSessionState.Joined -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.participants),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            val list = state.roomState.participants
                            Text(
                                text = list.joinToString(separator = " • ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                is TogetherSessionState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun SessionLinkRow(
    link: String,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.session_link),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = link,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { onCopyLink(link) }) { Text(text = stringResource(R.string.copy_link)) }
                TextButton(onClick = { onShareLink(link) }) { Text(text = stringResource(R.string.share)) }
            }
        }
    }
}

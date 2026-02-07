/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.EnableLastFMScrobblingKey
import moe.koiverse.archivetune.constants.LastFMSessionKey
import moe.koiverse.archivetune.constants.LastFMUsernameKey
import moe.koiverse.archivetune.constants.LastFMUseNowPlaying
import moe.koiverse.archivetune.constants.ScrobbleDelayPercentKey
import moe.koiverse.archivetune.constants.ScrobbleMinSongDurationKey
import moe.koiverse.archivetune.constants.ScrobbleDelaySecondsKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.utils.reportException
import moe.koiverse.archivetune.lastfm.LastFM
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFMSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val coroutineScope = rememberCoroutineScope()

    var lastfmUsername by rememberPreference(LastFMUsernameKey, "")
    var lastfmSession by rememberPreference(LastFMSessionKey, "")

    val isLoggedIn = remember(lastfmSession) {
        lastfmSession.isNotEmpty()
    }

    val (useNowPlaying, onUseNowPlayingChange) = rememberPreference(
        key = LastFMUseNowPlaying,
        defaultValue = false
    )

    val (lastfmScrobbling, onlastfmScrobblingChange) = rememberPreference(
        key = EnableLastFMScrobblingKey,
        defaultValue = false
    )

    val (scrobbleDelayPercent, onScrobbleDelayPercentChange) = rememberPreference(
        ScrobbleDelayPercentKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT
    )

    val (minTrackDuration, onMinTrackDurationChange) = rememberPreference(
        ScrobbleMinSongDurationKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION
    )

    val (scrobbleDelaySeconds, onScrobbleDelaySecondsChange) = rememberPreference(
        ScrobbleDelaySecondsKey,
        defaultValue = LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
    )

    var showLoginDialog by rememberSaveable { mutableStateOf(false) }
    var isLoggingIn by rememberSaveable { mutableStateOf(false) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }

    if (showLoginDialog) {
        var tempUsername by rememberSaveable { mutableStateOf("") }
        var tempPassword by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { 
                if (!isLoggingIn) {
                    showLoginDialog = false
                    loginError = null
                }
            },
            title = { Text(stringResource(R.string.login)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { 
                            tempUsername = it
                            loginError = null
                        },
                        label = { Text(stringResource(R.string.username)) },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { 
                            tempPassword = it
                            loginError = null
                        },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    loginError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (isLoggingIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.logging_in),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempUsername.isBlank() || tempPassword.isBlank()) {
                            loginError = "Please enter username and password"
                            return@TextButton
                        }
                        
                        if (!LastFM.isInitialized()) {
                            loginError = "Last.fm API key not configured"
                            Timber.e("Last.fm API key not configured")
                            return@TextButton
                        }
                        
                        isLoggingIn = true
                        loginError = null
                        
                        coroutineScope.launch(Dispatchers.IO) {
                            LastFM.getMobileSession(tempUsername, tempPassword)
                                .onSuccess { auth ->
                                    withContext(Dispatchers.Main) {
                                        lastfmUsername = auth.session.name
                                        lastfmSession = auth.session.key
                                        LastFM.sessionKey = auth.session.key
                                        isLoggingIn = false
                                        showLoginDialog = false
                                        loginError = null
                                        Timber.d("Last.fm login successful for user: ${auth.session.name}")
                                    }
                                }
                                .onFailure { exception ->
                                    withContext(Dispatchers.Main) {
                                        isLoggingIn = false
                                        val errorMessage = when (exception) {
                                            is LastFM.LastFmException -> {
                                                // Last.fm API error codes:
                                                // 4 = Invalid authentication token
                                                // 10 = Invalid API key
                                                // 13 = Invalid method signature
                                                // 26 = API key suspended
                                                when (exception.code) {
                                                    4 -> "Invalid username or password"
                                                    10 -> "Invalid API key. Please contact the developer."
                                                    13 -> "Authentication error. Please try again."
                                                    26 -> "API key suspended. Please contact the developer."
                                                    else -> exception.message
                                                }
                                            }
                                            else -> when {
                                                exception.message?.contains("network", ignoreCase = true) == true ||
                                                exception.message?.contains("connect", ignoreCase = true) == true ->
                                                    "Network error. Check your connection."
                                                else -> exception.message ?: "Login failed. Please try again."
                                            }
                                        }
                                        loginError = errorMessage
                                        Timber.e(exception, "Last.fm login failed")
                                        reportException(exception)
                                    }
                                }
                        }
                    },
                    enabled = !isLoggingIn && tempUsername.isNotBlank() && tempPassword.isNotBlank()
                ) {
                    Text(stringResource(R.string.login))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLoginDialog = false
                        loginError = null
                    },
                    enabled = !isLoggingIn
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.account),
        )

        PreferenceEntry(
            title = {
                Text(
                    text = if (isLoggedIn) lastfmUsername else stringResource(R.string.not_logged_in),
                    modifier = Modifier.alpha(if (isLoggedIn) 1f else 0.5f),
                )
            },
            description = null,
            icon = { Icon(painterResource(R.drawable.token), null) },
            trailingContent = {
                if (isLoggedIn) {
                    OutlinedButton(onClick = {
                        lastfmSession = ""
                        lastfmUsername = ""
                        LastFM.sessionKey = null
                        Timber.d("Last.fm session cleared")
                    }) {
                        Text(stringResource(R.string.action_logout))
                    }
                } else {
                    OutlinedButton(onClick = {
                        showLoginDialog = true
                    }) {
                        Text(stringResource(R.string.action_login))
                    }
                }
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.options),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_scrobbling)) },
            checked = lastfmScrobbling,
            onCheckedChange = onlastfmScrobblingChange,
            isEnabled = isLoggedIn,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lastfm_now_playing)) },
            checked = useNowPlaying,
            onCheckedChange = onUseNowPlayingChange,
            isEnabled = isLoggedIn && lastfmScrobbling,
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.scrobbling_configuration)
        )

        var showMinTrackDurationDialog by rememberSaveable { mutableStateOf(false) }

        if (showMinTrackDurationDialog) {
            var tempMinTrackDuration by remember { mutableIntStateOf(minTrackDuration) }

            AlertDialog(
                onDismissRequest = {
                    tempMinTrackDuration = minTrackDuration
                    showMinTrackDurationDialog = false
                },
                title = { Text(stringResource(R.string.scrobble_min_track_duration)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${tempMinTrackDuration}s",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Slider(
                            value = tempMinTrackDuration.toFloat(),
                            onValueChange = { tempMinTrackDuration = it.toInt() },
                            valueRange = 10f..60f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onMinTrackDurationChange(tempMinTrackDuration)
                            showMinTrackDurationDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            tempMinTrackDuration = minTrackDuration
                            showMinTrackDurationDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.scrobble_min_track_duration)) },
            description = "${minTrackDuration}s",
            onClick = { showMinTrackDurationDialog = true }
        )

        var showScrobbleDelayPercentDialog by rememberSaveable { mutableStateOf(false) }

        if (showScrobbleDelayPercentDialog) {
            var tempScrobbleDelayPercent by remember { mutableFloatStateOf(scrobbleDelayPercent) }

            AlertDialog(
                onDismissRequest = {
                    tempScrobbleDelayPercent = scrobbleDelayPercent
                    showScrobbleDelayPercentDialog = false
                },
                title = { Text(stringResource(R.string.scrobble_delay_percent)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${(tempScrobbleDelayPercent * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Slider(
                            value = tempScrobbleDelayPercent,
                            onValueChange = { tempScrobbleDelayPercent = it },
                            valueRange = 0.3f..0.95f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onScrobbleDelayPercentChange(tempScrobbleDelayPercent)
                            showScrobbleDelayPercentDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            tempScrobbleDelayPercent = scrobbleDelayPercent
                            showScrobbleDelayPercentDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.scrobble_delay_percent)) },
            description = "${(scrobbleDelayPercent * 100).roundToInt()}%",
            onClick = { showScrobbleDelayPercentDialog = true }
        )

        var showScrobbleDelaySecondsDialog by rememberSaveable { mutableStateOf(false) }

        if (showScrobbleDelaySecondsDialog) {
            var tempScrobbleDelaySeconds by remember { mutableIntStateOf(scrobbleDelaySeconds) }

            AlertDialog(
                onDismissRequest = {
                    tempScrobbleDelaySeconds = scrobbleDelaySeconds
                    showScrobbleDelaySecondsDialog = false
                },
                title = { Text(stringResource(R.string.scrobble_delay_minutes)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${tempScrobbleDelaySeconds}s",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Slider(
                            value = tempScrobbleDelaySeconds.toFloat(),
                            onValueChange = { tempScrobbleDelaySeconds = it.toInt() },
                            valueRange = 30f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onScrobbleDelaySecondsChange(tempScrobbleDelaySeconds)
                            showScrobbleDelaySecondsDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            tempScrobbleDelaySeconds = scrobbleDelaySeconds
                            showScrobbleDelaySecondsDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.scrobble_delay_minutes)) },
            description = "${scrobbleDelaySeconds}s",
            onClick = { showScrobbleDelaySecondsDialog = true }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lastfm_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

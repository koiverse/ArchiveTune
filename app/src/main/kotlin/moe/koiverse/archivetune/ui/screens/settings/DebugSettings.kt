package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.utils.rememberPreference
import androidx.datastore.preferences.core.booleanPreferencesKey
import moe.koiverse.archivetune.ui.screens.settings.DiscordPresenceManager
import androidx.compose.runtime.collectAsState
import moe.koiverse.archivetune.utils.makeTimeString
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun DebugSettings(
    navController: NavController
) {
    // Developer preferences
    val (showDevDebug, onShowDevDebugChange) = rememberPreference(
        key = booleanPreferencesKey("dev_show_discord_debug"),
        defaultValue = false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Settings") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding: androidx.compose.foundation.layout.PaddingValues ->
        Column(Modifier.padding(innerPadding).padding(16.dp)) {
            PreferenceEntry(
                title = { Text("Show Discord debug UI") },
                description = "Enable debug lines in Discord settings",
                icon = { Icon(painterResource(R.drawable.info), null) },
                trailingContent = {
                    Switch(checked = showDevDebug, onCheckedChange = onShowDevDebugChange)
                }
            )

            if (showDevDebug) {
                // Show manager status lines (observe flows so UI updates)
                val lastStartTs: Long? by DiscordPresenceManager.lastRpcStartTimeFlow.collectAsState(initial = null)
                val lastEndTs: Long? by DiscordPresenceManager.lastRpcEndTimeFlow.collectAsState(initial = null)
                val lastStart: String = lastStartTs?.let { makeTimeString(it) } ?: "-"
                val lastEnd: String = lastEndTs?.let { makeTimeString(it) } ?: "-"

                PreferenceEntry(
                    title = { Text(if (DiscordPresenceManager.isRunning()) "Presence manager: running" else "Presence manager: stopped") },
                    description = "Last RPC start: $lastStart  end: $lastEnd",
                    icon = { Icon(painterResource(R.drawable.info), null) }
                )
            }
        }
    }
}

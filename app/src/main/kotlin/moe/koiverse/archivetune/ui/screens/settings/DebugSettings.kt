package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.utils.rememberPreference
import androidx.datastore.preferences.core.stringPreferencesKey
import moe.koiverse.archivetune.ui.screens.settings.DiscordPresenceManager

@Composable
fun DebugSettings(
    navController: NavController
) {
    // Developer preferences
    val (showDevDebug, onShowDevDebugChange) = rememberPreference(
        key = stringPreferencesKey("dev_show_discord_debug"),
        defaultValue = false
    )

    Column(Modifier.padding(16.dp)) {
        PreferenceEntry(
            title = { Text("Show Discord debug UI") },
            description = "Enable debug lines in Discord settings",
            icon = { Icon(painterResource(R.drawable.info), null) },
            trailingContent = {
                Switch(checked = showDevDebug, onCheckedChange = onShowDevDebugChange)
            }
        )

        if (showDevDebug) {
            // Show manager status lines
            val lastStart = DiscordPresenceManager.lastRpcStartTime ?: "-"
            val lastEnd = DiscordPresenceManager.lastRpcEndTime ?: "-"
            PreferenceEntry(
                title = { Text(if (DiscordPresenceManager.isRunning()) "Presence manager: running" else "Presence manager: stopped") },
                description = "Last RPC start: $lastStart  end: $lastEnd",
                icon = { Icon(painterResource(R.drawable.info), null) }
            )
        }
    }
}

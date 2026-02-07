/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.extensions.isSyncEnabled
import moe.koiverse.archivetune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.logging.Logger

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        onDone = { playlistName ->
            coroutineScope.launch(Dispatchers.IO) {
                val browseId = if (syncedPlaylist && isSignedIn) {
                    YouTube.createPlaylist(playlistName).getOrNull()
                } else if (syncedPlaylist) {
                    Logger.getLogger("CreatePlaylistDialog").warning("Not signed in")
                    return@launch
                } else null

                database.withTransaction {
                    insert(
                        PlaylistEntity(
                            name = playlistName,
                            browseId = browseId,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true,
                        ),
                    )
                }
            }
        },
        extraContent = {
            if (allowSyncing) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 40.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sync_playlist),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.allows_for_sync_witch_youtube),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Switch(
                            checked = syncedPlaylist,
                            onCheckedChange = {
                                val isYtmSyncEnabled = context.isSyncEnabled()
                                if (!isSignedIn && !syncedPlaylist) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.not_logged_in_youtube),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (!isYtmSyncEnabled) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.sync_disabled),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    syncedPlaylist = !syncedPlaylist
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

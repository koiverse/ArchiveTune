package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.constants.ListenBrainzEnabledKey
import moe.koiverse.archivetune.constants.ListenBrainzTokenKey
import moe.koiverse.archivetune.ui.component.InfoLabel
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, false)
    val (listenBrainzToken, onListenBrainzTokenChange) = rememberPreference(ListenBrainzTokenKey, "")

    var showListenBrainzTokenEditor = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.integration)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.integration),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                PreferenceGroupTitle(
                    title = stringResource(R.string.general),
                )

                Spacer(Modifier.height(12.dp))

                PreferenceEntry(
                    title = { Text(stringResource(R.string.discord_integration)) },
                    icon = { Icon(painterResource(R.drawable.discord), null) },
                    onClick = {
                        navController.navigate("settings/discord")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )

                Spacer(Modifier.height(4.dp))

                SwitchPreference(
                    title = { Text(stringResource(R.string.listenbrainz_scrobbling)) },
                    description = stringResource(R.string.listenbrainz_scrobbling_description),
                    icon = { Icon(painterResource(R.drawable.token), null) },
                    checked = listenBrainzEnabled,
                    onCheckedChange = onListenBrainzEnabledChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )

                Spacer(Modifier.height(4.dp))

                PreferenceEntry(
                    title = { Text(if (listenBrainzToken.isBlank()) stringResource(R.string.set_listenbrainz_token) else stringResource(R.string.edit_listenbrainz_token)) },
                    icon = { Icon(painterResource(R.drawable.token), null) },
                    onClick = { showListenBrainzTokenEditor.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )

                if (showListenBrainzTokenEditor.value) {
                    TextFieldDialog(
                        initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(listenBrainzToken),
                        onDone = { data ->
                            onListenBrainzTokenChange(data)
                            showListenBrainzTokenEditor.value = false
                        },
                        onDismiss = { showListenBrainzTokenEditor.value = false },
                        singleLine = true,
                        maxLines = 1,
                        isInputValid = {
                            it.isNotEmpty()
                        },
                        extraContent = {
                            InfoLabel(text = stringResource(R.string.listenbrainz_scrobbling_description))
                        }
                    )
                }
            }
        }
    )
}

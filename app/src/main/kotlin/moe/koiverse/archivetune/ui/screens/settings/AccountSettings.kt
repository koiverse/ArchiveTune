package moe.koiverse.archivetune.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.utils.parseCookieString
import moe.koiverse.archivetune.App.Companion.forgetAccount
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AccountChannelHandleKey
import moe.koiverse.archivetune.constants.AccountEmailKey
import moe.koiverse.archivetune.constants.AccountNameKey
import moe.koiverse.archivetune.constants.DataSyncIdKey
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.constants.UseLoginForBrowse
import moe.koiverse.archivetune.constants.VisitorDataKey
import moe.koiverse.archivetune.constants.YtmSyncKey
import moe.koiverse.archivetune.constants.ListenBrainzEnabledKey
import moe.koiverse.archivetune.constants.ListenBrainzTokenKey
import moe.koiverse.archivetune.ui.component.InfoLabel
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.ReleaseNotesCard
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.utils.Updater
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.HomeViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.constants.SelectedYtmPlaylistsKey
import moe.koiverse.archivetune.innertube.utils.completed
import moe.koiverse.archivetune.utils.dataStore
import androidx.datastore.preferences.core.edit

@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)
    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, false)
    val (listenBrainzToken, onListenBrainzTokenChange) = rememberPreference(ListenBrainzTokenKey, "")

    val viewModel: HomeViewModel = hiltViewModel()
    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showListenBrainzTokenEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.close), contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))

        val accountSectionModifier = Modifier.clickable {
            onClose()
            if (isLoggedIn) {
                navController.navigate("account")
            } else {
                navController.navigate("login")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = accountSectionModifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            if (isLoggedIn && accountImageUrl != null) {
                AsyncImage(
                    model = accountImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.login),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) accountName else stringResource(R.string.login),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 5.dp)
                )
            }

            if (isLoggedIn) {
                OutlinedButton(
                    onClick = {
                        onInnerTubeCookieChange("")
                        forgetAccount(context)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.action_logout))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (showTokenEditor) {
            val text = """
                ***INNERTUBE COOKIE*** =$innerTubeCookie
                ***VISITOR DATA*** =$visitorData
                ***DATASYNC ID*** =$dataSyncId
                ***ACCOUNT NAME*** =$accountNamePref
                ***ACCOUNT EMAIL*** =$accountEmail
                ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
            """.trimIndent()

            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(text),
                onDone = { data ->
                    data.split("\n").forEach {
                        when {
                            it.startsWith("***INNERTUBE COOKIE*** =") -> onInnerTubeCookieChange(it.substringAfter("="))
                            it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                            it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                            it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                            it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                            it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> onAccountChannelHandleChange(it.substringAfter("="))
                        }
                    }
                },
                onDismiss = { showTokenEditor = false },
                singleLine = false,
                maxLines = 20,
                isInputValid = {
                    it.isNotEmpty() && "SAPISID" in parseCookieString(it)
                },
                extraContent = {
                    InfoLabel(text = stringResource(R.string.token_adv_login_description))
                }
            )
        }

        PreferenceEntry(
            title = {
                Text(
                    when {
                        !isLoggedIn -> stringResource(R.string.advanced_login)
                        showToken -> stringResource(R.string.token_shown)
                        else -> stringResource(R.string.token_hidden)
                    }
                )
            },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = {
                if (!isLoggedIn) showTokenEditor = true
                else if (!showToken) showToken = true
                else showTokenEditor = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        )

        Spacer(Modifier.height(4.dp))

        if (isLoggedIn) {
            SwitchPreference(
                title = { Text(stringResource(R.string.more_content)) },
                description = null,
                icon = { Icon(painterResource(R.drawable.add_circle), null) },
                checked = useLoginForBrowse,
                onCheckedChange = {
                    YouTube.useLoginForBrowse = it
                    onUseLoginForBrowseChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )
  
            Spacer(Modifier.height(4.dp))

            SwitchPreference(
                title = { Text(stringResource(R.string.yt_sync)) },
                icon = { Icon(painterResource(R.drawable.cached), null) },
                checked = ytmSync,
                onCheckedChange = onYtmSyncChange,
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
                onClick = { showListenBrainzTokenEditor = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        Spacer(Modifier.height(4.dp))

        var showPlaylistDialog by remember { mutableStateOf(false) }
        PreferenceEntry(
            title = { Text(stringResource(R.string.select_playlist_to_sync)) },
            icon = { Icon(painterResource(R.drawable.playlist_add), null) },
            onClick = { showPlaylistDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        )

        if (showListenBrainzTokenEditor) {
            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(listenBrainzToken),
                onDone = { data ->
                    onListenBrainzTokenChange(data)
                },
                onDismiss = { showListenBrainzTokenEditor = false },
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

        if (showPlaylistDialog) {
            val coroutineScope = rememberCoroutineScope()
            val context2 = LocalContext.current
            val (initialSelected, _) = rememberPreference(SelectedYtmPlaylistsKey, "")
            val selectedList = remember { mutableStateListOf<String>() }
            LaunchedEffect(initialSelected) {
                selectedList.clear()
                if (initialSelected.isNotEmpty()) selectedList.addAll(initialSelected.split(',').map { it.trim() }.filter { it.isNotEmpty() })
            }

            var loading by remember { mutableStateOf(true) }
            val playlists = remember { mutableStateListOf<moe.koiverse.archivetune.innertube.models.PlaylistItem>() }

            LaunchedEffect(Unit) {
                loading = true
                moe.koiverse.archivetune.innertube.YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
                    playlists.clear()
                    playlists.addAll(page.items.filterIsInstance<moe.koiverse.archivetune.innertube.models.PlaylistItem>().filterNot { it.id == "LM" || it.id == "SE" }.reversed())
                }.onFailure {
                }
                loading = false
            }

            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        coroutineScope.launch {
                            context2.dataStore.edit { settings ->
                                settings[SelectedYtmPlaylistsKey] = selectedList.joinToString(",")
                            }
                        }
                        showPlaylistDialog = false
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showPlaylistDialog = false }) { Text(stringResource(R.string.cancel_button)) }
                },
                title = { Text(stringResource(R.string.select_playlist_to_sync)) },
                text = {
                    if (loading) {
                        CircularProgressIndicator()
                    } else {
                        LazyColumn {
                            items(playlists) { pl ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedList.contains(pl.id),
                                        onCheckedChange = { checked ->
                                            if (checked) selectedList.add(pl.id) else selectedList.remove(pl.id)
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    AsyncImage(model = pl.thumbnail, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(8.dp))
                                    Text(pl.title)
                                }
                            }
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.discord_integration)) },
                icon = { Icon(painterResource(R.drawable.discord), null) },
                onClick = {
                    onClose()
                    navController.navigate("settings/discord")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            Spacer(Modifier.height(4.dp))

            PreferenceEntry(
                title = { Text(stringResource(R.string.settings)) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (latestVersionName != BuildConfig.VERSION_NAME) {
                                Badge()
                            }
                        }
                    ) {
                        Icon(painterResource(R.drawable.settings), contentDescription = null)
                    }
                },
                onClick = {
                    onClose()
                    navController.navigate("settings")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            Spacer(Modifier.height(4.dp))

            if (latestVersionName != BuildConfig.VERSION_NAME) {
                PreferenceEntry(
                    title = {
                        Text(text = stringResource(R.string.new_version_available))
                    },
                    description = latestVersionName,
                    icon = {
                        BadgedBox(badge = { Badge() }) {
                            Icon(painterResource(R.drawable.update), null)
                        }
                    },
                    onClick = {
                        uriHandler.openUri(Updater.getLatestDownloadUrl())
                    }
                )
            }
        }
    }
}

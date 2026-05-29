/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens.settings

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.spotify.SpotifyAuth
import moe.koiverse.archivetune.spotifyimport.SpotifyImportSourceType
import moe.koiverse.archivetune.spotifyimport.SpotifyImportSourceUi
import moe.koiverse.archivetune.spotifyimport.SpotifyImportSummaryUi
import moe.koiverse.archivetune.spotifyimport.SpotifyImportUiState
import moe.koiverse.archivetune.spotifyimport.SpotifyImportViewModel
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroup
import moe.koiverse.archivetune.ui.component.PreferenceGroupScope
import moe.koiverse.archivetune.ui.menu.AddToPlaylistDialogOnline
import moe.koiverse.archivetune.ui.menu.LoadingScreen
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.viewmodels.BackupCategory
import moe.koiverse.archivetune.viewmodels.BackupRestoreViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CSV_MIME_TYPES =
    arrayOf(
        "text/csv",
        "text/x-csv",
        "text/comma-separated-values",
        "text/x-comma-separated-values",
        "application/csv",
        "application/x-csv",
        "application/vnd.ms-excel",
        "text/plain",
        "text/*",
        "application/octet-stream",
    )

@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
    spotifyImportViewModel: SpotifyImportViewModel = hiltViewModel(),
) {
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by rememberSaveable { mutableStateOf(false) }
    var isProgressStarted by rememberSaveable { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var progressPercentage by rememberSaveable { mutableIntStateOf(0) }
    var showBackupOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showSpotifyLogin by rememberSaveable { mutableStateOf(false) }
    var showSpotifySources by rememberSaveable { mutableStateOf(false) }
    var pendingBackupCategories by remember { mutableStateOf(BackupCategory.entries.toSet()) }
    var pendingRestoreCategories by remember { mutableStateOf(BackupCategory.entries.toSet()) }

    val backupRestoreProgress by viewModel.backupRestoreProgress.collectAsStateWithLifecycle()
    val spotifyState by spotifyImportViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri, pendingBackupCategories)
            }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(context, uri, pendingRestoreCategories)
            }
        }
    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val result = viewModel.importPlaylistFromCsv(context, uri)
                importedSongs.clear()
                importedSongs.addAll(result)
                if (importedSongs.isNotEmpty()) {
                    showChoosePlaylistDialogOnline = true
                }
            }
        }
    val importM3uLauncherOnline =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val result = viewModel.loadM3UOnline(context, uri)
                importedSongs.clear()
                importedSongs.addAll(result)
                if (importedSongs.isNotEmpty()) {
                    showChoosePlaylistDialogOnline = true
                }
            }
        }

    LaunchedEffect(spotifyState.isAuthenticated) {
        if (spotifyState.isAuthenticated) {
            showSpotifyLogin = false
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroup(title = stringResource(R.string.internal_service)) {
            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.action_backup)) },
                    description = stringResource(R.string.backup_create_backup_desc),
                    icon = { Icon(painterResource(R.drawable.backup), null) },
                    onClick = { showBackupOptionsDialog = true },
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.action_restore)) },
                    description = stringResource(R.string.backup_restore_backup_desc),
                    icon = { Icon(painterResource(R.drawable.restore), null) },
                    onClick = { showRestoreOptionsDialog = true },
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.import_online)) },
                    description = stringResource(R.string.import_m3u_format),
                    icon = { Icon(painterResource(R.drawable.playlist_import), null) },
                    onClick = { importM3uLauncherOnline.launch(arrayOf("audio/*")) },
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.import_csv)) },
                    description = stringResource(R.string.import_csv_format),
                    icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                    onClick = { importPlaylistFromCsv.launch(CSV_MIME_TYPES) },
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.external_service)) {
            spotifyImportPreferences(
                state = spotifyState,
                onConnectClick = { showSpotifyLogin = true },
                onRefreshClick = spotifyImportViewModel::loadSources,
                onLogoutClick = {
                    showSpotifySources = false
                    spotifyImportViewModel.logout()
                },
                onSelectClick = { showSpotifySources = true },
                onImportClick = spotifyImportViewModel::importSelectedSources,
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.backup_restore)) },
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
        },
        scrollBehavior = scrollBehavior,
    )

    if (showBackupOptionsDialog) {
        BackupOptionsDialog(
            title = stringResource(R.string.backup_options_title),
            confirmLabel = stringResource(R.string.action_backup),
            onConfirm = { categories ->
                pendingBackupCategories = categories
                showBackupOptionsDialog = false
                val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                backupLauncher.launch(
                    "${context.getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup",
                )
            },
            onDismiss = { showBackupOptionsDialog = false },
        )
    }

    if (showRestoreOptionsDialog) {
        BackupOptionsDialog(
            title = stringResource(R.string.restore_options_title),
            confirmLabel = stringResource(R.string.action_restore),
            onConfirm = { categories ->
                pendingRestoreCategories = categories
                showRestoreOptionsDialog = false
                restoreLauncher.launch(arrayOf("application/octet-stream"))
            },
            onDismiss = { showRestoreOptionsDialog = false },
        )
    }

    if (showSpotifyLogin) {
        SpotifyLoginSheet(
            onDismiss = { showSpotifyLogin = false },
            onCookiesCaptured = { spDc, spKey ->
                showSpotifyLogin = false
                spotifyImportViewModel.connectWithCookies(spDc = spDc, spKey = spKey)
            },
        )
    }

    if (showSpotifySources && spotifyState.isAuthenticated) {
        SpotifySourcePickerSheet(
            state = spotifyState,
            onDismiss = { showSpotifySources = false },
            onToggleSource = spotifyImportViewModel::toggleSource,
            onSelectAll = spotifyImportViewModel::selectAllSources,
            onClearSelection = spotifyImportViewModel::clearSelection,
            onImport = {
                showSpotifySources = false
                spotifyImportViewModel.importSelectedSources()
            },
        )
    }

    spotifyState.summary?.let { summary ->
        SpotifyImportSummaryDialog(
            summary = summary,
            onDismiss = spotifyImportViewModel::dismissSummary,
        )
    }

    spotifyState.errorMessage?.let { error ->
        SpotifyErrorDialog(
            message = error,
            onDismiss = spotifyImportViewModel::dismissError,
        )
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { isProgressStarted = it },
        onPercentageChange = { progressPercentage = it },
        onStatusChange = { progressStatus = it },
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10_000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    val spotifyProgress = spotifyState.progress
    val spotifyProgressTitle = stringResource(R.string.spotify_import_in_progress)
    val spotifyProgressStep =
        spotifyProgress?.let {
            stringResource(
                R.string.spotify_import_progress_step,
                it.sourceTitle,
                it.completedSources,
                it.totalSources,
                it.matchedTracks,
                it.totalTracks,
            )
        }

    LoadingScreen(
        isVisible = backupRestoreProgress != null || isProgressStarted || spotifyProgress != null,
        value = backupRestoreProgress?.percent ?: spotifyProgress?.percent ?: progressPercentage,
        title = backupRestoreProgress?.title ?: if (spotifyProgress != null) spotifyProgressTitle else null,
        stepText = backupRestoreProgress?.step ?: spotifyProgressStep ?: progressStatus,
        indeterminate = backupRestoreProgress?.indeterminate ?: false,
    )
}

private fun PreferenceGroupScope.spotifyImportPreferences(
    state: SpotifyImportUiState,
    onConnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSelectClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    if (!state.isAuthenticated) {
        item {
            PreferenceEntry(
                title = { Text(stringResource(R.string.spotify_connect)) },
                description = stringResource(R.string.spotify_not_connected),
                icon = { Icon(painterResource(R.drawable.spotify_icon), null) },
                trailingContent = {
                    AnimatedVisibility(visible = state.isLoading) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                onClick = onConnectClick,
                isEnabled = state.progress == null,
            )
        }
        return
    }

    item {
        PreferenceEntry(
            title = {
                Text(
                    text = if (state.accountName.isNotBlank()) {
                        stringResource(R.string.spotify_connected_as, state.accountName)
                    } else {
                        stringResource(R.string.spotify_account)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            description = when {
                state.isLoading -> stringResource(R.string.spotify_loading_library)
                state.hasSources -> stringResource(R.string.spotify_available_count, state.sources.size)
                else -> stringResource(R.string.spotify_no_sources)
            },
            icon = { Icon(painterResource(R.drawable.spotify_icon), null) },
            trailingContent = {
                AnimatedVisibility(visible = state.isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            onClick = onSelectClick,
            isEnabled = state.hasSources && state.progress == null,
        )
    }

    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.spotify_select_sources)) },
            description = if (state.hasSources) {
                stringResource(R.string.spotify_available_count, state.sources.size)
            } else {
                stringResource(R.string.spotify_no_sources)
            },
            icon = { Icon(painterResource(R.drawable.playlist_play), null) },
            onClick = onSelectClick,
            isEnabled = state.hasSources && state.progress == null,
        )
    }

    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.spotify_import_selected)) },
            description = stringResource(R.string.spotify_selected_count, state.selectedSourceIds.size),
            icon = { Icon(painterResource(R.drawable.playlist_add), null) },
            onClick = onImportClick,
            isEnabled = state.canImport,
        )
    }

    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.spotify_refresh)) },
            description = stringResource(R.string.spotify_import_desc),
            icon = { Icon(painterResource(R.drawable.sync), null) },
            onClick = onRefreshClick,
            isEnabled = !state.isLoading && state.progress == null,
        )
    }

    item {
        PreferenceEntry(
            title = { Text(stringResource(R.string.action_logout)) },
            icon = { Icon(painterResource(R.drawable.logout), null) },
            onClick = onLogoutClick,
            isEnabled = !state.isLoading && state.progress == null,
        )
    }
}

@Composable
private fun SpotifySourcePickerSheet(
    state: SpotifyImportUiState,
    onDismiss: () -> Unit,
    onToggleSource: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onImport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.spotify_select_sources),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.spotify_selected_count, state.selectedSourceIds.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onClearSelection,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.spotify_clear_selection))
                }
                TextButton(
                    onClick = onSelectAll,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.spotify_select_all))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
            ) {
                items(
                    items = state.sources,
                    key = { it.id },
                    contentType = { it.type },
                ) { source ->
                    SpotifySourceRow(
                        source = source,
                        selected = source.id in state.selectedSourceIds,
                        onClick = { onToggleSource(source.id) },
                    )
                }
            }

            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                enabled = state.canImport,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.spotify_import_selected))
            }
        }
    }
}

@Composable
private fun SpotifySourceRow(
    source: SpotifyImportSourceUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val subtitle = when {
        source.subtitle.isNotBlank() -> source.subtitle
        source.type == SpotifyImportSourceType.LIKED_SONGS -> stringResource(R.string.spotify_liked_songs_desc)
        else -> stringResource(R.string.spotify_account)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SpotifySourceThumbnail(source)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = source.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = source.trackCount?.let { stringResource(R.string.spotify_track_count, it) } ?: subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
            )
        }
    }
}

@Composable
private fun SpotifySourceThumbnail(source: SpotifyImportSourceUi) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!source.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = source.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(
                    if (source.type == SpotifyImportSourceType.LIKED_SONGS) {
                        R.drawable.favorite
                    } else {
                        R.drawable.playlist_play
                    },
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SpotifyLoginSheet(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
            webView = null
        }
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.spotify_login_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.spotify_waiting_for_login),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large),
                factory = { context ->
                    WebView(context).apply {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = object : WebViewClient() {
                            private fun captureCookies(url: String?): Boolean {
                                if (captured) return true
                                val cookies = readSpotifyCookies(cookieManager, url)
                                val spDc = cookies["sp_dc"].orEmpty()
                                if (spDc.isBlank()) return false
                                captured = true
                                cookieManager.flush()
                                onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
                                return true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean = captureCookies(request.url?.toString())

                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: android.graphics.Bitmap?,
                            ) {
                                captureCookies(url)
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                captureCookies(url)
                            }
                        }
                        webView = this
                        loadUrl(SpotifyAuth.LOGIN_URL)
                    }
                },
                update = { view ->
                    webView = view
                },
            )
        }
    }
}

private fun readSpotifyCookies(
    cookieManager: CookieManager,
    currentUrl: String?,
): Map<String, String> {
    val urls = linkedSetOf(
        "https://open.spotify.com",
        "https://accounts.spotify.com",
        "https://spotify.com",
    )
    currentUrl?.toSpotifyCookieOrigin()?.let(urls::add)
    val cookies = linkedMapOf<String, String>()
    cookieManager.flush()
    urls.forEach { url ->
        cookieManager.getCookie(url)
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.forEach { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@forEach
                val key = part.substring(0, separator).trim()
                val value = part.substring(separator + 1).trim()
                if (key.isNotBlank()) {
                    cookies[key] = value
                }
            }
    }
    return cookies
}

private fun String.toSpotifyCookieOrigin(): String? {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    if (host != "spotify.com" && !host.endsWith(".spotify.com")) return null
    val scheme = uri.scheme
        ?.takeIf { it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true) }
        ?: "https"
    return "$scheme://$host"
}

@Composable
private fun SpotifyImportSummaryDialog(
    summary: SpotifyImportSummaryUi,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.spotify_import_complete)) },
        buttons = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(
                    R.string.spotify_import_summary,
                    summary.sourceCount,
                    summary.importedTracks,
                    summary.failedTracks,
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            summary.sources.forEach { source ->
                Text(
                    text = stringResource(
                        R.string.spotify_source_summary,
                        source.title,
                        source.importedTracks,
                        source.totalTracks,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SpotifyErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.import_failed)) },
        buttons = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IconBubble(
    icon: Painter,
    containerColor: Color,
    contentColor: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

@Composable
private fun BackupOptionsDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (Set<BackupCategory>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(BackupCategory.entries.toSet()) }

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(title) },
        buttons = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(
                onClick = { onConfirm(selected) },
                shapes = ButtonDefaults.shapes(),
                enabled = selected.isNotEmpty(),
            ) {
                Text(confirmLabel)
            }
        },
    ) {
        Spacer(Modifier.height(8.dp))
        BackupCategory.entries.forEach { category ->
            val isChecked = category in selected
            val labelRes = when (category) {
                BackupCategory.LIBRARY -> R.string.backup_category_library
                BackupCategory.ACCOUNT -> R.string.backup_category_account
                BackupCategory.SETTINGS -> R.string.backup_category_settings
            }
            val descRes = when (category) {
                BackupCategory.LIBRARY -> R.string.backup_category_library_desc
                BackupCategory.ACCOUNT -> R.string.backup_category_account_desc
                BackupCategory.SETTINGS -> R.string.backup_category_settings_desc
            }
            val iconRes = when (category) {
                BackupCategory.LIBRARY -> R.drawable.library_music
                BackupCategory.ACCOUNT -> R.drawable.account
                BackupCategory.SETTINGS -> R.drawable.settings
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                onClick = {
                    selected = if (isChecked) selected - category else selected + category
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconBubble(
                        icon = painterResource(iconRes),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        size = 40.dp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + category else selected - category
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

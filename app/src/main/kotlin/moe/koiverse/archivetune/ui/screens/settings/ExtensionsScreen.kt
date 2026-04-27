package moe.koiverse.archivetune.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.GithubExtensionSource
import moe.koiverse.archivetune.extensions.system.InstalledExtension
import moe.koiverse.archivetune.extensions.system.ExtensionManager
import moe.koiverse.archivetune.extensions.system.ExtensionManifest
import moe.koiverse.archivetune.ui.component.IconButton as M3IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.viewmodels.ExtensionGithubViewModel
import moe.koiverse.archivetune.viewmodels.GithubImportState
import moe.koiverse.archivetune.viewmodels.GithubUpdateState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context,
        ExtensionManagerEntryPoint::class.java
    )
    val manager = entryPoint.extensionManager()
    val extensions by manager.installed.collectAsState(emptyList())
    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var errorMessageToShow by remember { mutableStateOf<String?>(null) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    // GitHub ViewModel
    val githubViewModel: ExtensionGithubViewModel = hiltViewModel()
    val githubSources by githubViewModel.sources.collectAsState()
    val importState by githubViewModel.importState.collectAsState()
    val updateState by githubViewModel.updateState.collectAsState()
    var showGithubImportDialog by remember { mutableStateOf(false) }

    // Show snackbar for import/update results
    LaunchedEffect(importState) {
        when (val s = importState) {
            is GithubImportState.Success -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.extension_github_success, s.extensionId, s.tag)
                )
                githubViewModel.resetImportState()
            }
            is GithubImportState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                githubViewModel.resetImportState()
            }
            else -> {}
        }
    }
    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is GithubUpdateState.Done -> {
                val msg = if (s.updated == 0 && s.failed == 0) {
                    context.getString(R.string.extension_github_up_to_date)
                } else {
                    context.getString(R.string.extension_github_update_done, s.updated, s.failed)
                }
                snackbarHostState.showSnackbar(msg)
                githubViewModel.resetUpdateState()
            }
            is GithubUpdateState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                githubViewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    val filteredExtensions =
        remember(extensions, query.text) {
            val q = query.text.trim()
            if (q.isBlank()) {
                extensions
            } else {
                extensions.filter { ext ->
                    ext.manifest.name.contains(q, ignoreCase = true) ||
                        ext.manifest.id.contains(q, ignoreCase = true) ||
                        ext.manifest.author.contains(q, ignoreCase = true) ||
                        (ext.manifest.description?.contains(q, ignoreCase = true) == true)
                }
            }
        }
    
    val installLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val result = managerInstallFromDevice(manager, uri)
        if (result.isSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.extension_installed_success))
            }
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: context.getString(R.string.extension_unknown_error)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.extension_install_failed),
                    actionLabel = context.getString(R.string.extension_action_view),
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    errorMessageToShow = errorMessage
                }
            }
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    androidx.activity.compose.BackHandler(enabled = isSearching) {
        isSearching = false
        query = TextFieldValue()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Text(stringResource(R.string.extensions))
                    }
                },
                navigationIcon = {
                    M3IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = navController::backToMain
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(
                            onClick = {
                                if (query.text.isNotEmpty()) {
                                    query = TextFieldValue()
                                } else {
                                    isSearching = false
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Close, null)
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Filled.Search, null)
                        }
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(painterResource(R.drawable.more_vert), null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.install_from_device)) },
                            onClick = {
                                menuExpanded = false
                                installLauncher.launch(arrayOf("application/zip"))
                            },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.restore), null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.extension_github_import)) },
                            onClick = {
                                menuExpanded = false
                                showGithubImportDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Code, null)
                            }
                        )
                        if (githubSources.isNotEmpty()) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    val checking = updateState is GithubUpdateState.Checking
                                    Text(
                                        if (checking) stringResource(R.string.extension_github_checking)
                                        else stringResource(R.string.extension_github_check_updates)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    githubViewModel.checkAllUpdates()
                                },
                                enabled = updateState !is GithubUpdateState.Checking &&
                                    updateState !is GithubUpdateState.Updating,
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.update), null)
                                }
                            )
                            val updatesAvailable = githubSources.count { it.updateAvailable }
                            if (updatesAvailable > 0) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.extension_github_update_all)) },
                                    onClick = {
                                        menuExpanded = false
                                        githubViewModel.updateAllAvailable()
                                    },
                                    enabled = updateState is GithubUpdateState.Idle,
                                    leadingIcon = {
                                        BadgedBox(badge = {
                                            Badge { Text("$updatesAvailable") }
                                        }) {
                                            Icon(painterResource(R.drawable.update), null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (errorMessageToShow != null) {
            ErrorDialog(
                errorMessage = errorMessageToShow!!, 
                onDismiss = { errorMessageToShow = null }
            )
        }
        if (showGithubImportDialog) {
            GithubImportDialog(
                importState = importState,
                onImport = { url, asset -> githubViewModel.installFromGithub(url, asset.ifBlank { null }) },
                onDismiss = {
                    showGithubImportDialog = false
                    githubViewModel.resetImportState()
                }
            )
        }
        // Update progress overlay
        val updatingId = (updateState as? GithubUpdateState.Updating)?.extensionId
        if (updatingId != null) {
            val name = extensions.firstOrNull { it.manifest.id == updatingId }?.manifest?.name ?: updatingId
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.extension_github_update)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.extension_github_updating, name))
                    }
                },
                confirmButton = {}
            )
        }
        if (extensions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ExtensionWarningBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val visible = remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible.value = true }
                        AnimatedVisibility(
                            visible = visible.value,
                            enter = fadeIn(tween(350))
                        ) {
                            Image(
                                painter = painterResource(R.drawable.anime_blank),
                                contentDescription = null,
                                modifier = Modifier.height(140.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_extension_installed),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.extension_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        } else if (filteredExtensions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_results_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ExtensionWarningBanner()
                }
                items(filteredExtensions, key = { it.manifest.id }) { ext ->
                    val githubSource = githubSources.firstOrNull { it.extensionId == ext.manifest.id }
                    ExtensionItemCard(
                        extension = ext,
                        githubSource = githubSource,
                        onSettingsClick = { navController.navigate("settings/extension/${ext.manifest.id}") },
                        onEnableChange = { enabled -> if (enabled) manager.enable(ext.manifest.id) else manager.disable(ext.manifest.id) },
                        onUpdateClick = { githubViewModel.updateExtension(ext.manifest.id) },
                        onDeleteClick = {
                            val result = manager.delete(ext.manifest.id)
                            if (result.isSuccess) {
                                githubViewModel.removeTracking(ext.manifest.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.extension_deleted_success))
                                }
                            } else {
                                val errorMessage = result.exceptionOrNull()?.message ?: context.getString(R.string.extension_unknown_error)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.extension_delete_failed),
                                        actionLabel = context.getString(R.string.extension_action_view),
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        errorMessageToShow = errorMessage
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtensionWarningBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.experiment),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.extension_warning_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.extension_warning_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ExtensionItemCard(
    extension: InstalledExtension,
    githubSource: GithubExtensionSource?,
    onSettingsClick: () -> Unit,
    onEnableChange: (Boolean) -> Unit,
    onUpdateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val manifest = extension.manifest
    var expanded by remember { mutableStateOf(false) }
    val bannerFile = extension.dir.resolve(manifest.banner ?: "banner.png")
    val iconFile = extension.dir.resolve(manifest.icon ?: "icon.png")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            if (bannerFile.exists()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(bannerFile).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainerLow)))
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(iconFile).build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.integration),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = manifest.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (manifest.beta) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                        Text(stringResource(R.string.extension_badge_beta), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (manifest.experimental) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Icon(Icons.Default.Science, null, modifier = Modifier.size(10.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(stringResource(R.string.extension_badge_exp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (githubSource?.updateAvailable == true) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Icon(painterResource(R.drawable.update), null, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "v${manifest.version}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                manifest.author.takeIf { it.isNotBlank() }?.let {
                                    Text(" • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(2.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            manifest.category?.let {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(it, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                    Switch(checked = extension.enabled, onCheckedChange = onEnableChange)
                }
                manifest.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        manifest.website?.takeIf { it.isNotBlank() }?.let { url ->
                            FilledTonalIconButton(
                                onClick = { uriHandler.openUri(url) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Language, stringResource(R.string.extension_cd_website), modifier = Modifier.size(18.dp))
                            }
                        }
                        manifest.repository?.takeIf { it.isNotBlank() }?.let { url ->
                            FilledTonalIconButton(
                                onClick = { uriHandler.openUri(url) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Code, stringResource(R.string.extension_cd_repository), modifier = Modifier.size(18.dp))
                            }
                        }
                        if (manifest.allowSettings) {
                            FilledTonalIconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Settings, stringResource(R.string.extension_cd_settings), modifier = Modifier.size(18.dp))
                            }
                        }
                        if (githubSource?.updateAvailable == true) {
                            FilledTonalIconButton(
                                onClick = onUpdateClick,
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    painterResource(R.drawable.update),
                                    stringResource(R.string.extension_github_update),
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        FilledTonalIconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(Icons.Default.Delete, stringResource(R.string.extension_cd_delete), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, stringResource(R.string.extension_cd_expand), modifier = Modifier.size(18.dp))
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                        ExtensionDetailGrid(extension = extension, githubSource = githubSource)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtensionDetailGrid(extension: InstalledExtension, githubSource: GithubExtensionSource? = null) {
    val manifest = extension.manifest
    val extensionDir = extension.dir
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExtensionDetailChip(label = stringResource(R.string.extension_detail_id), value = manifest.id)
            ExtensionDetailChip(label = stringResource(R.string.extension_detail_version), value = manifest.version)
            manifest.author.takeIf { it.isNotBlank() }?.let { ExtensionDetailChip(label = stringResource(R.string.extension_detail_author), value = it) }
            manifest.license?.takeIf { it.isNotBlank() }?.let { ExtensionDetailChip(label = stringResource(R.string.extension_detail_license), value = it) }
            manifest.category?.let { ExtensionDetailChip(label = stringResource(R.string.extension_detail_category), value = it) }
        }
        if (manifest.minAppVersion != null || manifest.maxAppVersion != null) {
            Text(stringResource(R.string.extension_detail_app_version_compat), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                manifest.minAppVersion?.let { ExtensionDetailChip(label = stringResource(R.string.extension_detail_min), value = it) }
                manifest.maxAppVersion?.let { ExtensionDetailChip(label = stringResource(R.string.extension_detail_max), value = it) }
            }
        }
        if (manifest.permissions.isNotEmpty()) {
            Text(stringResource(R.string.extension_detail_permissions, manifest.permissions.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                manifest.permissions.take(8).forEach { perm ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(perm, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
                if (manifest.permissions.size > 8) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.extension_detail_more, manifest.permissions.size - 8), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
        if (manifest.tags.isNotEmpty()) {
            Text(stringResource(R.string.extension_detail_tags), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                manifest.tags.forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (manifest.settings.isNotEmpty()) {
                Text(stringResource(R.string.extension_detail_n_settings, manifest.settings.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (manifest.hooks.isNotEmpty()) {
                Text(stringResource(R.string.extension_detail_n_hooks, manifest.hooks.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (manifest.uiRoutes.isNotEmpty()) {
                Text(stringResource(R.string.extension_detail_n_ui_routes, manifest.uiRoutes.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // GitHub source info
        if (githubSource != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Code,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.extension_github_tracked),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExtensionDetailChip(
                    label = "${githubSource.owner}/${githubSource.repo}",
                    value = ""
                )
                githubSource.installedTag?.let {
                    ExtensionDetailChip(
                        label = stringResource(R.string.extension_github_installed_tag, it),
                        value = ""
                    )
                }
                if (githubSource.updateAvailable && githubSource.latestTag != null) {
                    ExtensionDetailChip(
                        label = stringResource(R.string.extension_github_update_available, githubSource.latestTag),
                        value = ""
                    )
                }
                val lastChecked = if (githubSource.lastCheckedAt == 0L) {
                    stringResource(R.string.extension_github_never_checked)
                } else {
                    stringResource(
                        R.string.extension_github_last_checked,
                        dateFormat.format(Date(githubSource.lastCheckedAt))
                    )
                }
                Text(
                    lastChecked,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExtensionDetailChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GithubImportDialog(
    importState: GithubImportState,
    onImport: (url: String, assetPattern: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var assetPattern by remember { mutableStateOf("") }
    val isLoading = importState is GithubImportState.Validating ||
        importState is GithubImportState.Downloading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.extension_github_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.extension_github_url_hint)) },
                    placeholder = { Text(stringResource(R.string.extension_github_url_example), style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Code, null) },
                )
                OutlinedTextField(
                    value = assetPattern,
                    onValueChange = { assetPattern = it },
                    label = { Text(stringResource(R.string.extension_github_asset_hint)) },
                    placeholder = { Text(stringResource(R.string.extension_github_asset_example), style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(visible = isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = when (importState) {
                                is GithubImportState.Validating -> stringResource(R.string.extension_github_validating)
                                is GithubImportState.Downloading -> stringResource(R.string.extension_github_downloading)
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(url.trim(), assetPattern.trim()) },
                enabled = url.isNotBlank() && !isLoading
            ) {
                Text(stringResource(R.string.extension_github_import_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

private fun managerInstallFromDevice(
    manager: ExtensionManager,
    uri: Uri
): Result<Unit> = manager.installFromZip(uri)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.extension_error_dialog_title)) },
        text = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.extension_error_dialog_body))
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok_button))
            }
        }
    )
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dagger.hilt.android.EntryPointAccessors
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.InstalledExtension
import moe.koiverse.archivetune.extensions.system.ExtensionManager
import moe.koiverse.archivetune.extensions.system.ExtensionManifest
import moe.koiverse.archivetune.ui.component.IconButton as M3IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import java.io.File

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
    var errorMessageToShow by remember { mutableStateOf<String?>(null) }
    
    val installLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val result = managerInstallFromDevice(manager, uri)
        if (result.isSuccess) {
            snackbarHostState.showSnackbar("Extension installed successfully!")
        } else {
            val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
            snackbarHostState.showSnackbar(
                message = "Installation failed",
                actionLabel = "View",
                duration = SnackbarDuration.Long
            ) {
                errorMessageToShow = errorMessage
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.extensions)) },
                navigationIcon = {
                    M3IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                actions = {
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
        if (extensions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { installLauncher.launch(arrayOf("application/zip")) }
                    ) {
                        Icon(painterResource(R.drawable.add), null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_extension))
                    }
                }
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
                items(extensions, key = { it.manifest.id }) { ext ->
                    ExtensionItemCard(
                        extension = ext,
                        onSettingsClick = { navController.navigate("settings/extension/${ext.manifest.id}") },
                        onEnableChange = { enabled -> if (enabled) manager.enable(ext.manifest.id) else manager.disable(ext.manifest.id) },
                        onDeleteClick = {
                            val result = manager.delete(ext.manifest.id)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("Extension deleted successfully!")
                            } else {
                                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                snackbarHostState.showSnackbar(
                                    message = "Delete failed",
                                    actionLabel = "View",
                                    duration = SnackbarDuration.Long
                                ) {
                                    errorMessageToShow = errorMessage
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
private fun ExtensionItemCard(
    extension: InstalledExtension,
    onSettingsClick: () -> Unit,
    onEnableChange: (Boolean) -> Unit,
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
                                        Text("BETA", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (manifest.experimental) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Icon(Icons.Default.Science, null, modifier = Modifier.size(10.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("EXP", style = MaterialTheme.typography.labelSmall)
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
                                Icon(Icons.Default.Language, "Website", modifier = Modifier.size(18.dp))
                            }
                        }
                        manifest.repository?.takeIf { it.isNotBlank() }?.let { url ->
                            FilledTonalIconButton(
                                onClick = { uriHandler.openUri(url) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Code, "Repository", modifier = Modifier.size(18.dp))
                            }
                        }
                        if (manifest.allowSettings) {
                            FilledTonalIconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Settings, "Settings", modifier = Modifier.size(18.dp))
                            }
                        }
                        FilledTonalIconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expand", modifier = Modifier.size(18.dp))
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                        ExtensionDetailGrid(extension = extension)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtensionDetailGrid(extension: InstalledExtension) {
    val manifest = extension.manifest
    val extensionDir = extension.dir
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExtensionDetailChip(label = "ID", value = manifest.id)
            ExtensionDetailChip(label = "Version", value = manifest.version)
            manifest.author.takeIf { it.isNotBlank() }?.let { ExtensionDetailChip(label = "Author", value = it) }
            manifest.license?.takeIf { it.isNotBlank() }?.let { ExtensionDetailChip(label = "License", value = it) }
            manifest.category?.let { ExtensionDetailChip(label = "Category", value = it) }
        }
        if (manifest.minAppVersion != null || manifest.maxAppVersion != null) {
            Text("App Version Compatibility", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                manifest.minAppVersion?.let { ExtensionDetailChip(label = "Min", value = it) }
                manifest.maxAppVersion?.let { ExtensionDetailChip(label = "Max", value = it) }
            }
        }
        if (manifest.permissions.isNotEmpty()) {
            Text("Permissions (${manifest.permissions.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                        label = { Text("+${manifest.permissions.size - 8} more", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
        if (manifest.tags.isNotEmpty()) {
            Text("Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                Text("${manifest.settings.size} settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (manifest.hooks.isNotEmpty()) {
                Text("${manifest.hooks.size} hooks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (manifest.uiRoutes.isNotEmpty()) {
                Text("${manifest.uiRoutes.size} UI routes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun managerInstallFromDevice(
    manager: ExtensionManager,
    uri: Uri
): Result<Unit> = manager.installFromZip(uri)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error Details") },
        text = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("An error occurred:")
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
                Text("OK")
            }
        }
    )
}

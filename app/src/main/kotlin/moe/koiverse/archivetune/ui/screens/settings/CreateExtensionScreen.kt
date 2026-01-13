package moe.koiverse.archivetune.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ExtensionContextAction
import moe.koiverse.archivetune.extensions.system.ExtensionFeaturePatch
import moe.koiverse.archivetune.extensions.system.ExtensionHook
import moe.koiverse.archivetune.extensions.system.ExtensionManifest
import moe.koiverse.archivetune.extensions.system.ExtensionMenuEntry
import moe.koiverse.archivetune.extensions.system.ExtensionPermission
import moe.koiverse.archivetune.extensions.system.ExtensionSettingsPage
import moe.koiverse.archivetune.extensions.system.ExtensionThemePatch
import moe.koiverse.archivetune.extensions.system.ExtensionUIRoute
import moe.koiverse.archivetune.extensions.system.PermissionGroups
import moe.koiverse.archivetune.extensions.system.PermissionRegistry
import moe.koiverse.archivetune.extensions.system.SettingAction
import moe.koiverse.archivetune.extensions.system.SettingCondition
import moe.koiverse.archivetune.extensions.system.SettingDefinition
import moe.koiverse.archivetune.extensions.system.SettingStyle
import moe.koiverse.archivetune.extensions.system.SettingType
import moe.koiverse.archivetune.extensions.system.SettingValidation
import moe.koiverse.archivetune.extensions.system.SettingVisibility
import moe.koiverse.archivetune.ui.component.IconButton as M3IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExtensionBuilderState(
    var id: String = "",
    var name: String = "",
    var version: String = "1.0.0",
    var author: String = "",
    var description: String = "",
    var website: String = "",
    var repository: String = "",
    var license: String = "MIT",
    var minAppVersion: String = "",
    var maxAppVersion: String = "",
    var category: String = "Utility",
    var tags: MutableList<String> = mutableListOf(),
    var allowSettings: Boolean = true,
    var autoEnable: Boolean = false,
    var hidden: Boolean = false,
    var beta: Boolean = false,
    var experimental: Boolean = false,
    var permissions: MutableList<String> = mutableListOf(),
    var settings: MutableList<SettingBuilderItem> = mutableListOf(),
    var settingsPages: MutableList<SettingsPageBuilderItem> = mutableListOf(),
    var uiRoutes: MutableList<UIRouteBuilderItem> = mutableListOf(),
    var featurePatches: MutableList<FeaturePatchBuilderItem> = mutableListOf(),
    var hooks: MutableList<HookBuilderItem> = mutableListOf(),
    var themePatches: MutableList<ThemePatchBuilderItem> = mutableListOf(),
    var menuEntries: MutableList<MenuEntryBuilderItem> = mutableListOf(),
    var contextActions: MutableList<ContextActionBuilderItem> = mutableListOf(),
    var entryCode: String = DEFAULT_ENTRY_CODE,
    var iconUri: String = "",
    var bannerUri: String = ""
)

data class SettingBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var key: String = "",
    var type: SettingType = SettingType.toggle,
    var label: String = "",
    var description: String = "",
    var icon: String = "",
    var placeholder: String = "",
    var defaultBoolean: Boolean = false,
    var defaultNumber: Int = 0,
    var defaultString: String = "",
    var options: MutableList<String> = mutableListOf(),
    var min: Int = 0,
    var max: Int = 100,
    var step: Int = 1,
    var category: String = "",
    var order: Int = 0,
    var restartRequired: Boolean = false,
    var experimental: Boolean = false,
    var deprecated: Boolean = false
)

data class SettingsPageBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var pageId: String = "",
    var title: String = "",
    var icon: String = "",
    var description: String = "",
    var order: Int = 0,
    var showInMainSettings: Boolean = false,
    var settings: MutableList<SettingBuilderItem> = mutableListOf()
)

data class UIRouteBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var route: String = "",
    var mode: String = "overlay",
    var position: String = "content",
    var priority: Int = 0
)

data class FeaturePatchBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var feature: String = "",
    var action: String = "modify",
    var target: String = "",
    var value: String = "",
    var priority: Int = 0,
    var condition: String = ""
)

data class HookBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var event: String = "",
    var handler: String = "",
    var priority: Int = 0,
    var async: Boolean = false
)

data class ThemePatchBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var target: String = "",
    var property: String = "",
    var value: String = "",
    var mode: String = "light"
)

data class MenuEntryBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var menuId: String = "",
    var label: String = "",
    var icon: String = "",
    var route: String = "",
    var action: String = "",
    var position: String = "bottom",
    var order: Int = 0,
    var showWhen: String = ""
)

data class ContextActionBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    var actionId: String = "",
    var label: String = "",
    var icon: String = "",
    var action: String = "",
    var context: MutableList<String> = mutableListOf(),
    var showWhen: String = ""
)

private const val DEFAULT_ENTRY_CODE = """function onLoad(api) {
    api.log("Extension loaded!");
}

function onUnload() {
    // Cleanup code here
}

function onTrackPlay(track) {
    // Called when a track starts playing
}

function onQueueBuild(queue) {
    // Called when queue is built
}"""

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateExtensionScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    var currentTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Basic", "Permissions", "Settings", "UI Routes", "Hooks", "Theme", "Menu", "Code", "Preview")

    val builderState = remember { mutableStateOf(ExtensionBuilderState()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    var showAddSettingDialog by remember { mutableStateOf(false) }
    var showAddUIRouteDialog by remember { mutableStateOf(false) }
    var showAddHookDialog by remember { mutableStateOf(false) }
    var showAddThemePatchDialog by remember { mutableStateOf(false) }
    var showAddMenuEntryDialog by remember { mutableStateOf(false) }
    var editingSettingIndex by remember { mutableStateOf<Int?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            scope.launch {
                val result = exportExtension(context, builderState.value, uri)
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Extension exported successfully!")
                } else {
                    snackbarHostState.showSnackbar("Export failed: ${'$'}{result.exceptionOrNull()?.message}")
                }
            }
        }
    }
        
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val result = importExtension(context, uri)
                if (result.isSuccess) {
                    builderState.value = result.getOrThrow()
                    snackbarHostState.showSnackbar("Extension loaded successfully!")
                } else {
                    snackbarHostState.showSnackbar("Failed to load extension: ${'$'}{result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Create Extension")
                        if (builderState.value.name.isNotEmpty()) {
                            Text(builderState.value.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    M3IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch("application/zip") }) { Icon(Icons.Default.Folder, "Import") }
                    IconButton(onClick = { showPreviewSheet = true }) { Icon(Icons.Default.Preview, "Preview") }
                    IconButton(onClick = { showExportDialog = true }) { Icon(Icons.Default.Save, "Export") }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (currentTab) {
                2 -> ExtendedFloatingActionButton(onClick = { showAddSettingDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Setting") })
                3 -> ExtendedFloatingActionButton(onClick = { showAddUIRouteDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add UI Route") })
                4 -> ExtendedFloatingActionButton(onClick = { showAddHookDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Hook") })
                5 -> ExtendedFloatingActionButton(onClick = { showAddThemePatchDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Theme Patch") })
                6 -> ExtendedFloatingActionButton(onClick = { showAddMenuEntryDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Menu Entry") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryScrollableTabRow(selectedTabIndex = currentTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = currentTab == index, onClick = { currentTab = index }, text = { Text(title) }, icon = {
                        Icon(when (index) { 0 -> Icons.Default.Info; 1 -> Icons.Default.Security; 2 -> Icons.Default.Settings; 3 -> Icons.Default.Widgets; 4 -> Icons.Default.Code; 5 -> Icons.Default.Palette; 6 -> Icons.Default.Extension; 7 -> Icons.Default.Code; else -> Icons.Default.Preview }, null)
                    })
                }
            }

            when (currentTab) {
                0 -> BasicInfoTab(builderState.value) { builderState.value = it }
                1 -> PermissionsTab(builderState.value) { builderState.value = it }
                2 -> SettingsTab(builderState.value, onAddClick = { showAddSettingDialog = true }, onEditClick = { editingSettingIndex = it }, onDeleteClick = { index -> builderState.value = builderState.value.copy(settings = builderState.value.settings.toMutableList().apply { removeAt(index) }) })
                3 -> UIRoutesTab(builderState.value, onAddClick = { showAddUIRouteDialog = true }, onDeleteClick = { index -> builderState.value = builderState.value.copy(uiRoutes = builderState.value.uiRoutes.toMutableList().apply { removeAt(index) }) })
                4 -> HooksTab(builderState.value, onAddClick = { showAddHookDialog = true }, onDeleteClick = { index -> builderState.value = builderState.value.copy(hooks = builderState.value.hooks.toMutableList().apply { removeAt(index) }) })
                5 -> ThemePatchesTab(builderState.value, onAddClick = { showAddThemePatchDialog = true }, onDeleteClick = { index -> builderState.value = builderState.value.copy(themePatches = builderState.value.themePatches.toMutableList().apply { removeAt(index) }) })
                6 -> MenuEntriesTab(builderState.value, onAddClick = { showAddMenuEntryDialog = true }, onDeleteClick = { index -> builderState.value = builderState.value.copy(menuEntries = builderState.value.menuEntries.toMutableList().apply { removeAt(index) }) })
                7 -> CodeEditorTab(builderState.value) { builderState.value = it }
                8 -> PreviewTab(builderState.value, clipboardManager, snackbarHostState, scope)
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            state = builderState.value,
            onDismiss = { showExportDialog = false },
            onExportZip = {
                showExportDialog = false
                val filename = "${builderState.value.id.ifEmpty { "extension" }}.zip"
                exportLauncher.launch(filename)
            },
            onCopyManifest = {
                val json = generateManifestJson(builderState.value)
                clipboardManager.setText(AnnotatedString(json))
                scope.launch { snackbarHostState.showSnackbar("Manifest copied to clipboard!") }
                showExportDialog = false
            }
        )
    }

    if (showAddSettingDialog) {
        AddSettingDialog(
            onDismiss = { showAddSettingDialog = false },
            onAdd = { setting ->
                builderState.value = builderState.value.copy(settings = builderState.value.settings.toMutableList().apply { add(setting) })
                showAddSettingDialog = false
            }
        )
    }

    editingSettingIndex?.let { index ->
        EditSettingDialog(
            setting = builderState.value.settings[index],
            onDismiss = { editingSettingIndex = null },
            onSave = { setting ->
                builderState.value = builderState.value.copy(settings = builderState.value.settings.toMutableList().apply { set(index, setting) })
                editingSettingIndex = null
            }
        )
    }

    if (showAddUIRouteDialog) {
        AddUIRouteDialog(
            onDismiss = { showAddUIRouteDialog = false },
            onAdd = { route ->
                builderState.value = builderState.value.copy(uiRoutes = builderState.value.uiRoutes.toMutableList().apply { add(route) })
                showAddUIRouteDialog = false
            }
        )
    }

    if (showAddHookDialog) {
        AddHookDialog(
            onDismiss = { showAddHookDialog = false },
            onAdd = { hook ->
                builderState.value = builderState.value.copy(hooks = builderState.value.hooks.toMutableList().apply { add(hook) })
                showAddHookDialog = false
            }
        )
    }

    if (showAddThemePatchDialog) {
        AddThemePatchDialog(
            onDismiss = { showAddThemePatchDialog = false },
            onAdd = { patch ->
                builderState.value = builderState.value.copy(themePatches = builderState.value.themePatches.toMutableList().apply { add(patch) })
                showAddThemePatchDialog = false
            }
        )
    }

    if (showAddMenuEntryDialog) {
        AddMenuEntryDialog(
            onDismiss = { showAddMenuEntryDialog = false },
            onAdd = { entry ->
                builderState.value = builderState.value.copy(menuEntries = builderState.value.menuEntries.toMutableList().apply { add(entry) })
                showAddMenuEntryDialog = false
            }
        )
    }

    if (showPreviewSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showPreviewSheet = false }, sheetState = sheetState) {
            PreviewSheetContent(builderState.value)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BasicInfoTab(state: ExtensionBuilderState, onStateChange: (ExtensionBuilderState) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionHeader("Extension Identity", Icons.Default.Extension)
        }
        item {
            OutlinedTextField(value = state.id, onValueChange = { onStateChange(state.copy(id = it.lowercase().replace(Regex("[^a-z0-9._-]"), ""))) }, label = { Text("Extension ID *") }, placeholder = { Text("com.example.myextension") }, supportingText = { Text("Unique identifier (lowercase, alphanumeric, dots, dashes)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = state.id.isNotEmpty() && !state.id.matches(Regex("^[a-z0-9._-]{2,64}$")))
        }
        item {
            OutlinedTextField(value = state.name, onValueChange = { onStateChange(state.copy(name = it)) }, label = { Text("Extension Name *") }, placeholder = { Text("My Awesome Extension") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.version, onValueChange = { onStateChange(state.copy(version = it)) }, label = { Text("Version *") }, placeholder = { Text("1.0.0") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = state.author, onValueChange = { onStateChange(state.copy(author = it)) }, label = { Text("Author *") }, placeholder = { Text("Your Name") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        item {
            OutlinedTextField(value = state.description, onValueChange = { onStateChange(state.copy(description = it)) }, label = { Text("Description") }, placeholder = { Text("A brief description of what your extension does...") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
        }
        item {
            SectionHeader("Links & Info", Icons.Default.Info)
        }
        item {
            OutlinedTextField(value = state.website, onValueChange = { onStateChange(state.copy(website = it)) }, label = { Text("Website URL") }, placeholder = { Text("https://example.com") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(value = state.repository, onValueChange = { onStateChange(state.copy(repository = it)) }, label = { Text("Repository URL") }, placeholder = { Text("https://github.com/user/repo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            SectionHeader("Assets", Icons.Default.Folder)
        }
        item {
            AssetPicker("Icon", state.iconUri, "image/*") { uri -> onStateChange(state.copy(iconUri = uri.toString())) }
        }
        item {
            AssetPicker("Banner", state.bannerUri, "image/*") { uri -> onStateChange(state.copy(bannerUri = uri.toString())) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                var licenseExpanded by remember { mutableStateOf(false) }
                val licenses = listOf("MIT", "Apache-2.0", "GPL-3.0", "BSD-3-Clause", "ISC", "MPL-2.0", "Unlicense", "Other")
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = state.license, onValueChange = {}, label = { Text("License") }, readOnly = true, trailingIcon = { IconButton(onClick = { licenseExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DropdownMenu(expanded = licenseExpanded, onDismissRequest = { licenseExpanded = false }) {
                        licenses.forEach { license -> DropdownMenuItem(text = { Text(license) }, onClick = { onStateChange(state.copy(license = license)); licenseExpanded = false }) }
                    }
                }
                var categoryExpanded by remember { mutableStateOf(false) }
                val categories = listOf("Utility", "Enhancement", "Integration", "Lyrics", "Theme", "Player", "Library", "Social", "Developer", "Other")
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = state.category, onValueChange = {}, label = { Text("Category") }, readOnly = true, trailingIcon = { IconButton(onClick = { categoryExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { onStateChange(state.copy(category = cat)); categoryExpanded = false }) }
                    }
                }
            }
        }
        item {
            SectionHeader("Tags", Icons.Default.Widgets)
        }
        item {
            var newTag by remember { mutableStateOf("") }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newTag, onValueChange = { newTag = it }, label = { Text("Add Tag") }, modifier = Modifier.weight(1f), singleLine = true)
                FilledTonalButton(onClick = { if (newTag.isNotBlank() && !state.tags.contains(newTag)) { onStateChange(state.copy(tags = state.tags.toMutableList().apply { add(newTag) })); newTag = "" } }, enabled = newTag.isNotBlank()) { Icon(Icons.Default.Add, null) }
            }
        }
        item {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tags.forEachIndexed { index, tag ->
                    InputChip(selected = false, onClick = {}, label = { Text(tag) }, trailingIcon = { IconButton(onClick = { onStateChange(state.copy(tags = state.tags.toMutableList().apply { removeAt(index) })) }, modifier = Modifier.size(18.dp)) { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) } })
                }
            }
        }
        item {
            SectionHeader("App Version Constraints", Icons.Default.Info)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.minAppVersion, onValueChange = { onStateChange(state.copy(minAppVersion = it)) }, label = { Text("Min App Version") }, placeholder = { Text("1.0.0") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = state.maxAppVersion, onValueChange = { onStateChange(state.copy(maxAppVersion = it)) }, label = { Text("Max App Version") }, placeholder = { Text("") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        item {
            SectionHeader("Flags", Icons.Default.Settings)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlagRow("Allow Settings Page", state.allowSettings) { onStateChange(state.copy(allowSettings = it)) }
                FlagRow("Auto Enable on Install", state.autoEnable) { onStateChange(state.copy(autoEnable = it)) }
                FlagRow("Hidden Extension", state.hidden) { onStateChange(state.copy(hidden = it)) }
                FlagRow("Beta Version", state.beta) { onStateChange(state.copy(beta = it)) }
                FlagRow("Experimental", state.experimental) { onStateChange(state.copy(experimental = it)) }
            }
        }
    }
}

@Composable
private fun FlagRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionsTab(state: ExtensionBuilderState, onStateChange: (ExtensionBuilderState) -> Unit) {
    val permissionGroups = mapOf(
        "Playback" to listOf(ExtensionPermission.PlaybackEvents, ExtensionPermission.PlaybackControl, ExtensionPermission.QueueObserve, ExtensionPermission.QueueModify, ExtensionPermission.QueueReorder),
        "UI" to listOf(ExtensionPermission.UIOverride, ExtensionPermission.UIInject, ExtensionPermission.UITopBar, ExtensionPermission.UIBottomBar, ExtensionPermission.UIFloatingAction, ExtensionPermission.UIContextMenu, ExtensionPermission.UINavigation, ExtensionPermission.UIPlayer, ExtensionPermission.UILyrics, ExtensionPermission.UIQueue, ExtensionPermission.UIHome, ExtensionPermission.UISearch, ExtensionPermission.UISettings),
        "Theme" to listOf(ExtensionPermission.ThemeOverride, ExtensionPermission.ThemeColors, ExtensionPermission.ThemeTypography, ExtensionPermission.ThemeShapes),
        "Storage" to listOf(ExtensionPermission.StorageRead, ExtensionPermission.StorageWrite, ExtensionPermission.StorageCache, ExtensionPermission.StorageDownloads),
        "Library" to listOf(ExtensionPermission.LibraryRead, ExtensionPermission.LibraryWrite, ExtensionPermission.LibraryPlaylists, ExtensionPermission.LibraryHistory, ExtensionPermission.LibraryFavorites),
        "Media" to listOf(ExtensionPermission.MediaMetadata, ExtensionPermission.MediaArtwork, ExtensionPermission.MediaLyrics, ExtensionPermission.MediaDownload),
        "Network" to listOf(ExtensionPermission.NetworkAccess, ExtensionPermission.NetworkInternet, ExtensionPermission.NetworkLocal),
        "Settings" to listOf(ExtensionPermission.SettingsRead, ExtensionPermission.SettingsWrite),
        "Account" to listOf(ExtensionPermission.AccountInfo, ExtensionPermission.AccountSync),
        "System" to listOf(ExtensionPermission.NotificationShow, ExtensionPermission.NotificationMedia, ExtensionPermission.BackgroundService, ExtensionPermission.BackgroundPlayback, ExtensionPermission.WakeLock, ExtensionPermission.Clipboard, ExtensionPermission.SystemInfo, ExtensionPermission.DeviceInfo)
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Selected Permissions: ${state.permissions.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Select only permissions your extension needs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
        }
        permissionGroups.forEach { (groupName, permissions) ->
            item {
                var expanded by remember { mutableStateOf(true) }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(headlineContent = { Text(groupName, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("${permissions.count { state.permissions.contains(it.name) }}/${permissions.size} selected") }, trailingContent = { Row {
                            TextButton(onClick = { val names = permissions.map { it.name }; onStateChange(state.copy(permissions = state.permissions.toMutableList().apply { addAll(names.filter { !contains(it) }) })) }) { Text("All") }
                            TextButton(onClick = { val names = permissions.map { it.name }; onStateChange(state.copy(permissions = state.permissions.toMutableList().apply { removeAll(names.toSet()) })) }) { Text("None") }
                            IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }
                        } }, modifier = Modifier.clickable { expanded = !expanded })
                        AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                permissions.forEach { permission ->
                                    val isSelected = state.permissions.contains(permission.name)
                                    val isDangerous = PermissionRegistry.isDangerous(permission)
                                    Row(modifier = Modifier.fillMaxWidth().clickable { onStateChange(state.copy(permissions = state.permissions.toMutableList().apply { if (isSelected) remove(permission.name) else add(permission.name) })) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isSelected, onCheckedChange = { onStateChange(state.copy(permissions = state.permissions.toMutableList().apply { if (it) add(permission.name) else remove(permission.name) })) })
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(permission.name, style = MaterialTheme.typography.bodyLarge)
                                                if (isDangerous) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Badge(containerColor = MaterialTheme.colorScheme.error) { Text("Dangerous", style = MaterialTheme.typography.labelSmall) }
                                                }
                                            }
                                            PermissionRegistry.getInfo(permission)?.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(state: ExtensionBuilderState, onAddClick: () -> Unit, onEditClick: (Int) -> Unit, onDeleteClick: (Int) -> Unit) {
    if (state.settings.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Settings, title = "No Settings Defined", description = "Add settings to let users customize your extension", buttonText = "Add First Setting", onButtonClick = onAddClick)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Text("${state.settings.size} Setting(s) Defined", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            itemsIndexed(state.settings) { index, setting ->
                SettingItemCard(setting = setting, onEdit = { onEditClick(index) }, onDelete = { onDeleteClick(index) }, onMoveUp = if (index > 0) { { val list = state.settings.toMutableList(); val item = list.removeAt(index); list.add(index - 1, item) } } else null, onMoveDown = if (index < state.settings.size - 1) { { val list = state.settings.toMutableList(); val item = list.removeAt(index); list.add(index + 1, item) } } else null)
            }
        }
    }
}

@Composable
private fun SettingItemCard(setting: SettingBuilderItem, onEdit: () -> Unit, onDelete: () -> Unit, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(setting.label.ifEmpty { setting.key }) },
            supportingContent = {
                Column {
                    Text("Key: ${setting.key}", style = MaterialTheme.typography.bodySmall)
                    Text("Type: ${setting.type.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            leadingContent = {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Text(setting.type.name.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            },
            trailingContent = {
                Row {
                    onMoveUp?.let { IconButton(onClick = it) { Icon(Icons.Default.KeyboardArrowUp, "Move Up") } }
                    onMoveDown?.let { IconButton(onClick = it) { Icon(Icons.Default.KeyboardArrowDown, "Move Down") } }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
}

@Composable
private fun UIRoutesTab(state: ExtensionBuilderState, onAddClick: () -> Unit, onDeleteClick: (Int) -> Unit) {
    if (state.uiRoutes.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Widgets, title = "No UI Routes Defined", description = "Add UI routes to inject custom UI into app screens", buttonText = "Add First UI Route", onButtonClick = onAddClick)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.uiRoutes) { index, route ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text(route.route) }, supportingContent = { Text("Mode: ${route.mode} | Position: ${route.position} | Priority: ${route.priority}") }, trailingContent = { IconButton(onClick = { onDeleteClick(index) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } })
                }
            }
        }
    }
}

@Composable
private fun HooksTab(state: ExtensionBuilderState, onAddClick: () -> Unit, onDeleteClick: (Int) -> Unit) {
    if (state.hooks.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Code, title = "No Hooks Defined", description = "Add hooks to respond to app events", buttonText = "Add First Hook", onButtonClick = onAddClick)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.hooks) { index, hook ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text(hook.event) }, supportingContent = { Text("Handler: ${hook.handler} | Priority: ${hook.priority} | Async: ${hook.async}") }, trailingContent = { IconButton(onClick = { onDeleteClick(index) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } })
                }
            }
        }
    }
}

@Composable
private fun ThemePatchesTab(state: ExtensionBuilderState, onAddClick: () -> Unit, onDeleteClick: (Int) -> Unit) {
    if (state.themePatches.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Palette, title = "No Theme Patches Defined", description = "Add theme patches to customize app appearance", buttonText = "Add First Theme Patch", onButtonClick = onAddClick)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.themePatches) { index, patch ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text("${patch.target}.${patch.property}") }, supportingContent = { Text("Value: ${patch.value} | Mode: ${patch.mode}") }, leadingContent = { Box(modifier = Modifier.size(24.dp).background(if (patch.value.startsWith("#")) try { Color(android.graphics.Color.parseColor(patch.value)) } catch (e: Exception) { MaterialTheme.colorScheme.primary } else MaterialTheme.colorScheme.primary, CircleShape)) }, trailingContent = { IconButton(onClick = { onDeleteClick(index) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } })
                }
            }
        }
    }
}

@Composable
private fun MenuEntriesTab(state: ExtensionBuilderState, onAddClick: () -> Unit, onDeleteClick: (Int) -> Unit) {
    if (state.menuEntries.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Extension, title = "No Menu Entries Defined", description = "Add menu entries to add navigation items", buttonText = "Add First Menu Entry", onButtonClick = onAddClick)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.menuEntries) { index, entry ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text(entry.label) }, supportingContent = { Text("ID: ${entry.menuId} | Position: ${entry.position} | Order: ${entry.order}") }, trailingContent = { IconButton(onClick = { onDeleteClick(index) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } })
                }
            }
        }
    }
}

@Composable
private fun CodeEditorTab(state: ExtensionBuilderState, onStateChange: (ExtensionBuilderState) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Entry Point Code", style = MaterialTheme.typography.titleMedium)
                    Text("JavaScript code that runs when your extension loads", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedTextField(value = state.entryCode, onValueChange = { onStateChange(state.copy(entryCode = it)) }, modifier = Modifier.fillMaxSize().weight(1f), textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp), minLines = 20)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onStateChange(state.copy(entryCode = DEFAULT_ENTRY_CODE)) }) { Text("Reset to Default") }
            OutlinedButton(onClick = { onStateChange(state.copy(entryCode = state.entryCode + "\n\n// New function\nfunction newFunction() {\n    \n}")) }) { Text("Add Function") }
        }
    }
}

@Composable
private fun PreviewTab(state: ExtensionBuilderState, clipboardManager: androidx.compose.ui.platform.ClipboardManager, snackbarHostState: SnackbarHostState, scope: kotlinx.coroutines.CoroutineScope) {
    val json = remember(state) { generateManifestJson(state) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("manifest.json Preview", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { clipboardManager.setText(AnnotatedString(json)); scope.launch { snackbarHostState.showSnackbar("Copied!") } }) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Copy") }
            }
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(text = json, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text("index.js Preview", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(text = state.entryCode, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PreviewSheetContent(state: ExtensionBuilderState) {
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Extension Preview", style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider()
        ListItem(headlineContent = { Text(state.name.ifEmpty { "Untitled Extension" }) }, supportingContent = { Text("v${state.version} by ${state.author.ifEmpty { "Unknown" }}") }, leadingContent = { Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) { Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Extension, null) } } })
        if (state.description.isNotEmpty()) Text(state.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.tags.isNotEmpty()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("ID", state.id.ifEmpty { "Not set" })
                InfoRow("Category", state.category)
                InfoRow("License", state.license)
                InfoRow("Permissions", "${state.permissions.size}")
                InfoRow("Settings", "${state.settings.size}")
                InfoRow("UI Routes", "${state.uiRoutes.size}")
                InfoRow("Hooks", "${state.hooks.size}")
                InfoRow("Icon", if (state.iconUri.isNotEmpty()) "Included" else "None")
                InfoRow("Banner", if (state.bannerUri.isNotEmpty()) "Included" else "None")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AssetPicker(label: String, uri: String, mimeType: String, onUriSelected: (android.net.Uri) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onUriSelected(it) }
    }
    
    Row(modifier = Modifier.fillMaxWidth().clickable { launcher.launch(mimeType) }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (uri.isNotEmpty()) {
            val fileName = try {
                context.contentResolver.query(android.net.Uri.parse(uri), arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else "Selected"
                } ?: "Selected"
            } catch (e: Exception) { "Selected" }
            Text(fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        } else {
            Text("Select", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyStateView(icon: ImageVector, title: String, description: String, buttonText: String, onButtonClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Button(onClick = onButtonClick) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(buttonText) }
        }
    }
}

@Composable
private fun ExportDialog(state: ExtensionBuilderState, onDismiss: () -> Unit, onExportZip: () -> Unit, onCopyManifest: () -> Unit) {
    val errors = validateExtension(state)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Extension") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errors.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Validation Errors:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            errors.forEach { error -> Text("• $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) }
                        }
                    }
                } else {
                    Text("Your extension is ready to export!")
                    Text("The ZIP file will contain:", style = MaterialTheme.typography.bodySmall)
                    Text("• manifest.json", style = MaterialTheme.typography.bodySmall)
                    Text("• index.js", style = MaterialTheme.typography.bodySmall)
                    if (state.iconUri.isNotEmpty()) Text("• icon.png", style = MaterialTheme.typography.bodySmall)
                    if (state.bannerUri.isNotEmpty()) Text("• banner.png", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopyManifest) { Text("Copy JSON") }
                Button(onClick = onExportZip, enabled = errors.isEmpty()) { Text("Export ZIP") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSettingDialog(onDismiss: () -> Unit, onAdd: (SettingBuilderItem) -> Unit) {
    var setting by remember { mutableStateOf(SettingBuilderItem()) }
    var typeExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Setting") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = setting.key, onValueChange = { setting = setting.copy(key = it.lowercase().replace(Regex("[^a-z0-9_]"), "")) }, label = { Text("Key *") }, placeholder = { Text("setting_key") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = setting.label, onValueChange = { setting = setting.copy(label = it) }, label = { Text("Label *") }, placeholder = { Text("Setting Label") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Box {
                    OutlinedTextField(value = setting.type.name, onValueChange = {}, label = { Text("Type") }, readOnly = true, trailingIcon = { IconButton(onClick = { typeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        SettingType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.name) }, onClick = { setting = setting.copy(type = type); typeExpanded = false }) }
                    }
                }
                OutlinedTextField(value = setting.description, onValueChange = { setting = setting.copy(description = it) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                when (setting.type) {
                    SettingType.toggle, SettingType.checkbox -> FlagRow("Default Value", setting.defaultBoolean) { setting = setting.copy(defaultBoolean = it) }
                    SettingType.slider, SettingType.number, SettingType.stepper -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = setting.min.toString(), onValueChange = { setting = setting.copy(min = it.toIntOrNull() ?: 0) }, label = { Text("Min") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = setting.max.toString(), onValueChange = { setting = setting.copy(max = it.toIntOrNull() ?: 100) }, label = { Text("Max") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = setting.defaultNumber.toString(), onValueChange = { setting = setting.copy(defaultNumber = it.toIntOrNull() ?: 0) }, label = { Text("Default") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    SettingType.text, SettingType.password, SettingType.textarea -> OutlinedTextField(value = setting.defaultString, onValueChange = { setting = setting.copy(defaultString = it) }, label = { Text("Default Value") }, modifier = Modifier.fillMaxWidth())
                    SettingType.select, SettingType.radio, SettingType.segmented -> {
                        var optionsText by remember { mutableStateOf(setting.options.joinToString("\n")) }
                        OutlinedTextField(value = optionsText, onValueChange = { optionsText = it; setting = setting.copy(options = it.split("\n").filter { o -> o.isNotBlank() }.toMutableList()) }, label = { Text("Options (one per line)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(setting) }, enabled = setting.key.isNotBlank() && setting.label.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSettingDialog(setting: SettingBuilderItem, onDismiss: () -> Unit, onSave: (SettingBuilderItem) -> Unit) {
    var editedSetting by remember { mutableStateOf(setting) }
    var typeExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Setting") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = editedSetting.key, onValueChange = { editedSetting = editedSetting.copy(key = it) }, label = { Text("Key") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = editedSetting.label, onValueChange = { editedSetting = editedSetting.copy(label = it) }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedTextField(value = editedSetting.type.name, onValueChange = {}, label = { Text("Type") }, readOnly = true, trailingIcon = { IconButton(onClick = { typeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) { SettingType.entries.forEach { type -> DropdownMenuItem(text = { Text(type.name) }, onClick = { editedSetting = editedSetting.copy(type = type); typeExpanded = false }) } }
                }
                OutlinedTextField(value = editedSetting.description, onValueChange = { editedSetting = editedSetting.copy(description = it) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = editedSetting.category, onValueChange = { editedSetting = editedSetting.copy(category = it) }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                FlagRow("Restart Required", editedSetting.restartRequired) { editedSetting = editedSetting.copy(restartRequired = it) }
                FlagRow("Experimental", editedSetting.experimental) { editedSetting = editedSetting.copy(experimental = it) }
            }
        },
        confirmButton = { Button(onClick = { onSave(editedSetting) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddUIRouteDialog(onDismiss: () -> Unit, onAdd: (UIRouteBuilderItem) -> Unit) {
    var route by remember { mutableStateOf(UIRouteBuilderItem()) }
    var modeExpanded by remember { mutableStateOf(false) }
    val modes = listOf("replace", "overlay", "prepend", "append", "wrap", "inject")
    val routes = listOf("home", "search", "library", "player", "queue", "lyrics", "album", "artist", "playlist", "settings", "custom")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add UI Route") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var routeExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(value = route.route, onValueChange = { route = route.copy(route = it) }, label = { Text("Route") }, trailingIcon = { IconButton(onClick = { routeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) { routes.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { route = route.copy(route = r); routeExpanded = false }) } }
                }
                Box {
                    OutlinedTextField(value = route.mode, onValueChange = {}, label = { Text("Mode") }, readOnly = true, trailingIcon = { IconButton(onClick = { modeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) { modes.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { route = route.copy(mode = m); modeExpanded = false }) } }
                }
                OutlinedTextField(value = route.priority.toString(), onValueChange = { route = route.copy(priority = it.toIntOrNull() ?: 0) }, label = { Text("Priority") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { Button(onClick = { onAdd(route) }, enabled = route.route.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddHookDialog(onDismiss: () -> Unit, onAdd: (HookBuilderItem) -> Unit) {
    var hook by remember { mutableStateOf(HookBuilderItem()) }
    val events = listOf("onLoad", "onUnload", "onTrackPlay", "onTrackPause", "onTrackEnd", "onQueueBuild", "onQueueChange", "onVolumeChange", "onSeek", "onError", "onNavigate", "onThemeChange", "onSettingChange")
    var eventExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Hook") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(value = hook.event, onValueChange = { hook = hook.copy(event = it) }, label = { Text("Event") }, trailingIcon = { IconButton(onClick = { eventExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = eventExpanded, onDismissRequest = { eventExpanded = false }) { events.forEach { e -> DropdownMenuItem(text = { Text(e) }, onClick = { hook = hook.copy(event = e); eventExpanded = false }) } }
                }
                OutlinedTextField(value = hook.handler, onValueChange = { hook = hook.copy(handler = it) }, label = { Text("Handler Function") }, placeholder = { Text("handleEvent") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hook.priority.toString(), onValueChange = { hook = hook.copy(priority = it.toIntOrNull() ?: 0) }, label = { Text("Priority") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                FlagRow("Async", hook.async) { hook = hook.copy(async = it) }
            }
        },
        confirmButton = { Button(onClick = { onAdd(hook) }, enabled = hook.event.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddThemePatchDialog(onDismiss: () -> Unit, onAdd: (ThemePatchBuilderItem) -> Unit) {
    var patch by remember { mutableStateOf(ThemePatchBuilderItem()) }
    val targets = listOf("colorScheme", "typography", "shapes")
    val properties = mapOf("colorScheme" to listOf("primary", "secondary", "tertiary", "background", "surface", "error", "onPrimary", "onSecondary", "onBackground", "onSurface"), "typography" to listOf("displayLarge", "displayMedium", "headlineLarge", "headlineMedium", "titleLarge", "bodyLarge", "labelLarge"), "shapes" to listOf("small", "medium", "large", "extraLarge"))
    var targetExpanded by remember { mutableStateOf(false) }
    var propertyExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Theme Patch") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(value = patch.target, onValueChange = { patch = patch.copy(target = it) }, label = { Text("Target") }, trailingIcon = { IconButton(onClick = { targetExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = targetExpanded, onDismissRequest = { targetExpanded = false }) { targets.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { patch = patch.copy(target = t); targetExpanded = false }) } }
                }
                Box {
                    OutlinedTextField(value = patch.property, onValueChange = { patch = patch.copy(property = it) }, label = { Text("Property") }, trailingIcon = { IconButton(onClick = { propertyExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = propertyExpanded, onDismissRequest = { propertyExpanded = false }) { (properties[patch.target] ?: emptyList()).forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { patch = patch.copy(property = p); propertyExpanded = false }) } }
                }
                OutlinedTextField(value = patch.value, onValueChange = { patch = patch.copy(value = it) }, label = { Text("Value") }, placeholder = { Text("#FF5722 or value") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedTextField(value = patch.mode, onValueChange = {}, label = { Text("Mode") }, readOnly = true, trailingIcon = { IconButton(onClick = { modeExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) { listOf("light", "dark", "both").forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { patch = patch.copy(mode = m); modeExpanded = false }) } }
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(patch) }, enabled = patch.target.isNotBlank() && patch.property.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddMenuEntryDialog(onDismiss: () -> Unit, onAdd: (MenuEntryBuilderItem) -> Unit) {
    var entry by remember { mutableStateOf(MenuEntryBuilderItem()) }
    var positionExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Menu Entry") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = entry.menuId, onValueChange = { entry = entry.copy(menuId = it) }, label = { Text("Menu ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = entry.label, onValueChange = { entry = entry.copy(label = it) }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = entry.icon, onValueChange = { entry = entry.copy(icon = it) }, label = { Text("Icon") }, placeholder = { Text("settings, home, etc.") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = entry.route, onValueChange = { entry = entry.copy(route = it) }, label = { Text("Route") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedTextField(value = entry.position, onValueChange = {}, label = { Text("Position") }, readOnly = true, trailingIcon = { IconButton(onClick = { positionExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(expanded = positionExpanded, onDismissRequest = { positionExpanded = false }) { listOf("top", "bottom", "settings", "more").forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { entry = entry.copy(position = p); positionExpanded = false }) } }
                }
                OutlinedTextField(value = entry.order.toString(), onValueChange = { entry = entry.copy(order = it.toIntOrNull() ?: 0) }, label = { Text("Order") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { Button(onClick = { onAdd(entry) }, enabled = entry.menuId.isNotBlank() && entry.label.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun validateExtension(state: ExtensionBuilderState): List<String> {
    val errors = mutableListOf<String>()
    if (state.id.isBlank()) errors.add("Extension ID is required")
    else if (!state.id.matches(Regex("^[a-z0-9._-]{2,64}$"))) errors.add("Invalid extension ID format")
    if (state.name.isBlank()) errors.add("Extension name is required")
    if (state.version.isBlank()) errors.add("Version is required")
    if (state.author.isBlank()) errors.add("Author is required")
    state.settings.forEachIndexed { index, setting ->
        if (setting.key.isBlank()) errors.add("Setting #${index + 1}: Key is required")
        if (setting.label.isBlank()) errors.add("Setting #${index + 1}: Label is required")
    }
    return errors
}

private fun generateManifestJson(state: ExtensionBuilderState): String {
    val json = Json { prettyPrint = true; encodeDefaults = false }
    val manifest = ExtensionManifest(
        id = state.id,
        name = state.name,
        version = state.version,
        author = state.author,
        entry = "index.js",
        description = state.description.takeIf { it.isNotBlank() },
        website = state.website.takeIf { it.isNotBlank() },
        repository = state.repository.takeIf { it.isNotBlank() },
        license = state.license.takeIf { it.isNotBlank() },
        minAppVersion = state.minAppVersion.takeIf { it.isNotBlank() },
        maxAppVersion = state.maxAppVersion.takeIf { it.isNotBlank() },
        category = state.category.takeIf { it.isNotBlank() },
        tags = state.tags.toList(),
        allowSettings = state.allowSettings,
        permissions = state.permissions.toList(),
        settings = state.settings.map { s ->
            SettingDefinition(
                key = s.key,
                type = s.type,
                label = s.label,
                description = s.description.takeIf { it.isNotBlank() },
                icon = s.icon.takeIf { it.isNotBlank() },
                placeholder = s.placeholder.takeIf { it.isNotBlank() },
                defaultBoolean = if (s.type == SettingType.toggle || s.type == SettingType.checkbox) s.defaultBoolean else null,
                defaultNumber = if (s.type in listOf(SettingType.slider, SettingType.number, SettingType.stepper)) s.defaultNumber else null,
                defaultString = if (s.type in listOf(SettingType.text, SettingType.password, SettingType.textarea, SettingType.select)) s.defaultString.takeIf { it.isNotBlank() } else null,
                options = if (s.type in listOf(SettingType.select, SettingType.radio, SettingType.segmented) && s.options.isNotEmpty()) s.options.toList() else null,
                min = if (s.type == SettingType.slider) s.min else null,
                max = if (s.type == SettingType.slider) s.max else null,
                step = if (s.type == SettingType.slider) s.step else null,
                category = s.category.takeIf { it.isNotBlank() },
                order = s.order,
                restartRequired = s.restartRequired,
                experimental = s.experimental,
                deprecated = s.deprecated
            )
        },
        uiRoutes = state.uiRoutes.map { r -> ExtensionUIRoute(route = r.route, mode = r.mode, position = r.position, priority = r.priority) },
        hooks = state.hooks.map { h -> ExtensionHook(event = h.event, handler = h.handler, priority = h.priority, async = h.async) },
        themePatches = state.themePatches.map { p -> ExtensionThemePatch(target = p.target, property = p.property, value = p.value, mode = p.mode) },
        menuEntries = state.menuEntries.map { e -> ExtensionMenuEntry(id = e.menuId, label = e.label, icon = e.icon.takeIf { it.isNotBlank() }, route = e.route.takeIf { it.isNotBlank() }, action = e.action.takeIf { it.isNotBlank() }, position = e.position, order = e.order, showWhen = e.showWhen.takeIf { it.isNotBlank() }) },
        autoEnable = state.autoEnable,
        hidden = state.hidden,
        beta = state.beta,
        experimental = state.experimental,
        icon = if (state.iconUri.isNotEmpty()) "icon.png" else null,
        banner = if (state.bannerUri.isNotEmpty()) "banner.png" else null
    )
    return json.encodeToString(manifest)
}

private fun exportExtension(context: Context, state: ExtensionBuilderState, uri: android.net.Uri): Result<Unit> {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                val manifestJson = generateManifestJson(state)
                zipOut.putNextEntry(ZipEntry("manifest.json"))
                zipOut.write(manifestJson.toByteArray())
                zipOut.closeEntry()
                zipOut.putNextEntry(ZipEntry("index.js"))
                zipOut.write(state.entryCode.toByteArray())
                zipOut.closeEntry()
                
                if (state.iconUri.isNotEmpty()) {
                    try {
                        val iconUri = android.net.Uri.parse(state.iconUri)
                        context.contentResolver.openInputStream(iconUri)?.use { inputStream ->
                            zipOut.putNextEntry(ZipEntry("icon.png"))
                            inputStream.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    } catch (e: Exception) {}
                }
                
                if (state.bannerUri.isNotEmpty()) {
                    try {
                        val bannerUri = android.net.Uri.parse(state.bannerUri)
                        context.contentResolver.openInputStream(bannerUri)?.use { inputStream ->
                            zipOut.putNextEntry(ZipEntry("banner.png"))
                            inputStream.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    } catch (e: Exception) {}
                }
            }
        } ?: throw Exception("Could not open output stream")
    }
}

private fun importExtension(context: Context, uri: android.net.Uri): Result<ExtensionBuilderState> {
    return runCatching {
        val tempDir = File(context.cacheDir, "temp_extension_import")
        tempDir.mkdirs()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                java.util.zip.ZipInputStream(inputStream).use { zipIn ->
                    var entry: java.util.zip.ZipEntry?
                    while (zipIn.nextEntry.also { entry = it } != null) {
                        val file = File(tempDir, entry!!.name)
                        if (entry!!.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { output ->
                                zipIn.copyTo(output)
                            }
                        }
                        zipIn.closeEntry()
                    }
                }
            }
            
            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                throw Exception("manifest.json not found in ZIP file")
            }
            
            val manifestContent = manifestFile.readText()
            val json = Json { ignoreUnknownKeys = true }
            val manifest = json.decodeFromString<ExtensionManifest>(manifestContent)
            
            val jsFile = File(tempDir, "index.js")
            val entryCode = if (jsFile.exists()) {
                jsFile.readText()
            } else {
                DEFAULT_ENTRY_CODE
            }
            
            val state = ExtensionBuilderState(
                id = manifest.id,
                name = manifest.name,
                version = manifest.version,
                author = manifest.author,
                description = manifest.description ?: "",
                website = manifest.website ?: "",
                repository = manifest.repository ?: "",
                license = manifest.license ?: "MIT",
                minAppVersion = manifest.minAppVersion ?: "",
                maxAppVersion = manifest.maxAppVersion ?: "",
                category = manifest.category ?: "Utility",
                tags = manifest.tags.toMutableList(),
                allowSettings = manifest.allowSettings,
                autoEnable = manifest.autoEnable,
                hidden = manifest.hidden,
                beta = manifest.beta,
                experimental = manifest.experimental,
                permissions = manifest.permissions.toMutableList(),
                settings = manifest.settings.map { s ->
                    SettingBuilderItem(
                        key = s.key,
                        type = s.type,
                        label = s.label,
                        description = s.description ?: "",
                        icon = s.icon ?: "",
                        placeholder = s.placeholder ?: "",
                        defaultBoolean = s.defaultBoolean ?: false,
                        defaultNumber = s.defaultNumber ?: 0,
                        defaultString = s.defaultString ?: "",
                        options = s.options?.toMutableList() ?: mutableListOf(),
                        min = s.min ?: 0,
                        max = s.max ?: 100,
                        step = s.step ?: 1,
                        category = s.category ?: "",
                        order = s.order,
                        restartRequired = s.restartRequired,
                        experimental = s.experimental,
                        deprecated = s.deprecated
                    )
                }.toMutableList(),
                uiRoutes = manifest.uiRoutes.map { r ->
                    UIRouteBuilderItem(
                        route = r.route,
                        mode = r.mode,
                        position = r.position,
                        priority = r.priority
                    )
                }.toMutableList(),
                hooks = manifest.hooks.map { h ->
                    HookBuilderItem(
                        event = h.event,
                        handler = h.handler,
                        priority = h.priority,
                        async = h.async
                    )
                }.toMutableList(),
                themePatches = manifest.themePatches.map { p ->
                    ThemePatchBuilderItem(
                        target = p.target,
                        property = p.property,
                        value = p.value,
                        mode = p.mode
                    )
                }.toMutableList(),
                menuEntries = manifest.menuEntries.map { e ->
                    MenuEntryBuilderItem(
                        menuId = e.id,
                        label = e.label,
                        icon = e.icon ?: "",
                        route = e.route ?: "",
                        action = e.action ?: "",
                        position = e.position,
                        order = e.order,
                        showWhen = e.showWhen ?: ""
                    )
                }.toMutableList(),
                contextActions = manifest.contextActions.map { ca ->
                    ContextActionBuilderItem(
                        actionId = ca.id,
                        label = ca.label,
                        icon = ca.icon ?: "",
                        action = ca.action,
                        context = ca.context.toMutableList(),
                        showWhen = ca.showWhen ?: ""
                    )
                }.toMutableList(),
                entryCode = entryCode
            )
            
            state
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
                
package moe.koiverse.archivetune.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ExtensionManager
import moe.koiverse.archivetune.extensions.system.ExtensionSettingsStore
import moe.koiverse.archivetune.extensions.system.InstalledExtension
import moe.koiverse.archivetune.extensions.system.SettingAction
import moe.koiverse.archivetune.extensions.system.SettingCondition
import moe.koiverse.archivetune.extensions.system.SettingDefinition
import moe.koiverse.archivetune.extensions.system.SettingType
import moe.koiverse.archivetune.extensions.system.SettingVisibility
import moe.koiverse.archivetune.extensions.ui.ExtensionsUiContainer
import moe.koiverse.archivetune.ui.component.IconButton as M3IconButton
import moe.koiverse.archivetune.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionSettingsScreen(
    navController: NavController,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    backStackEntry: NavBackStackEntry
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager: ExtensionManager = entryPoint.extensionManager()
    val id = backStackEntry.arguments?.getString("id") ?: ""
    val installed by manager.installed.collectAsState(emptyList())
    val ext = installed.firstOrNull { it.manifest.id == id }

    if (ext == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Extension Settings") },
                    navigationIcon = {
                        M3IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                            Icon(painterResource(R.drawable.arrow_back), null)
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Extension not found", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    val store = remember { ExtensionSettingsStore(context, id) }
    val scope = rememberCoroutineScope()
    val routeKey = "settings_extension_${ext.manifest.id}"
    val settingsValues = remember { mutableStateMapOf<String, Any>() }

    LaunchedEffect(ext) {
        ext.manifest.settings.forEach { s ->
            when (s.type) {
                SettingType.toggle, SettingType.checkbox -> settingsValues[s.key] = store.getBoolean(s.key, s.defaultBoolean ?: false)
                SettingType.slider, SettingType.stepper, SettingType.number -> settingsValues[s.key] = store.getInt(s.key, s.defaultNumber ?: 0)
                SettingType.rating -> settingsValues[s.key] = store.getInt(s.key, s.defaultNumber ?: 0).toFloat()
                else -> settingsValues[s.key] = store.getString(s.key, s.defaultString ?: "")
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ext.manifest.name)
                        ext.manifest.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    M3IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                actions = {
                    if (ext.manifest.experimental) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary) { Text("EXPERIMENTAL") }
                    }
                    if (ext.manifest.beta) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("BETA") }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        ExtensionsUiContainer(routeKey) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (ext.manifest.banner != null) {
                    item {
                        val bannerFile = ext.dir.resolve(ext.manifest.banner)
                        if (bannerFile.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(bannerFile).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                val groupedSettings = ext.manifest.settings.groupBy { it.category ?: "General" }.toSortedMap()
                groupedSettings.forEach { (category, settings) ->
                    if (groupedSettings.size > 1 || category != "General") {
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    items(settings.sortedBy { it.order }) { setting ->
                        val isVisible = evaluateVisibility(setting, settingsValues)
                        AnimatedVisibility(visible = isVisible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            SettingItem(
                                setting = setting,
                                store = store,
                                values = settingsValues,
                                extension = ext,
                                manager = manager,
                                scope = scope,
                                onValueChange = { key, value ->
                                    settingsValues[key] = value
                                    scope.launch {
                                        when (value) {
                                            is Boolean -> store.setBoolean(key, value)
                                            is Int -> store.setInt(key, value)
                                            is Float -> store.setInt(key, value.toInt())
                                            is String -> store.setString(key, value)
                                            is List<*> -> store.setString(key, value.joinToString(","))
                                        }
                                        manager.reload(id)
                                    }
                                },
                                navController = navController
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    ExtensionInfoCard(ext)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingItem(
    setting: SettingDefinition,
    store: ExtensionSettingsStore,
    values: Map<String, Any>,
    extension: InstalledExtension,
    manager: ExtensionManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onValueChange: (String, Any) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val isEnabled = setting.visibility != SettingVisibility.disabled
    val isDeprecated = setting.deprecated

    val content: @Composable () -> Unit = {
        when (setting.type) {
            SettingType.toggle -> ToggleSetting(setting, values, onValueChange, isEnabled)
            SettingType.slider -> SliderSetting(setting, values, onValueChange, isEnabled)
            SettingType.text -> TextSetting(setting, values, onValueChange, isEnabled)
            SettingType.select -> SelectSetting(setting, values, onValueChange, isEnabled)
            SettingType.button -> ButtonSetting(setting, context, navController, extension, manager, scope)
            SettingType.checkbox -> CheckboxSetting(setting, values, onValueChange, isEnabled)
            SettingType.radio -> RadioSetting(setting, values, onValueChange, isEnabled)
            SettingType.multiSelect -> MultiSelectSetting(setting, values, onValueChange, isEnabled)
            SettingType.color -> ColorSetting(setting, values, onValueChange, isEnabled)
            SettingType.date -> DateSetting(setting, values, onValueChange, isEnabled)
            SettingType.time -> TimeSetting(setting, values, onValueChange, isEnabled)
            SettingType.section -> SectionSetting(setting)
            SettingType.divider -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingType.group -> GroupSetting(setting, store, values, extension, manager, scope, onValueChange, navController)
            SettingType.card -> CardSetting(setting, store, values, extension, manager, scope, onValueChange, navController)
            SettingType.image -> ImageSetting(setting, extension)
            SettingType.link -> LinkSetting(setting, context)
            SettingType.password -> PasswordSetting(setting, values, onValueChange, isEnabled)
            SettingType.number -> NumberSetting(setting, values, onValueChange, isEnabled)
            SettingType.textarea -> TextareaSetting(setting, values, onValueChange, isEnabled)
            SettingType.chip -> ChipSetting(setting, values, onValueChange, isEnabled)
            SettingType.chipGroup -> ChipGroupSetting(setting, values, onValueChange, isEnabled)
            SettingType.segmented -> SegmentedSetting(setting, values, onValueChange, isEnabled)
            SettingType.stepper -> StepperSetting(setting, values, onValueChange, isEnabled)
            SettingType.rating -> RatingSetting(setting, values, onValueChange, isEnabled)
            SettingType.progress -> ProgressSetting(setting)
            SettingType.custom -> CustomSetting(setting)
        }
    }

    if (isDeprecated) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deprecated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                setting.deprecationMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                content()
            }
        }
    } else {
        content()
    }
}

@Composable
private fun ToggleSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var checked by remember { mutableStateOf((values[setting.key] as? Boolean) ?: setting.defaultBoolean ?: false) }
    LaunchedEffect(values[setting.key]) { checked = (values[setting.key] as? Boolean) ?: checked }
    ListItem(
        headlineContent = { Text(setting.label) },
        supportingContent = setting.description?.let { { Text(it) } },
        leadingContent = setting.icon?.let { { Icon(painterResource(resolveIcon(it)), null) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (setting.restartRequired) Badge(containerColor = MaterialTheme.colorScheme.tertiary) { Text("Restart") }
                Switch(checked = checked, onCheckedChange = { checked = it; onValueChange(setting.key, it) }, enabled = enabled)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SliderSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    val def = setting.defaultNumber ?: 0
    val min = setting.min ?: 0
    val max = setting.max ?: 100
    val step = setting.step ?: 1
    var value by remember { mutableIntStateOf((values[setting.key] as? Int) ?: def) }
    LaunchedEffect(values[setting.key]) { value = (values[setting.key] as? Int) ?: value }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(setting.label, style = MaterialTheme.typography.titleMedium)
                setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("$value", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.toFloat(), onValueChange = { value = it.toInt() }, onValueChangeFinished = { onValueChange(setting.key, value.coerceIn(min, max)) }, valueRange = min.toFloat()..max.toFloat(), steps = ((max - min) / step).let { if (it > 0) it - 1 else 0 }, enabled = enabled)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("$min", style = MaterialTheme.typography.bodySmall); Text("$max", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun TextSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var text by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: "") }
    LaunchedEffect(values[setting.key]) { text = (values[setting.key] as? String) ?: text }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it; onValueChange(setting.key, it) }, label = { Text(setting.label) }, placeholder = setting.placeholder?.let { { Text(it) } }, supportingText = setting.description?.let { { Text(it) } } ?: setting.hint?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, leadingIcon = setting.icon?.let { { Icon(painterResource(resolveIcon(it)), null) } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    val options = setting.options ?: emptyList()
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: options.firstOrNull().orEmpty()) }
    LaunchedEffect(values[setting.key]) { selected = (values[setting.key] as? String) ?: selected }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(setting.label, style = MaterialTheme.typography.titleMedium)
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(value = setting.optionLabels?.getOrNull(options.indexOf(selected)) ?: selected, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), enabled = enabled)
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, opt ->
                    val label = setting.optionLabels?.getOrNull(index) ?: opt
                    DropdownMenuItem(text = { Text(label) }, onClick = { selected = opt; expanded = false; onValueChange(setting.key, opt) }, leadingIcon = setting.optionIcons?.getOrNull(index)?.let { { Icon(painterResource(resolveIcon(it)), null) } })
                }
            }
        }
    }
}

@Composable
private fun ButtonSetting(setting: SettingDefinition, context: android.content.Context, navController: NavController, extension: InstalledExtension, manager: ExtensionManager, scope: kotlinx.coroutines.CoroutineScope) {
    var showConfirm by remember { mutableStateOf(false) }
    val action = setting.action
    val onClick: () -> Unit = {
        if (action?.confirm == true) showConfirm = true
        else executeAction(action, context, navController, extension, manager, scope)
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Button(onClick = onClick) {
            setting.icon?.let { Icon(painterResource(resolveIcon(it)), null); Spacer(Modifier.width(4.dp)) }
            Text(setting.label)
        }
    }
    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false }, title = { Text("Confirm") }, text = { Text(action?.confirmMessage ?: "Are you sure?") }, confirmButton = { TextButton(onClick = { showConfirm = false; executeAction(action, context, navController, extension, manager, scope) }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } })
    }
}

@Composable
private fun CheckboxSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var checked by remember { mutableStateOf((values[setting.key] as? Boolean) ?: setting.defaultBoolean ?: false) }
    LaunchedEffect(values[setting.key]) { checked = (values[setting.key] as? Boolean) ?: checked }
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { checked = !checked; onValueChange(setting.key, checked) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { checked = it; onValueChange(setting.key, it) }, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.bodyLarge)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun RadioSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    val options = setting.options ?: emptyList()
    var selected by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: options.firstOrNull().orEmpty()) }
    LaunchedEffect(values[setting.key]) { selected = (values[setting.key] as? String) ?: selected }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(setting.label, style = MaterialTheme.typography.titleMedium)
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Column(modifier = Modifier.selectableGroup()) {
            options.forEachIndexed { index, opt ->
                val label = setting.optionLabels?.getOrNull(index) ?: opt
                Row(modifier = Modifier.fillMaxWidth().selectable(selected = selected == opt, onClick = { selected = opt; onValueChange(setting.key, opt) }, role = Role.RadioButton, enabled = enabled).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == opt, onClick = null, enabled = enabled)
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun MultiSelectSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    val options = setting.options ?: emptyList()
    val selectedList = remember { mutableStateListOf<String>().apply { addAll((values[setting.key] as? String)?.split(",")?.filter { it.isNotEmpty() } ?: setting.defaultList ?: emptyList()) } }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(setting.label, style = MaterialTheme.typography.titleMedium)
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        options.forEachIndexed { index, opt ->
            val label = setting.optionLabels?.getOrNull(index) ?: opt
            val isSelected = selectedList.contains(opt)
            Row(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { if (isSelected) selectedList.remove(opt) else selectedList.add(opt); onValueChange(setting.key, selectedList.joinToString(",")) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSelected, onCheckedChange = { if (it) selectedList.add(opt) else selectedList.remove(opt); onValueChange(setting.key, selectedList.joinToString(",")) }, enabled = enabled)
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ColorSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var color by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultColor ?: "#000000") }
    LaunchedEffect(values[setting.key]) { color = (values[setting.key] as? String) ?: color }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(parseColor(color), RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable(enabled = enabled) { })
            Spacer(Modifier.width(8.dp))
            Text(color, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DateSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var dateStr by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: "") }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        OutlinedButton(onClick = { }, enabled = enabled) { Text(dateStr.ifEmpty { "Select Date" }) }
    }
}

@Composable
private fun TimeSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var timeStr by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: "") }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        OutlinedButton(onClick = { }, enabled = enabled) { Text(timeStr.ifEmpty { "Select Time" }) }
    }
}

@Composable
private fun SectionSetting(setting: SettingDefinition) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(setting.sectionTitle ?: setting.label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        setting.sectionSubtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSetting(setting: SettingDefinition, store: ExtensionSettingsStore, values: Map<String, Any>, extension: InstalledExtension, manager: ExtensionManager, scope: kotlinx.coroutines.CoroutineScope, onValueChange: (String, Any) -> Unit, navController: NavController) {
    var expanded by remember { mutableStateOf(!setting.groupCollapsed) }
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(headlineContent = { Text(setting.label, fontWeight = FontWeight.SemiBold) }, supportingContent = setting.description?.let { { Text(it) } }, trailingContent = { IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) } }, modifier = Modifier.clickable { expanded = !expanded })
        AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                setting.children?.forEach { child ->
                    SettingItem(setting = child, store = store, values = values, extension = extension, manager = manager, scope = scope, onValueChange = onValueChange, navController = navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardSetting(setting: SettingDefinition, store: ExtensionSettingsStore, values: Map<String, Any>, extension: InstalledExtension, manager: ExtensionManager, scope: kotlinx.coroutines.CoroutineScope, onValueChange: (String, Any) -> Unit, navController: NavController) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(defaultElevation = (setting.cardElevation ?: 2).dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(8.dp))
            setting.children?.forEach { child ->
                SettingItem(setting = child, store = store, values = values, extension = extension, manager = manager, scope = scope, onValueChange = onValueChange, navController = navController)
            }
        }
    }
}

@Composable
private fun ImageSetting(setting: SettingDefinition, extension: InstalledExtension) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        setting.label.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(8.dp))
        val imagePath = setting.imagePath?.let { extension.dir.resolve(it) }
        val imageUrl = setting.imageUrl
        if (imagePath?.exists() == true) {
            AsyncImage(model = ImageRequest.Builder(context).data(imagePath).build(), contentDescription = null, modifier = Modifier.fillMaxWidth().height((setting.imageHeight ?: 120).dp).clip(RoundedCornerShape(8.dp)))
        } else if (imageUrl != null) {
            AsyncImage(model = ImageRequest.Builder(context).data(imageUrl).build(), contentDescription = null, modifier = Modifier.fillMaxWidth().height((setting.imageHeight ?: 120).dp).clip(RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun LinkSetting(setting: SettingDefinition, context: android.content.Context) {
    ListItem(headlineContent = { Text(setting.linkText ?: setting.label, color = MaterialTheme.colorScheme.primary) }, supportingContent = setting.description?.let { { Text(it) } }, leadingContent = { Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable { setting.linkUrl?.let { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } })
}

@Composable
private fun PasswordSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var text by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: "") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(values[setting.key]) { text = (values[setting.key] as? String) ?: text }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it; onValueChange(setting.key, it) }, label = { Text(setting.label) }, placeholder = setting.placeholder?.let { { Text(it) } }, supportingText = setting.description?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
    }
}

@Composable
private fun NumberSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var text by remember { mutableStateOf(((values[setting.key] as? Int) ?: setting.defaultNumber ?: 0).toString()) }
    LaunchedEffect(values[setting.key]) { text = ((values[setting.key] as? Int) ?: text.toIntOrNull() ?: 0).toString() }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedTextField(value = text, onValueChange = { newText -> text = newText.filter { it.isDigit() || it == '-' }; text.toIntOrNull()?.let { onValueChange(setting.key, it) } }, label = { Text(setting.label) }, placeholder = setting.placeholder?.let { { Text(it) } }, supportingText = setting.description?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
private fun TextareaSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var text by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: "") }
    LaunchedEffect(values[setting.key]) { text = (values[setting.key] as? String) ?: text }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it; onValueChange(setting.key, it) }, label = { Text(setting.label) }, placeholder = setting.placeholder?.let { { Text(it) } }, supportingText = setting.description?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth().height(((setting.lines ?: 4) * 24 + 56).dp), enabled = enabled, minLines = setting.lines ?: 4, maxLines = setting.maxLines ?: 8)
    }
}

@Composable
private fun ChipSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var selected by remember { mutableStateOf((values[setting.key] as? Boolean) ?: setting.chipSelectable) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        FilterChip(selected = selected, onClick = { selected = !selected; onValueChange(setting.key, selected) }, label = { Text(setting.label) }, enabled = enabled)
    }
}

@Composable
private fun ChipGroupSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    val options = setting.options ?: emptyList()
    val selectedList = remember { mutableStateListOf<String>().apply { addAll((values[setting.key] as? String)?.split(",")?.filter { it.isNotEmpty() } ?: setting.defaultList ?: emptyList()) } }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(setting.label, style = MaterialTheme.typography.titleMedium)
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, opt ->
                val label = setting.optionLabels?.getOrNull(index) ?: opt
                val isSelected = selectedList.contains(opt)
                FilterChip(selected = isSelected, onClick = { if (setting.chipMultiple) { if (isSelected) selectedList.remove(opt) else selectedList.add(opt) } else { selectedList.clear(); selectedList.add(opt) }; onValueChange(setting.key, selectedList.joinToString(",")) }, label = { Text(label) }, enabled = enabled)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    val options = setting.options ?: emptyList()
    var selected by remember { mutableStateOf((values[setting.key] as? String) ?: setting.defaultString ?: options.firstOrNull().orEmpty()) }
    LaunchedEffect(values[setting.key]) { selected = (values[setting.key] as? String) ?: selected }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(setting.label, style = MaterialTheme.typography.titleMedium)
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, opt ->
                val label = setting.optionLabels?.getOrNull(index) ?: opt
                SegmentedButton(shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size), onClick = { selected = opt; onValueChange(setting.key, opt) }, selected = selected == opt, enabled = enabled) { Text(label) }
            }
        }
    }
}

@Composable
private fun StepperSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var value by remember { mutableIntStateOf((values[setting.key] as? Int) ?: setting.defaultNumber ?: 0) }
    LaunchedEffect(values[setting.key]) { value = (values[setting.key] as? Int) ?: value }
    val min = setting.min ?: Int.MIN_VALUE
    val max = setting.max ?: Int.MAX_VALUE
    val step = setting.step ?: 1
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { if (value > min) { value -= step; onValueChange(setting.key, value) } }, enabled = enabled && value > min, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Delete, null) }
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            FilledTonalButton(onClick = { if (value < max) { value += step; onValueChange(setting.key, value) } }, enabled = enabled && value < max, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Add, null) }
        }
    }
}

@Composable
private fun RatingSetting(setting: SettingDefinition, values: Map<String, Any>, onValueChange: (String, Any) -> Unit, enabled: Boolean) {
    var value by remember { mutableFloatStateOf((values[setting.key] as? Float) ?: (setting.defaultNumber ?: 0).toFloat()) }
    LaunchedEffect(values[setting.key]) { value = (values[setting.key] as? Float) ?: (values[setting.key] as? Int)?.toFloat() ?: value }
    val max = setting.ratingMax
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(max) { index ->
                val filled = value > index
                Icon(imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size(28.dp).clickable(enabled = enabled) { value = (index + 1).toFloat(); onValueChange(setting.key, value) }, tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ProgressSetting(setting: SettingDefinition) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(setting.label, style = MaterialTheme.typography.titleMedium)
        setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(8.dp))
        if (setting.progressIndeterminate) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        else LinearProgressIndicator(progress = { setting.progressValue ?: 0f }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CustomSetting(setting: SettingDefinition) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(setting.label, style = MaterialTheme.typography.titleMedium)
            setting.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Custom renderer: ${setting.customRenderer ?: "none"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExtensionInfoCard(ext: InstalledExtension) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Extension Info", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            InfoRow("ID", ext.manifest.id)
            InfoRow("Version", ext.manifest.version)
            InfoRow("Author", ext.manifest.author)
            ext.manifest.license?.let { InfoRow("License", it) }
            ext.manifest.website?.let { InfoRow("Website", it) }
            if (ext.manifest.permissions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Permissions", style = MaterialTheme.typography.labelMedium)
                ext.manifest.permissions.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun evaluateVisibility(setting: SettingDefinition, values: Map<String, Any>): Boolean {
    if (setting.visibility == SettingVisibility.hidden) return false
    val condition = setting.condition ?: return true
    val dependsValue = values[condition.dependsOn]
    return when (condition.operator) {
        "equals" -> dependsValue?.toString() == condition.value
        "notEquals" -> dependsValue?.toString() != condition.value
        "contains" -> condition.values?.contains(dependsValue?.toString()) == true
        "notContains" -> condition.values?.contains(dependsValue?.toString()) != true
        "isTrue" -> dependsValue == true
        "isFalse" -> dependsValue == false
        "isEmpty" -> dependsValue?.toString().isNullOrEmpty()
        "isNotEmpty" -> !dependsValue?.toString().isNullOrEmpty()
        "greaterThan" -> (dependsValue as? Number)?.toDouble()?.let { it > (condition.value?.toDoubleOrNull() ?: 0.0) } == true
        "lessThan" -> (dependsValue as? Number)?.toDouble()?.let { it < (condition.value?.toDoubleOrNull() ?: 0.0) } == true
        else -> true
    }
}

private fun executeAction(action: SettingAction?, context: android.content.Context, navController: NavController, extension: InstalledExtension, manager: ExtensionManager, scope: kotlinx.coroutines.CoroutineScope) {
    action ?: return
    when (action.type) {
        "navigate" -> action.route?.let { navController.navigate(it) }
        "url" -> action.url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
        "reload" -> scope.launch { manager.reload(extension.manifest.id) }
        "toast" -> action.payload?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        "reset" -> scope.launch { manager.reload(extension.manifest.id) }
    }
}

private fun parseColor(colorStr: String): Color {
    return try {
        if (colorStr.startsWith("#")) Color(android.graphics.Color.parseColor(colorStr))
        else Color.Gray
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun resolveIcon(name: String): Int {
    return when (name) {
        "add" -> R.drawable.add
        "settings" -> R.drawable.settings
        "restore" -> R.drawable.restore
        "arrow_back" -> R.drawable.arrow_back
        "delete" -> R.drawable.delete
        "more_vert" -> R.drawable.more_vert
        else -> R.drawable.info
    }
}

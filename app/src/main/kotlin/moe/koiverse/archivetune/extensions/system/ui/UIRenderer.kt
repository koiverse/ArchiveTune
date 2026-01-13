package moe.koiverse.archivetune.extensions.system.ui

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.io.File

typealias UIActionHandler = (String) -> Unit
typealias UIValueChangeHandler = (String, Any) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderUI(
    config: UIConfig,
    baseDir: File,
    onAction: UIActionHandler = {},
    onValueChange: UIValueChangeHandler = { _, _ -> },
    values: Map<String, Any> = emptyMap()
) {
    RenderNode(config.root, baseDir, onAction, onValueChange, values)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderNode(
    node: UINode,
    baseDir: File,
    onAction: UIActionHandler,
    onValueChange: UIValueChangeHandler,
    values: Map<String, Any>
) {
    if (node.visible == false) return
    val modifier = node.modifier?.toModifier(onAction, node.action) ?: Modifier

    when (node) {
        is UINode.Column -> {
            Column(
                modifier = modifier,
                verticalArrangement = node.verticalArrangement.toVerticalArrangement(node.spacing),
                horizontalAlignment = node.horizontalAlignment.toHorizontalAlignment()
            ) {
                node.children.forEach { RenderNode(it, baseDir, onAction, onValueChange, values) }
            }
        }
        is UINode.Row -> {
            Row(
                modifier = modifier,
                horizontalArrangement = node.horizontalArrangement.toHorizontalArrangement(node.spacing),
                verticalAlignment = node.verticalAlignment.toVerticalAlignment()
            ) {
                node.children.forEach { RenderNode(it, baseDir, onAction, onValueChange, values) }
            }
        }
        is UINode.Box -> {
            Box(modifier = modifier, contentAlignment = node.contentAlignment.toAlignment()) {
                node.children.forEach { RenderNode(it, baseDir, onAction, onValueChange, values) }
            }
        }
        is UINode.LazyColumn -> {
            LazyColumn(
                modifier = modifier,
                verticalArrangement = node.verticalArrangement.toVerticalArrangement(node.spacing),
                horizontalAlignment = node.horizontalAlignment.toHorizontalAlignment(),
                contentPadding = PaddingValues(node.contentPadding?.dp ?: 0.dp)
            ) {
                items(node.children) { child -> RenderNode(child, baseDir, onAction, onValueChange, values) }
            }
        }
        is UINode.LazyRow -> {
            LazyRow(
                modifier = modifier,
                horizontalArrangement = node.horizontalArrangement.toHorizontalArrangement(node.spacing),
                verticalAlignment = node.verticalAlignment.toVerticalAlignment(),
                contentPadding = PaddingValues(node.contentPadding?.dp ?: 0.dp)
            ) {
                items(node.children) { child -> RenderNode(child, baseDir, onAction, onValueChange, values) }
            }
        }
        is UINode.Card -> {
            Card(
                modifier = modifier,
                elevation = CardDefaults.cardElevation(defaultElevation = (node.elevation ?: 1).dp),
                shape = node.shape.toShape()
            ) {
                node.children.forEach { RenderNode(it, baseDir, onAction, onValueChange, values) }
            }
        }
        is UINode.Surface -> {
            Surface(
                modifier = modifier,
                tonalElevation = (node.elevation ?: 0).dp,
                shape = node.shape.toShape(),
                color = node.color?.toColor() ?: MaterialTheme.colorScheme.surface
            ) {
                node.children.forEach { RenderNode(it, baseDir, onAction, onValueChange, values) }
            }
        }
        is UINode.Text -> {
            Text(
                text = node.text,
                modifier = modifier,
                style = node.style.toTextStyle(),
                maxLines = node.style?.maxLines ?: Int.MAX_VALUE,
                overflow = node.style?.overflow.toTextOverflow()
            )
        }
        is UINode.Image -> {
            val context = LocalContext.current
            val file = baseDir.resolve(node.path)
            val model = ImageRequest.Builder(context).data(file).build()
            val w = node.widthDp?.dp ?: 140.dp
            val h = node.heightDp?.dp ?: 140.dp
            AsyncImage(model = model, contentDescription = node.contentDescription, modifier = modifier.width(w).height(h), contentScale = node.contentScale.toContentScale())
        }
        is UINode.NetworkImage -> {
            val context = LocalContext.current
            val model = ImageRequest.Builder(context).data(node.url).build()
            val w = node.widthDp?.dp ?: 140.dp
            val h = node.heightDp?.dp ?: 140.dp
            AsyncImage(model = model, contentDescription = node.contentDescription, modifier = modifier.width(w).height(h), contentScale = node.contentScale.toContentScale())
        }
        is UINode.Icon -> {
            Icon(imageVector = node.name.toIconVector(), contentDescription = node.contentDescription, modifier = modifier.size((node.size ?: 24).dp), tint = node.tint?.toColor() ?: MaterialTheme.colorScheme.onSurface)
        }
        is UINode.Button -> {
            val buttonModifier = if (node.action != null) modifier.clickable { onAction(node.action) } else modifier
            when (node.style) {
                "outlined" -> OutlinedButton(onClick = { node.action?.let(onAction) }, modifier = buttonModifier, enabled = node.enabled ?: true) {
                    node.icon?.let { Icon(it.toIconVector(), null); Spacer(Modifier.width(6.dp)) }
                    Text(node.text)
                }
                "text" -> TextButton(onClick = { node.action?.let(onAction) }, modifier = buttonModifier, enabled = node.enabled ?: true) {
                    node.icon?.let { Icon(it.toIconVector(), null); Spacer(Modifier.width(6.dp)) }
                    Text(node.text)
                }
                "tonal" -> FilledTonalButton(onClick = { node.action?.let(onAction) }, modifier = buttonModifier, enabled = node.enabled ?: true) {
                    node.icon?.let { Icon(it.toIconVector(), null); Spacer(Modifier.width(6.dp)) }
                    Text(node.text)
                }
                else -> Button(onClick = { node.action?.let(onAction) }, modifier = buttonModifier, enabled = node.enabled ?: true) {
                    node.icon?.let { Icon(it.toIconVector(), null); Spacer(Modifier.width(6.dp)) }
                    Text(node.text)
                }
            }
        }
        is UINode.IconButton -> {
            IconButton(onClick = { node.action?.let(onAction) }, modifier = modifier, enabled = node.enabled ?: true) {
                Icon(imageVector = node.icon.toIconVector(), contentDescription = node.contentDescription, modifier = Modifier.size((node.size ?: 24).dp), tint = node.tint?.toColor() ?: MaterialTheme.colorScheme.onSurface)
            }
        }
        is UINode.FloatingActionButton -> {
            if (node.extended == true && node.text != null) {
                ExtendedFloatingActionButton(onClick = { node.action?.let(onAction) }, modifier = modifier, containerColor = node.containerColor?.toColor() ?: MaterialTheme.colorScheme.primaryContainer, contentColor = node.contentColor?.toColor() ?: MaterialTheme.colorScheme.onPrimaryContainer) {
                    Icon(node.icon.toIconVector(), null); Spacer(Modifier.width(8.dp)); Text(node.text)
                }
            } else {
                FloatingActionButton(onClick = { node.action?.let(onAction) }, modifier = modifier, containerColor = node.containerColor?.toColor() ?: MaterialTheme.colorScheme.primaryContainer, contentColor = node.contentColor?.toColor() ?: MaterialTheme.colorScheme.onPrimaryContainer) {
                    Icon(node.icon.toIconVector(), null)
                }
            }
        }
        is UINode.TextField -> {
            var textValue by remember { mutableStateOf(values[node.key]?.toString() ?: node.value ?: "") }
            TextField(value = textValue, onValueChange = { textValue = it; onValueChange(node.key, it) }, modifier = modifier, label = node.label?.let { { Text(it) } }, placeholder = node.placeholder?.let { { Text(it) } }, leadingIcon = node.leadingIcon?.let { { Icon(it.toIconVector(), null) } }, trailingIcon = node.trailingIcon?.let { { Icon(it.toIconVector(), null) } }, singleLine = node.singleLine ?: false, maxLines = node.maxLines ?: Int.MAX_VALUE, enabled = node.enabled ?: true, readOnly = node.readOnly ?: false, isError = node.isError ?: false, supportingText = node.supportingText?.let { { Text(it) } }, visualTransformation = if (node.visualTransformation == "password") PasswordVisualTransformation() else VisualTransformation.None)
        }
        is UINode.OutlinedTextField -> {
            var textValue by remember { mutableStateOf(values[node.key]?.toString() ?: node.value ?: "") }
            OutlinedTextField(value = textValue, onValueChange = { textValue = it; onValueChange(node.key, it) }, modifier = modifier, label = node.label?.let { { Text(it) } }, placeholder = node.placeholder?.let { { Text(it) } }, leadingIcon = node.leadingIcon?.let { { Icon(it.toIconVector(), null) } }, trailingIcon = node.trailingIcon?.let { { Icon(it.toIconVector(), null) } }, singleLine = node.singleLine ?: false, maxLines = node.maxLines ?: Int.MAX_VALUE, enabled = node.enabled ?: true, readOnly = node.readOnly ?: false, isError = node.isError ?: false, supportingText = node.supportingText?.let { { Text(it) } }, visualTransformation = if (node.visualTransformation == "password") PasswordVisualTransformation() else VisualTransformation.None)
        }
        is UINode.Switch -> {
            var checked by remember { mutableStateOf((values[node.key] as? Boolean) ?: node.checked ?: false) }
            Switch(checked = checked, onCheckedChange = { checked = it; onValueChange(node.key, it); node.action?.let(onAction) }, modifier = modifier, enabled = node.enabled ?: true)
        }
        is UINode.Checkbox -> {
            var checked by remember { mutableStateOf((values[node.key] as? Boolean) ?: node.checked ?: false) }
            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { checked = it; onValueChange(node.key, it); node.action?.let(onAction) }, enabled = node.enabled ?: true)
                node.label?.let { Spacer(Modifier.width(8.dp)); Text(it) }
            }
        }
        is UINode.RadioButton -> {
            var selected by remember { mutableStateOf((values[node.key] as? String) == node.value || node.selected == true) }
            Row(modifier = modifier.selectable(selected = selected, onClick = { selected = true; onValueChange(node.key, node.value); node.action?.let(onAction) }, role = Role.RadioButton), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null, enabled = node.enabled ?: true)
                node.label?.let { Spacer(Modifier.width(8.dp)); Text(it) }
            }
        }
        is UINode.RadioGroup -> {
            var selected by remember { mutableStateOf((values[node.key] as? String) ?: node.selected ?: node.options.firstOrNull()) }
            val isVertical = node.orientation != "horizontal"
            if (isVertical) {
                Column(modifier = modifier.selectableGroup()) {
                    node.options.forEachIndexed { index, option ->
                        val label = node.optionLabels?.getOrNull(index) ?: option
                        Row(modifier = Modifier.fillMaxWidth().selectable(selected = selected == option, onClick = { selected = option; onValueChange(node.key, option); node.action?.let(onAction) }, role = Role.RadioButton).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == option, onClick = null)
                            Spacer(Modifier.width(8.dp)); Text(label)
                        }
                    }
                }
            } else {
                Row(modifier = modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    node.options.forEachIndexed { index, option ->
                        val label = node.optionLabels?.getOrNull(index) ?: option
                        Row(modifier = Modifier.selectable(selected = selected == option, onClick = { selected = option; onValueChange(node.key, option); node.action?.let(onAction) }, role = Role.RadioButton), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == option, onClick = null); Spacer(Modifier.width(4.dp)); Text(label)
                        }
                    }
                }
            }
        }
        is UINode.Slider -> {
            var value by remember { mutableFloatStateOf((values[node.key] as? Float) ?: node.value ?: 0f) }
            Column(modifier = modifier) {
                if (node.showValue == true) Text(text = node.valueFormat?.replace("{value}", value.toString()) ?: value.toString(), style = MaterialTheme.typography.bodySmall)
                Slider(value = value, onValueChange = { value = it }, onValueChangeFinished = { onValueChange(node.key, value); node.action?.let(onAction) }, valueRange = (node.min ?: 0f)..(node.max ?: 100f), steps = node.steps ?: 0, enabled = node.enabled ?: true)
            }
        }
        is UINode.RangeSlider -> {
            var valueStart by remember { mutableFloatStateOf((values["${node.key}_start"] as? Float) ?: node.valueStart ?: 0f) }
            var valueEnd by remember { mutableFloatStateOf((values["${node.key}_end"] as? Float) ?: node.valueEnd ?: 100f) }
            RangeSlider(value = valueStart..valueEnd, onValueChange = { range -> valueStart = range.start; valueEnd = range.endInclusive }, onValueChangeFinished = { onValueChange("${node.key}_start", valueStart); onValueChange("${node.key}_end", valueEnd); node.action?.let(onAction) }, valueRange = (node.min ?: 0f)..(node.max ?: 100f), steps = node.steps ?: 0, modifier = modifier, enabled = node.enabled ?: true)
        }
        is UINode.DropdownMenu -> {
            var expanded by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf((values[node.key] as? String) ?: node.selected ?: node.options.firstOrNull().orEmpty()) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
                TextField(value = node.optionLabels?.getOrNull(node.options.indexOf(selected)) ?: selected, onValueChange = {}, readOnly = true, label = node.label?.let { { Text(it) } }, placeholder = node.placeholder?.let { { Text(it) } }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), enabled = node.enabled ?: true)
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    node.options.forEachIndexed { index, option ->
                        val label = node.optionLabels?.getOrNull(index) ?: option
                        DropdownMenuItem(text = { Text(label) }, onClick = { selected = option; expanded = false; onValueChange(node.key, option); node.action?.let(onAction) })
                    }
                }
            }
        }
        is UINode.Chip -> {
            var isSelected by remember { mutableStateOf(node.selected ?: false) }
            when (node.style) {
                "input" -> InputChip(selected = isSelected, onClick = { isSelected = !isSelected; node.action?.let(onAction) }, label = { Text(node.text) }, modifier = modifier, enabled = node.enabled ?: true, leadingIcon = node.leadingIcon?.let { { Icon(it.toIconVector(), null, Modifier.size(18.dp)) } }, trailingIcon = node.trailingIcon?.let { { Icon(it.toIconVector(), null, Modifier.size(18.dp)) } })
                else -> FilterChip(selected = isSelected, onClick = { isSelected = !isSelected; node.action?.let(onAction) }, label = { Text(node.text) }, modifier = modifier, enabled = node.enabled ?: true, leadingIcon = node.leadingIcon?.let { { Icon(it.toIconVector(), null, Modifier.size(18.dp)) } }, trailingIcon = node.trailingIcon?.let { { Icon(it.toIconVector(), null, Modifier.size(18.dp)) } })
            }
        }
        is UINode.ChipGroup -> {
            val selectedChips = remember { mutableStateListOf<String>().apply { addAll((values[node.key] as? List<*>)?.filterIsInstance<String>() ?: node.selected ?: emptyList()) } }
            Row(modifier = modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                node.chips.forEachIndexed { index, chip ->
                    val label = node.chipLabels?.getOrNull(index) ?: chip
                    val isSelected = selectedChips.contains(chip)
                    FilterChip(selected = isSelected, onClick = { if (node.multiSelect == true) { if (isSelected) selectedChips.remove(chip) else selectedChips.add(chip) } else { selectedChips.clear(); selectedChips.add(chip) }; onValueChange(node.key, selectedChips.toList()); node.action?.let(onAction) }, label = { Text(label) })
                }
            }
        }
        is UINode.ProgressIndicator -> {
            if (node.linear == true) {
                if (node.indeterminate == true) LinearProgressIndicator(modifier = modifier, color = node.color?.toColor() ?: MaterialTheme.colorScheme.primary, trackColor = node.trackColor?.toColor() ?: MaterialTheme.colorScheme.surfaceVariant)
                else LinearProgressIndicator(progress = { node.progress ?: 0f }, modifier = modifier, color = node.color?.toColor() ?: MaterialTheme.colorScheme.primary, trackColor = node.trackColor?.toColor() ?: MaterialTheme.colorScheme.surfaceVariant)
            } else {
                if (node.indeterminate == true) CircularProgressIndicator(modifier = modifier, color = node.color?.toColor() ?: MaterialTheme.colorScheme.primary, trackColor = node.trackColor?.toColor() ?: MaterialTheme.colorScheme.surfaceVariant)
                else CircularProgressIndicator(progress = { node.progress ?: 0f }, modifier = modifier, color = node.color?.toColor() ?: MaterialTheme.colorScheme.primary, trackColor = node.trackColor?.toColor() ?: MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        is UINode.Divider -> HorizontalDivider(modifier = modifier.padding(start = (node.startIndent ?: 0).dp, end = (node.endIndent ?: 0).dp), thickness = (node.thickness ?: 1).dp, color = node.color?.toColor() ?: MaterialTheme.colorScheme.outlineVariant)
        is UINode.Spacer -> {
            val spacerModifier = when { node.width != null && node.height != null -> Modifier.width(node.width.dp).height(node.height.dp); node.width != null -> Modifier.width(node.width.dp); node.height != null -> Modifier.height(node.height.dp); else -> modifier }
            Spacer(spacerModifier)
        }
        is UINode.Badge -> Badge(modifier = modifier, containerColor = node.containerColor?.toColor() ?: MaterialTheme.colorScheme.error, contentColor = node.contentColor?.toColor() ?: MaterialTheme.colorScheme.onError) { node.content?.let { Text(it) } }
        is UINode.ListItem -> ListItem(headlineContent = { Text(node.headlineText) }, modifier = modifier.then(if (node.action != null) Modifier.clickable { onAction(node.action) } else Modifier), supportingContent = node.supportingText?.let { { Text(it) } }, overlineContent = node.overlineText?.let { { Text(it) } }, leadingContent = node.leadingContent?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } }, trailingContent = node.trailingContent?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } })
        is UINode.NavigationItem -> Row(modifier = modifier.fillMaxWidth().clickable { node.action?.let(onAction) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(node.icon.toIconVector(), null); Spacer(Modifier.width(16.dp)); Text(node.label, style = MaterialTheme.typography.bodyLarge); node.badge?.let { Spacer(Modifier.weight(1f)); Badge { Text(it) } } }
        is UINode.TabRow -> {
            var selectedIndex by remember { mutableIntStateOf(node.selectedIndex ?: 0) }
            TabRow(selectedTabIndex = selectedIndex, modifier = modifier) { node.tabs.forEachIndexed { index, tab -> Tab(selected = selectedIndex == index, onClick = { selectedIndex = index; node.action?.let(onAction) }, text = { Text(tab) }, icon = node.tabIcons?.getOrNull(index)?.let { { Icon(it.toIconVector(), null) } }) } }
        }
        is UINode.TopAppBar -> TopAppBar(title = { Column { Text(node.title); node.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) } } }, modifier = modifier, navigationIcon = node.navigationIcon?.let { { IconButton(onClick = { node.action?.let(onAction) }) { Icon(it.toIconVector(), null) } } } ?: {}, actions = { node.actions?.forEach { RenderNode(it, baseDir, onAction, onValueChange, values) } })
        is UINode.BottomSheet -> Surface(modifier = modifier, tonalElevation = 1.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) { RenderNode(node.content, baseDir, onAction, onValueChange, values) }
        is UINode.Dialog -> {
            var showDialog by remember { mutableStateOf(true) }
            if (showDialog) AlertDialog(onDismissRequest = { showDialog = false }, modifier = modifier, title = node.title?.let { { Text(it) } }, text = node.content?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } }, confirmButton = node.confirmButton?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } } ?: {}, dismissButton = node.dismissButton?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } }, icon = node.icon?.let { { Icon(it.toIconVector(), null) } })
        }
        is UINode.Snackbar -> Snackbar(modifier = modifier, action = node.actionLabel?.let { { TextButton(onClick = { node.action?.let(onAction) }) { Text(it) } } }, dismissAction = if (node.withDismissAction == true) { { IconButton(onClick = { }) { Icon(Icons.Default.Close, null) } } } else null) { Text(node.message) }
        is UINode.AlertDialog -> {
            var showDialog by remember { mutableStateOf(true) }
            if (showDialog) AlertDialog(onDismissRequest = { showDialog = false }, modifier = modifier, title = { Text(node.title) }, text = { Text(node.text) }, confirmButton = { TextButton(onClick = { showDialog = false; node.action?.let(onAction) }) { Text(node.confirmButtonText) } }, dismissButton = node.dismissButtonText?.let { { TextButton(onClick = { showDialog = false }) { Text(it) } } }, icon = node.icon?.let { { Icon(it.toIconVector(), null) } })
        }
        is UINode.Scaffold -> Scaffold(modifier = modifier, topBar = node.topBar?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } } ?: {}, bottomBar = node.bottomBar?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } } ?: {}, floatingActionButton = node.floatingActionButton?.let { { RenderNode(it, baseDir, onAction, onValueChange, values) } } ?: {}) { paddingValues -> Box(Modifier.padding(paddingValues)) { RenderNode(node.content, baseDir, onAction, onValueChange, values) } }
        is UINode.AnimatedVisibility -> AnimatedVisibility(visible = node.visible ?: true, modifier = modifier, enter = when (node.enter) { "slideInHorizontally" -> slideInHorizontally(); "slideInVertically" -> slideInVertically(); else -> fadeIn() }, exit = when (node.exit) { "slideOutHorizontally" -> slideOutHorizontally(); "slideOutVertically" -> slideOutVertically(); else -> fadeOut() }) { RenderNode(node.child, baseDir, onAction, onValueChange, values) }
        is UINode.Pager -> {
            val isHorizontal = node.orientation != "vertical"
            if (isHorizontal) LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy((node.pageSpacing ?: 0).dp), userScrollEnabled = node.userScrollEnabled ?: true) { items(node.pages) { page -> RenderNode(page, baseDir, onAction, onValueChange, values) } }
            else LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy((node.pageSpacing ?: 0).dp), userScrollEnabled = node.userScrollEnabled ?: true) { items(node.pages) { page -> RenderNode(page, baseDir, onAction, onValueChange, values) } }
        }
        is UINode.SwipeRefresh -> Box(modifier = modifier) { RenderNode(node.content, baseDir, onAction, onValueChange, values) }
        is UINode.PullRefresh -> Box(modifier = modifier) { RenderNode(node.content, baseDir, onAction, onValueChange, values) }
        is UINode.SegmentedButton -> {
            var selected by remember { mutableStateOf((values[node.key] as? String) ?: node.selected ?: node.options.firstOrNull()) }
            SingleChoiceSegmentedButtonRow(modifier = modifier) { node.options.forEachIndexed { index, option -> val label = node.optionLabels?.getOrNull(index) ?: option; SegmentedButton(shape = SegmentedButtonDefaults.itemShape(index = index, count = node.options.size), onClick = { selected = option; onValueChange(node.key, option); node.action?.let(onAction) }, selected = selected == option, icon = node.optionIcons?.getOrNull(index)?.let { { Icon(it.toIconVector(), null, Modifier.size(18.dp)) } } ?: { SegmentedButtonDefaults.Icon(selected == option) }) { Text(label) } } }
        }
        is UINode.DatePicker -> {
            val value by remember { mutableStateOf((values[node.key] as? Long) ?: node.value) }
            Column(modifier = modifier) { node.label?.let { Text(it, style = MaterialTheme.typography.labelMedium) }; OutlinedButton(onClick = { node.action?.let(onAction) }) { Text(value?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "Select Date") } }
        }
        is UINode.TimePicker -> Column(modifier = modifier) { node.label?.let { Text(it, style = MaterialTheme.typography.labelMedium) }; val hour = node.hour ?: 0; val minute = node.minute ?: 0; OutlinedButton(onClick = { node.action?.let(onAction) }) { Text(String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute)) } }
        is UINode.ColorPicker -> {
            var color by remember { mutableStateOf(node.value ?: "#000000") }
            Column(modifier = modifier) { node.label?.let { Text(it, style = MaterialTheme.typography.labelMedium) }; Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).background(color.toColor(), RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable { node.action?.let(onAction) }); Spacer(Modifier.width(12.dp)); Text(color, style = MaterialTheme.typography.bodyMedium) } }
        }
        is UINode.Rating -> {
            var value by remember { mutableFloatStateOf((values[node.key] as? Float) ?: node.value ?: 0f) }
            val max = node.max ?: 5; val activeColor = node.activeColor?.toColor() ?: MaterialTheme.colorScheme.primary; val inactiveColor = node.inactiveColor?.toColor() ?: MaterialTheme.colorScheme.outlineVariant
            Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(max) { index -> val filled = value > index; Icon(imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size((node.size ?: 24).dp).clickable { value = (index + 1).toFloat(); onValueChange(node.key, value); node.action?.let(onAction) }, tint = if (filled) activeColor else inactiveColor) } }
        }
        is UINode.Stepper -> {
            var value by remember { mutableIntStateOf((values[node.key] as? Int) ?: node.value ?: 0) }
            val min = node.min ?: Int.MIN_VALUE; val max = node.max ?: Int.MAX_VALUE; val step = node.step ?: 1
            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { node.label?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }; Spacer(Modifier.weight(1f)); IconButton(onClick = { if (value > min) { value -= step; onValueChange(node.key, value); node.action?.let(onAction) } }, enabled = value > min) { Icon(Icons.Default.Delete, null) }; Text(value.toString(), style = MaterialTheme.typography.bodyLarge); IconButton(onClick = { if (value < max) { value += step; onValueChange(node.key, value); node.action?.let(onAction) } }, enabled = value < max) { Icon(Icons.Default.Add, null) } }
        }
        is UINode.WebView -> Box(modifier = modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("WebView: ${node.url ?: "HTML Content"}") }
        is UINode.Custom -> Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(16.dp)) { Text("Custom: ${node.type}", style = MaterialTheme.typography.bodyMedium) }
        is UINode.Placeholder -> Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) { Text("Placeholder: ${node.key}", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun UIModifier.toModifier(onAction: UIActionHandler, action: String?): Modifier {
    var mod: Modifier = Modifier
    width?.let { w -> mod = when (w) { "fill" -> mod.fillMaxWidth(); "wrap" -> mod.wrapContentWidth(); else -> w.removeSuffix("dp").toIntOrNull()?.let { mod.width(it.dp) } ?: mod } }
    height?.let { h -> mod = when (h) { "fill" -> mod.fillMaxHeight(); "wrap" -> mod.wrapContentHeight(); else -> h.removeSuffix("dp").toIntOrNull()?.let { mod.height(it.dp) } ?: mod } }
    if (fillMaxSize == true) mod = mod.fillMaxSize()
    if (fillMaxWidth == true) mod = mod.fillMaxWidth()
    if (fillMaxHeight == true) mod = mod.fillMaxHeight()
    if (wrapContentWidth == true) mod = mod.wrapContentWidth()
    if (wrapContentHeight == true) mod = mod.wrapContentHeight()
    padding?.let { mod = mod.padding(it.dp) }
    if (paddingHorizontal != null || paddingVertical != null) mod = mod.padding(horizontal = (paddingHorizontal ?: 0).dp, vertical = (paddingVertical ?: 0).dp)
    if (paddingStart != null || paddingEnd != null || paddingTop != null || paddingBottom != null) mod = mod.padding(start = (paddingStart ?: 0).dp, end = (paddingEnd ?: 0).dp, top = (paddingTop ?: 0).dp, bottom = (paddingBottom ?: 0).dp)
    backgroundColor?.let { mod = mod.background(it.toColor(), cornerRadius?.let { r -> RoundedCornerShape(r.dp) } ?: RoundedCornerShape(0.dp)) }
    cornerRadius?.let { if (clip == true) mod = mod.clip(RoundedCornerShape(it.dp)) }
    if (cornerRadiusTopStart != null || cornerRadiusTopEnd != null || cornerRadiusBottomStart != null || cornerRadiusBottomEnd != null) { val shape = RoundedCornerShape(topStart = (cornerRadiusTopStart ?: 0).dp, topEnd = (cornerRadiusTopEnd ?: 0).dp, bottomStart = (cornerRadiusBottomStart ?: 0).dp, bottomEnd = (cornerRadiusBottomEnd ?: 0).dp); if (clip == true) mod = mod.clip(shape) }
    borderWidth?.let { bw -> borderColor?.let { bc -> mod = mod.border(bw.dp, bc.toColor(), cornerRadius?.let { RoundedCornerShape(it.dp) } ?: RoundedCornerShape(0.dp)) } }
    alpha?.let { mod = mod.alpha(it) }
    aspectRatio?.let { mod = mod.aspectRatio(it) }
    if (scrollable == true) mod = mod.verticalScroll(rememberScrollState())
    if (horizontalScroll == true) mod = mod.horizontalScroll(rememberScrollState())
    if (clickable == true && action != null) mod = mod.clickable { onAction(action) }
    return mod
}

@Composable
private fun UIArrangement?.toVerticalArrangement(spacing: Int?): Arrangement.Vertical = when (this) { UIArrangement.start, UIArrangement.top -> Arrangement.Top; UIArrangement.center -> Arrangement.Center; UIArrangement.end, UIArrangement.bottom -> Arrangement.Bottom; UIArrangement.spaceBetween -> Arrangement.SpaceBetween; UIArrangement.spaceAround -> Arrangement.SpaceAround; UIArrangement.spaceEvenly -> Arrangement.SpaceEvenly; else -> spacing?.let { Arrangement.spacedBy(it.dp) } ?: Arrangement.spacedBy(8.dp) }
@Composable
private fun UIArrangement?.toHorizontalArrangement(spacing: Int?): Arrangement.Horizontal = when (this) { UIArrangement.start -> Arrangement.Start; UIArrangement.center -> Arrangement.Center; UIArrangement.end -> Arrangement.End; UIArrangement.spaceBetween -> Arrangement.SpaceBetween; UIArrangement.spaceAround -> Arrangement.SpaceAround; UIArrangement.spaceEvenly -> Arrangement.SpaceEvenly; else -> spacing?.let { Arrangement.spacedBy(it.dp) } ?: Arrangement.spacedBy(8.dp) }
@Composable
private fun UIAlignment?.toHorizontalAlignment(): Alignment.Horizontal = when (this) { UIAlignment.start -> Alignment.Start; UIAlignment.center -> Alignment.CenterHorizontally; UIAlignment.end -> Alignment.End; else -> Alignment.Start }
@Composable
private fun UIAlignment?.toVerticalAlignment(): Alignment.Vertical = when (this) { UIAlignment.start -> Alignment.Top; UIAlignment.center -> Alignment.CenterVertically; UIAlignment.end -> Alignment.Bottom; else -> Alignment.CenterVertically }
@Composable
private fun UIAlignment?.toAlignment(): Alignment = when (this) { UIAlignment.start -> Alignment.TopStart; UIAlignment.center -> Alignment.Center; UIAlignment.end -> Alignment.BottomEnd; else -> Alignment.TopStart }
@Composable
private fun String?.toShape(): androidx.compose.ui.graphics.Shape = when (this) { "circle" -> CircleShape; "rounded" -> RoundedCornerShape(12.dp); "roundedSmall" -> RoundedCornerShape(4.dp); "roundedMedium" -> RoundedCornerShape(8.dp); "roundedLarge" -> RoundedCornerShape(16.dp); "roundedExtraLarge" -> RoundedCornerShape(24.dp); else -> this?.removeSuffix("dp")?.toIntOrNull()?.let { RoundedCornerShape(it.dp) } ?: RoundedCornerShape(12.dp) }
@Composable
private fun UITextStyle?.toTextStyle(): androidx.compose.ui.text.TextStyle { if (this == null) return androidx.compose.ui.text.TextStyle.Default; return androidx.compose.ui.text.TextStyle(fontSize = fontSize?.sp ?: 14.sp, fontWeight = fontWeight.toFontWeight(), fontStyle = when (fontStyle) { "italic" -> FontStyle.Italic; else -> FontStyle.Normal }, color = color?.toColor() ?: Color.Unspecified, textAlign = textAlign.toTextAlign(), textDecoration = when (textDecoration) { "underline" -> TextDecoration.Underline; "lineThrough" -> TextDecoration.LineThrough; else -> TextDecoration.None }, letterSpacing = letterSpacing?.sp ?: 0.sp, lineHeight = lineHeight?.sp ?: 20.sp) }
@Composable
private fun String?.toFontWeight(): FontWeight = when (this) { "thin" -> FontWeight.Thin; "extraLight" -> FontWeight.ExtraLight; "light" -> FontWeight.Light; "normal" -> FontWeight.Normal; "medium" -> FontWeight.Medium; "semiBold" -> FontWeight.SemiBold; "bold" -> FontWeight.Bold; "extraBold" -> FontWeight.ExtraBold; "black" -> FontWeight.Black; else -> FontWeight.Normal }
@Composable
private fun String?.toTextAlign(): TextAlign = when (this) { "start", "left" -> TextAlign.Start; "center" -> TextAlign.Center; "end", "right" -> TextAlign.End; "justify" -> TextAlign.Justify; else -> TextAlign.Start }
@Composable
private fun String?.toTextOverflow(): TextOverflow = when (this) { "clip" -> TextOverflow.Clip; "ellipsis" -> TextOverflow.Ellipsis; "visible" -> TextOverflow.Visible; else -> TextOverflow.Clip }
@Composable
private fun String?.toContentScale(): androidx.compose.ui.layout.ContentScale = when (this) { "crop" -> androidx.compose.ui.layout.ContentScale.Crop; "fit" -> androidx.compose.ui.layout.ContentScale.Fit; "fillBounds" -> androidx.compose.ui.layout.ContentScale.FillBounds; "fillWidth" -> androidx.compose.ui.layout.ContentScale.FillWidth; "fillHeight" -> androidx.compose.ui.layout.ContentScale.FillHeight; "inside" -> androidx.compose.ui.layout.ContentScale.Inside; "none" -> androidx.compose.ui.layout.ContentScale.None; else -> androidx.compose.ui.layout.ContentScale.Fit }
@Composable
private fun String.toColor(): Color = try { if (startsWith("#")) Color(AndroidColor.parseColor(this)) else when (lowercase()) { "primary" -> Color(0xFF6200EE); "secondary" -> Color(0xFF03DAC6); "tertiary" -> Color(0xFF3700B3); "error" -> Color(0xFFB00020); "surface" -> Color(0xFFFFFFFF); "background" -> Color(0xFFFAFAFA); "white" -> Color.White; "black" -> Color.Black; "red" -> Color.Red; "green" -> Color.Green; "blue" -> Color.Blue; "yellow" -> Color.Yellow; "cyan" -> Color.Cyan; "magenta" -> Color.Magenta; "gray", "grey" -> Color.Gray; "lightgray", "lightgrey" -> Color.LightGray; "darkgray", "darkgrey" -> Color.DarkGray; "transparent" -> Color.Transparent; else -> Color.Unspecified } } catch (e: Exception) { Color.Unspecified }
@Composable
private fun String.toIconVector(): ImageVector = when (lowercase()) { "add", "plus" -> Icons.Default.Add; "arrow_back", "back" -> Icons.Default.ArrowBack; "check" -> Icons.Default.Check; "clear" -> Icons.Default.Clear; "close" -> Icons.Default.Close; "delete" -> Icons.Default.Delete; "edit" -> Icons.Default.Edit; "favorite", "heart" -> Icons.Default.Favorite; "home" -> Icons.Default.Home; "info" -> Icons.Default.Info; "menu" -> Icons.Default.Menu; "more_vert", "more" -> Icons.Default.MoreVert; "play", "play_arrow" -> Icons.Default.PlayArrow; "refresh" -> Icons.Default.Refresh; "search" -> Icons.Default.Search; "settings" -> Icons.Default.Settings; "share" -> Icons.Default.Share; "star" -> Icons.Default.Star; else -> Icons.Default.Info }
private fun resolveIcon(name: String): Int? = when (name) { "add" -> moe.koiverse.archivetune.R.drawable.add; "settings" -> moe.koiverse.archivetune.R.drawable.settings; "restore" -> moe.koiverse.archivetune.R.drawable.restore; "arrow_back" -> moe.koiverse.archivetune.R.drawable.arrow_back; "delete" -> moe.koiverse.archivetune.R.drawable.delete; "more_vert" -> moe.koiverse.archivetune.R.drawable.more_vert; else -> null }

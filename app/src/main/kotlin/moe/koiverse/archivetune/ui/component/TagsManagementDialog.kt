/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.TagEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsManagementDialog(
    database: MusicDatabase,
    onDismiss: () -> Unit
) {
    val allTags by database.allTags().collectAsState(initial = emptyList())
    
    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagToEdit by remember { mutableStateOf<TagEntity?>(null) }
    var showColorPicker by remember { mutableStateOf<TagEntity?>(null) }

    if (showAddTagDialog) {
        AddEditTagDialog(
            tag = tagToEdit,
            onDismiss = {
                showAddTagDialog = false
                tagToEdit = null
            },
            onSave = { name, color ->
                if (tagToEdit != null) {
                    database.transaction {
                        update(tagToEdit!!.copy(name = name, color = color))
                    }
                } else {
                    database.transaction {
                        insert(TagEntity(name = name, color = color))
                    }
                }
                showAddTagDialog = false
                tagToEdit = null
            }
        )
    }

    showColorPicker?.let { tag ->
        ColorPickerDialog(
            currentColor = tag.color,
            onDismiss = { showColorPicker = null },
            onColorSelected = { color ->
                database.transaction {
                    update(tag.copy(color = color))
                }
                showColorPicker = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.manage_tags),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        EditableTagChip(
                            tag = tag,
                            onEdit = {
                                tagToEdit = tag
                                showAddTagDialog = true
                            },
                            onColorClick = {
                                showColorPicker = tag
                            },
                            onRemove = {
                                database.transaction {
                                    delete(tag)
                                }
                            }
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showAddTagDialog = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = stringResource(R.string.add_tag),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditTagDialog(
    tag: TagEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var tagName by remember(tag) { mutableStateOf(tag?.name ?: "") }
    var selectedColor by remember(tag) { mutableStateOf(tag?.color ?: TagEntity.DEFAULT_COLORS[0]) }
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = selectedColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = {
                selectedColor = it
                showColorPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tag == null) stringResource(R.string.add_tag) else stringResource(R.string.edit_tag)) },
        text = {
            Column {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.tag_color),
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor.toColorInt()))
                            .clickable { showColorPicker = true }
                    )
                    
                    Text(
                        text = stringResource(R.string.tap_to_change_color),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tagName, selectedColor) },
                enabled = tagName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    currentColor: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_color)) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TagEntity.DEFAULT_COLORS.forEach { color ->
                    val isSelected = color == currentColor
                    val size by animateDpAsState(if (isSelected) 48.dp else 40.dp, label = "colorSize")
                    
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(Color(color.toColorInt()))
                            .clickable { onColorSelected(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@Composable
private fun EditableTagChip(
    tag: TagEntity,
    onEdit: () -> Unit,
    onColorClick: () -> Unit,
    onRemove: () -> Unit
) {
    val backgroundColor = remember(tag.color) {
        Color(tag.color.toColorInt()).copy(alpha = 0.2f)
    }
    val contentColor = remember(tag.color) {
        Color(tag.color.toColorInt())
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.5.dp, contentColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(contentColor)
                    .clickable(onClick = onColorClick)
            )

            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 4.dp)
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .background(contentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */



@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)


package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.koiverse.archivetune.R
import kotlin.math.roundToInt

val LocalPreferenceInGroup = compositionLocalOf { false }

@Composable
private fun rememberPreferenceIconShape(): Shape {
    return MaterialShapes.Ghostish.toShape()
}

@Composable
private fun rememberPreferenceGroupItemShape(): Shape {
    return MaterialShapes.Arch.toShape()
}

@Composable
fun PreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    val inGroup = LocalPreferenceInGroup.current
    val preferenceIconShape = rememberPreferenceIconShape()
    val preferenceGroupItemShape = rememberPreferenceGroupItemShape()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !inGroup) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "prefScale",
    )

    val rowContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isEnabled && onClick != null) Modifier.focusable() else Modifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = if (inGroup) LocalIndication.current else null,
                    enabled = isEnabled && onClick != null,
                    onClick = onClick ?: {},
                )
                .alpha(if (isEnabled) 1f else 0.5f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(40.dp)
                        .clip(preferenceIconShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                Spacer(Modifier.width(14.dp))
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) {
                    title()
                }
                if (description != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content?.invoke()
            }

            if (trailingContent != null) {
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    trailingContent()
                }
            }
        }
    }

    if (inGroup) {
        Surface(
            shape = preferenceGroupItemShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            rowContent()
        }
    } else {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            rowContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    selectedValue: T,
    values: List<T>,
    valueText: @Composable (T) -> String,
    onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        isEnabled = isEnabled,
        content = {
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                values.forEachIndexed { index, value ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = values.size),
                        onClick = { onValueSelected(value) },
                        selected = value == selectedValue,
                        enabled = isEnabled,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = valueText(value),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    )
}

@Composable
inline fun <reified T : Enum<T>> EnumSegmentedPreference(
    modifier: Modifier = Modifier,
    noinline title: @Composable () -> Unit,
    description: String? = null,
    noinline icon: (@Composable () -> Unit)? = null,
    selectedValue: T,
    noinline valueText: @Composable (T) -> String,
    noinline onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
) {
    SegmentedPreference(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        selectedValue = selectedValue,
        values = enumValues<T>().toList(),
        valueText = valueText,
        onValueSelected = onValueSelected,
        isEnabled = isEnabled,
    )
}

@Composable
fun <T> ListPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    selectedValue: T,
    values: List<T>,
    valueText: @Composable (T) -> String,
    onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    if (showDialog) {
        var pendingValue by remember { mutableStateOf(selectedValue) }
        DefaultDialog(
            onDismiss = { showDialog = false },
            title = title,
            contentScrollable = true,
            buttons = {
                TextButton(
                    onClick = { showDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onValueSelected(pendingValue)
                        showDialog = false
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column {
                values.forEach { value ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = value == pendingValue,
                                onClick = { pendingValue = value },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = 16.dp),
                    ) {
                        RadioButton(
                            selected = value == pendingValue,
                            onClick = null,
                        )
                        Text(
                            text = valueText(value),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = valueText(selectedValue),
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
    )
}

@Composable
inline fun <reified T : Enum<T>> EnumListPreference(
    modifier: Modifier = Modifier,
    noinline title: @Composable () -> Unit,
    noinline icon: (@Composable () -> Unit)?,
    selectedValue: T,
    noinline valueText: @Composable (T) -> String,
    noinline onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
) {
    ListPreference(
        modifier = modifier,
        title = title,
        icon = icon,
        selectedValue = selectedValue,
        values = enumValues<T>().toList(),
        valueText = valueText,
        onValueSelected = onValueSelected,
        isEnabled = isEnabled,
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled,
                thumbContent = {
                    AnimatedContent(
                        targetState = checked,
                        transitionSpec = {
                            fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                        },
                        label = "switchThumbIcon",
                    ) { isChecked ->
                        Icon(
                            painter = painterResource(
                                id = if (isChecked) R.drawable.check else R.drawable.close
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
        onClick = { onCheckedChange(!checked) },
        isEnabled = isEnabled
    )
}

@Composable
fun EditTextPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    if (showDialog) {
        TextFieldDialog(
            title = title,
            initialTextFieldValue =
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            ),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            isInputValid = isInputValid,
            onDone = onValueChange,
            onDismiss = { showDialog = false },
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value,
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
    )
}

@Composable
fun NumberEditTextPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: Int,
    onValueChange: (Int) -> Unit,
    isInputValid: (String) -> Boolean = { it.toIntOrNull() != null },
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    if (showDialog) {
        TextFieldDialog(
            title = title,
            initialTextFieldValue =
            TextFieldValue(
                text = value.toString(),
                selection = TextRange(value.toString().length),
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            isInputValid = isInputValid,
            onDone = { it.toIntOrNull()?.let(onValueChange) },
            onDismiss = { showDialog = false },
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value.toString(),
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(value)
    }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_duration),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                onValueChange.invoke(sliderValue)
            },
            onCancel = {
                sliderValue = value
                showDialog = false
            },
            onReset = {
                sliderValue = 30f
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.seconds,
                            sliderValue.roundToInt(),
                            sliderValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    val sliderState = rememberSliderState(
                        value = sliderValue,
                        valueRange = 15f..60f,
                        onValueChangeFinished = {},
                    )
                    sliderState.onValueChange = { sliderValue = it }
                    sliderState.value = sliderValue

                    Slider(
                        state = sliderState,
                        modifier = Modifier.fillMaxWidth(),
                        track = {
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                trackCornerSize = 12.dp,
                            )
                        },
                    )
                }
            }
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value.roundToInt().toString(),
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossfadeSliderPreference(
    modifier: Modifier = Modifier,
    valueSeconds: Float,
    onValueChange: (Float) -> Unit,
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(valueSeconds.coerceIn(0f, 10f))
    }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.audio_crossfade_dialog_title),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                val rounded =
                    ((sliderValue * 2f).roundToInt().toFloat() / 2f)
                        .coerceIn(0f, 10f)
                sliderValue = rounded
                showDialog = false
                onValueChange.invoke(rounded)
            },
            onCancel = {
                sliderValue = valueSeconds.coerceIn(0f, 10f)
                showDialog = false
            },
            onReset = {
                sliderValue = 5f
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rounded =
                        ((sliderValue * 2f).roundToInt().toFloat() / 2f)
                            .coerceIn(0f, 10f)
                    val isWhole = (rounded - rounded.roundToInt().toFloat()).let { delta ->
                        kotlin.math.abs(delta) < 0.001f
                    }
                    val displayValue =
                        if (isWhole) rounded.roundToInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", rounded)
                    Text(
                        text =
                        if (rounded <= 0f) {
                            stringResource(R.string.dark_theme_off)
                        } else {
                            stringResource(R.string.audio_crossfade_seconds, displayValue)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.audio_crossfade_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Spacer(Modifier.height(16.dp))

                    val crossfadeSliderState = rememberSliderState(
                        value = sliderValue,
                        steps = 19,
                        valueRange = 0f..10f,
                        onValueChangeFinished = {},
                    )
                    crossfadeSliderState.onValueChange = { sliderValue = it.coerceIn(0f, 10f) }
                    crossfadeSliderState.value = sliderValue

                    Slider(
                        state = crossfadeSliderState,
                        modifier = Modifier.fillMaxWidth(),
                        track = {
                            SliderDefaults.Track(
                                sliderState = crossfadeSliderState,
                                trackCornerSize = 12.dp,
                            )
                        },
                    )
                }
            }
        )
    }

    val rounded =
        ((valueSeconds * 2f).roundToInt().toFloat() / 2f)
            .coerceIn(0f, 10f)
    val isWhole = (rounded - rounded.roundToInt().toFloat()).let { delta ->
        kotlin.math.abs(delta) < 0.001f
    }
    val displayValue =
        if (isWhole) rounded.roundToInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", rounded)
    val descriptionText =
        if (rounded <= 0f) {
            stringResource(R.string.dark_theme_off)
        } else {
            stringResource(R.string.audio_crossfade_seconds, displayValue)
        }

    PreferenceEntry(
        modifier = modifier,
        title = { Text(stringResource(R.string.audio_crossfade_title)) },
        description = descriptionText,
        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
        onClick = { if (isEnabled) showDialog = true },
        isEnabled = isEnabled,
    )
}

@Composable
fun NumberPickerPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 0,
    maxValue: Int = 10,
    valueText: (Int) -> String = { it.toString() },
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(value.toFloat())
    }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    title()
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                val rounded = sliderValue.roundToInt().coerceIn(minValue, maxValue)
                sliderValue = rounded.toFloat()
                showDialog = false
                onValueChange.invoke(rounded)
            },
            onCancel = {
                sliderValue = value.toFloat()
                showDialog = false
            },
            onReset = {
                sliderValue = minValue.toFloat()
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rounded = sliderValue.roundToInt().coerceIn(minValue, maxValue)
                    Text(
                        text = valueText(rounded),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    val pickerSliderState = rememberSliderState(
                        value = sliderValue,
                        steps = maxValue - minValue - 1,
                        valueRange = minValue.toFloat()..maxValue.toFloat(),
                        onValueChangeFinished = {},
                    )
                    pickerSliderState.onValueChange = {
                        sliderValue = it.coerceIn(minValue.toFloat(), maxValue.toFloat())
                    }
                    pickerSliderState.value = sliderValue

                    Slider(
                        state = pickerSliderState,
                        modifier = Modifier.fillMaxWidth(),
                        track = {
                            SliderDefaults.Track(
                                sliderState = pickerSliderState,
                                trackCornerSize = 12.dp,
                            )
                        },
                    )
                }
            }
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = valueText(value),
        icon = icon,
        onClick = { if (isEnabled) showDialog = true },
        isEnabled = isEnabled,
    )
}

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        CompositionLocalProvider(LocalPreferenceInGroup provides true) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
fun PreferenceGroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = 60.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

@Composable
fun PreferenceGroupTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}
/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.ui.component.ListItem
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.utils.TranslatorLanguages
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordExperimental(
    navController: NavController,
) {
    Scaffold { inner ->
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.experiment_settings)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .padding(inner.calculateBottomPadding())
                    .padding(bottom = 80.dp) // extra space for mini player
            ) {
                item {
                    Text(
                        text = stringResource(R.string.translator_options),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    val (translatorEnabled, onTranslatorEnabledChange) =
                        rememberPreference(key = EnableTranslatorKey, defaultValue = false)
                    val (translatorContexts, onTranslatorContextsChange) =
                        rememberPreference(
                            key = TranslatorContextsKey,
                            defaultValue = "{song}, {artist}, {album}"
                        )
                    val (translatorTargetLang, onTranslatorTargetLangChange) =
                        rememberPreference(key = TranslatorTargetLangKey, defaultValue = "ENGLISH")

                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_translator)) },
                        description = stringResource(R.string.enable_translator_desc),
                        icon = { Icon(painterResource(R.drawable.translate), null) },
                        checked = translatorEnabled,
                        onCheckedChange = onTranslatorEnabledChange,
                    )

                    AnimatedVisibility(visible = translatorEnabled) {
                        Column {
                            TextField(
                                value = translatorContexts,
                                onValueChange = { onTranslatorContextsChange(it) },
                                label = { Text(stringResource(R.string.context_info)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                supportingText = {
                                    Text(stringResource(R.string.translator_info_usage))
                                }
                            )

                            var showLangDialog by remember { mutableStateOf(false) }
                            val context = LocalContext.current
                            val languages = remember { TranslatorLanguages.load(context) }
                            val currentLangName =
                                languages.find { it.code == translatorTargetLang }?.name
                                    ?: translatorTargetLang

                            PreferenceEntry(
                                title = { Text(stringResource(R.string.target_language)) },
                                description = currentLangName,
                                icon = { Icon(painterResource(R.drawable.translate), null) },
                                trailingContent = {
                                    TextButton(onClick = { showLangDialog = true }) {
                                        Text(stringResource(R.string.select_dialog))
                                    }
                                }
                            )

                            if (showLangDialog) {
                                AlertDialog(
                                    onDismissRequest = { showLangDialog = false },
                                    title = { Text(stringResource(R.string.select_language)) },
                                    text = {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(languages) { lang ->
                                                ListItem(
                                                    title = lang.name,
                                                    modifier =
                                                        Modifier.fillMaxWidth().clickable {
                                                            onTranslatorTargetLangChange(lang.code)
                                                            showLangDialog = false
                                                        },
                                                    thumbnailContent = {},
                                                    isActive = (lang.code == translatorTargetLang)
                                                )
                                                Divider()
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showLangDialog = false }) {
                                            Text(stringResource(R.string.close_dialog))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.discord_button_options),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                item {
                    val (button1Label, onButton1LabelChange) =
                        rememberPreference(
                            key = DiscordActivityButton1LabelKey,
                            defaultValue = "Listen on YouTube Music"
                        )
                    val (button1Enabled, onButton1EnabledChange) =
                        rememberPreference(
                            key = DiscordActivityButton1EnabledKey,
                            defaultValue = true
                        )
                    val (button2Label, onButton2LabelChange) =
                        rememberPreference(
                            key = DiscordActivityButton2LabelKey,
                            defaultValue = "Go to ArchiveTune"
                        )
                    val (button2Enabled, onButton2EnabledChange) =
                        rememberPreference(
                            key = DiscordActivityButton2EnabledKey,
                            defaultValue = true
                        )

                    val urlOptions = listOf("songurl", "artisturl", "albumurl", "custom")
                    val (button1UrlSource, onButton1UrlSourceChange) =
                        rememberPreference(
                            key = DiscordActivityButton1UrlSourceKey,
                            defaultValue = "songurl"
                        )
                    val (button1CustomUrl, onButton1CustomUrlChange) =
                        rememberPreference(
                            key = DiscordActivityButton1CustomUrlKey,
                            defaultValue = ""
                        )
                    val (button2UrlSource, onButton2UrlSourceChange) =
                        rememberPreference(
                            key = DiscordActivityButton2UrlSourceKey,
                            defaultValue = "custom"
                        )
                    val (button2CustomUrl, onButton2CustomUrlChange) =
                        rememberPreference(
                            key = DiscordActivityButton2CustomUrlKey,
                            defaultValue = "https://github.com/koiverse/ArchiveTune"
                        )

                    PreferenceEntry(
                        title = { Text(stringResource(R.string.show_button)) },
                        description = stringResource(R.string.show_button1_description),
                        icon = { Icon(painterResource(R.drawable.buttons), null) },
                        trailingContent = {
                            Switch(
                                checked = button1Enabled,
                                onCheckedChange = onButton1EnabledChange
                            )
                        }
                    )

                    if (button1Enabled) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            TextField(
                                value = button1UrlSource,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.discord_activity_button_1_url)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .menuAnchor()
                                        .pointerInput(Unit) {
                                            detectTapGestures { expanded = true }
                                        }
                                        .padding(horizontal = 13.dp, vertical = 16.dp),
                                leadingIcon = { Icon(painterResource(R.drawable.link), null) }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                urlOptions.forEach { opt ->
                                    val display =
                                        when (opt) {
                                            "songurl" -> "Song URL"
                                            "artisturl" -> "Artist URL"
                                            "albumurl" -> "Album URL"
                                            "custom" -> "Custom URL"
                                            else -> opt
                                        }
                                    DropdownMenuItem(
                                        text = { Text(display) },
                                        onClick = {
                                            onButton1UrlSourceChange(opt)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        EditablePreference(
                            title = stringResource(R.string.discord_activity_button1_label),
                            iconRes = R.drawable.buttons,
                            value = button1Label,
                            defaultValue = "Listen on YouTube Music",
                            onValueChange = onButton1LabelChange
                        )
                        if (button1UrlSource == "custom") {
                            EditablePreference(
                                title = stringResource(R.string.discord_activity_button1_url),
                                iconRes = R.drawable.link,
                                value = button1CustomUrl,
                                defaultValue = "",
                                onValueChange = onButton1CustomUrlChange
                            )
                        }
                    }

                    PreferenceEntry(
                        title = { Text(stringResource(R.string.show_button)) },
                        description = stringResource(R.string.show_button2_description),
                        icon = { Icon(painterResource(R.drawable.buttons), null) },
                        trailingContent = {
                            Switch(
                                checked = button2Enabled,
                                onCheckedChange = onButton2EnabledChange
                            )
                        }
                    )

                    if (button2Enabled) {
                        var expanded2 by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded2,
                            onExpandedChange = { expanded2 = it }
                        ) {
                            TextField(
                                value = button2UrlSource,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.discord_activity_button_2_url)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded2)
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .menuAnchor()
                                        .pointerInput(Unit) {
                                            detectTapGestures { expanded2 = true }
                                        }
                                        .padding(horizontal = 13.dp, vertical = 16.dp),
                                leadingIcon = { Icon(painterResource(R.drawable.link), null) }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded2,
                                onDismissRequest = { expanded2 = false }
                            ) {
                                urlOptions.forEach { opt ->
                                    val display =
                                        when (opt) {
                                            "songurl" -> "Song URL"
                                            "artisturl" -> "Artist URL"
                                            "albumurl" -> "Album URL"
                                            "custom" -> "Custom URL"
                                            else -> opt
                                        }
                                    DropdownMenuItem(
                                        text = { Text(display) },
                                        onClick = {
                                            onButton2UrlSourceChange(opt)
                                            expanded2 = false
                                        }
                                    )
                                }
                            }
                        }
                        EditablePreference(
                            title = stringResource(R.string.discord_activity_button2_label),
                            iconRes = R.drawable.buttons,
                            value = button2Label,
                            defaultValue = "Go to ArchiveTune",
                            onValueChange = onButton2LabelChange
                        )
                        if (button2UrlSource == "custom") {
                            EditablePreference(
                                title = stringResource(R.string.discord_activity_button2_url),
                                iconRes = R.drawable.link,
                                value = button2CustomUrl,
                                defaultValue = "",
                                onValueChange = onButton2CustomUrlChange
                            )
                        }
                    }
                }
            }
        }
    }
}

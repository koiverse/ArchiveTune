package moe.koiverse.archivetune.ui.menu

import android.app.SearchManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import me.bush.translator.Translator
import me.bush.translator.Language
import moe.koiverse.archivetune.utils.TranslatorLanguages
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.LyricsEntity
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.ListDialog
import moe.koiverse.archivetune.ui.component.NewAction
import moe.koiverse.archivetune.ui.component.NewActionGrid
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.viewmodels.LyricsMenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsMenu(
    lyricsProvider: () -> LyricsEntity?,
    mediaMetadataProvider: () -> MediaMetadata,
    onDismiss: () -> Unit,
    viewModel: LyricsMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showTranslateDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showEditDialog) {
        TextFieldDialog(
            onDismiss = { showEditDialog = false },
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = mediaMetadataProvider().title) },
            initialTextFieldValue = TextFieldValue(lyricsProvider()?.lyrics.orEmpty()),
            singleLine = false,
            onDone = {
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadataProvider().id,
                            lyrics = it,
                        ),
                    )
                }
            },
        )
    }

    var showSearchDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showSearchResultDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val searchMediaMetadata =
        remember(showSearchDialog) {
            mediaMetadataProvider()
        }
    val (titleField, onTitleFieldChange) =
        rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
            mutableStateOf(
                TextFieldValue(
                    text = mediaMetadataProvider().title,
                ),
            )
        }
    val (artistField, onArtistFieldChange) =
        rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
            mutableStateOf(
                TextFieldValue(
                    text = mediaMetadataProvider().artists.joinToString { it.name },
                ),
            )
        }

    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

    if (showSearchDialog) {
        DefaultDialog(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onDismiss = { showSearchDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.search_lyrics)) },
            buttons = {
                TextButton(
                    onClick = { showSearchDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        showSearchDialog = false
                        onDismiss()
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_WEB_SEARCH).apply {
                                    putExtra(
                                        SearchManager.QUERY,
                                        "${artistField.text} ${titleField.text} lyrics"
                                    )
                                },
                            )
                        } catch (_: Exception) {
                        }
                    },
                ) {
                    Text(stringResource(R.string.search_online))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        // Try search regardless of network status indicator
                        // as it might be a false negative
                        viewModel.search(
                            searchMediaMetadata.id,
                            titleField.text,
                            artistField.text,
                            searchMediaMetadata.duration
                        )
                        showSearchResultDialog = true
                        
                        // Show warning only if network is definitely unavailable
                        if (!isNetworkAvailable) {
                            Toast.makeText(context, context.getString(R.string.error_no_internet), Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            OutlinedTextField(
                value = titleField,
                onValueChange = onTitleFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_title)) },
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = artistField,
                onValueChange = onArtistFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_artists)) },
            )
        }
    }

    if (showSearchResultDialog) {
        val results by viewModel.results.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        var expandedItemIndex by rememberSaveable {
            mutableStateOf(-1)
        }

        ListDialog(
            onDismiss = { showSearchResultDialog = false },
        ) {
            itemsIndexed(results) { index, result ->
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            viewModel.cancelSearch()
                            database.query {
                                upsert(
                                    LyricsEntity(
                                        id = searchMediaMetadata.id,
                                        lyrics = result.lyrics,
                                    ),
                                )
                            }
                        }
                        .padding(12.dp)
                        .animateContentSize(),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = result.lyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (index == expandedItemIndex) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = result.providerName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                            )
                            if (result.lyrics.startsWith("[")) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier =
                                    Modifier
                                        .padding(start = 4.dp)
                                        .size(18.dp),
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            expandedItemIndex = if (expandedItemIndex == index) -1 else index
                        },
                    ) {
                        Icon(
                            painter = painterResource(if (index == expandedItemIndex) R.drawable.expand_less else R.drawable.expand_more),
                            contentDescription = null,
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!isLoading && results.isEmpty()) {
                item {
                    Text(
                        text = context.getString(R.string.lyrics_not_found),
                        textAlign = TextAlign.Center,
                        modifier =
                        Modifier
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Enhanced Action Grid using NewMenuComponents
    NewActionGrid(
        actions = listOf(
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                text = stringResource(R.string.edit),
                onClick = { showEditDialog = true }
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.cached),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                text = stringResource(R.string.refetch),
                onClick = {
                    onDismiss()
                    viewModel.refetchLyrics(mediaMetadataProvider(), lyricsProvider())
                }
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.translate),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                text = stringResource(R.string.translate),
                onClick = { showTranslateDialog = true }
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                text = stringResource(R.string.search),
                onClick = { showSearchDialog = true }
            )
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
    )

    // Translate dialog moved outside of action list
    if (showTranslateDialog) {
        val initialText = lyricsProvider()?.lyrics.orEmpty()
        val (textFieldValue, setTextFieldValue) = rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(text = initialText)) }
    val languages = remember { TranslatorLanguages.load(context) }
    var expanded by remember { mutableStateOf(false) }
    var selectedLanguageCode by rememberSaveable { mutableStateOf("ENGLISH") }
    var isTranslating by remember { mutableStateOf(false) }
        val selectedLanguageName = languages.firstOrNull { it.code == selectedLanguageCode }?.name ?: selectedLanguageCode

        DefaultDialog(
            onDismiss = { showTranslateDialog = false },
            icon = { Icon(painter = painterResource(R.drawable.translate), contentDescription = null) },
            title = { Text(stringResource(R.string.translate)) },
            buttons = {
                TextButton(onClick = { showTranslateDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                // Show progress indicator while translating
                if (isTranslating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    TextButton(onClick = {
                        // Kick off translation (batched)
                        isTranslating = true
                        coroutineScope.launch {
                            try {
                                val translator = Translator()
                                val lang = Language.valueOf(selectedLanguageCode)

                                // Prepare lines and only translate the lyric content parts
                                val lines = textFieldValue.text.split("\n")
                                val tsRegex = Regex("^((?:\\[[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?\\])+")

                                // Collect contents to translate in order; null means blank/no-translate
                                val contents = mutableListOf<String?>()
                                val stampsFor = mutableListOf<String?>()

                                for (line in lines) {
                                    val trimmed = line.trimEnd()
                                    val m = tsRegex.find(trimmed)
                                    if (m != null) {
                                        val stamps = m.groupValues[1]
                                        val content = trimmed.substring(m.range.last + 1).trimStart()
                                        stampsFor.add(stamps)
                                        if (content.isBlank()) contents.add(null) else contents.add(content)
                                    } else {
                                        stampsFor.add(null)
                                        if (trimmed.isBlank()) contents.add(null) else contents.add(trimmed)
                                    }
                                }

                                // Batch translate non-null contents using a unique delimiter to preserve splits
                                val SPLIT = "__ARCHIVETUNE_SPLIT__"
                                val toTranslate = contents.map { it ?: "" }.joinToString("\n$SPLIT\n")

                                val rawResult = withContext(Dispatchers.IO) {
                                    translator.translateBlocking(toTranslate, lang)
                                }.translatedText

                                val translatedParts = rawResult.split("\n$SPLIT\n")

                                // Reconstruct lines
                                val out = mutableListOf<String>()
                                var idx = 0
                                for (i in contents.indices) {
                                    val stamp = stampsFor[i]
                                    val c = contents[i]
                                    if (c == null) {
                                        // preserve stamp or blank line
                                        if (stamp != null) out.add(stamp) else out.add("")
                                    } else {
                                        val translated = if (idx < translatedParts.size) translatedParts[idx++] else ""
                                        if (stamp != null) out.add("$stamp $translated") else out.add(translated)
                                    }
                                }

                                val translated = out.joinToString("\n")
                                database.query {
                                    upsert(LyricsEntity(id = mediaMetadataProvider().id, lyrics = translated))
                                }
                                Toast.makeText(context, context.getString(R.string.translation_success), Toast.LENGTH_SHORT).show()
                                showTranslateDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.translation_failed), Toast.LENGTH_SHORT).show()
                            } finally {
                                isTranslating = false
                            }
                        }
                    }) { Text(stringResource(R.string.translate)) }
                }
            }
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = setTextFieldValue,
                    singleLine = false,
                    label = { Text(stringResource(R.string.lyrics)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 220.dp)
                )

                Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.language_label),
                    modifier = Modifier.width(96.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = selectedLanguageName,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.name) },
                                onClick = {
                                    selectedLanguageCode = lang.code
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
          }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {

    }
}

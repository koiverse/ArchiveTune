/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.menu

import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import me.bush.translator.Translator
import me.bush.translator.Language
import moe.koiverse.archivetune.utils.TranslatorLanguages
import moe.koiverse.archivetune.utils.TranslatorLang
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.LyricsEntity
import moe.koiverse.archivetune.lyrics.LyricsUtils.isTtml
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseLyrics
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseTtml
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.ListDialog
import moe.koiverse.archivetune.ui.component.NewAction
import moe.koiverse.archivetune.ui.component.NewActionGrid
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.viewmodels.LyricsMenuViewModel
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
                viewModel.updateLyrics(mediaMetadataProvider(), it)
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
        SearchLyricsInputDialog(
            titleField = titleField,
            onTitleFieldChange = onTitleFieldChange,
            artistField = artistField,
            onArtistFieldChange = onArtistFieldChange,
            onDismiss = { showSearchDialog = false },
            onSearchOnline = {
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
            onSearch = {
                viewModel.search(
                    searchMediaMetadata.id,
                    titleField.text,
                    artistField.text,
                    searchMediaMetadata.duration
                )
                showSearchResultDialog = true

                if (!isNetworkAvailable) {
                    Toast.makeText(context, context.getString(R.string.error_no_internet), Toast.LENGTH_SHORT).show()
                }
            }
        )
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.search_lyrics),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AnimatedVisibility(visible = results.isNotEmpty()) {
                            Text(
                                text = "${results.size} ${stringResource(R.string.search)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    if (isLoading) {
                        LoadingIndicator(
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            itemsIndexed(results) { index, result ->
                val isExpanded = index == expandedItemIndex

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isExpanded)
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = {
                        onDismiss()
                        viewModel.cancelSearch()
                        viewModel.updateLyrics(searchMediaMetadata, result.lyrics)
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val displayLyrics = remember(result.lyrics) {
                            val raw = result.lyrics.trim()
                            when {
                                isTtml(raw) -> parseTtml(raw).joinToString("\n") { it.text }.trim()
                                raw.startsWith("[") -> parseLyrics(raw).joinToString("\n") { it.text }.trim()
                                else -> raw
                            }
                        }

                        Text(
                            text = displayLyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                ) {
                                    Text(
                                        text = result.providerName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (result.lyrics.startsWith("[") || isTtml(result.lyrics)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.sync),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Synced",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    expandedItemIndex = if (isExpanded) -1 else index
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isExpanded) R.drawable.expand_less else R.drawable.expand_more
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading && results.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.search) + "...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!isLoading && results.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = context.getString(R.string.lyrics_not_found),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Translate dialog moved outside of action list
        if (showTranslateDialog) {
            val initialText = lyricsProvider()?.lyrics.orEmpty()
            val (textFieldValue, setTextFieldValue) =
                rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue(text = initialText))
                }

            val languages by produceState(initialValue = emptyList<TranslatorLang>()) {
                withContext(Dispatchers.IO) {
                    value = TranslatorLanguages.load(context)
                }
            }
            var expanded by remember { mutableStateOf(false) }
            var selectedLanguageCode by rememberSaveable { mutableStateOf("ENGLISH") }
            var isTranslating by remember { mutableStateOf(false) }
            val selectedLanguageName =
                languages.firstOrNull { it.code == selectedLanguageCode }?.name ?: selectedLanguageCode

            DefaultDialog(
                onDismiss = { showTranslateDialog = false },
                icon = {
                    Icon(painter = painterResource(R.drawable.translate), contentDescription = null)
                },
                title = { Text(stringResource(R.string.translate)) },
                buttons = {
                    TextButton(onClick = { showTranslateDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    if (isTranslating) {
                        CircularProgressIndicator(
                        modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically),
                        strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(onClick = {
                            isTranslating = true
                            val inputText = textFieldValue.text
                            val languageCode = selectedLanguageCode
                            val languageName = selectedLanguageName
                            coroutineScope.launch {
                                try {
                                    val lang = try {
                                        Language(languageCode)
                                    } catch (e: Exception) {
                                        try { Language(languageName) } catch (_: Exception) { null }
                                    }

                                    if (lang == null) {
                                        Toast.makeText(
                                            context,
                                            "Unsupported language: $languageName",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }

                                    val translatedLyrics = withContext(Dispatchers.IO) {
                                        val translator = Translator()

                                        val lines = inputText.split("\n")
                                        val tsRegex =
                                            Regex("^((?:\\[[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?\\])+)")
                                        val contents = mutableListOf<String?>()
                                        val stampsFor = mutableListOf<String?>()

                                        for (line in lines) {
                                            val trimmed = line.trimEnd()
                                            val m = tsRegex.find(trimmed)
                                            if (m != null) {
                                                val stamps = m.groupValues[1]
                                                val content =
                                                    trimmed.substring(m.range.last + 1).trimStart()
                                                stampsFor.add(stamps)
                                                contents.add(if (content.isBlank()) null else content)
                                            } else {
                                                stampsFor.add(null)
                                                contents.add(if (trimmed.isBlank()) null else trimmed)
                                            }
                                        }

                                        val translatableIndices =
                                            contents.mapIndexedNotNull { idx, c -> if (c != null) idx else null }
                                        val translatedMap = mutableMapOf<Int, String>()

                                        if (translatableIndices.isNotEmpty()) {
                                            var sep = "<<<SEP-${UUID.randomUUID()}>>>"
                                            while (contents.any { it?.contains(sep) == true }) {
                                                sep = "<<<SEP-${UUID.randomUUID()}>>>"
                                            }

                                            val maxCharsPerRequest = 4000
                                            val maxItemsPerBatch = 50

                                            var cursor = 0
                                            while (cursor < translatableIndices.size) {
                                                var currentChars = 0
                                                val batchIndices = mutableListOf<Int>()
                                                while (cursor < translatableIndices.size && batchIndices.size < maxItemsPerBatch) {
                                                    val idx = translatableIndices[cursor]
                                                    val pieceLen = contents[idx]!!.length
                                                    if (batchIndices.isEmpty() || currentChars + pieceLen + sep.length <= maxCharsPerRequest) {
                                                        batchIndices.add(idx)
                                                        currentChars += pieceLen + sep.length
                                                        cursor++
                                                    } else break
                                                }

                                                val batchTexts = batchIndices.map { contents[it]!! }
                                                val joined = batchTexts.joinToString(separator = sep)
                                                val translatedJoined =
                                                    translator.translateBlocking(joined, lang).translatedText

                                                val parts = translatedJoined.split(sep)
                                                if (parts.size == batchTexts.size) {
                                                    for (i in batchIndices.indices) {
                                                        translatedMap[batchIndices[i]] = parts[i]
                                                    }
                                                } else {
                                                    for (idx in batchIndices) {
                                                        val original = contents[idx]!!
                                                        val singleTranslated = runCatching {
                                                            translator.translateBlocking(original, lang).translatedText
                                                        }.getOrNull() ?: original
                                                        translatedMap[idx] = singleTranslated
                                                    }
                                                }
                                            }
                                        }

                                        val out = mutableListOf<String>()
                                        for (i in contents.indices) {
                                            val stamp = stampsFor[i]
                                            val c = contents[i]
                                            if (c == null) {
                                                if (stamp != null) out.add(stamp) else out.add("")
                                            } else {
                                                val translatedText = translatedMap[i] ?: c
                                                if (stamp != null) out.add("$stamp $translatedText") else out.add(translatedText)
                                            }
                                        }

                                        out.joinToString("\n")
                                    }
                                    viewModel.updateLyrics(mediaMetadataProvider(), translatedLyrics)
                                    showTranslateDialog = false
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.translation_failed) + ": " + (e.localizedMessage ?: e.toString()),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isTranslating = false
                                }
                            }
                        }) {
                            Text(stringResource(R.string.translate))
                        }
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
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
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
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
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
                            viewModel.refetchLyrics(mediaMetadataProvider(), lyricsProvider())
                            onDismiss()
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchLyricsInputDialog(
    titleField: TextFieldValue,
    onTitleFieldChange: (TextFieldValue) -> Unit,
    artistField: TextFieldValue,
    onArtistFieldChange: (TextFieldValue) -> Unit,
    onDismiss: () -> Unit,
    onSearchOnline: () -> Unit,
    onSearch: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(24.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.search_lyrics),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = titleField,
                    onValueChange = onTitleFieldChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.song_title)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = artistField,
                    onValueChange = onArtistFieldChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.song_artists)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    )
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    FilledTonalButton(
                        onClick = onSearchOnline,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.language),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.search_online))
                    }

                    Button(onClick = onSearch) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.ui.utils.formatFileSize
import moe.koiverse.archivetune.utils.AudioExporter
import moe.koiverse.archivetune.utils.DuplicateHandling
import moe.koiverse.archivetune.utils.ExportConfig
import moe.koiverse.archivetune.utils.ExportDestination
import moe.koiverse.archivetune.utils.ExportResult
import moe.koiverse.archivetune.utils.FilenameTemplate
import moe.koiverse.archivetune.viewmodels.ExportSource
import moe.koiverse.archivetune.viewmodels.ExportState
import moe.koiverse.archivetune.viewmodels.ExportViewModel
import moe.koiverse.archivetune.viewmodels.ExportableSong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSongsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val songs by viewModel.exportableSongs.collectAsState()
    val config by viewModel.exportConfig.collectAsState()
    val progress by viewModel.exportProgress.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sourceFilter by viewModel.sourceFilter.collectAsState()

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.updateConfig(config.copy(destination = ExportDestination.SAF, safUri = it))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_songs)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                actions = {
                    if (progress.state == ExportState.IDLE) {
                        val anySelected = songs.any { it.isSelected }
                        IconButton(
                            onClick = {
                                if (anySelected) viewModel.deselectAll() else viewModel.selectAll()
                            }
                        ) {
                            Icon(
                                painterResource(
                                    if (anySelected) R.drawable.deselect else R.drawable.select_all
                                ),
                                contentDescription = stringResource(
                                    if (anySelected) R.string.export_deselect_all
                                    else R.string.export_select_all
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when (progress.state) {
            ExportState.IDLE -> SongSelectionContent(
                paddingValues = paddingValues,
                songs = songs,
                config = config,
                searchQuery = searchQuery,
                sourceFilter = sourceFilter,
                selectedCount = viewModel.selectedCount,
                selectedTotalSize = viewModel.selectedTotalSize,
                onSearchQueryChange = viewModel::setSearchQuery,
                onSourceFilterChange = viewModel::setSourceFilter,
                onToggleSelection = viewModel::toggleSelection,
                onConfigChange = viewModel::updateConfig,
                onBrowseFolder = { safLauncher.launch(null) },
                onStartExport = viewModel::startExport,
            )
            ExportState.EXPORTING -> ExportProgressContent(
                paddingValues = paddingValues,
                progress = progress,
                onCancel = viewModel::cancelExport,
            )
            ExportState.COMPLETE -> ExportResultsContent(
                paddingValues = paddingValues,
                progress = progress,
                onDone = {
                    viewModel.resetExport()
                },
                onOpenFolder = {
                    openMusicFolder(navController.context)
                },
            )
        }
    }
}

@Composable
private fun SongSelectionContent(
    paddingValues: PaddingValues,
    songs: List<ExportableSong>,
    config: ExportConfig,
    searchQuery: String,
    sourceFilter: ExportSource,
    selectedCount: Int,
    selectedTotalSize: Long,
    onSearchQueryChange: (String) -> Unit,
    onSourceFilterChange: (ExportSource) -> Unit,
    onToggleSelection: (String) -> Unit,
    onConfigChange: (ExportConfig) -> Unit,
    onBrowseFolder: () -> Unit,
    onStartExport: () -> Unit,
) {
    var showConfigSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text(stringResource(R.string.export_search_songs)) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.search), contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    FilterChip(
                        selected = sourceFilter == ExportSource.ALL,
                        onClick = { onSourceFilterChange(ExportSource.ALL) },
                        label = { Text(stringResource(R.string.export_source_all)) },
                    )
                    FilterChip(
                        selected = sourceFilter == ExportSource.DOWNLOAD,
                        onClick = { onSourceFilterChange(ExportSource.DOWNLOAD) },
                        label = { Text(stringResource(R.string.export_source_downloads)) },
                    )
                    FilterChip(
                        selected = sourceFilter == ExportSource.CACHE,
                        onClick = { onSourceFilterChange(ExportSource.CACHE) },
                        label = { Text(stringResource(R.string.export_source_cache)) },
                    )
                }
            }

            if (songs.isEmpty()) {
                item {
                    EmptyState()
                }
            }

            items(
                items = songs,
                key = { it.song.id },
            ) { item ->
                ExportSongItem(
                    item = item,
                    onToggle = { onToggleSelection(item.song.id) },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))

                ExportConfigCard(
                    config = config,
                    expanded = showConfigSection,
                    onToggleExpanded = { showConfigSection = !showConfigSection },
                    onConfigChange = onConfigChange,
                    onBrowseFolder = onBrowseFolder,
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        ExportBottomBar(
            selectedCount = selectedCount,
            selectedTotalSize = selectedTotalSize,
            onStartExport = onStartExport,
        )
    }
}

@Composable
private fun ExportSongItem(
    item: ExportableSong,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(40.dp),
            )

            AsyncImage(
                model = item.song.song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.song.song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = item.song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    item.song.format?.let { format ->
                        InfoBadge(
                            text = format.codecs.uppercase(),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        InfoBadge(
                            text = "${format.bitrate / 1000} kbps",
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        format.sampleRate?.let { sr ->
                            InfoBadge(
                                text = "${sr / 1000f} kHz",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }

                    InfoBadge(
                        text = formatFileSize(item.fileSize),
                        color = MaterialTheme.colorScheme.outline,
                    )

                    InfoBadge(
                        text = stringResource(
                            if (item.source == ExportableSong.SongSource.DOWNLOAD)
                                R.string.export_song_source_download
                            else R.string.export_song_source_cache
                        ),
                        color = if (item.source == ExportableSong.SongSource.DOWNLOAD)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
                    )
                }

                item.song.song.albumName?.let { album ->
                    Text(
                        text = album,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_download),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.export_no_songs),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.export_no_songs_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExportConfigCard(
    config: ExportConfig,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onConfigChange: (ExportConfig) -> Unit,
    onBrowseFolder: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp),
            ) {
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.settings),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.export_configuration),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        buildConfigSummary(config),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    painterResource(
                        if (expanded) R.drawable.expand_less else R.drawable.expand_more
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    SectionTitle(stringResource(R.string.export_destination))

                    DestinationOption(
                        selected = config.destination == ExportDestination.MEDIA_STORE,
                        title = stringResource(R.string.export_destination_music_folder),
                        subtitle = stringResource(R.string.export_destination_music_folder_desc),
                        onClick = {
                            onConfigChange(config.copy(destination = ExportDestination.MEDIA_STORE))
                        },
                    )
                    DestinationOption(
                        selected = config.destination == ExportDestination.SAF,
                        title = stringResource(R.string.export_destination_custom),
                        subtitle = config.safUri?.lastPathSegment
                            ?: stringResource(R.string.export_destination_choose_folder),
                        onClick = {
                            if (config.safUri != null) {
                                onConfigChange(config.copy(destination = ExportDestination.SAF))
                            } else {
                                onBrowseFolder()
                            }
                        },
                        trailingAction = {
                            OutlinedButton(
                                onClick = onBrowseFolder,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    stringResource(R.string.export_destination_choose_folder),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.export_filename_template))

                    FilenameTemplate.entries.forEach { template ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onConfigChange(config.copy(filenameTemplate = template))
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            RadioButton(
                                selected = config.filenameTemplate == template,
                                onClick = { onConfigChange(config.copy(filenameTemplate = template)) },
                            )
                            Text(
                                text = templateDisplayName(template),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    SectionTitle(stringResource(R.string.export_duplicate_handling))

                    DuplicateHandling.entries.forEach { handling ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onConfigChange(config.copy(duplicateHandling = handling))
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            RadioButton(
                                selected = config.duplicateHandling == handling,
                                onClick = {
                                    onConfigChange(config.copy(duplicateHandling = handling))
                                },
                            )
                            Text(
                                text = duplicateDisplayName(handling),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.export_embed_metadata),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(R.string.export_embed_metadata_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = config.embedMetadata,
                            onCheckedChange = {
                                onConfigChange(config.copy(embedMetadata = it))
                            },
                            thumbContent = {
                                Icon(
                                    painterResource(
                                        if (config.embedMetadata) R.drawable.done else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    }

                    AnimatedVisibility(visible = config.embedMetadata) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.export_include_cover_art),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    stringResource(R.string.export_include_cover_art_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = config.includeCoverArt,
                                onCheckedChange = {
                                    onConfigChange(config.copy(includeCoverArt = it))
                                },
                                thumbContent = {
                                    Icon(
                                        painterResource(
                                            if (config.includeCoverArt) R.drawable.done
                                            else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun DestinationOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailingAction?.invoke()
    }
}

@Composable
private fun ExportBottomBar(
    selectedCount: Int,
    selectedTotalSize: Long,
    onStartExport: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.export_songs_selected, selectedCount),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (selectedTotalSize > 0) {
                    Text(
                        text = stringResource(R.string.export_total_size, formatFileSize(selectedTotalSize)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onStartExport,
                enabled = selectedCount > 0,
            ) {
                Icon(
                    painterResource(R.drawable.export),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.export_start))
            }
        }
    }
}

@Composable
private fun ExportProgressContent(
    paddingValues: PaddingValues,
    progress: moe.koiverse.archivetune.viewmodels.ExportProgress,
    onCancel: () -> Unit,
) {
    val overallProgress by animateFloatAsState(
        targetValue = if (progress.total > 0)
            progress.completed.toFloat() / progress.total
        else 0f,
        label = "overallProgress",
    )

    val fileProgress by animateFloatAsState(
        targetValue = if (progress.currentTotalBytes > 0)
            progress.currentBytesWritten.toFloat() / progress.currentTotalBytes
        else 0f,
        label = "fileProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier.size(120.dp),
                strokeWidth = 8.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.export_progress, progress.completed, progress.total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${(overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.export_in_progress),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = progress.currentSongName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { fileProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )

        Text(
            text = "${formatFileSize(progress.currentBytesWritten)} / ${formatFileSize(progress.currentTotalBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.weight(1f))

        if (progress.results.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    progress.results.takeLast(3).forEach { result ->
                        ExportResultRow(result)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.export_cancel))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExportResultsContent(
    paddingValues: PaddingValues,
    progress: moe.koiverse.archivetune.viewmodels.ExportProgress,
    onDone: () -> Unit,
    onOpenFolder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Icon(
            painterResource(R.drawable.done),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape,
                )
                .padding(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.export_complete),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.export_result_summary,
                progress.completed,
                progress.failed,
                progress.skipped,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(progress.results) { result ->
                ExportResultRow(result)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onOpenFolder,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painterResource(R.drawable.open_in_new),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.export_open_folder))
            }
            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.export_done))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExportResultRow(result: ExportResult) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        val (icon, tint, statusText, detail) = when (result) {
            is ExportResult.Success -> listOf(
                R.drawable.done,
                MaterialTheme.colorScheme.primary,
                stringResource(R.string.export_status_success),
                result.path,
            )
            is ExportResult.Failed -> listOf(
                R.drawable.close,
                MaterialTheme.colorScheme.error,
                stringResource(R.string.export_status_failed),
                result.reason,
            )
            is ExportResult.Skipped -> listOf(
                R.drawable.info,
                MaterialTheme.colorScheme.tertiary,
                stringResource(R.string.export_status_skipped),
                result.reason,
            )
        }

        Icon(
            painterResource(icon as Int),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint as Color,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusText as String,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = tint,
            )
            Text(
                text = detail as String,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun buildConfigSummary(config: ExportConfig): String {
    val dest = when (config.destination) {
        ExportDestination.MEDIA_STORE -> stringResource(R.string.export_destination_music_folder)
        ExportDestination.SAF -> stringResource(R.string.export_destination_custom)
    }
    val template = templateDisplayName(config.filenameTemplate)
    return "$dest · $template"
}

@Composable
private fun templateDisplayName(template: FilenameTemplate): String = when (template) {
    FilenameTemplate.TITLE_ARTIST -> stringResource(R.string.export_filename_title_artist)
    FilenameTemplate.ARTIST_TITLE -> stringResource(R.string.export_filename_artist_title)
    FilenameTemplate.TITLE_ONLY -> stringResource(R.string.export_filename_title_only)
    FilenameTemplate.ARTIST_ALBUM_TITLE -> stringResource(R.string.export_filename_artist_album_title)
}

@Composable
private fun duplicateDisplayName(handling: DuplicateHandling): String = when (handling) {
    DuplicateHandling.SKIP -> stringResource(R.string.export_duplicate_skip)
    DuplicateHandling.OVERWRITE -> stringResource(R.string.export_duplicate_overwrite)
    DuplicateHandling.RENAME -> stringResource(R.string.export_duplicate_rename)
}

private fun openMusicFolder(context: android.content.Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music/ArchiveTune")
                setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                data = Uri.fromFile(java.io.File(musicDir, "ArchiveTune"))
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

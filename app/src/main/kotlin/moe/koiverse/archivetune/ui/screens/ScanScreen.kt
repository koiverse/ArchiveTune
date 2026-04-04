@file:OptIn(ExperimentalMaterial3Api::class)

package moe.koiverse.archivetune.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.viewmodels.ScanState
import moe.koiverse.archivetune.viewmodels.ScanViewModel

@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsState()
    val localSongCount by viewModel.localSongCount.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()
    val excludedFolders by viewModel.excludedFolders.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.checkPermissionAndSetState()
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.startSafFolderScan(it)
        }
    }

    val excludeFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.addExcludedFolder(it.toString())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndSetState()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.scan_library)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        )

        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item(key = "permission") {
                AnimatedVisibility(
                    visible = scanState is ScanState.PermissionRequired,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ElevatedCard(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.grant_permission),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = stringResource(R.string.audio_permission_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            FilledTonalButton(
                                onClick = {
                                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    permissionLauncher.launch(permission)
                                },
                            ) {
                                Text(stringResource(R.string.grant_permission))
                            }
                        }
                    }
                }
            }

            item(key = "scan_actions") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ElevatedCard(
                        shape = RoundedCornerShape(28.dp),
                        onClick = {
                            if (viewModel.hasAudioPermission()) {
                                viewModel.startMediaStoreScan()
                            } else {
                                viewModel.checkPermissionAndSetState()
                            }
                        },
                        enabled = scanState !is ScanState.Scanning,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.storage),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.scan_device),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.scan_device_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        shape = RoundedCornerShape(28.dp),
                        onClick = { safFolderLauncher.launch(null) },
                        enabled = scanState !is ScanState.Scanning,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.library_music),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.pick_folder),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.pick_folder_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "progress") {
                AnimatedVisibility(
                    visible = scanState is ScanState.Scanning,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val scanning = scanState as? ScanState.Scanning
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.scanning),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(12.dp))
                            if (scanning != null && scanning.total > 0) {
                                LinearProgressIndicator(
                                    progress = { scanning.progress.toFloat() / scanning.total },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${scanning.progress} / ${scanning.total}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            item(key = "result") {
                AnimatedVisibility(
                    visible = scanState is ScanState.Complete,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val result = (scanState as? ScanState.Complete)?.result
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.scan_complete),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(12.dp))
                            if (result != null) {
                                ResultRow(stringResource(R.string.imported), result.imported)
                                ResultRow(stringResource(R.string.updated), result.updated)
                                ResultRow(stringResource(R.string.skipped), result.skipped)
                                ResultRow(stringResource(R.string.failed), result.failed)
                                ResultRow(stringResource(R.string.total_found), result.totalFound)
                            }
                            Spacer(Modifier.height(16.dp))
                            FilledTonalButton(
                                onClick = { navController.navigate("local_songs") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(painterResource(R.drawable.music_note), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.view_local_songs))
                            }
                        }
                    }
                }
            }

            item(key = "error") {
                AnimatedVisibility(
                    visible = scanState is ScanState.Error,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val error = (scanState as? ScanState.Error)?.message
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.scan_error),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            if (error != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = { viewModel.resetState() }) {
                                Text(stringResource(R.string.scan_again))
                            }
                        }
                    }
                }
            }

            item(key = "info") {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.local_songs),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = pluralStringResource(R.plurals.n_song, localSongCount, localSongCount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.last_scan),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (lastScanTime > 0L) {
                                    DateUtils.getRelativeTimeSpanString(
                                        lastScanTime,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS,
                                    ).toString()
                                } else {
                                    stringResource(R.string.never)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (localSongCount > 0) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { navController.navigate("local_songs") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.view_local_songs))
                            }
                        }
                    }
                }
            }

            item(key = "excluded_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.excluded_folders),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.IconButton(
                        onClick = { excludeFolderLauncher.launch(null) },
                    ) {
                        Icon(
                            painterResource(R.drawable.add),
                            contentDescription = stringResource(R.string.add_excluded_folder),
                        )
                    }
                }
            }

            if (excludedFolders.isEmpty()) {
                item(key = "no_excluded") {
                    Text(
                        text = stringResource(R.string.no_excluded_folders),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = excludedFolders.toList(),
                    key = { it },
                ) { folder ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.block),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = android.net.Uri.decode(folder.substringAfterLast(":")),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.removeExcludedFolder(folder) },
                            ) {
                                Icon(
                                    painterResource(R.drawable.close),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Int) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

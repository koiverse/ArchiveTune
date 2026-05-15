/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */


@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.menu.AddToPlaylistDialogOnline
import moe.koiverse.archivetune.ui.menu.LoadingScreen
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.viewmodels.BackupCategory
import moe.koiverse.archivetune.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.floor

private val CardShape = RoundedCornerShape(28.dp)
private val InnerTileShape = RoundedCornerShape(22.dp)
private val HeroIconSize = 72.dp
private val QuickTileIconSize = 48.dp
private val RowIconSize = 42.dp
private const val PressScale = 0.96f

private val CSV_MIME_TYPES =
    arrayOf(
        "text/csv",
        "text/x-csv",
        "text/comma-separated-values",
        "text/x-comma-separated-values",
        "application/csv",
        "application/x-csv",
        "application/vnd.ms-excel",
        "text/plain",
        "text/*",
        "application/octet-stream",
    )

@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by rememberSaveable {
        mutableStateOf(false)
    }

    var isProgressStarted by rememberSaveable {
        mutableStateOf(false)
    }

    var progressStatus by remember { mutableStateOf("") }

    var progressPercentage by rememberSaveable {
        mutableIntStateOf(0)
    }

    var showBackupOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var pendingBackupCategories by remember { mutableStateOf(BackupCategory.entries.toSet()) }
    var pendingRestoreCategories by remember { mutableStateOf(BackupCategory.entries.toSet()) }

    val backupRestoreProgress by viewModel.backupRestoreProgress.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri, pendingBackupCategories)
            }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(context, uri, pendingRestoreCategories)
            }
        }
    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val result = viewModel.importPlaylistFromCsv(context, uri)
                importedSongs.clear()
                importedSongs.addAll(result)

                if (importedSongs.isNotEmpty()) {
                    showChoosePlaylistDialogOnline = true
                }
            }
        }
    val importM3uLauncherOnline = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val result = viewModel.loadM3UOnline(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)

            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.backup_restore),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                BackupRestoreHeroCard()
            }

            item {
                ActionTilesRow(
                    onBackupClick = { showBackupOptionsDialog = true },
                    onRestoreClick = { showRestoreOptionsDialog = true },
                )
            }

            item {
                ImportSectionCard(
                    onImportM3u = { importM3uLauncherOnline.launch(arrayOf("audio/*")) },
                    onImportCsv = { importPlaylistFromCsv.launch(CSV_MIME_TYPES) },
                )
            }
        }
    }

    if (showBackupOptionsDialog) {
        BackupOptionsDialog(
            title = stringResource(R.string.backup_options_title),
            confirmLabel = stringResource(R.string.action_backup),
            onConfirm = { categories ->
                pendingBackupCategories = categories
                showBackupOptionsDialog = false
                val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                backupLauncher.launch(
                    "${context.getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup"
                )
            },
            onDismiss = { showBackupOptionsDialog = false },
        )
    }

    if (showRestoreOptionsDialog) {
        BackupOptionsDialog(
            title = stringResource(R.string.restore_options_title),
            confirmLabel = stringResource(R.string.action_restore),
            onConfirm = { categories ->
                pendingRestoreCategories = categories
                showRestoreOptionsDialog = false
                restoreLauncher.launch(arrayOf("application/octet-stream"))
            },
            onDismiss = { showRestoreOptionsDialog = false },
        )
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { newVal -> isProgressStarted = newVal },
        onPercentageChange = { newPercentage -> progressPercentage = newPercentage },
        onStatusChange = { progressStatus = it }
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    LoadingScreen(
        isVisible = backupRestoreProgress != null || isProgressStarted,
        value = backupRestoreProgress?.percent ?: progressPercentage,
        title = backupRestoreProgress?.title,
        stepText = backupRestoreProgress?.step ?: progressStatus,
        indeterminate = backupRestoreProgress?.indeterminate ?: false,
    )
}

@Composable
private fun BackupRestoreHeroCard() {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PressScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "heroScale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0f),
                        ),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(HeroIconSize)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.backup),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.backup_restore),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = ".backup  ·  .m3u  ·  .csv",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormatLabel(text = ".backup", color = MaterialTheme.colorScheme.primaryContainer)
                FormatLabel(text = ".m3u", color = MaterialTheme.colorScheme.tertiaryContainer)
                FormatLabel(text = ".csv", color = MaterialTheme.colorScheme.secondaryContainer)
            }
        }
    }
}

@Composable
private fun FormatLabel(
    text: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.65f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionTilesRow(
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionTile(
            modifier = Modifier.weight(1f),
            icon = painterResource(R.drawable.backup),
            label = stringResource(R.string.action_backup),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onBackupClick,
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            icon = painterResource(R.drawable.restore),
            label = stringResource(R.string.action_restore),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onRestoreClick,
        )
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier = Modifier,
    icon: Painter,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileElevation",
    )

    Surface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = InnerTileShape,
        color = containerColor,
        tonalElevation = elevation,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = contentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(QuickTileIconSize),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ImportSectionCard(
    onImportM3u: () -> Unit,
    onImportCsv: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.import_playlist),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                ImportActionRow(
                    icon = painterResource(R.drawable.playlist_import),
                    title = stringResource(R.string.import_online),
                    subtitle = "audio/*",
                    onClick = onImportM3u,
                )

                SectionDivider()

                ImportActionRow(
                    icon = painterResource(R.drawable.playlist_add),
                    title = stringResource(R.string.import_csv),
                    subtitle = "text/csv",
                    onClick = onImportCsv,
                )
            }
        }
    }
}

@Composable
private fun ImportActionRow(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rowScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(InnerTileShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExpressiveRowIcon(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun ExpressiveRowIcon(
    icon: Painter,
    tint: Color,
) {
    Surface(
        modifier = Modifier.size(RowIconSize),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.10f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = tint,
            )
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp, end = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}

@Composable
private fun BackupOptionsDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (Set<BackupCategory>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(BackupCategory.entries.toSet()) }
    val density = LocalDensity.current
    val cbStrokeWidthPx = remember(density) { with(density) { floor(CheckboxDefaults.StrokeWidth.toPx()) } }
    val cbCheckmarkStroke = remember(cbStrokeWidthPx) {
        Stroke(width = cbStrokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val cbOutlineStroke = remember(cbStrokeWidthPx) { Stroke(width = cbStrokeWidthPx) }

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(title) },
        buttons = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(
                onClick = { onConfirm(selected) },
                shapes = ButtonDefaults.shapes(),
                enabled = selected.isNotEmpty(),
            ) {
                Text(confirmLabel)
            }
        },
    ) {
        Spacer(Modifier.height(8.dp))
        BackupCategory.entries.forEach { category ->
            val isChecked = category in selected
            val labelRes = when (category) {
                BackupCategory.LIBRARY -> R.string.backup_category_library
                BackupCategory.ACCOUNT -> R.string.backup_category_account
                BackupCategory.SETTINGS -> R.string.backup_category_settings
            }
            val descRes = when (category) {
                BackupCategory.LIBRARY -> R.string.backup_category_library_desc
                BackupCategory.ACCOUNT -> R.string.backup_category_account_desc
                BackupCategory.SETTINGS -> R.string.backup_category_settings_desc
            }
            val iconRes = when (category) {
                BackupCategory.LIBRARY -> R.drawable.library_music
                BackupCategory.ACCOUNT -> R.drawable.account
                BackupCategory.SETTINGS -> R.drawable.settings
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                    ) {
                        selected = if (isChecked) selected - category else selected + category
                    }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(descRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        selected = if (checked) selected + category else selected - category
                    },
                    checkmarkStroke = cbCheckmarkStroke,
                    outlineStroke = cbOutlineStroke,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

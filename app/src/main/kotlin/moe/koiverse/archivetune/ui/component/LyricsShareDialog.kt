@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.component

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.palette.graphics.Palette
import androidx.window.core.layout.WindowSizeClass
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.utils.ComposeToImage

fun shareLyricsAsText(
    context: Context,
    payload: LyricsSharePayload,
    songId: String?,
) {
    val songLink = songId?.takeIf { it.isNotBlank() }?.let { "https://music.youtube.com/watch?v=$it" }
    val shareBody =
        buildString {
            append("\"")
            append(payload.lyricsText)
            append("\"\n\n")
            append(payload.songTitle)
            append(" - ")
            append(payload.artists)
            if (songLink != null) {
                append('\n')
                append(songLink)
            }
        }

    val shareIntent =
        Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.share_lyrics),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsShareImageDialog(
    mediaMetadata: MediaMetadata?,
    payload: LyricsSharePayload,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompactLayout =
        !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    var isSharing by remember { mutableStateOf(false) }
    var selectedGlassStyle by remember { mutableStateOf(LyricsGlassStyle.FrostedDark) }
    var paletteGlassStyle by remember { mutableStateOf<LyricsGlassStyle?>(null) }
    var options by remember { mutableStateOf(LyricsShareImageOptions()) }

    LaunchedEffect(mediaMetadata?.thumbnailUrl) {
        val coverUrl = mediaMetadata?.thumbnailUrl
        if (coverUrl == null) {
            paletteGlassStyle = null
            return@LaunchedEffect
        }
        val extractedStyle = withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()
                val bitmap = loader.execute(request).image?.toBitmap() ?: return@runCatching null
                LyricsGlassStyle.fromPalette(Palette.from(bitmap).generate())
            }.getOrNull()
        }
        paletteGlassStyle = extractedStyle
    }

    val availableStyles by remember(paletteGlassStyle) {
        derivedStateOf {
            buildList {
                paletteGlassStyle?.let(::add)
                addAll(LyricsGlassStyle.allPresets.filterNot { it == paletteGlassStyle })
            }
        }
    }

    val handleShare: () -> Unit = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(context, R.string.lyrics_share_export_not_supported, Toast.LENGTH_SHORT).show()
        } else {
            isSharing = true
            scope.launch {
                try {
                    val image = ComposeToImage.createLyricsImage(
                        context = context,
                        coverArtUrl = mediaMetadata?.thumbnailUrl,
                        songTitle = payload.songTitle,
                        artistName = payload.artists,
                        lyrics = payload.lyricsText,
                        width = options.aspectRatio.exportWidth,
                        height = options.aspectRatio.exportHeight,
                        glassStyle = selectedGlassStyle,
                        shareOptions = options,
                    )
                    val fileName = "lyrics_${System.currentTimeMillis()}"
                    val uri = ComposeToImage.saveBitmapAsFile(context, image, fileName)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.share_lyrics),
                        ),
                    )
                    onDismissRequest()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.lyrics_share_export_failed, e.message ?: ""),
                        Toast.LENGTH_SHORT,
                    ).show()
                } finally {
                    isSharing = false
                }
            }
        }
    }

    if (isCompactLayout) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isSharing) onDismissRequest()
            },
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { LyricsShareDragHandle() },
            modifier = Modifier.fillMaxHeight(),
        ) {
            LyricsShareStudioScaffold(
                mediaMetadata = mediaMetadata,
                payload = payload,
                options = options,
                onOptionsChange = { options = it },
                availableStyles = availableStyles,
                selectedGlassStyle = selectedGlassStyle,
                onStyleSelect = { selectedGlassStyle = it },
                isSharing = isSharing,
                isCompactLayout = true,
                onShare = handleShare,
                onDismiss = onDismissRequest,
            )
        }
    } else {
        Dialog(
            onDismissRequest = {
                if (!isSharing) onDismissRequest()
            },
            properties = DialogProperties(
                dismissOnBackPress = !isSharing,
                dismissOnClickOutside = !isSharing,
                usePlatformDefaultWidth = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                ) {
                    LyricsShareStudioScaffold(
                        mediaMetadata = mediaMetadata,
                        payload = payload,
                        options = options,
                        onOptionsChange = { options = it },
                        availableStyles = availableStyles,
                        selectedGlassStyle = selectedGlassStyle,
                        onStyleSelect = { selectedGlassStyle = it },
                        isSharing = isSharing,
                        isCompactLayout = false,
                        onShare = handleShare,
                        onDismiss = onDismissRequest,
                    )
                }
            }
        }
    }

    if (isSharing) {
        LyricsShareLoadingDialog()
    }
}

@Composable
private fun LyricsShareLoadingDialog() {
    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                Text(
                    text = stringResource(R.string.generating_image),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.please_wait),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LyricsShareStudioScaffold(
    mediaMetadata: MediaMetadata?,
    payload: LyricsSharePayload,
    options: LyricsShareImageOptions,
    onOptionsChange: (LyricsShareImageOptions) -> Unit,
    availableStyles: List<LyricsGlassStyle>,
    selectedGlassStyle: LyricsGlassStyle,
    onStyleSelect: (LyricsGlassStyle) -> Unit,
    isSharing: Boolean,
    isCompactLayout: Boolean,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = if (isCompactLayout) 760.dp else 860.dp)
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isCompactLayout) 20.dp else 32.dp,
                    vertical = if (isCompactLayout) 12.dp else 28.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (isCompactLayout) 20.dp else 24.dp),
        ) {
            LyricsShareHero(
                payload = payload,
                options = options,
                modifier = Modifier.fillMaxWidth(),
            )
            PreviewContainer(
                payload = payload,
                mediaMetadata = mediaMetadata,
                selectedGlassStyle = selectedGlassStyle,
                options = options,
                isCompactLayout = isCompactLayout,
                modifier = Modifier.fillMaxWidth(),
            )
            ControlsSection(
                options = options,
                onOptionsChange = onOptionsChange,
                availableStyles = availableStyles,
                selectedGlassStyle = selectedGlassStyle,
                onStyleSelect = onStyleSelect,
                isCompactLayout = isCompactLayout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ActionsSection(
            isSharing = isSharing,
            isCompactLayout = isCompactLayout,
            onShare = onShare,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun LyricsShareHero(
    payload: LyricsSharePayload,
    options: LyricsShareImageOptions,
    modifier: Modifier = Modifier,
) {
    val lyricSnippet =
        remember(payload.lyricsText) {
            payload.lyricsText
                .lineSequence()
                .map(String::trim)
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        ),
                    ),
                    shape = RoundedCornerShape(36.dp),
                )
                .padding(28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = payload.songTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = payload.artists,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (lyricSnippet.isNotBlank()) {
                    Text(
                        text = lyricSnippet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LyricsShareInfoPill(text = stringResource(options.aspectRatio.labelRes))
                    LyricsShareInfoPill(
                        text = "${options.aspectRatio.exportWidth} x ${options.aspectRatio.exportHeight}",
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewContainer(
    payload: LyricsSharePayload,
    mediaMetadata: MediaMetadata?,
    selectedGlassStyle: LyricsGlassStyle,
    options: LyricsShareImageOptions,
    isCompactLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    val previewWidthFraction =
        when (options.aspectRatio) {
            LyricsShareAspectRatio.Square -> 1f
            LyricsShareAspectRatio.Portrait -> if (isCompactLayout) 0.78f else 0.62f
            LyricsShareAspectRatio.Story -> if (isCompactLayout) 0.56f else 0.44f
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedGlassStyle.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${options.aspectRatio.exportWidth} x ${options.aspectRatio.exportHeight}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                LyricsShareInfoPill(
                    text = stringResource(options.aspectRatio.labelRes),
                    emphasized = true,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(previewWidthFraction)
                        .aspectRatio(options.aspectRatio.previewAspectRatio)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    selectedGlassStyle.surfaceTint.copy(alpha = 0.28f),
                                    selectedGlassStyle.overlayColor.copy(alpha = 0.22f),
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                                ),
                            ),
                            shape = RoundedCornerShape(32.dp),
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LyricsImageCard(
                        lyricText = payload.lyricsText,
                        songTitle = payload.songTitle,
                        artistName = payload.artists,
                        coverArtUrl = mediaMetadata?.thumbnailUrl,
                        glassStyle = selectedGlassStyle,
                        shareOptions = options,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlsSection(
    options: LyricsShareImageOptions,
    onOptionsChange: (LyricsShareImageOptions) -> Unit,
    availableStyles: List<LyricsGlassStyle>,
    selectedGlassStyle: LyricsGlassStyle,
    onStyleSelect: (LyricsGlassStyle) -> Unit,
    isCompactLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        LyricsShareControlCard(title = stringResource(R.string.lyrics_share_layout)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val entries = LyricsShareAspectRatio.entries
                entries.forEachIndexed { index, aspectRatio ->
                    SegmentedButton(
                        selected = options.aspectRatio == aspectRatio,
                        onClick = { onOptionsChange(options.copy(aspectRatio = aspectRatio)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = entries.size),
                        icon = {},
                    ) {
                        Text(
                            text = stringResource(aspectRatio.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        LyricsShareControlCard(title = stringResource(R.string.customize_colors)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = if (isCompactLayout) 2 else 3,
            ) {
                availableStyles.forEach { style ->
                    LyricsStyleOption(
                        style = style,
                        selected = selectedGlassStyle == style,
                        onClick = { onStyleSelect(style) },
                    )
                }
            }
        }

        LyricsShareControlCard(title = stringResource(R.string.more_options)) {
            LyricsShareSlider(
                title = stringResource(R.string.lyrics_share_background_blur),
                valueLabel = stringResource(R.string.lyrics_share_background_blur_value, options.sanitizedBlurRadius.toInt()),
                value = options.blurRadius,
                onValueChange = { onOptionsChange(options.copy(blurRadius = it)) },
                valueRange = 0f..48f,
            )
            LyricsShareSlider(
                title = stringResource(R.string.lyrics_share_background_dim),
                valueLabel = stringResource(R.string.lyrics_share_background_dim_value, (options.sanitizedDimAmount * 100).toInt()),
                value = options.dimAmount,
                onValueChange = { onOptionsChange(options.copy(dimAmount = it)) },
                valueRange = 0.6f..1.6f,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onOptionsChange(options.copy(showArtwork = !options.showArtwork)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lyrics_share_show_cover),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.lyrics_share_show_cover_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Switch(
                    checked = options.showArtwork,
                    onCheckedChange = { onOptionsChange(options.copy(showArtwork = it)) },
                )
            }
        }
    }
}

@Composable
private fun LyricsShareControlCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun LyricsShareInfoPill(
    text: String,
    emphasized: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = if (emphasized) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (emphasized) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LyricsStyleOption(
    style: LyricsGlassStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        },
        label = "lyricsStyleBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        },
        label = "lyricsStyleContainer",
    )

    Column(
        modifier = modifier
            .width(108.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(28.dp),
            color = containerColor,
            border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = borderColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                style.surfaceTint.copy(alpha = 0.8f),
                                style.overlayColor.copy(alpha = 0.6f),
                            ),
                        ),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = style.surfaceTint.copy(alpha = style.surfaceAlpha),
                            shape = RoundedCornerShape(20.dp),
                        ),
                )
                Text(
                    text = "Aa",
                    color = style.textColor,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Text(
            text = style.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun LyricsShareSlider(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionsSection(
    isSharing: Boolean,
    isCompactLayout: Boolean,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isCompactLayout) 20.dp else 32.dp,
                vertical = if (isCompactLayout) 20.dp else 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onShare,
            enabled = !isSharing,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.share),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        TextButton(
            onClick = onDismiss,
            enabled = !isSharing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
        ) {
            Text(
                text = stringResource(R.string.cancel),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun LyricsShareDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            ),
    )
}

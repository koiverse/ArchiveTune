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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.palette.graphics.Palette
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

    val availableStyles = remember(paletteGlassStyle) {
        buildList {
            paletteGlassStyle?.let(::add)
            addAll(LyricsGlassStyle.allPresets.filterNot { it == paletteGlassStyle })
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

    if (isSharing) {
        BasicAlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text(
                        text = stringResource(R.string.generating_image),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = { if (!isSharing) onDismissRequest() },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(24.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(40.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(32.dp))
                PreviewContainer(
                    payload = payload,
                    mediaMetadata = mediaMetadata,
                    selectedGlassStyle = selectedGlassStyle,
                    options = options,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
                ControlsSection(
                    options = options,
                    onOptionsChange = { options = it },
                    availableStyles = availableStyles,
                    selectedGlassStyle = selectedGlassStyle,
                    onStyleSelect = { selectedGlassStyle = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
                ActionsSection(
                    isSharing = isSharing,
                    onShare = handleShare,
                    onDismiss = onDismissRequest
                )
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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(options.aspectRatio.previewAspectRatio)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                shape = RoundedCornerShape(32.dp),
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
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

@Composable
private fun ControlsSection(
    options: LyricsShareImageOptions,
    onOptionsChange: (LyricsShareImageOptions) -> Unit,
    availableStyles: List<LyricsGlassStyle>,
    selectedGlassStyle: LyricsGlassStyle,
    onStyleSelect: (LyricsGlassStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        LyricsShareSection(title = stringResource(R.string.lyrics_share_layout)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LyricsShareAspectRatio.entries.forEach { aspectRatio ->
                    val selected = options.aspectRatio == aspectRatio
                    LyricsShareChoiceChip(
                        label = stringResource(aspectRatio.labelRes),
                        selected = selected,
                        onClick = { onOptionsChange(options.copy(aspectRatio = aspectRatio)) },
                    )
                }
            }
        }

        LyricsShareSection(title = stringResource(R.string.customize_colors)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                availableStyles.forEach { style ->
                    val selected = selectedGlassStyle == style
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        style.surfaceTint.copy(alpha = 0.8f),
                                        style.overlayColor.copy(alpha = 0.6f),
                                    ),
                                ),
                                shape = RoundedCornerShape(28.dp),
                            )
                            .border(
                                width = if (selected) 4.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(28.dp),
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { onStyleSelect(style) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    style.surfaceTint.copy(alpha = style.surfaceAlpha),
                                    RoundedCornerShape(20.dp),
                                ),
                        )
                        Text(
                            text = "Aa",
                            color = style.textColor,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(32.dp),
                )
                .clip(RoundedCornerShape(32.dp))
                .clickable { onOptionsChange(options.copy(showArtwork = !options.showArtwork)) }
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lyrics_share_show_cover),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.lyrics_share_show_cover_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Switch(
                checked = options.showArtwork,
                onCheckedChange = null,
            )
        }
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
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onShare,
            enabled = !isSharing,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
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
                .height(64.dp),
            shape = CircleShape
        ) {
            Text(
                text = stringResource(R.string.cancel),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun LyricsShareSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun LyricsShareChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                shape = CircleShape,
            )
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */
 

package moe.koiverse.archivetune.ui.component

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    context: android.content.Context,
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
        val extractedStyle =
            withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(context)
                val request =
                    ImageRequest.Builder(context)
                        .data(coverUrl)
                        .allowHardware(false)
                        .build()
                val bitmap = loader.execute(request).image?.toBitmap() ?: return@runCatching null
                LyricsGlassStyle.fromPalette(Palette.from(bitmap).generate())
            }.getOrNull()
        }
        paletteGlassStyle = extractedStyle
    }

    val availableStyles =
        remember(paletteGlassStyle) {
            buildList {
                paletteGlassStyle?.let(::add)
                addAll(LyricsGlassStyle.allPresets.filterNot { it == paletteGlassStyle })
            }
        }

    if (isSharing) {
        BasicAlertDialog(onDismissRequest = {}) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Text(
                        text = stringResource(R.string.generating_image),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    BasicAlertDialog(onDismissRequest = { if (!isSharing) onDismissRequest() }) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(options.aspectRatio.previewAspectRatio)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(24.dp),
                            )
                            .padding(8.dp),
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

                Spacer(modifier = Modifier.height(20.dp))

                LyricsShareSection(title = stringResource(R.string.lyrics_share_layout)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LyricsShareAspectRatio.entries.forEach { aspectRatio ->
                            val selected = options.aspectRatio == aspectRatio
                            LyricsShareChoiceChip(
                                label = stringResource(aspectRatio.labelRes),
                                selected = selected,
                                onClick = { options = options.copy(aspectRatio = aspectRatio) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LyricsShareSection(title = stringResource(R.string.customize_colors)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        availableStyles.forEach { style ->
                            val selected = selectedGlassStyle == style
                            Box(
                                modifier =
                                    Modifier
                                        .size(width = 72.dp, height = 72.dp)
                                        .background(
                                            brush =
                                                Brush.verticalGradient(
                                                    colors =
                                                        listOf(
                                                            style.surfaceTint.copy(alpha = 0.6f),
                                                            style.overlayColor.copy(alpha = 0.45f),
                                                        ),
                                                ),
                                            shape = RoundedCornerShape(18.dp),
                                        )
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color =
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                },
                                            shape = RoundedCornerShape(18.dp),
                                        )
                                        .clickable { selectedGlassStyle = style }
                                        .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .matchParentSize()
                                            .background(
                                                style.surfaceTint.copy(alpha = style.surfaceAlpha),
                                                RoundedCornerShape(12.dp),
                                            ),
                                )
                                Text(
                                    text = "Aa",
                                    color = style.textColor,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LyricsShareSection(
                    title = stringResource(R.string.lyrics_share_background_blur),
                    subtitle = stringResource(R.string.lyrics_share_background_blur_value, options.sanitizedBlurRadius.toInt()),
                ) {
                    Slider(
                        value = options.blurRadius,
                        onValueChange = { options = options.copy(blurRadius = it) },
                        valueRange = 0f..48f,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LyricsShareSection(
                    title = stringResource(R.string.lyrics_share_background_dim),
                    subtitle = stringResource(R.string.lyrics_share_background_dim_value, (options.sanitizedDimAmount * 100).toInt()),
                ) {
                    Slider(
                        value = options.dimAmount,
                        onValueChange = { options = options.copy(dimAmount = it) },
                        valueRange = 0.6f..1.6f,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.lyrics_share_show_cover),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = stringResource(R.string.lyrics_share_show_cover_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    Switch(
                        checked = options.showArtwork,
                        onCheckedChange = { options = options.copy(showArtwork = it) },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            Toast.makeText(context, R.string.lyrics_share_export_not_supported, Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSharing = true
                        scope.launch {
                            try {
                                val image =
                                    ComposeToImage.createLyricsImage(
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
                                val shareIntent =
                                    Intent(Intent.ACTION_SEND).apply {
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
                    },
                    enabled = !isSharing,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(
                        text = stringResource(R.string.share),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onDismissRequest,
                    enabled = !isSharing,
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun LyricsShareSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
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
        modifier =
            Modifier
                .background(
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    shape = CircleShape,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

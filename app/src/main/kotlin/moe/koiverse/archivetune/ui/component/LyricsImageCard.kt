/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.UseSystemFontKey
import moe.koiverse.archivetune.utils.rememberPreference

@Composable
fun rememberAdjustedFontSize(
    text: String,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
    initialFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 14.sp,
    style: TextStyle = TextStyle.Default,
    textMeasurer: androidx.compose.ui.text.TextMeasurer? = null,
): TextUnit {
    val measurer = textMeasurer ?: rememberTextMeasurer()

    var calculatedFontSize by remember(text, maxWidth, maxHeight, style, density) {
        val initialSize =
            when {
                text.length < 50 -> initialFontSize
                text.length < 100 -> (initialFontSize.value * 0.8f).sp
                text.length < 200 -> (initialFontSize.value * 0.6f).sp
                else -> (initialFontSize.value * 0.5f).sp
            }
        mutableStateOf(initialSize)
    }

    LaunchedEffect(text, maxWidth, maxHeight) {
        val targetWidthPx = with(density) { maxWidth.toPx() * 0.92f }
        val targetHeightPx = with(density) { maxHeight.toPx() * 0.92f }
        if (text.isBlank()) {
            calculatedFontSize = minFontSize
            return@LaunchedEffect
        }

        if (text.length < 20) {
            val largerSize = (initialFontSize.value * 1.1f).sp
            val result = measurer.measure(text = AnnotatedString(text), style = style.copy(fontSize = largerSize))
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                calculatedFontSize = largerSize
                return@LaunchedEffect
            }
        } else if (text.length < 30) {
            val largerSize = (initialFontSize.value * 0.9f).sp
            val result = measurer.measure(text = AnnotatedString(text), style = style.copy(fontSize = largerSize))
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                calculatedFontSize = largerSize
                return@LaunchedEffect
            }
        }

        var minSize = minFontSize.value
        var maxSize = initialFontSize.value
        var bestFit = minSize
        var iterations = 0

        while (minSize <= maxSize && iterations < 20) {
            iterations++
            val midSize = (minSize + maxSize) / 2
            val result = measurer.measure(text = AnnotatedString(text), style = style.copy(fontSize = midSize.sp))
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                bestFit = midSize
                minSize = midSize + 0.5f
            } else {
                maxSize = midSize - 0.5f
            }
        }

        calculatedFontSize = if (bestFit < minFontSize.value) minFontSize else bestFit.sp
    }

    return calculatedFontSize
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricsImageCard(
    lyricText: String,
    songTitle: String,
    artistName: String,
    coverArtUrl: String?,
    glassStyle: LyricsGlassStyle = LyricsGlassStyle.FrostedDark,
    shareOptions: LyricsShareImageOptions = LyricsShareImageOptions(),
    textColor: Color? = null,
    secondaryTextColor: Color? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)
    val lyricsFontFamily =
        remember(useSystemFont) {
            if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold))
        }

    val mainTextColor = textColor ?: glassStyle.textColor
    val secondaryColor = secondaryTextColor ?: glassStyle.secondaryTextColor
    val effectiveBlurRadius = shareOptions.previewBlurRadius
    val dimAlpha = (glassStyle.backgroundDimAlpha * shareOptions.sanitizedDimAmount).coerceIn(0f, 0.95f)

    val artworkPainter =
        rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(coverArtUrl)
                .crossfade(true)
                .build(),
        )

    var glassComponentSize by remember { mutableStateOf(Size.Zero) }
    val lensCenter = remember(glassComponentSize) { Offset(glassComponentSize.width / 2f, glassComponentSize.height / 2f) }
    val lensSize = remember(glassComponentSize) { Size(glassComponentSize.width, glassComponentSize.height) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (coverArtUrl != null) {
            Image(
                painter = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .cloudy(radius = effectiveBlurRadius),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = dimAlpha * 0.75f),
                                    Color.Black.copy(alpha = dimAlpha),
                                    Color.Black.copy(alpha = (dimAlpha * 1.15f).coerceAtMost(0.98f)),
                                ),
                        ),
                    ),
            )

        val glassShape = RoundedCornerShape(22.dp)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clip(glassShape)
                    .onSizeChanged { size ->
                        glassComponentSize = Size(size.width.toFloat(), size.height.toFloat())
                    },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .cloudy(radius = effectiveBlurRadius)
                        .then(
                            if (glassComponentSize.width > 0f && glassComponentSize.height > 0f) {
                                Modifier.liquidGlass(
                                    lensCenter = lensCenter,
                                    lensSize = lensSize,
                                    cornerRadius = glassStyle.glassCornerRadius,
                                    refraction = glassStyle.refraction,
                                    curve = glassStyle.curve,
                                    dispersion = glassStyle.dispersion,
                                    saturation = glassStyle.glassSaturation,
                                    contrast = glassStyle.glassContrast,
                                    tint = glassStyle.glassTint,
                                    edge = glassStyle.glassEdge,
                                )
                            } else {
                                Modifier
                            }
                        )
                        .drawWithContent {
                            drawContent()
                            drawRect(glassStyle.surfaceTint.copy(alpha = glassStyle.surfaceAlpha))
                            drawRect(glassStyle.overlayColor.copy(alpha = glassStyle.overlayAlpha))
                        },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    if (shareOptions.showArtwork) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Image(
                                painter = artworkPainter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(14.dp),
                                        ),
                            )
                            Spacer(modifier = Modifier.size(14.dp))
                            SongTextBlock(
                                songTitle = songTitle,
                                artistName = artistName,
                                mainTextColor = mainTextColor,
                                secondaryColor = secondaryColor,
                                centered = false,
                            )
                        }
                    } else {
                        SongTextBlock(
                            songTitle = songTitle,
                            artistName = artistName,
                            mainTextColor = mainTextColor,
                            secondaryColor = secondaryColor,
                            centered = true,
                        )
                    }
                }

                BoxWithConstraints(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val textStyle =
                        TextStyle(
                            color = mainTextColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.01).em,
                            fontFamily = lyricsFontFamily,
                        )
                    val textMeasurer = rememberTextMeasurer()
                    val initialSize =
                        when {
                            lyricText.length < 50 -> 24.sp
                            lyricText.length < 100 -> 20.sp
                            lyricText.length < 200 -> 17.sp
                            lyricText.length < 300 -> 15.sp
                            else -> 13.sp
                        }

                    val dynamicFontSize =
                        rememberAdjustedFontSize(
                            text = lyricText,
                            maxWidth = maxWidth - 8.dp,
                            maxHeight = maxHeight - 8.dp,
                            density = density,
                            initialFontSize = initialSize,
                            minFontSize = 11.sp,
                            style = textStyle,
                            textMeasurer = textMeasurer,
                        )

                    Text(
                        text = lyricText,
                        style =
                            textStyle.copy(
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicFontSize.value.sp * 1.35f,
                            ),
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(secondaryColor.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.small_icon),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            colorFilter =
                                ColorFilter.tint(
                                    if (glassStyle.isDark) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        text = context.getString(R.string.app_name),
                        color = secondaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.02.em,
                    )
                }
            }
        }
    }
}

@Composable
private fun SongTextBlock(
    songTitle: String,
    artistName: String,
    mainTextColor: Color,
    secondaryColor: Color,
    centered: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = songTitle,
            color = mainTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(letterSpacing = (-0.02).em),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = artistName,
            color = secondaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

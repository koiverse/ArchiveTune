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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.*
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.UseSystemFontKey
import moe.koiverse.archivetune.models.MediaMetadata
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
    textMeasurer: androidx.compose.ui.text.TextMeasurer? = null
): TextUnit {
    val measurer = textMeasurer ?: rememberTextMeasurer()

    var calculatedFontSize by remember(text, maxWidth, maxHeight, style, density) {
        val initialSize = when {
            text.length < 50 -> initialFontSize
            text.length < 100 -> (initialFontSize.value * 0.8f).sp
            text.length < 200 -> (initialFontSize.value * 0.6f).sp
            else -> (initialFontSize.value * 0.5f).sp
        }
        mutableStateOf(initialSize)
    }

    LaunchedEffect(key1 = text, key2 = maxWidth, key3 = maxHeight) {
        val targetWidthPx = with(density) { maxWidth.toPx() * 0.92f }
        val targetHeightPx = with(density) { maxHeight.toPx() * 0.92f }
        if (text.isBlank()) {
            calculatedFontSize = minFontSize
            return@LaunchedEffect
        }

        if (text.length < 20) {
            val largerSize = (initialFontSize.value * 1.1f).sp
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = largerSize)
            )
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                calculatedFontSize = largerSize
                return@LaunchedEffect
            }
        } else if (text.length < 30) {
            val largerSize = (initialFontSize.value * 0.9f).sp
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = largerSize)
            )
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
            val midSizeSp = midSize.sp

            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = midSizeSp)
            )

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
    mediaMetadata: MediaMetadata,
    glassStyle: LyricsGlassStyle = LyricsGlassStyle.FrostedDark,
    darkBackground: Boolean = true,
    backgroundColor: Color? = null,
    textColor: Color? = null,
    secondaryTextColor: Color? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)
    val lyricsFontFamily = remember(useSystemFont) {
        if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold))
    }

    val cardSizeDp = 340.dp
    val glassCornerRadius = 24.dp
    val glassPadding = 24.dp
    val coverArtSize = 56.dp

    val mainTextColor = textColor ?: glassStyle.textColor
    val secondaryColor = secondaryTextColor ?: glassStyle.secondaryTextColor

    val artworkPainter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(mediaMetadata.thumbnailUrl)
            .crossfade(true)
            .build()
    )

    var glassComponentSize by remember { mutableStateOf(Size.Zero) }
    val lensCenter = remember(glassComponentSize) {
        Offset(glassComponentSize.width / 2f, glassComponentSize.height / 2f)
    }
    val lensSize = remember(glassComponentSize) {
        Size(glassComponentSize.width, glassComponentSize.height)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(cardSizeDp)
                .clip(RoundedCornerShape(glassCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .cloudy(radius = glassStyle.cloudyRadius)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = glassStyle.backgroundDimAlpha * 0.8f),
                                Color.Black.copy(alpha = glassStyle.backgroundDimAlpha),
                                Color.Black.copy(alpha = glassStyle.backgroundDimAlpha * 1.2f),
                            )
                        )
                    )
            )

            val glassShape = RoundedCornerShape(20.dp)

            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        glassComponentSize = Size(size.width.toFloat(), size.height.toFloat())
                    }
                    .clip(glassShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .cloudy(radius = glassStyle.cloudyRadius)
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
                            } else Modifier
                        )
                        .drawWithContent {
                            drawContent()
                            drawRect(glassStyle.surfaceTint.copy(alpha = glassStyle.surfaceAlpha))
                            drawRect(glassStyle.overlayColor.copy(alpha = glassStyle.overlayAlpha))
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(glassPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Image(
                            painter = artworkPainter,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(coverArtSize)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(14.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = mediaMetadata.title,
                                color = mainTextColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 2.dp),
                                style = TextStyle(
                                    letterSpacing = (-0.02).em
                                )
                            )
                            Text(
                                text = mediaMetadata.artists.joinToString { it.name },
                                color = secondaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val availableWidth = maxWidth
                        val availableHeight = maxHeight
                        val textStyle = TextStyle(
                            color = mainTextColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.01).em,
                            fontFamily = lyricsFontFamily,
                        )

                        val textMeasurer = rememberTextMeasurer()
                        val initialSize = when {
                            lyricText.length < 50 -> 22.sp
                            lyricText.length < 100 -> 19.sp
                            lyricText.length < 200 -> 16.sp
                            lyricText.length < 300 -> 14.sp
                            else -> 12.sp
                        }

                        val dynamicFontSize = rememberAdjustedFontSize(
                            text = lyricText,
                            maxWidth = availableWidth - 6.dp,
                            maxHeight = availableHeight - 6.dp,
                            density = density,
                            initialFontSize = initialSize,
                            minFontSize = 11.sp,
                            style = textStyle,
                            textMeasurer = textMeasurer
                        )

                        Text(
                            text = lyricText,
                            style = textStyle.copy(
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicFontSize.value.sp * 1.35f
                            ),
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(secondaryColor.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.small_icon),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                colorFilter = ColorFilter.tint(
                                    if (glassStyle.isDark) Color.Black.copy(alpha = 0.85f)
                                    else Color.White.copy(alpha = 0.9f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = context.getString(R.string.app_name),
                            color = secondaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.02.em
                        )
                    }
                }
            }
        }
    }
}

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */


package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeBreathingDots
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeBreathingDotsDefaults
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLineText
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsLineItem
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyllableLayout
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.SyncedLineText
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.measureSyllablesAndDetermineAnimation
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import com.mocharealm.accompanist.lyrics.ui.utils.modifier.springPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@Immutable
private data class EnhancedLyricsFocusState(
    val firstIndex: Int,
    val allIndices: List<Int>,
    val activeInterludeIndex: Int?,
    val activeIntro: Boolean,
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun LyricsEnhancedKaraokeView(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    sourceText: String?,
    modifier: Modifier = Modifier,
    normalLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    accompanimentLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    sourceTextStyle: TextStyle = normalLineTextStyle,
    phoneticTextStyle: TextStyle = normalLineTextStyle.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
    ),
    textColor: Color = Color.White,
    sourceTextColor: Color = textColor.copy(alpha = 0.52f),
    breathingDotsDefaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
    blendMode: BlendMode = BlendMode.Plus,
    useBlurEffect: Boolean = true,
    showTranslation: Boolean = true,
    showPhonetic: Boolean = true,
    offset: Dp = 32.dp,
    keepAliveZone: Dp = 100.dp,
    blurDelta: Float = 3f,
    isManualScrolling: Boolean,
    showDebugRectangles: Boolean = false,
) {
    val density = LocalDensity.current
    val stableNormalTextStyle = remember(normalLineTextStyle) { normalLineTextStyle }
    val stableAccompanimentTextStyle = remember(accompanimentLineTextStyle) { accompanimentLineTextStyle }
    val stablePhoneticTextStyle = remember(phoneticTextStyle) { phoneticTextStyle }
    val stableSourceTextStyle = remember(sourceTextStyle) { sourceTextStyle }
    val stableOffset = remember(offset) { offset }
    val stableBlendMode = remember(blendMode) { blendMode }

    val textMeasurer = rememberTextMeasurer()
    val layoutCache = remember { mutableStateMapOf<Int, List<SyllableLayout>>() }

    LaunchedEffect(
        lyrics,
        stableNormalTextStyle,
        stableAccompanimentTextStyle,
        stablePhoneticTextStyle,
    ) {
        layoutCache.clear()
        withContext(Dispatchers.Default) {
            val normalStyle = stableNormalTextStyle.copy(textDirection = TextDirection.Content)
            val accompanimentStyle = stableAccompanimentTextStyle.copy(textDirection = TextDirection.Content)
            val phoneticStyle = stablePhoneticTextStyle.copy(textDirection = TextDirection.Content)
            val normalSpaceWidth = textMeasurer.measure(" ", normalStyle).size.width.toFloat()
            val accompanimentSpaceWidth = textMeasurer.measure(" ", accompanimentStyle).size.width.toFloat()

            lyrics.lines.forEachIndexed { index, line ->
                if (!isActive) return@forEachIndexed
                if (line is KaraokeLine) {
                    val style = if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentStyle else normalStyle
                    val spaceWidth = if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentSpaceWidth else normalSpaceWidth
                    val processedSyllables = if (line.alignment == KaraokeAlignment.End) {
                        line.syllables.dropLastWhile { it.content.isBlank() }
                    } else {
                        line.syllables
                    }
                    val layout = measureSyllablesAndDetermineAnimation(
                        syllables = processedSyllables,
                        textMeasurer = textMeasurer,
                        style = style,
                        phoneticStyle = phoneticStyle,
                        isAccompanimentLine = line is KaraokeLine.AccompanimentKaraokeLine,
                        spaceWidth = spaceWidth,
                    )
                    withContext(Dispatchers.Main) {
                        layoutCache[index] = layout
                    }
                }
            }
        }
    }

    val accompanimentToMainMap = remember(lyrics.lines) {
        val map = mutableMapOf<Int, Int>()
        val mainLineIndices = lyrics.lines.indices.filter { index ->
            val line = lyrics.lines[index]
            line !is KaraokeLine || line !is KaraokeLine.AccompanimentKaraokeLine
        }
        if (mainLineIndices.isNotEmpty()) {
            lyrics.lines.forEachIndexed { index, line ->
                if (line is KaraokeLine.AccompanimentKaraokeLine) {
                    val beforeIndex = mainLineIndices.findLast { it <= index }
                    val afterIndex = mainLineIndices.find { it >= index }
                    val anchorIndex = when {
                        beforeIndex != null && afterIndex != null -> {
                            val distanceBefore = (line.start - lyrics.lines[beforeIndex].start).absoluteValue
                            val distanceAfter = (lyrics.lines[afterIndex].start - line.start).absoluteValue
                            if (distanceBefore <= distanceAfter) beforeIndex else afterIndex
                        }

                        beforeIndex != null -> beforeIndex
                        afterIndex != null -> afterIndex
                        else -> mainLineIndices.first()
                    }
                    map[index] = anchorIndex
                }
            }
        }
        map
    }
    val effectiveEndTimes = remember(lyrics.lines) {
        IntArray(lyrics.lines.size) { index ->
            val line = lyrics.lines[index]
            var maxEnd = line.end
            if (line is KaraokeLine.MainKaraokeLine) {
                line.accompanimentLines?.forEach { accompaniment ->
                    if (accompaniment.end > maxEnd) {
                        maxEnd = accompaniment.end
                    }
                }
            }
            maxEnd
        }
    }
    val firstLine = lyrics.lines.firstOrNull()
    val haveDotsIntro by remember(firstLine) {
        derivedStateOf { firstLine != null && firstLine.start > 5000 }
    }
    val lyricsFocusState by remember(lyrics, effectiveEndTimes, accompanimentToMainMap, haveDotsIntro, currentPosition) {
        derivedStateOf {
            val time = currentPosition()
            val activeIndex = lyrics.lines.indices.find { index ->
                time >= lyrics.lines[index].start && time < effectiveEndTimes[index]
            }
            val firstIndex = if (activeIndex != null) {
                activeIndex
            } else {
                val nextIndex = lyrics.lines.indexOfFirst { it.start > time }
                if (nextIndex != -1) nextIndex else lyrics.lines.lastIndex
            }
            val activeIndices = lyrics.lines.indices.filter { index ->
                time >= lyrics.lines[index].start && time < effectiveEndTimes[index]
            }.toMutableSet()
            activeIndices.toList().forEach { index ->
                val line = lyrics.lines.getOrNull(index)
                if (line is KaraokeLine.AccompanimentKaraokeLine) {
                    accompanimentToMainMap[index]?.let(activeIndices::add)
                }
            }
            val activeInterludeIndex = lyrics.lines.indices.find { index ->
                val line = lyrics.lines[index]
                val previousLine = lyrics.lines.getOrNull(index - 1)
                previousLine != null &&
                    (line.start - previousLine.end > 5000) &&
                    time in previousLine.end..line.start
            }
            val activeIntro = haveDotsIntro && time in 0 until (firstLine?.start ?: 0)
            EnhancedLyricsFocusState(
                firstIndex = firstIndex,
                allIndices = activeIndices.toList().sorted(),
                activeInterludeIndex = activeInterludeIndex,
                activeIntro = activeIntro,
            )
        }
    }

    LookaheadScope {
        Crossfade(lyrics) { renderedLyrics ->
            Box(modifier = modifier.clipToBounds()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                val topFade = 20.dp.toPx() / size.height
                                val bottomFade = 100.dp.toPx() / size.height
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        topFade to Color.Black,
                                        1f - bottomFade to Color.Black,
                                        1f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        }
                        .layout { measurable, constraints ->
                            val extraHeightPx = (keepAliveZone * 2).roundToPx()
                            val placeable = measurable.measure(
                                constraints.copy(maxHeight = constraints.maxHeight + extraHeightPx),
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(0, -keepAliveZone.roundToPx())
                            }
                        },
                    contentPadding = PaddingValues(vertical = stableOffset + keepAliveZone),
                ) {
                    if (sourceText != null) {
                        item(key = "lyrics_source", contentType = "lyrics_source") {
                            Text(
                                text = sourceText,
                                style = stableSourceTextStyle,
                                color = sourceTextColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }

                    itemsIndexed(
                        items = renderedLyrics.lines,
                        key = { index, line -> "${line.start}-${line.end}-$index" },
                        contentType = { _, line ->
                            when (line) {
                                is KaraokeLine.MainKaraokeLine -> "mainKaraoke"
                                is KaraokeLine.AccompanimentKaraokeLine -> "accompanimentKaraoke"
                                is SyncedLine -> "syncedLine"
                                else -> "lyricsLine"
                            }
                        },
                    ) { index, line ->
                        val isFocused = index in lyricsFocusState.allIndices
                        val isLineRtl = when (line) {
                            is KaraokeLine -> remember(line.syllables) { line.syllables.any { it.content.isRtl() } }
                            is SyncedLine -> remember(line.content) { line.content.isRtl() }
                            else -> false
                        }
                        val isLineRightAligned = when (line) {
                            is KaraokeLine -> remember(line.alignment) { line.alignment == KaraokeAlignment.End }
                            else -> false
                        }
                        val isVisualRightAligned = remember(isLineRightAligned, isLineRtl) {
                            if (isLineRightAligned) !isLineRtl else isLineRtl
                        }
                        val distanceWeightState = remember(useBlurEffect, lyricsFocusState, index) {
                            derivedStateOf {
                                val start = lyricsFocusState.allIndices.firstOrNull() ?: lyricsFocusState.firstIndex
                                val end = lyricsFocusState.allIndices.lastOrNull() ?: lyricsFocusState.firstIndex
                                maxOf(0, start - index, index - end)
                            }
                        }
                        val dynamicStiffness by remember(distanceWeightState.value) {
                            derivedStateOf { (120f - (distanceWeightState.value * 20f)).coerceAtLeast(20f) }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .springPlacement(
                                    lookaheadScope = this@LookaheadScope,
                                    itemKey = "${line.start}-${line.end}-$index",
                                    isManualScrolling = isManualScrolling,
                                    stiffness = dynamicStiffness,
                                ),
                            horizontalAlignment = if (isVisualRightAligned) Alignment.End else Alignment.Start,
                        ) {
                            val previousLine = renderedLyrics.lines.getOrNull(index - 1)
                            val showDotsInterlude = lyricsFocusState.activeInterludeIndex == index
                            val showDotsIntro = lyricsFocusState.activeIntro && index == 0

                            androidx.compose.animation.AnimatedVisibility(showDotsInterlude || showDotsIntro) {
                                KaraokeBreathingDots(
                                    alignment = when (val anchorLine = previousLine ?: firstLine) {
                                        is KaraokeLine -> anchorLine.alignment
                                        is SyncedLine -> if (anchorLine.content.isRtl()) KaraokeAlignment.End else KaraokeAlignment.Start
                                        else -> KaraokeAlignment.Start
                                    },
                                    startTimeMs = previousLine?.end ?: 0,
                                    endTimeMs = if (showDotsIntro) firstLine!!.start else line.start,
                                    currentTimeProvider = currentPosition,
                                    defaults = breathingDotsDefaults,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }

                            val blurRadiusState by animateFloatAsState(
                                targetValue = if (!useBlurEffect) {
                                    0f
                                } else if (distanceWeightState.value > 0 && (!listState.isScrollInProgress || !isManualScrolling)) {
                                    distanceWeightState.value * blurDelta
                                } else {
                                    0f
                                },
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutSlowInEasing,
                                ),
                                label = "lyricsBlurRadius",
                            )

                            when (line) {
                                is KaraokeLine.MainKaraokeLine -> {
                                    LyricsLineItem(
                                        isFocused = isFocused,
                                        isRightAligned = isVisualRightAligned,
                                        onLineClicked = { onLineClicked(line) },
                                        onLinePressed = { onLinePressed(line) },
                                        blurRadius = { blurRadiusState },
                                        blendMode = stableBlendMode,
                                    ) {
                                        KaraokeLineText(
                                            line = line,
                                            currentTimeProvider = currentPosition,
                                            normalLineTextStyle = stableNormalTextStyle,
                                            accompanimentLineTextStyle = stableAccompanimentTextStyle,
                                            phoneticTextStyle = stablePhoneticTextStyle,
                                            activeColor = textColor,
                                            blendMode = stableBlendMode,
                                            showDebugRectangles = showDebugRectangles,
                                            showTranslation = showTranslation,
                                            showPhonetic = showPhonetic,
                                            precalculatedLayouts = layoutCache[index],
                                        )
                                    }
                                }

                                is KaraokeLine.AccompanimentKaraokeLine -> Unit

                                is SyncedLine -> {
                                    LyricsLineItem(
                                        isFocused = isFocused,
                                        isRightAligned = isLineRtl,
                                        onLineClicked = { onLineClicked(line) },
                                        onLinePressed = { onLinePressed(line) },
                                        blurRadius = { blurRadiusState },
                                        blendMode = stableBlendMode,
                                    ) {
                                        SyncedLineText(
                                            line = line,
                                            isLineRtl = isLineRtl,
                                            textStyle = stableNormalTextStyle.copy(lineHeight = 1.2.em),
                                            textColor = textColor,
                                            showTranslation = showTranslation,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "lyrics_bottom_spacing", contentType = "lyrics_bottom_spacing") {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2000.dp),
                        )
                    }
                }
            }
        }
    }
}

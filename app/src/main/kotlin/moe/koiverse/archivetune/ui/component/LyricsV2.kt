/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse) & WTTexe!
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.LyricsClickKey
import moe.koiverse.archivetune.constants.LyricsScrollKey
import moe.koiverse.archivetune.constants.LyricsTextPositionKey
import moe.koiverse.archivetune.constants.LyricsTextSizeKey
import moe.koiverse.archivetune.constants.LyricsLineSpacingKey
import moe.koiverse.archivetune.constants.LyricsRomanizeJapaneseKey
import moe.koiverse.archivetune.constants.LyricsRomanizeKoreanKey
import moe.koiverse.archivetune.constants.PlayerBackgroundStyle
import moe.koiverse.archivetune.constants.PlayerBackgroundStyleKey
import moe.koiverse.archivetune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import moe.koiverse.archivetune.lyrics.LyricsEntry
import moe.koiverse.archivetune.lyrics.LyricsUtils.findCurrentLineIndex
import moe.koiverse.archivetune.lyrics.LyricsUtils.isChinese
import moe.koiverse.archivetune.lyrics.LyricsUtils.isJapanese
import moe.koiverse.archivetune.lyrics.LyricsUtils.isKorean
import moe.koiverse.archivetune.lyrics.LyricsUtils.isTtml
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseLyrics
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseTtml
import moe.koiverse.archivetune.lyrics.LyricsUtils.romanizeJapanese
import moe.koiverse.archivetune.lyrics.LyricsUtils.romanizeKorean
import moe.koiverse.archivetune.lyrics.WordTimestamp
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.ui.screens.settings.LyricsPosition
import moe.koiverse.archivetune.ui.utils.smoothFadingEdge
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import kotlin.math.abs


// ──────────────────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────────────────

/** Lead time offset for LRC-style line-synced lyrics (ms). */
private const val LRC_LEAD_MS = 300L

/** Lead time offset for TTML word-synced lyrics (ms). */
private const val TTML_LEAD_MS = 0L

/** Seconds to wait before auto-scroll resumes after manual scroll. */
private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L

/** Apple-Music-style easing for smooth deceleration. */
private val V2Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/** Liquid fill easing: fast attack, very smooth deceleration (Apple Music-like). */
private val LiquidFillEasing = CubicBezierEasing(0.0f, 0.0f, 0.15f, 1.0f)

/** Sentinel entry prepended so auto-scroll has headroom above the first line. */
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")


// ──────────────────────────────────────────────────────────────────────
// Main Composable
// ──────────────────────────────────────────────────────────────────────


@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ── Preferences ──
    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    // ── Text colour derived from background style ──
    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT)
        MaterialTheme.colorScheme.onBackground
    else
        Color.White

    val inactiveAlpha = 0.35f

    // ── Lyrics data ──
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = currentLyrics?.lyrics

    // ── Parse lyrics into entries ──
    val isSynced = remember(lyrics) { lyrics != null && (lyrics!!.startsWith("[") || isTtml(lyrics!!)) }
    val isTtmlFormat = remember(lyrics) { lyrics != null && isTtml(lyrics!!) }

    val lyricsEntries: List<LyricsEntry> = remember(lyrics) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) return@remember emptyList()
        val parsed = when {
            isTtml(lyrics!!) -> parseTtml(lyrics!!)
            lyrics!!.startsWith("[") -> parseLyrics(lyrics!!)
            else -> lyrics!!.lines()
                .filter { it.isNotBlank() }
                .mapIndexed { index, line ->
                    LyricsEntry(time = -1L, text = line.trim())
                }
        }
        if (parsed.isNotEmpty() && parsed.first().time >= 0) {
            listOf(HEAD_LYRICS_ENTRY) + parsed
        } else {
            parsed
        }
    }

    // ── Synthesize word timings for LRC entries that lack them ──
    val entriesWithWords: List<LyricsEntry> = remember(lyricsEntries) {
        if (lyricsEntries.isEmpty()) return@remember emptyList()
        lyricsEntries.mapIndexed { index, entry ->
            if (entry.words != null || entry.time < 0 || entry.text.isBlank()) {
                entry // Already has word timings (TTML) or is non-synced
            } else {
                // Synthesize word-level timings for this LRC line
                val nextEntryTime = if (index < lyricsEntries.lastIndex) {
                    lyricsEntries[index + 1].time
                } else {
                    entry.time + 5000L // 5s fallback for last line
                }
                val lineDurationMs = (nextEntryTime - entry.time).coerceAtLeast(500L)
                val lineStartSec = entry.time / 1000.0

                val isCjkText = isJapanese(entry.text) || isChinese(entry.text) || isKorean(entry.text)
                val tokens = if (isCjkText) {
                    val chars = mutableListOf<String>()
                    var currentWord = StringBuilder()
                    entry.text.forEach { char ->
                        if (char.isWhitespace()) {
                            if (currentWord.isNotEmpty()) {
                                chars.add(currentWord.toString())
                                currentWord.clear()
                            }
                            chars.add(char.toString())
                        } else if (isJapanese(char.toString()) || isChinese(char.toString()) || isKorean(char.toString())) {
                            if (currentWord.isNotEmpty()) {
                                chars.add(currentWord.toString())
                                currentWord.clear()
                            }
                            chars.add(char.toString())
                        } else {
                            currentWord.append(char)
                        }
                    }
                    if (currentWord.isNotEmpty()) {
                        chars.add(currentWord.toString())
                    }

                    // Group spaces onto the preceding word
                    val groupedTokens = mutableListOf<String>()
                    var tempStr = StringBuilder()
                    chars.forEachIndexed { i, c ->
                        if (c.isBlank()) {
                            if (groupedTokens.isNotEmpty()) {
                                groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c
                            }
                        } else {
                            groupedTokens.add(c)
                        }
                    }
                    groupedTokens
                } else {
                    entry.text.split(Regex("\\s+"))
                }
                if (tokens.isEmpty()) return@mapIndexed entry

                // Weight each token by character count for proportional distribution
                val totalChars = tokens.sumOf { it.length }.coerceAtLeast(1)
                val words = mutableListOf<WordTimestamp>()
                var currentOffsetMs = 0.0

                tokens.forEachIndexed { wordIdx, token ->
                    val weight = token.length.toDouble() / totalChars
                    val wordDurMs = lineDurationMs * weight
                    val wordStartSec = lineStartSec + (currentOffsetMs / 1000.0)
                    val wordEndSec = wordStartSec + (wordDurMs / 1000.0)

                    val wordText = if (wordIdx < tokens.lastIndex && !isCjkText) "$token " else token
                    words.add(
                        WordTimestamp(
                            text = wordText,
                            startTime = wordStartSec,
                            endTime = wordEndSec,
                        )
                    )
                    currentOffsetMs += wordDurMs
                }
                entry.copy(words = words)
            }
        }
    }

    // ── Romanization ──
    LaunchedEffect(entriesWithWords, romanizeJapanese, romanizeKorean) {
        if (!romanizeJapanese && !romanizeKorean) return@LaunchedEffect
        entriesWithWords.forEach { entry ->
            if (entry.text.isBlank() || entry.romanizedTextFlow.value != null) return@forEach
            scope.launch(Dispatchers.Default) {
                val romanized = when {
                    romanizeJapanese && isJapanese(entry.text) -> romanizeJapanese(entry.text)
                    romanizeKorean && isKorean(entry.text) -> romanizeKorean(entry.text)
                    else -> null
                }
                if (romanized != null) entry.romanizedTextFlow.value = romanized
            }
        }
    }

    // ── Playback position tracking ──
    val leadMs = if (isTtmlFormat) TTML_LEAD_MS else LRC_LEAD_MS
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var currentLineIndex by remember { mutableIntStateOf(0) }

    // Frame-accurate position loop
    LaunchedEffect(entriesWithWords, isSynced) {
        if (!isSynced || entriesWithWords.isEmpty()) return@LaunchedEffect
        while (isActive) {
            val sliderPos = sliderPositionProvider()
            val pos = sliderPos ?: player.currentPosition
            
            // Add a visual tuning offset so animations feel instantly responsive and perfectly land on beat
            val visualTuningOffsetMs = 150L 
            currentPositionMs = pos + leadMs + visualTuningOffsetMs
            
            currentLineIndex = findCurrentLineIndex(entriesWithWords, currentPositionMs, 0L)
            delay(16L) // ~60fps polling
        }
    }

    // ── Scroll State ──
    val listState = rememberLazyListState()
    var isManualScrolling by remember { mutableStateOf(false) }
    var lastManualScrollTime by remember { mutableLongStateOf(0L) }

    // Detect manual scrolling
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    isManualScrolling = true
                    lastManualScrollTime = System.currentTimeMillis()
                }
                return Offset.Zero
            }
        }
    }

    // Resume auto-scroll after timeout
    LaunchedEffect(isManualScrolling, lastManualScrollTime) {
        if (isManualScrolling) {
            delay(MANUAL_SCROLL_TIMEOUT_MS)
            isManualScrolling = false
        }
    }

    // Auto-scroll to active line
    LaunchedEffect(currentLineIndex, isManualScrolling, lyricsScroll) {
        if (!lyricsScroll || isManualScrolling || !isSynced) return@LaunchedEffect
        if (currentLineIndex < 0 || currentLineIndex >= entriesWithWords.size) return@LaunchedEffect

        val visibleInfo = listState.layoutInfo
        val viewportHeight = visibleInfo.viewportSize.height
        val targetOffset = (viewportHeight * 0.35f).toInt() // Center bias at 35% from top

        val distance = abs(currentLineIndex - (listState.firstVisibleItemIndex))
        if (distance > 15) {
            // Far jump — snap first, then settle
            listState.scrollToItem(
                (currentLineIndex - 2).coerceAtLeast(0),
                0
            )
        }
        listState.animateScrollToItem(
            index = currentLineIndex,
            scrollOffset = -targetOffset
        )
    }

    // ── Keep screen alive ──
    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ── Render ──
    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        if (lyrics == LYRICS_NOT_FOUND) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            return@BoxWithConstraints
        }

        if (lyrics == null) {
            ShimmerHost {
                repeat(6) {
                    TextPlaceholder()
                }
            }
            return@BoxWithConstraints
        }

        if (entriesWithWords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            return@BoxWithConstraints
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .smoothFadingEdge(vertical = 80.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(
                items = entriesWithWords,
                key = { index, entry -> "${index}_${entry.time}" }
            ) { index, item ->
                if (item == HEAD_LYRICS_ENTRY) {
                    Spacer(modifier = Modifier.height(120.dp))
                    return@itemsIndexed
                }

                // ── Agent-based positioning ──
                // v1 or null -> Start, v2 -> End, others -> Center
                val textAlign = when (item.agent?.lowercase()) {
                    "v1", null -> TextAlign.Start
                    "v2" -> TextAlign.End
                    else -> TextAlign.Center
                }
                val horizontalAlignment = when (item.agent?.lowercase()) {
                    "v1", null -> Alignment.Start
                    "v2" -> Alignment.End
                    else -> Alignment.CenterHorizontally
                }

                val isActive = isSynced && index == currentLineIndex
                val isPast = isSynced && index < currentLineIndex
                val isFuture = isSynced && index > currentLineIndex

                // Distance-based alpha for non-active lines
                val distanceFromActive = if (isSynced) abs(index - currentLineIndex) else 0
                // For word-synced lines, each word handles its own alpha independently
                // so we use 1f for active lines to not double-dim
                val lineAlpha = when {
                    !isSynced -> 0.9f
                    isActive -> 1f
                    else -> (inactiveAlpha - (distanceFromActive - 1) * 0.03f)
                        .coerceIn(0.15f, inactiveAlpha)
                }
                // Word-synced lines: pass 1f alpha so individual words control their own
                val wordLineAlpha = if (item.words != null && isSynced) 1f else lineAlpha

                // Scale — active line slightly enlarged with smooth tween
                val targetScale = if (isActive) 1.0f else 0.97f
                val lineScale = remember { Animatable(targetScale) }
                LaunchedEffect(isActive) {
                    lineScale.animateTo(
                        targetValue = if (isActive) 1.0f else 0.97f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.LinearOutSlowInEasing
                        )
                    )
                }

                // Background vocal detection
                val hasBackgroundWords = item.words?.any { it.isBackground } == true
                val isAllBackground = item.words?.all { it.isBackground || it.text.isBlank() } == true

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (isAllBackground) 24.dp else 12.dp,
                            end = 12.dp,
                            top = if (index == 0 || (index == 1 && entriesWithWords[0] == HEAD_LYRICS_ENTRY)) 0.dp else (lyricsLineSpacing * 8).dp,
                            bottom = (lyricsLineSpacing * 8).dp,
                        )
                        .scale(lineScale.value)
                        .alpha(wordLineAlpha)
                        .then(
                            if (lyricsClick && isSynced && item.time > 0) {
                                Modifier.clickable {
                                    player.seekTo(item.time)
                                }
                            } else Modifier
                        ),
                    horizontalAlignment = horizontalAlignment,
                ) {
                    if (item.words != null && isSynced) {
                        // ── Word-synced rendering ──
                        LyricsLineV2(
                            words = item.words!!,
                            isActive = isActive,
                            isPast = isPast,
                            currentPositionMs = currentPositionMs,
                            textColor = textColor,
                            inactiveAlpha = inactiveAlpha,
                            baseFontSize = lyricsTextSize,
                            isLineAllBackground = isAllBackground,
                            textAlign = textAlign,
                        )
                    } else {
                        // ── Plain text rendering ──
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = if (isAllBackground) (lyricsTextSize * 0.82f).sp else lyricsTextSize.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                            ),
                            color = textColor.copy(alpha = if (isActive) 1f else inactiveAlpha),
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // ── Romanization ──
                    val romanizedText by item.romanizedTextFlow.collectAsState()
                    if (romanizedText != null) {
                        Text(
                            text = romanizedText!!,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (lyricsTextSize * 0.55f).sp,
                                lineHeight = (lyricsTextSize * 0.75f).sp,
                                fontWeight = FontWeight.Normal,
                                fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal,
                            ),
                            color = textColor.copy(alpha = if (isActive) 0.75f else inactiveAlpha * 0.7f),
                            textAlign = textAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = (lyricsTextSize * 0.3f).dp),
                        )
                    }
                }
            }

            // Bottom spacer for overscroll
            item {
                Spacer(modifier = Modifier.height(300.dp))
            }
        }

        // ── Resume auto-scroll button ──
        if (isManualScrolling && isSynced) {
            androidx.compose.material3.FilledTonalButton(
                onClick = {
                    isManualScrolling = false
                    scope.launch {
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        listState.animateScrollToItem(
                            index = currentLineIndex,
                            scrollOffset = -(viewportHeight * 0.35f).toInt()
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = "Resume",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}


// ──────────────────────────────────────────────────────────────────────
// Line-level composable: renders words with fluid fill animation
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineV2(
    words: List<WordTimestamp>,
    isActive: Boolean,
    isPast: Boolean,
    currentPositionMs: Long,
    textColor: Color,
    inactiveAlpha: Float,
    baseFontSize: Float,
    isLineAllBackground: Boolean,
    textAlign: TextAlign,
) {
    val arrangement = when (textAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.End -> Arrangement.End
        else -> Arrangement.Start
    }

    // Split words into main and background
    val mainWords = words.filter { !it.isBackground }
    val bgWords = words.filter { it.isBackground }

    // 1. Render main words First (if any)
    if (mainWords.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
        ) {
            mainWords.forEachIndexed { wordIndex, word ->
                if (word.text == " ") {
                    Text(
                        text = " ",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = if (isLineAllBackground) (baseFontSize * 0.82f).sp else baseFontSize.sp,
                        ),
                        color = Color.Transparent,
                    )
                    return@forEachIndexed
                }
                if (word.text == "\n") {
                    Spacer(modifier = Modifier.fillMaxWidth())
                    return@forEachIndexed
                }

                AnimatedWordV2(
                    word = word,
                    wordIndex = wordIndex,
                    isLineActive = isActive,
                    isLinePast = isPast,
                    currentPositionMs = currentPositionMs,
                    textColor = textColor,
                    inactiveAlpha = inactiveAlpha,
                    fontSize = if (isLineAllBackground) baseFontSize * 0.82f else baseFontSize,
                    isBackground = isLineAllBackground,
                )
            }
        }
    }

    // 2. Render background words explicitly on a NEW line, noticeably smaller
    if (bgWords.isNotEmpty()) {
        val spacerHeight = if (mainWords.isNotEmpty()) 4.dp else 0.dp
        if (mainWords.isNotEmpty()) Spacer(modifier = Modifier.height(spacerHeight))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth().alpha(0.85f), // Slightly dimmer overall
            horizontalArrangement = arrangement,
        ) {
            bgWords.forEachIndexed { wordIndex, word ->
                if (word.text == " ") {
                    Text(
                        text = " ",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = (baseFontSize * 0.65f).sp,
                        ),
                        color = Color.Transparent,
                    )
                    return@forEachIndexed
                }
                
                AnimatedWordV2(
                    word = word,
                    wordIndex = wordIndex + mainWords.size,
                    isLineActive = isActive,
                    isLinePast = isPast,
                    currentPositionMs = currentPositionMs,
                    textColor = textColor,
                    inactiveAlpha = inactiveAlpha,
                    fontSize = baseFontSize * 0.65f, // ~65% size of main text
                    isBackground = true, // Force dimmer styling inside AnimatedWordV2
                )
            }
        }
    }
}


// ──────────────────────────────────────────────────────────────────────
// Word-level composable: liquid fill sweep + glow + bounce
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedWordV2(
    word: WordTimestamp,
    wordIndex: Int,
    isLineActive: Boolean,
    isLinePast: Boolean,
    currentPositionMs: Long,
    textColor: Color,
    inactiveAlpha: Float,
    fontSize: Float,
    isBackground: Boolean,
) {
    val wordStartMs = (word.startTime * 1000).toLong()
    val wordEndMs = (word.endTime * 1000).toLong()
    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)

    val isWordComplete = currentPositionMs >= wordEndMs
    val isWordActive = currentPositionMs in wordStartMs until wordEndMs

    // Perfect linear progress [0..1] that matches individual word timings
    val progress = when {
        isWordComplete -> 1f
        currentPositionMs <= wordStartMs -> 0f
        else -> ((currentPositionMs - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    // ── Bounce and Float animation ──
    // Subtle scale up peaking halfway through the word. Exact timing sync!
    val sinProgress = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
    val wordScale = 1f + (0.015f * sinProgress)
    
    // Float is only applied when the word is actively sung, making it pop from the line.
    // We use animateFloatAsState so that when it finishes (and drops to 0f), 
    // it smoothly decays back into place rather than a harsh mathematical snap.
    val targetFloat = if (isWordActive) -4f * sinProgress else 0f
    val floatOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (isWordActive) 50 else 350,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        )
    )

    // ── Glow intensity ──
    // "lines and words that are done animating shouldnt continue to glow"
    // Make glow build up faster: reach max intensity at 50% progress
    val glowProgress = (progress * 2f).coerceAtMost(1f)
    val glowAlpha = if (isWordActive) glowProgress * 0.45f else 0f
    val glowRadius = if (isWordActive) glowProgress * 12f else 0f

    val actualFontSize = if (isBackground) fontSize * 0.85f else fontSize
    val fontWeight = FontWeight.SemiBold // Consistent weight — no thin→bold jump

    // ── Two-layer rendering: dim base + liquid fill overlay ──
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = floatOffset * density
                scaleX = wordScale
                scaleY = wordScale
            }
    ) {
        // Layer 1: Base text (always dimmed)
        Text(
            text = word.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = actualFontSize.sp,
                fontWeight = fontWeight,
                fontStyle = FontStyle.Normal,
                lineHeight = (actualFontSize * 1.35f).sp,
            ),
            color = textColor.copy(alpha = if (isBackground) inactiveAlpha * 0.7f else inactiveAlpha),
        )

        // Layer 2: Filled overlay with liquid sweep mask + glow
        if (isWordComplete || isWordActive || isLinePast) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = actualFontSize.sp,
                    fontWeight = fontWeight,
                    fontStyle = FontStyle.Normal,
                    lineHeight = (actualFontSize * 1.35f).sp,
                    shadow = if (glowAlpha > 0f) {
                        Shadow(
                            color = textColor.copy(alpha = glowAlpha),
                            offset = Offset.Zero,
                            blurRadius = glowRadius.coerceAtLeast(1f),
                        )
                    } else null,
                ),
                color = textColor.copy(
                    alpha = if (isBackground) 0.75f else 1f
                ),
                modifier = if (isWordActive && !isWordComplete) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgeWidth = 8.dp.toPx()
                            val center = (size.width + edgeWidth * 2) * progress - edgeWidth
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = center - edgeWidth,
                                    endX = center + edgeWidth,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                } else {
                    Modifier 
                }
            )
        }
    }
}

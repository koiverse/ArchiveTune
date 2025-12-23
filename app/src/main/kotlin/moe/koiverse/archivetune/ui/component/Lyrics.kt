package moe.koiverse.archivetune.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.foundation.text.InlineTextContent
import kotlin.math.sin
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.activity.compose.BackHandler
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.view.WindowManager
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.DarkModeKey
import moe.koiverse.archivetune.constants.LyricsClickKey
import moe.koiverse.archivetune.constants.LyricsRomanizeJapaneseKey
import moe.koiverse.archivetune.constants.LyricsRomanizeKoreanKey
import moe.koiverse.archivetune.constants.LyricsScrollKey
import moe.koiverse.archivetune.constants.LyricsTextPositionKey
import moe.koiverse.archivetune.constants.LyricsAnimationStyle
import moe.koiverse.archivetune.constants.LyricsAnimationStyleKey
import moe.koiverse.archivetune.constants.LyricsTextSizeKey
import moe.koiverse.archivetune.constants.LyricsLineSpacingKey
import moe.koiverse.archivetune.constants.PlayerBackgroundStyle
import moe.koiverse.archivetune.constants.PlayerBackgroundStyleKey
import moe.koiverse.archivetune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import moe.koiverse.archivetune.lyrics.LyricsEntry
import moe.koiverse.archivetune.lyrics.LyricsUtils.isChinese
import moe.koiverse.archivetune.lyrics.LyricsUtils.findCurrentLineIndex
import moe.koiverse.archivetune.lyrics.LyricsUtils.isJapanese
import moe.koiverse.archivetune.lyrics.LyricsUtils.isKorean
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseLyrics
import moe.koiverse.archivetune.lyrics.LyricsUtils.romanizeJapanese
import moe.koiverse.archivetune.lyrics.LyricsUtils.romanizeKorean
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.ui.menu.LyricsMenu
import moe.koiverse.archivetune.ui.screens.settings.DarkMode
import moe.koiverse.archivetune.ui.screens.settings.LyricsPosition
import moe.koiverse.archivetune.ui.utils.fadingEdge
import moe.koiverse.archivetune.ui.utils.smoothFadingEdge
import moe.koiverse.archivetune.utils.ComposeToImage
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.exp
import kotlin.time.Duration.Companion.seconds


private val AppleMusicEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
private val SmoothDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

/**
 * Renders a single word with karaoke fill animation.
 * Optimized to perform animation in the draw phase to avoid recomposition.
 */
@Composable
private fun KaraokeWord(
    text: String,
    startTime: Long,
    endTime: Long,
    currentTimeProvider: () -> Long,
    fontSize: TextUnit,
    textColor: Color,
    inactiveAlpha: Float,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    modifier: Modifier = Modifier
) {
    val duration = endTime - startTime

    Box(
        modifier = modifier.graphicsLayer {
            val currentTime = currentTimeProvider()
            val progress = if (duration > 0) {
                val elapsed = currentTime - startTime
                (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else if (currentTime >= endTime) {
                1f
            } else {
                0f
            }

            // Nudge Animation: Decoupled from word duration.
            // Fixed duration impulse (e.g., 500ms) to avoid "wobble" on long words.
            val nudgeDuration = 500L
            val elapsed = currentTime - startTime
            val isNudging = elapsed in 0..nudgeDuration

            if (isNudging) {
                // Progress 0..1 over 500ms
                val t = elapsed.toFloat() / nudgeDuration.toFloat()
                // Curve: Cubic impulse for quick push and slow return.
                // t * (1-t)^2 peaks at t=0.33
                // Scaled to ~4dp (approx 12px) peak
                val amplitude = 12f * 3f 
                val nudge = (t * (1f - t) * (1f - t)) * amplitude
                translationX = nudge
            } else {
                translationX = 0f
            }
        }
    ) {
        // 1. Inactive (unfilled) layer - Always visible foundation
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor.copy(alpha = inactiveAlpha),
            fontWeight = fontWeight
        )

        // 2. Completed (filled) layer - NO GLOW
        // Only drawn when progress is fully complete (1f)
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor,
            fontWeight = fontWeight,
            modifier = Modifier.drawWithContent {
                val currentTime = currentTimeProvider()
                val isDone = currentTime >= endTime
                if (isDone) {
                    drawContent()
                }
            }
        )

        // 3. Active (filling) layer - GLOW + SOFT MASK
        // Uses offscreen compositing to apply the alpha mask properly
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor,
            fontWeight = fontWeight,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(
                    color = textColor,
                    blurRadius = 30f // Strong glow
                )
            ),
            modifier = Modifier
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    
                    // Fade out logic when word is done
                    val currentTime = currentTimeProvider()
                    val fadeDuration = 400L // Fade out glow over 400ms
                    
                    if (currentTime >= endTime) {
                        val timeSinceEnd = currentTime - endTime
                        val fadeProgress = (timeSinceEnd.toFloat() / fadeDuration.toFloat()).coerceIn(0f, 1f)
                        alpha = 1f - fadeProgress
                    } else {
                        alpha = 1f
                    }
                }
                .drawWithContent {
                    val currentTime = currentTimeProvider()
                    val progress = if (duration > 0) {
                        val elapsed = currentTime - startTime
                        (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else if (currentTime >= endTime) {
                        1f
                    } else {
                        0f
                    }

                    // Draw if filling OR fading out (alpha > 0)
                    // We check alpha implicitly via time check or if progress is active
                    val fadeDuration = 400L
                    val isFading = currentTime >= endTime && currentTime < (endTime + fadeDuration)
                    
                    if ((progress > 0f && progress < 1f) || isFading) {
                        drawContent()
                        
                        // Soft Mask Gradient
                        // If fully filled (fading out), fillWidth is full width
                        val fillWidth = size.width * progress
                        val fadeWidth = 40f 
                        
                        val softFillBrush = Brush.horizontalGradient(
                            0f to Color.Black,
                            ((fillWidth) / size.width).coerceAtLeast(0f) to Color.Black,
                             ((fillWidth + fadeWidth) / size.width).coerceAtMost(1f) to Color.Transparent
                        )
                        
                        drawRect(
                            brush = softFillBrush,
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        )
    }
}


@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val landscapeOffset =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.LEFT)
    val lyricsAnimationStyle by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.APPLE)
    val lyricsTextSize by rememberPreference(LyricsTextSizeKey, 26f)
    val lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val romanizeJapaneseLyrics by rememberPreference(LyricsRomanizeJapaneseKey, true)
    val romanizeKoreanLyrics by rememberPreference(LyricsRomanizeKoreanKey, true)
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val lines = remember(lyrics, scope) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            val parsedLines = parseLyrics(lyrics)
            parsedLines.map { entry ->
                val newEntry = LyricsEntry(entry.time, entry.text, entry.words)
                if (romanizeJapaneseLyrics) {
                    if (isJapanese(entry.text) && !isChinese(entry.text)) {
                        scope.launch {
                            try {
                                newEntry.romanizedTextFlow.value = romanizeJapanese(entry.text)
                            } catch (e: Exception) {
                                moe.koiverse.archivetune.utils.reportException(e)
                            }
                        }
                    }
                }
                if (romanizeKoreanLyrics) {
                    if (isKorean(entry.text)) {
                        scope.launch {
                            try {
                                newEntry.romanizedTextFlow.value = romanizeKorean(entry.text)
                            } catch (e: Exception) {
                                moe.koiverse.archivetune.utils.reportException(e)
                            }
                        }
                    }
                }
                newEntry
            }.let {
                listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
            }
        } else {
            lyrics.lines().mapIndexed { index, line ->
                val newEntry = LyricsEntry(index * 100L, line)
                if (romanizeJapaneseLyrics) {
                    if (isJapanese(line) && !isChinese(line)) {
                        scope.launch {
                            try {
                                newEntry.romanizedTextFlow.value = romanizeJapanese(line)
                            } catch (e: Exception) {
                                moe.koiverse.archivetune.utils.reportException(e)
                            }
                        }
                    }
                }
                if (romanizeKoreanLyrics) {
                    if (isKorean(line)) {
                        scope.launch {
                            try {
                                newEntry.romanizedTextFlow.value = romanizeKorean(line)
                            } catch (e: Exception) {
                                moe.koiverse.archivetune.utils.reportException(e)
                            }
                        }
                    }
                }
                newEntry
            }
        }
    }
    val isSynced =
        remember(lyrics) {
            !lyrics.isNullOrEmpty() && lyrics.startsWith("[")
        }

    val lyricsBaseColor = if (useDarkTheme) Color.White else Color.Black
    val lyricsGlowColor = if (useDarkTheme) Color.White else Color.Black
    val textColor = lyricsBaseColor

    var currentLineIndex by remember {
        mutableIntStateOf(-1)
    }
    var deferredCurrentLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var currentPlaybackPosition by remember {
        mutableLongStateOf(0L)
    }

    var previousLineIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    var lastPreviewTime by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var isSeeking by remember {
        mutableStateOf(false)
    }

    var initialScrollDone by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldScrollToFirstLine by rememberSaveable {
        mutableStateOf(true)
    }

    var isAppMinimized by rememberSaveable {
        mutableStateOf(false)
    }

    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var showColorPickerDialog by remember { mutableStateOf(false) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    var isAnimating by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    val maxSelectionLimit = 5

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if(event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    LaunchedEffect(lyrics) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) {
            currentLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(8)
            val sliderPosition = sliderPositionProvider()
            isSeeking = sliderPosition != null
            val position = sliderPosition ?: playerConnection.player.currentPosition
            currentPlaybackPosition = position
            currentLineIndex = findCurrentLineIndex(
                lines,
                position
            )
        }
    }

    var isManualScrolling by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(LyricsPreviewTime)
            if (!isManualScrolling) {
                lastPreviewTime = 0L
            } else {
            }
        }
    }

    LaunchedEffect(currentLineIndex, lastPreviewTime, initialScrollDone) {

        fun calculateOffset() = with(density) {
            if (currentLineIndex < 0 || currentLineIndex >= lines.size) return@with 0
            val currentItem = lines[currentLineIndex]
            val totalNewLines = currentItem.text.count { it == '\n' }

            val dpValue = if (landscapeOffset) 16.dp else 20.dp
            dpValue.toPx().toInt() * totalNewLines
        }

        if (!isSynced) return@LaunchedEffect

        suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 600, isSeek: Boolean = false) {
            if (isAnimating) return

            isAnimating = true

            try {
                val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                if (itemInfo != null) {
                    val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                    val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val offset = itemCenter - center

                    if (abs(offset) > 5) {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = if (isSeek) {
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            } else {
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessVeryLow
                                )
                            }
                        )
                    }
                } else {
                    val firstVisibleIndex = lazyListState.firstVisibleItemIndex
                    val distance = abs(targetIndex - firstVisibleIndex)

                    if (distance > 10) {
                        lazyListState.scrollToItem(targetIndex)
                        delay(16)
                        val newItemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                        if (newItemInfo != null) {
                            val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                            val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                            val itemCenter = newItemInfo.offset + newItemInfo.size / 2
                            val finalOffset = itemCenter - center
                            if (abs(finalOffset) > 5) {
                                lazyListState.animateScrollBy(
                                    value = finalOffset.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    } else {
                        lazyListState.animateScrollToItem(targetIndex)
                    }
                }
            } finally {
                isAnimating = false
            }
        }

        if((currentLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
            shouldScrollToFirstLine = false
            val initialCenterIndex = kotlin.math.max(0, currentLineIndex)
            performSmoothPageScroll(initialCenterIndex, duration = 500)
            if(!isAppMinimized) {
                initialScrollDone = true
            }
        } else if (currentLineIndex != -1) {
            deferredCurrentLineIndex = currentLineIndex
            if (isSeeking) {
                val seekCenterIndex = kotlin.math.max(0, currentLineIndex)
                performSmoothPageScroll(seekCenterIndex, duration = 300, isSeek = true)
            } else if ((lastPreviewTime == 0L || currentLineIndex != previousLineIndex) && scrollLyrics && !isManualScrolling) {
                if (currentLineIndex != previousLineIndex) {
                    val centerTargetIndex = kotlin.math.max(0, currentLineIndex)
                    performSmoothPageScroll(centerTargetIndex, duration = 600)
                }
            }
        }
        if(currentLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousLineIndex = currentLineIndex
    }

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
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        } else {
            LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.systemBars
                .only(WindowInsetsSides.Top)
                .add(WindowInsets(top = maxHeight / 2, bottom = maxHeight / 2))
                .asPaddingValues(),
            modifier = Modifier
                .smoothFadingEdge(vertical = 72.dp)
                .nestedScroll(remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (!isSelectionModeActive && source == NestedScrollSource.UserInput) {
                                lastPreviewTime = System.currentTimeMillis()
                                isManualScrolling = true
                            }
                            return super.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            if (!isSelectionModeActive) {
                                lastPreviewTime = System.currentTimeMillis()
                                isManualScrolling = true
                            }
                            return super.onPostFling(consumed, available)
                        }
                    }
                })
        ) {
            val displayedCurrentLineIndex =
                if (isSeeking || isSelectionModeActive) deferredCurrentLineIndex else currentLineIndex

            if (lyrics == null) {
                item {
                    ShimmerHost {
                        repeat(10) {
                            Box(
                                contentAlignment = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Alignment.CenterStart
                                    LyricsPosition.CENTER -> Alignment.Center
                                    LyricsPosition.RIGHT -> Alignment.CenterEnd
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                            ) {
                                TextPlaceholder()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = lines,
                    key = { index, _ -> index }
                ) { index, item ->
                    val isSelected = selectedIndices.contains(index)

                    val distance = abs(index - displayedCurrentLineIndex)

                    val targetAlpha = when {
                        !isSynced || (isSelectionModeActive && isSelected) -> 1f
                        isManualScrolling -> when {
                            index == displayedCurrentLineIndex -> 1f
                            distance == 1 -> 0.85f
                            distance == 2 -> 0.70f
                            distance == 3 -> 0.55f
                            else -> 0.45f
                        }
                        index == displayedCurrentLineIndex -> 1f
                        distance == 1 -> 0.65f
                        distance == 2 -> 0.40f
                        distance == 3 -> 0.25f
                        else -> 0.15f
                    }

                    val animatedAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = SmoothDecelerateEasing
                        ),
                        label = "lyricAlpha"
                    )

                    val targetScale = when {
                        !isSynced || index == displayedCurrentLineIndex -> 1f
                        isManualScrolling -> when {
                            distance == 1 -> 0.98f
                            distance == 2 -> 0.96f
                            else -> 0.95f
                        }
                        distance == 1 -> 0.97f
                        distance == 2 -> 0.94f
                        else -> 0.92f
                    }

                    val animatedScale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "lyricScale"
                    )

                    val targetBlur = when {
                        !isSynced || index == displayedCurrentLineIndex -> 0f
                        isManualScrolling -> when {
                            distance == 1 -> 0.15f
                            distance == 2 -> 0.25f
                            else -> 0.35f
                        }
                        distance == 1 -> 0.3f
                        distance == 2 -> 0.6f
                        else -> 1f
                    }

                    val animatedBlur by animateFloatAsState(
                        targetValue = targetBlur,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        ),
                        label = "lyricBlur"
                    )

                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            enabled = true,
                            onClick = {
                                if (isSelectionModeActive) {
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive = false
                                        }
                                    } else {
                                        if (selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (isSynced && changeLyrics) {
                                    playerConnection.player.seekTo(item.time)
                                    scope.launch {
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                        if (itemInfo != null) {
                                            val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val offset = itemCenter - center

                                            if (abs(offset) > 10) {
                                                lazyListState.animateScrollBy(
                                                    value = offset.toFloat(),
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessVeryLow
                                                    )
                                                )
                                            }
                                        } else {
                                            lazyListState.animateScrollToItem(index)
                                        }
                                    }
                                    lastPreviewTime = 0L
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    showMaxSelectionToast = true
                                }
                            }
                        )
                        .background(
                            if (isSelected && isSelectionModeActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .padding(
                            horizontal = 24.dp,
                            vertical = (8 + (lyricsTextSize - 24f) * 0.6f).dp
                        )
                        .alpha(animatedAlpha)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            if (animatedBlur > 0.1f && distance > 2) {
                                alpha = animatedAlpha * (1f - animatedBlur * 0.1f)
                            }
                        }

                    Column(
                        modifier = itemModifier,
                        horizontalAlignment = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> Alignment.Start
                            LyricsPosition.CENTER -> Alignment.CenterHorizontally
                            LyricsPosition.RIGHT -> Alignment.End
                        }
                    ) {
                        val isActiveLine = index == displayedCurrentLineIndex && isSynced
                        val lineColor = if (isActiveLine) lyricsBaseColor else lyricsBaseColor.copy(alpha = 0.7f)
                        val alignment = when (lyricsTextPosition) {
                            LyricsPosition.LEFT -> TextAlign.Left
                            LyricsPosition.CENTER -> TextAlign.Center
                            LyricsPosition.RIGHT -> TextAlign.Right
                        }

                        val hasWordTimings = item.words?.isNotEmpty() == true
                        val romanizedText by item.romanizedTextFlow.collectAsState()
                        val hasRomanization = romanizedText != null

                        val effectiveAnimationStyle = if (lyricsAnimationStyle == LyricsAnimationStyle.KARAOKE && !hasWordTimings) {
                            LyricsAnimationStyle.APPLE
                        } else {
                            lyricsAnimationStyle
                        }

                        if (hasWordTimings && item.words != null && effectiveAnimationStyle == LyricsAnimationStyle.KARAOKE) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = when (lyricsTextPosition) {
                                    LyricsPosition.LEFT -> Arrangement.Start
                                    LyricsPosition.CENTER -> Arrangement.Center
                                    LyricsPosition.RIGHT -> Arrangement.End
                                },
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    
                                    val isUpcoming = isActiveLine && currentPlaybackPosition < wordStartMs
                                    
                                    KaraokeWord(
                                        text = word.text,
                                        startTime = wordStartMs,
                                        endTime = wordEndMs,
                                        currentTimeProvider = { currentPlaybackPosition },
                                        fontSize = lyricsTextSize.sp,
                                        textColor = lyricsBaseColor,
                                        inactiveAlpha = if (isUpcoming && isActiveLine) 0.3f else if (!isActiveLine) 0.5f else 0.3f,
                                        fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.ExtraBold
                                    )
                                    
                                    if (wordIndex < item.words.size - 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }
                            }
                        } else if (hasWordTimings && item.words != null && effectiveAnimationStyle == LyricsAnimationStyle.APPLE) {

                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && currentPlaybackPosition > wordEndMs

                                    val transitionProgress = when {
                                        !isActiveLine -> 0f
                                        hasWordPassed -> 1f
                                        isWordActive && wordDuration > 0 -> {
                                            val elapsed = currentPlaybackPosition - wordStartMs

                                            val linear = (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)

                                            linear * linear * (3f - 2f * linear) 
                                        }
                                        else -> 0f
                                    }

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.7f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.5f + (0.5f * transitionProgress) 
                                        else -> 0.35f 
                                    }

                                    val wordColor = lyricsBaseColor.copy(alpha = wordAlpha)

                                    val wordWeight = if (hasRomanization) {
                                        FontWeight.Bold
                                    } else {
                                        when {
                                            !isActiveLine -> FontWeight.Bold
                                            hasWordPassed -> FontWeight.Bold
                                            isWordActive -> FontWeight.ExtraBold
                                            else -> FontWeight.Medium
                                        }
                                    }

                                    withStyle(
                                        style = SpanStyle(
                                            color = wordColor,
                                            fontWeight = wordWeight
                                        )
                                    ) {
                                        append(word.text)
                                    }

                                    if (wordIndex < item.words.size - 1) {
                                        append(" ")
                                    }
                                }
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && item.words != null && effectiveAnimationStyle == LyricsAnimationStyle.FADE) {

                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition <= wordEndMs
                                    val hasWordPassed = isActiveLine && currentPlaybackPosition > wordEndMs

                                    val fadeProgress = if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = currentPlaybackPosition - wordStartMs
                                        val linear = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)

                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) {
                                        1f
                                    } else {
                                        0f
                                    }

                                    val wordAlpha = if (isActiveLine) {
                                        0.35f + (0.65f * fadeProgress)
                                    } else {
                                        0.65f
                                    }

                                    val wordColor = lyricsBaseColor.copy(alpha = wordAlpha)

                                    val wordWeight = if (hasRomanization) {
                                        FontWeight.Bold
                                    } else {
                                        when {
                                            !isActiveLine -> FontWeight.Bold
                                            hasWordPassed -> FontWeight.Bold
                                            isWordActive -> FontWeight.ExtraBold
                                            else -> FontWeight.Medium
                                        }
                                    }

                                    withStyle(
                                        style = SpanStyle(
                                            color = wordColor,
                                            fontWeight = wordWeight
                                        )
                                    ) {
                                        append(word.text)
                                    }

                                    if (wordIndex < item.words.size - 1) {
                                        append(" ")
                                    }
                                }
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && item.words != null && effectiveAnimationStyle == LyricsAnimationStyle.GLOW) {

                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition in wordStartMs..wordEndMs
                                    val hasWordPassed = isActiveLine && currentPlaybackPosition > wordEndMs

                                    val fillProgress = if (isWordActive && wordDuration > 0) {
                                        val linear = ((currentPlaybackPosition - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)

                                        linear * linear * (3f - 2f * linear)
                                    } else if (hasWordPassed) {
                                        1f
                                    } else {
                                        0f
                                    }

                                    val glowIntensity = fillProgress * fillProgress 
                                    val brightness = 0.45f + (0.55f * fillProgress)

                                    val wordColor = when {
                                        !isActiveLine -> lyricsBaseColor.copy(alpha = 0.5f)
                                        isWordActive || hasWordPassed -> lyricsBaseColor.copy(alpha = brightness)
                                        else -> lyricsBaseColor.copy(alpha = 0.35f)
                                    }

                                    val wordWeight = if (hasRomanization) {
                                        FontWeight.Bold
                                    } else {
                                        when {
                                            !isActiveLine -> FontWeight.Bold
                                            isWordActive -> FontWeight.ExtraBold
                                            hasWordPassed -> FontWeight.Bold
                                            else -> FontWeight.Medium
                                        }
                                    }

                                    val floatOffset = if (isWordActive && fillProgress > 0.1f) {

                                        val floatAmount = sin(fillProgress * Math.PI).toFloat() * 0.5f
                                        Offset(0f, -floatAmount)
                                    } else {
                                        Offset.Zero
                                    }

                                    val wordShadow = if (isWordActive && glowIntensity > 0.05f) {
                                        Shadow(
                                            color = lyricsGlowColor.copy(alpha = if (hasRomanization) 0.6f + (0.3f * glowIntensity) else 0.5f + (0.3f * glowIntensity)),
                                            offset = floatOffset,
                                            blurRadius = if (hasRomanization) 20f + (12f * glowIntensity) else 16f + (12f * glowIntensity)
                                        )
                                    } else if (hasWordPassed) {
                                        Shadow(
                                            color = lyricsGlowColor.copy(alpha = if (hasRomanization) 0.35f else 0.25f),
                                            offset = Offset.Zero,
                                            blurRadius = if (hasRomanization) 12f else 8f
                                        )
                                    } else null

                                    withStyle(
                                        style = SpanStyle(
                                            color = wordColor,
                                            fontWeight = wordWeight,
                                            shadow = wordShadow
                                        )
                                    ) {
                                        append(word.text)
                                    }

                                    if (wordIndex < item.words.size - 1) {
                                        append(" ")
                                    }
                                }
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && item.words != null && effectiveAnimationStyle == LyricsAnimationStyle.SLIDE) {

                            val firstWordStartMs = (item.words.firstOrNull()?.startTime?.times(1000))?.toLong() ?: 0L
                            val lastWordEndMs = (item.words.lastOrNull()?.endTime?.times(1000))?.toLong() ?: 0L
                            val lineDuration = lastWordEndMs - firstWordStartMs

                            val isLineActive = isActiveLine && currentPlaybackPosition >= firstWordStartMs && currentPlaybackPosition <= lastWordEndMs
                            val hasLinePassed = isActiveLine && currentPlaybackPosition > lastWordEndMs

                            if (isLineActive && lineDuration > 0) {

                                val timeElapsed = currentPlaybackPosition - firstWordStartMs
                                val linearProgress = (timeElapsed.toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)

                                val fillProgress = linearProgress

                                val breatheValue = (timeElapsed % 3000) / 3000f
                                val breatheEffect = (sin(breatheValue * Math.PI.toFloat() * 2f) * 0.03f).coerceIn(0f, 0.03f)
                                val glowIntensity = (0.3f + fillProgress * 0.7f + breatheEffect).coerceIn(0f, 1.1f)

                                val slideBrush = Brush.horizontalGradient(
                                    0.0f to lyricsBaseColor,
                                    (fillProgress * 0.95f).coerceIn(0f, 1f) to lyricsBaseColor,
                                    fillProgress to lyricsBaseColor.copy(alpha = 0.9f),
                                    (fillProgress + 0.02f).coerceIn(0f, 1f) to lyricsBaseColor.copy(alpha = 0.5f),
                                    (fillProgress + 0.08f).coerceIn(0f, 1f) to lyricsBaseColor.copy(alpha = 0.35f),
                                    1.0f to lyricsBaseColor.copy(alpha = 0.35f)
                                )

                                val styledText = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            brush = slideBrush,
                                            fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.ExtraBold
                                        )
                                    ) {
                                        append(item.text)
                                    }
                                }

                                Text(
                                    text = styledText,
                                    fontSize = lyricsTextSize.sp,
                                    textAlign = alignment,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                                )
                            } else if (hasLinePassed) {

                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    color = lyricsBaseColor,
                                    textAlign = alignment,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                                )
                            } else {

                                Text(
                                    text = item.text,
                                    fontSize = lyricsTextSize.sp,
                                    color = if (!isActiveLine) lineColor else lyricsBaseColor.copy(alpha = 0.35f),
                                    textAlign = alignment,
                                    fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.Medium,
                                    lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                                )
                            }
                        } else if (hasWordTimings && item.words != null && effectiveAnimationStyle == LyricsAnimationStyle.KARAOKE) {
                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && currentPlaybackPosition >= wordEndMs) || (!isActiveLine && index < displayedCurrentLineIndex)
                                    val isUpcoming = isActiveLine && currentPlaybackPosition < wordStartMs

                                    if (isWordActive && wordDuration > 0) {
                                        val timeElapsed = currentPlaybackPosition - wordStartMs
                                        val linearProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                        
                                        val fillProgress = linearProgress * linearProgress * (3f - 2f * linearProgress)
                                        
                                        val breatheCycleDuration = wordDuration.toFloat().coerceIn(400f, 2000f)
                                        val breathePhase = (timeElapsed % breatheCycleDuration) / breatheCycleDuration
                                        val breatheEffect = (sin(breathePhase * Math.PI.toFloat()) * 0.05f).coerceIn(0f, 0.05f)
                                        
                                        val glowIntensity = (fillProgress + breatheEffect).coerceIn(0f, 1.0f)

                                        val wordBrush = Brush.horizontalGradient(
                                            0.0f to lyricsBaseColor,
                                            (fillProgress * 0.85f).coerceIn(0f, 0.99f) to lyricsBaseColor,
                                            fillProgress.coerceIn(0.01f, 0.99f) to lyricsBaseColor.copy(alpha = 0.85f),
                                            (fillProgress + 0.02f).coerceIn(0.01f, 1f) to lyricsBaseColor.copy(alpha = 0.45f),
                                            (fillProgress + 0.08f).coerceIn(0.01f, 1f) to lyricsBaseColor.copy(alpha = 0.3f),
                                            1.0f to lyricsBaseColor.copy(alpha = 0.3f)
                                        )

                                        withStyle(
                                            style = SpanStyle(
                                                brush = wordBrush,
                                                fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.ExtraBold
                                            )
                                        ) {
                                            append(word.text)
                                        }
                                    } else if (hasWordPassed) {
                                        withStyle(
                                            style = SpanStyle(
                                                color = lyricsBaseColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(word.text)
                                        }
                                    } else if (isUpcoming && isActiveLine) {
                                        withStyle(
                                            style = SpanStyle(
                                                color = lyricsBaseColor.copy(alpha = 0.3f),
                                                fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.Medium
                                            )
                                        ) {
                                            append(word.text)
                                        }
                                    } else {
                                        val wordColor = if (!isActiveLine) lineColor else lyricsBaseColor.copy(alpha = 0.3f)

                                        withStyle(
                                            style = SpanStyle(
                                                color = wordColor,
                                                fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.Medium
                                            )
                                        ) {
                                            append(word.text)
                                        }
                                    }

                                    if (wordIndex < item.words.size - 1) {
                                        append(" ")
                                    }
                                }
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (hasWordTimings && item.words != null && effectiveAnimationStyle == LyricsAnimationStyle.APPLE) {

                            val styledText = buildAnnotatedString {
                                item.words.forEachIndexed { wordIndex, word ->
                                    val wordStartMs = (word.startTime * 1000).toLong()
                                    val wordEndMs = (word.endTime * 1000).toLong()
                                    val wordDuration = wordEndMs - wordStartMs

                                    val isWordActive = isActiveLine && currentPlaybackPosition >= wordStartMs && currentPlaybackPosition < wordEndMs
                                    val hasWordPassed = (isActiveLine && currentPlaybackPosition >= wordEndMs) || (!isActiveLine && index < displayedCurrentLineIndex)

                                    val rawProgress = if (isWordActive && wordDuration > 0) {
                                        val elapsed = currentPlaybackPosition - wordStartMs
                                        (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                    } else if (hasWordPassed) {
                                        1f
                                    } else {
                                        0f
                                    }

                                    val smoothProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)

                                    val wordAlpha = when {
                                        !isActiveLine -> 0.55f
                                        hasWordPassed -> 1f
                                        isWordActive -> 0.55f + (0.45f * smoothProgress)
                                        else -> 0.35f
                                    }

                                    val wordColor = lyricsBaseColor.copy(alpha = wordAlpha)

                                    val wordWeight = if (hasRomanization) {
                                        FontWeight.Bold
                                    } else {
                                        when {
                                            !isActiveLine -> FontWeight.SemiBold
                                            hasWordPassed -> FontWeight.Bold
                                            isWordActive -> FontWeight.ExtraBold
                                            else -> FontWeight.Normal
                                        }
                                    }

                                    withStyle(
                                        style = SpanStyle(
                                            color = wordColor,
                                            fontWeight = wordWeight
                                        )
                                    ) {
                                        append(word.text)
                                    }

                                    if (wordIndex < item.words.size - 1) {
                                        append(" ")
                                    }
                                }
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (isActiveLine && effectiveAnimationStyle == LyricsAnimationStyle.GLOW) {

                            val styledText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        shadow = Shadow(
                                            color = lyricsGlowColor.copy(alpha = if (hasRomanization) 0.9f else 0.8f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = if (hasRomanization) 36f else 30f
                                        )
                                    )
                                ) {
                                    append(item.text)
                                }
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                color = lyricsBaseColor,
                                textAlign = alignment,
                                fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                            )
                        } else if (isActiveLine && effectiveAnimationStyle == LyricsAnimationStyle.SLIDE) {

                            val popInScale = remember { Animatable(0.95f) }

                            val fillProgress = remember { Animatable(0f) }

                            LaunchedEffect(index) {

                                popInScale.snapTo(0.95f)
                                popInScale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = FastOutSlowInEasing
                                    )
                                )

                                fillProgress.snapTo(0f)
                                fillProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 1200,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }

                            val fill = fillProgress.value

                            val slideBrush = Brush.horizontalGradient(
                                0.0f to lyricsBaseColor.copy(alpha = 0.3f),
                                (fill * 0.7f).coerceIn(0f, 1f) to lyricsBaseColor.copy(alpha = 0.9f),
                                fill to lyricsBaseColor,
                                (fill + 0.1f).coerceIn(0f, 1f) to lyricsBaseColor.copy(alpha = 0.7f),
                                1.0f to lyricsBaseColor.copy(alpha = if (fill >= 1f) 1f else 0.3f)
                            )

                            val styledText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        brush = slideBrush
                                    )
                                ) {
                                    append(item.text)
                                }
                            }

                            val bounceScale = if (fill < 0.3f) {
                                1f + (sin(fill * 3.33f * Math.PI.toFloat()) * 0.03f)
                            } else {
                                1f
                            }

                            Text(
                                text = styledText,
                                fontSize = lyricsTextSize.sp,
                                textAlign = alignment,
                                fontWeight = if (hasRomanization) FontWeight.Bold else FontWeight.ExtraBold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                modifier = Modifier.graphicsLayer {
                                    val combinedScale = bounceScale * popInScale.value
                                    scaleX = combinedScale
                                    scaleY = combinedScale
                                }
                            )
                        } else if (isActiveLine && effectiveAnimationStyle == LyricsAnimationStyle.KARAOKE) {
                             if (hasWordTimings && item.words != null) {
                                  val currentTimeProvider: () -> Long = { sliderPositionProvider() ?: 0L }
                                  
                                  FlowRow(
                                      modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
                                      horizontalArrangement = when (lyricsTextPosition) {
                                          LyricsPosition.LEFT -> Arrangement.Start
                                          LyricsPosition.CENTER -> Arrangement.Center
                                          LyricsPosition.RIGHT -> Arrangement.End
                                      },
                                      verticalArrangement = Arrangement.Center
                                  ) {
                                      item.words.forEachIndexed { idx, word ->
                                           val wordStartMs = (word.startTime * 1000).toLong()
                                           val wordEndMs = (word.endTime * 1000).toLong()
                                           
                                           KaraokeWord(
                                              text = word.text,
                                              startTime = wordStartMs,
                                              endTime = wordEndMs,
                                              currentTimeProvider = currentTimeProvider,
                                              fontSize = lyricsTextSize.sp,
                                              textColor = lyricsBaseColor,
                                              inactiveAlpha = 0.3f,
                                              modifier = Modifier.padding(horizontal = 2.dp)
                                           )
                                      }
                                  }
                             } else {
                                  // Fallback to standard text if timings are missing
                                  Text(
                                      text = item.text,
                                      fontSize = lyricsTextSize.sp,
                                      color = lyricsBaseColor,
                                      textAlign = alignment,
                                      fontWeight = FontWeight.ExtraBold,
                                      lineHeight = (lyricsTextSize * lyricsLineSpacing).sp
                                  )
                             }
                        } else if (isActiveLine && effectiveAnimationStyle == LyricsAnimationStyle.APPLE) {

                            val popInScale = remember { Animatable(0.96f) }

                            LaunchedEffect(index) {

                                popInScale.snapTo(0.96f)
                                popInScale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }

                            Text(
                                text = item.text,
                                fontSize = lyricsTextSize.sp,
                                color = lyricsBaseColor,
                                textAlign = alignment,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = popInScale.value
                                    scaleY = popInScale.value
                                }
                            )
                        } else {

                            val popInScale = remember { Animatable(1f) }

                            LaunchedEffect(isActiveLine) {
                                if (isActiveLine) {
                                    popInScale.snapTo(0.96f)
                                    popInScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }

                            Text(
                                text = item.text,
                                fontSize = lyricsTextSize.sp,
                                color = lineColor,
                                textAlign = alignment,
                                fontWeight = if (hasRomanization) FontWeight.Bold else if (isActiveLine) FontWeight.ExtraBold else FontWeight.Bold,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                modifier = if (isActiveLine) Modifier.graphicsLayer {
                                    scaleX = popInScale.value
                                    scaleY = popInScale.value
                                } else Modifier
                            )
                        }
                        if (romanizeJapaneseLyrics || romanizeKoreanLyrics) {

                            val romanizedFontSize = 16.sp
                            romanizedText?.let { romanized ->

                                if (hasWordTimings && item.words != null && isActiveLine && effectiveAnimationStyle != LyricsAnimationStyle.NONE) {

                                    val romanizedWords = romanized.split(" ")
                                    val mainWords = item.words

                                    val romanizedStyledText = buildAnnotatedString {
                                        romanizedWords.forEachIndexed { romIndex, romWord ->

                                            val wordIndex = (romIndex.toFloat() / romanizedWords.size * mainWords.size).toInt().coerceIn(0, mainWords.size - 1)
                                            val word = mainWords.getOrNull(wordIndex)

                                            if (word != null) {
                                                val wordStartMs = (word.startTime * 1000).toLong()
                                                val wordEndMs = (word.endTime * 1000).toLong()
                                                val wordDuration = wordEndMs - wordStartMs
                                                val isWordActive = currentPlaybackPosition in wordStartMs..wordEndMs
                                                val hasWordPassed = currentPlaybackPosition > wordEndMs

                                                when (effectiveAnimationStyle) {
                                                    LyricsAnimationStyle.APPLE -> {
                                                        val rawProgress = if (isWordActive && wordDuration > 0) {
                                                            val elapsed = currentPlaybackPosition - wordStartMs
                                                            (elapsed.toFloat() / wordDuration).coerceIn(0f, 1f)
                                                        } else if (hasWordPassed) {
                                                            1f
                                                        } else {
                                                            0f
                                                        }
                                                        val smoothProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)

                                                        val romAlpha = when {
                                                            hasWordPassed -> 0.8f
                                                            isWordActive -> 0.4f + (0.4f * smoothProgress)
                                                            else -> 0.3f
                                                        }

                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = lyricsBaseColor.copy(alpha = romAlpha),
                                                                fontWeight = if (hasWordPassed || isWordActive) FontWeight.Medium else FontWeight.Normal
                                                            )
                                                        ) {
                                                            append(romWord)
                                                        }
                                                    }
                                                    LyricsAnimationStyle.FADE -> {
                                                        val fadeProgress = if (isWordActive && wordDuration > 0) {
                                                            val timeElapsed = currentPlaybackPosition - wordStartMs
                                                            (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                                        } else if (hasWordPassed) {
                                                            1f
                                                        } else {
                                                            0f
                                                        }
                                                        val romAlpha = 0.3f + (0.5f * fadeProgress)

                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = lyricsBaseColor.copy(alpha = romAlpha),
                                                                fontWeight = if (hasWordPassed || isWordActive) FontWeight.Medium else FontWeight.Normal
                                                            )
                                                        ) {
                                                            append(romWord)
                                                        }
                                                    }
                                                    LyricsAnimationStyle.SLIDE -> {
                                                        if (isWordActive && wordDuration > 0) {
                                                            val timeElapsed = currentPlaybackPosition - wordStartMs
                                                            val fillProgress = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)

                                                            val romBrush = Brush.horizontalGradient(
                                                                0.0f to lyricsBaseColor.copy(alpha = 0.8f),
                                                                (fillProgress * 0.95f).coerceIn(0f, 1f) to lyricsBaseColor.copy(alpha = 0.8f),
                                                                fillProgress to lyricsBaseColor.copy(alpha = 0.5f),
                                                                (fillProgress + 0.05f).coerceIn(0f, 1f) to lyricsBaseColor.copy(alpha = 0.3f),
                                                                1.0f to lyricsBaseColor.copy(alpha = 0.3f)
                                                            )

                                                            withStyle(
                                                                style = SpanStyle(
                                                                    brush = romBrush,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            ) {
                                                                append(romWord)
                                                            }
                                                        } else {
                                                            val romColor = when {
                                                                hasWordPassed -> lyricsBaseColor.copy(alpha = 0.7f)
                                                                else -> lyricsBaseColor.copy(alpha = 0.3f)
                                                            }
                                                            withStyle(
                                                                style = SpanStyle(
                                                                    color = romColor,
                                                                    fontWeight = if (hasWordPassed) FontWeight.Medium else FontWeight.Normal
                                                                )
                                                            ) {
                                                                append(romWord)
                                                            }
                                                        }
                                                    }
                                                    LyricsAnimationStyle.GLOW -> {
                                                        val fillProgress = if (isWordActive && wordDuration > 0) {
                                                            val timeElapsed = currentPlaybackPosition - wordStartMs
                                                            (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                                                        } else if (hasWordPassed) {
                                                            1f
                                                        } else {
                                                            0f
                                                        }

                                                        val romAlpha = 0.4f + (0.4f * fillProgress)
                                                        val romShadow = if (isWordActive && fillProgress > 0.05f) {
                                                            Shadow(
                                                                color = lyricsGlowColor.copy(alpha = 0.5f * fillProgress),
                                                                offset = Offset.Zero,
                                                                blurRadius = 10f * fillProgress
                                                            )
                                                        } else null

                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = lyricsBaseColor.copy(alpha = romAlpha),
                                                                fontWeight = if (isWordActive) FontWeight.Medium else FontWeight.Normal,
                                                                shadow = romShadow
                                                            )
                                                        ) {
                                                            append(romWord)
                                                        }
                                                    }
                                                    else -> {

                                                        val romColor = when {
                                                            !isActiveLine -> lyricsBaseColor.copy(alpha = 0.5f)
                                                            isWordActive -> lyricsBaseColor.copy(alpha = 0.8f)
                                                            hasWordPassed -> lyricsBaseColor.copy(alpha = 0.7f)
                                                            else -> lyricsBaseColor.copy(alpha = 0.4f)
                                                        }

                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = romColor,
                                                                fontWeight = if (isWordActive) FontWeight.Medium else FontWeight.Normal
                                                            )
                                                        ) {
                                                            append(romWord)
                                                        }
                                                    }
                                                }
                                            } else {
                                                withStyle(
                                                    style = SpanStyle(
                                                        color = lyricsBaseColor.copy(alpha = 0.5f),
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                ) {
                                                    append(romWord)
                                                }
                                            }

                                            if (romIndex < romanizedWords.size - 1) {
                                                append(" ")
                                            }
                                        }
                                    }

                                    Text(
                                        text = romanizedStyledText,
                                        fontSize = romanizedFontSize,
                                        textAlign = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> TextAlign.Left
                                            LyricsPosition.CENTER -> TextAlign.Center
                                            LyricsPosition.RIGHT -> TextAlign.Right
                                        },
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {

                                    Text(
                                        text = romanized,
                                        fontSize = romanizedFontSize,
                                        color = lyricsBaseColor.copy(alpha = if (isActiveLine) 0.6f else 0.5f),
                                        textAlign = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> TextAlign.Left
                                            LyricsPosition.CENTER -> TextAlign.Center
                                            LyricsPosition.RIGHT -> TextAlign.Right
                                        },
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

            AnimatedVisibility(
                visible = isManualScrolling && scrollLyrics && !isSelectionModeActive,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    initialOffsetY = { it * 2 }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    targetOffsetY = { it * 2 }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            isManualScrolling = false
                            lastPreviewTime = 0L

                            // Automatic scroll to current lyric
                            if (currentLineIndex >= 0) {
                                scope.launch {
                                    lazyListState.animateScrollToItem(
                                        index = currentLineIndex,
                                        scrollOffset = 0
                                    )
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.resume_autoscroll),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

        if (isSelectionModeActive) {
            mediaMetadata?.let { metadata ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp), 
                    contentAlignment = Alignment.Center
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(48.dp) 
                                .background(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = stringResource(R.string.cancel),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .background(
                                    color = if (selectedIndices.isNotEmpty()) 
                                        Color.White.copy(alpha = 0.9f) 
                                    else 
                                        Color.White.copy(alpha = 0.5f), 
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(enabled = selectedIndices.isNotEmpty()) {
                                    if (selectedIndices.isNotEmpty()) {
                                        val sortedIndices = selectedIndices.sorted()
                                        val selectedLyricsText = sortedIndices
                                            .mapNotNull { lines.getOrNull(it)?.text }
                                            .joinToString("\n")

                                        if (selectedLyricsText.isNotBlank()) {
                                            shareDialogData = Triple(
                                                selectedLyricsText,
                                                metadata.title,
                                                metadata.artists.joinToString { it.name }
                                            )
                                            showShareDialog = true
                                        }
                                        isSelectionModeActive = false
                                        selectedIndices.clear()
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.share),
                                contentDescription = stringResource(R.string.share_selected),
                                tint = Color.Black, 
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.share),
                                color = Color.Black, 
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = {  }) {
            Card( 
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!! 
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    val songLink = "https://music.youtube.com/watch?v=${mediaMetadata?.id}"

                                    putExtra(Intent.EXTRA_TEXT, "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showColorPickerDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share), 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showShareDialog = false }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showColorPickerDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        val coverUrl = mediaMetadata?.thumbnailUrl
        val paletteColors = remember { mutableStateListOf<Color>() }

        val previewCardWidth = configuration.screenWidthDp.dp * 0.90f
        val previewPadding = 20.dp * 2
        val previewBoxPadding = 28.dp * 2
        val previewAvailableWidth = previewCardWidth - previewPadding - previewBoxPadding
        val previewBoxHeight = 340.dp
        val headerFooterEstimate = (48.dp + 14.dp + 16.dp + 20.dp + 8.dp + 28.dp * 2)
        val previewAvailableHeight = previewBoxHeight - headerFooterEstimate

        val textStyleForMeasurement = TextStyle(
            color = previewTextColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        val textMeasurer = rememberTextMeasurer()

        rememberAdjustedFontSize(
            text = lyricsText,
            maxWidth = previewAvailableWidth,
            maxHeight = previewAvailableHeight,
            density = density,
            initialFontSize = 50.sp,
            minFontSize = 22.sp,
            style = textStyleForMeasurement,
            textMeasurer = textMeasurer
        )

        LaunchedEffect(coverUrl) {
            if (coverUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                        val result = loader.execute(req)
                        val bmp = result.image?.toBitmap()
                        if (bmp != null) {
                            val palette = Palette.from(bmp).generate()
                            val swatches = palette.swatches.sortedByDescending { it.population }
                            val colors = swatches.map { Color(it.rgb) }
                                .filter { color ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hsv[1] > 0.2f
                                }
                            paletteColors.clear()
                            paletteColors.addAll(colors.take(5))
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        BasicAlertDialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.customize_colors),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(8.dp)
                    ) {
                        LyricsImageCard(
                            lyricText = lyricsText,
                            mediaMetadata = mediaMetadata ?: return@Box,
                            backgroundColor = previewBackgroundColor,
                            textColor = previewTextColor,
                            secondaryTextColor = previewSecondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(text = stringResource(id = R.string.background_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color(0xFF242424), Color(0xFF121212), Color.White, Color.Black, Color(0xFFF5F5F5))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewBackgroundColor = color }
                                    .border(2.dp, if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors + listOf(Color.White, Color.Black, Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewTextColor = color }
                                    .border(2.dp, if (previewTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Text(text = stringResource(id = R.string.secondary_text_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        (paletteColors.map { it.copy(alpha = 0.7f) } + listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.7f), Color(0xFF1DB954))).distinct().take(8).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, shape = RoundedCornerShape(8.dp))
                                    .clickable { previewSecondaryTextColor = color }
                                    .border(2.dp, if (previewSecondaryTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showColorPickerDialog = false
                            showProgressDialog = true
                            scope.launch {
                                try {
                                    val screenWidth = configuration.screenWidthDp
                                    val screenHeight = configuration.screenHeightDp

                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = coverUrl,
                                        songTitle = songTitle,
                                        artistName = artists,
                                        lyrics = lyricsText,
                                        width = (screenWidth * density.density).toInt(),
                                        height = (screenHeight * density.density).toInt(),
                                        backgroundColor = previewBackgroundColor.toArgb(),
                                        textColor = previewTextColor.toArgb(),
                                        secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                    )
                                    val timestamp = System.currentTimeMillis()
                                    val filename = "lyrics_$timestamp"
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to create image: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    showProgressDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.share))
                    }
                }
            }
        }
        } 
    }
}

private const val ArchiveTune_AUTO_SCROLL_DURATION = 1500L 
private const val ArchiveTune_INITIAL_SCROLL_DURATION = 1000L 
private const val ArchiveTune_SEEK_DURATION = 800L 
private const val ArchiveTune_FAST_SEEK_DURATION = 600L 

val LyricsPreviewTime = 2.seconds
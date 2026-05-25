/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.component

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.LyricsClickKey
import moe.koiverse.archivetune.constants.LyricsLineBlurKey
import moe.koiverse.archivetune.constants.LyricsRomanizeChineseKey
import moe.koiverse.archivetune.constants.LyricsRomanizeHindiKey
import moe.koiverse.archivetune.constants.LyricsRomanizeJapaneseKey
import moe.koiverse.archivetune.constants.LyricsRomanizeKoreanKey
import moe.koiverse.archivetune.constants.LyricsRomanizeOtherLanguagesKey
import moe.koiverse.archivetune.constants.LyricsTextSizeKey
import moe.koiverse.archivetune.constants.PlayerBackgroundStyle
import moe.koiverse.archivetune.constants.PlayerBackgroundStyleKey
import moe.koiverse.archivetune.constants.UseSystemFontKey
import moe.koiverse.archivetune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import moe.koiverse.archivetune.lyrics.LyricsEntry
import moe.koiverse.archivetune.lyrics.LyricsRomanizationPreferences
import moe.koiverse.archivetune.lyrics.LyricsUtils.isTtml
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseLyrics
import moe.koiverse.archivetune.lyrics.LyricsUtils.parseTtml
import moe.koiverse.archivetune.lyrics.LyricsUtils.romanizeLyricsLine
import moe.koiverse.archivetune.lyrics.LyricsUtils.shouldRomanizeLyricsLine
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.utils.reportException
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LRC_LEAD_MS = 300L
private const val TTML_LEAD_MS = 0L
private const val LYRIC_VISUAL_TUNING_OFFSET_MS = 150L
private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L
private const val MANUAL_SCROLL_DEBOUNCE_MS = 50L
private const val ACTIVE_LINE_REVEAL_DELAY_MS = 90L
private const val ACTIVE_LINE_FAR_SCROLL_THRESHOLD = 10

@Composable
fun LyricsEnhanced(
    sliderPositionProvider: () -> Long?,
    lyricsSyncOffset: Int,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val (lyricsLineBlur) = rememberPreference(LyricsLineBlurKey, defaultValue = true)
    val (romanizeChinese) = rememberPreference(LyricsRomanizeChineseKey, defaultValue = true)
    val (romanizeHindi) = rememberPreference(LyricsRomanizeHindiKey, defaultValue = true)
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (romanizeOtherLanguages) = rememberPreference(LyricsRomanizeOtherLanguagesKey, defaultValue = true)
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)

    val romanizationPreferences = remember(
        romanizeJapanese, romanizeKorean, romanizeChinese, romanizeHindi, romanizeOtherLanguages,
    ) {
        LyricsRomanizationPreferences(
            romanizeJapanese = romanizeJapanese,
            romanizeKorean = romanizeKorean,
            romanizeChinese = romanizeChinese,
            romanizeHindi = romanizeHindi,
            romanizeOther = romanizeOtherLanguages,
        )
    }

    val lyricsFontFamily = remember(useSystemFont) {
        if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold))
    }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT)
        MaterialTheme.colorScheme.onBackground
    else
        Color.White

    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedLineStarts = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    val maxSelectionLimit = 5
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showShareImageDialog by remember { mutableStateOf(false) }

    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = currentLyrics?.lyrics

    val isSynced = remember(lyrics) { lyrics != null && (lyrics!!.startsWith("[") || isTtml(lyrics!!)) }
    val isTtmlFormat = remember(lyrics) { lyrics != null && isTtml(lyrics!!) }

    val lyricsEntries: List<LyricsEntry> = remember(lyrics) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) return@remember emptyList()
        when {
            isTtml(lyrics!!) -> parseTtml(lyrics!!)
            lyrics!!.startsWith("[") -> parseLyrics(lyrics!!)
            else -> lyrics!!.lines()
                .filter { it.isNotBlank() }
                .map { line -> LyricsEntry(time = -1L, text = line.trim()) }
        }
    }

    var syncedLyrics by remember(lyricsEntries, isTtmlFormat) {
        mutableStateOf(buildSyncedLyrics(lyricsEntries, isTtmlFormat, emptyMap()))
    }

    LaunchedEffect(lyricsEntries, romanizationPreferences) {
        syncedLyrics = buildSyncedLyrics(lyricsEntries, isTtmlFormat, emptyMap())
        if (!romanizationPreferences.isEnabled) return@LaunchedEffect

        val toRomanize = lyricsEntries.mapIndexedNotNull { index, entry ->
            if (shouldRomanizeLyricsLine(entry.text, romanizationPreferences)) index to entry else null
        }
        if (toRomanize.isEmpty()) return@LaunchedEffect

        val jobs = toRomanize.map { (index, entry) ->
            async {
                val romanized: List<String?> = try {
                    if (isTtmlFormat && entry.words != null) {
                        entry.words!!.filter { !it.isBackground }.map { word ->
                            romanizeLyricsLine(word.text, romanizationPreferences)
                        }
                    } else {
                        listOf(romanizeLyricsLine(entry.text, romanizationPreferences))
                    }
                } catch (e: Exception) {
                    reportException(e)
                    if (isTtmlFormat && entry.words != null) {
                        List(entry.words!!.count { !it.isBackground }) { null }
                    } else {
                        listOf(null)
                    }
                }
                index to romanized
            }
        }
        val tempMap = mutableMapOf<Int, List<String?>>()
        jobs.awaitAll().forEach { (index, romanized) ->
            tempMap[index] = romanized
        }
        syncedLyrics = buildSyncedLyrics(lyricsEntries, isTtmlFormat, tempMap)
    }

    val leadMs = if (isTtmlFormat) TTML_LEAD_MS else LRC_LEAD_MS

    val latestSliderPositionProvider = rememberUpdatedState(sliderPositionProvider)
    val latestLyricsSyncOffset = rememberUpdatedState(lyricsSyncOffset)
    val latestLeadMs = rememberUpdatedState(leadMs)
    val playbackPositionMs = remember(player) {
        mutableLongStateOf(player.currentPosition.coerceAtLeast(0L))
    }
    var isManualScrolling by remember { mutableStateOf(false) }
    var lastManualScrollTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player) {
        var wasSliderActive = false
        while (isActive) {
            val sliderPosition = latestSliderPositionProvider.value()
            val isSliderActive = sliderPosition != null
            if (isSliderActive && !wasSliderActive) {
                isManualScrolling = false
            }
            wasSliderActive = isSliderActive
            val nextPosition = (sliderPosition ?: player.currentPosition).coerceAtLeast(0L)
            if (playbackPositionMs.longValue != nextPosition) {
                playbackPositionMs.longValue = nextPosition
            }
            if (sliderPosition == null && !player.isPlaying) {
                delay(100L)
            } else {
                withFrameNanos { }
            }
        }
    }

    val currentPosition: () -> Int = remember {
        {
            (
                playbackPositionMs.longValue +
                    latestLyricsSyncOffset.value.toLong() +
                    latestLeadMs.value +
                    LYRIC_VISUAL_TUNING_OFFSET_MS
                )
                .coerceIn(0L, Int.MAX_VALUE.toLong())
                .toInt()
        }
    }
    val focusedPosition: () -> Int = remember(syncedLyrics) {
        {
            syncedLyrics.positionForStableLineFocus(currentPosition())
        }
    }

    val listState = rememberLazyListState()

    val nestedScrollConnection = remember {
        var lastUserScrollEventMs = 0L
        object : NestedScrollConnection {
            private fun markManualScroll() {
                val now = System.currentTimeMillis()
                if (now - lastUserScrollEventMs >= MANUAL_SCROLL_DEBOUNCE_MS) {
                    isManualScrolling = true
                    lastManualScrollTime = now
                    lastUserScrollEventMs = now
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isSelectionModeActive && source == NestedScrollSource.UserInput) {
                    markManualScroll()
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!isSelectionModeActive) {
                    isManualScrolling = true
                    lastManualScrollTime = System.currentTimeMillis()
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isManualScrolling, lastManualScrollTime) {
        if (isManualScrolling) {
            delay(MANUAL_SCROLL_TIMEOUT_MS)
            isManualScrolling = false
        }
    }

    LaunchedEffect(syncedLyrics, isSynced) {
        if (!isSynced || syncedLyrics.lines.isEmpty()) return@LaunchedEffect
        var isFirstEmit = true
        snapshotFlow {
            syncedLyrics.getCurrentFirstHighlightLineIndexByTime(focusedPosition())
        }
            .distinctUntilChanged()
            .collectLatest { index ->
                if (index !in syncedLyrics.lines.indices) return@collectLatest
                if (isSelectionModeActive || isManualScrolling) return@collectLatest
                if (isFirstEmit) {
                    isFirstEmit = false
                    snapshotFlow { listState.layoutInfo.viewportEndOffset > 0 }.first { it }
                    val viewportHeight = listState.layoutInfo.let { it.viewportEndOffset - it.viewportStartOffset }
                    listState.scrollToItem(index, -(viewportHeight * 0.42f).roundToInt())
                } else {
                    if (latestSliderPositionProvider.value() == null) {
                        delay(ACTIVE_LINE_REVEAL_DELAY_MS)
                    }
                    if (!isSelectionModeActive && !isManualScrolling) {
                        listState.keepLyricLineVisible(index)
                    }
                }
            }
    }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedLineStarts.clear()
    }

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT,
            ).show()
            showMaxSelectionToast = false
        }
    }

    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val normalTextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = lyricsTextSize.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
    )
    val accompanimentTextStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = (lyricsTextSize * 0.82f).sp,
        fontFamily = lyricsFontFamily ?: MaterialTheme.typography.titleLarge.fontFamily,
    )
    val phoneticTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = (lyricsTextSize * 0.55f).sp,
        fontWeight = FontWeight.Normal,
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
    ) {
        when {
            lyrics == LYRICS_NOT_FOUND -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            lyrics == null -> {
                ShimmerHost {
                    repeat(6) { TextPlaceholder() }
                }
            }
            syncedLyrics.lines.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                ) {
                    val lyricsViewportOffset = remember(maxHeight) { maxHeight * 0.38f }

                    KaraokeLyricsView(
                        listState = listState,
                        lyrics = syncedLyrics,
                        currentPosition = focusedPosition,
                        onLineClicked = { line ->
                            if (isSelectionModeActive) {
                                val start = line.start
                                if (selectedLineStarts.contains(start)) {
                                    selectedLineStarts.remove(start)
                                    if (selectedLineStarts.isEmpty()) isSelectionModeActive = false
                                } else if (selectedLineStarts.size < maxSelectionLimit) {
                                    selectedLineStarts.add(start)
                                } else {
                                    showMaxSelectionToast = true
                                }
                            } else if (lyricsClick && isSynced && line.start > 0) {
                                player.seekTo(line.start.toLong())
                            }
                        },
                        onLinePressed = { line ->
                            if (!isSelectionModeActive) {
                                isSelectionModeActive = true
                                if (!selectedLineStarts.contains(line.start)) {
                                    selectedLineStarts.add(line.start)
                                }
                            } else if (!selectedLineStarts.contains(line.start)) {
                                if (selectedLineStarts.size < maxSelectionLimit) {
                                    selectedLineStarts.add(line.start)
                                } else {
                                    showMaxSelectionToast = true
                                }
                            }
                        },
                        textColor = textColor,
                        normalLineTextStyle = normalTextStyle,
                        accompanimentLineTextStyle = accompanimentTextStyle,
                        phoneticTextStyle = phoneticTextStyle,
                        blendMode = BlendMode.SrcOver,
                        useBlurEffect = lyricsLineBlur,
                        showTranslation = true,
                        showPhonetic = romanizationPreferences.isEnabled,
                        offset = lyricsViewportOffset,
                        keepAliveZone = 72.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (isManualScrolling && isSynced) {
                    FilledTonalButton(
                        onClick = {
                            isManualScrolling = false
                            scope.launch {
                                val firstIndex = syncedLyrics.getCurrentFirstHighlightLineIndexByTime(
                                    focusedPosition()
                                )
                                if (firstIndex in syncedLyrics.lines.indices) {
                                    listState.keepLyricLineVisible(firstIndex)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(
                            text = stringResource(R.string.resume_autoscroll),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                if (isSelectionModeActive) {
                    mediaMetadata?.let { metadata ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = Color.Black.copy(alpha = 0.3f),
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            isSelectionModeActive = false
                                            selectedLineStarts.clear()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.close),
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .background(
                                            color = if (selectedLineStarts.isNotEmpty())
                                                Color.White.copy(alpha = 0.9f)
                                            else
                                                Color.White.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(24.dp),
                                        )
                                        .clickable(enabled = selectedLineStarts.isNotEmpty()) {
                                            val sortedStarts = selectedLineStarts.sorted()
                                            val selectedLyricsText = sortedStarts
                                                .mapNotNull { start ->
                                                    syncedLyrics.lines.find { it.start == start }?.lineText()
                                                }
                                                .joinToString("\n")
                                            if (selectedLyricsText.isNotBlank()) {
                                                shareDialogData = Triple(
                                                    selectedLyricsText,
                                                    metadata.title,
                                                    metadata.artists.joinToString { it.name },
                                                )
                                                showShareDialog = true
                                            }
                                            isSelectionModeActive = false
                                            selectedLineStarts.clear()
                                        }
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.share),
                                        contentDescription = stringResource(R.string.share_selected),
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.share),
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        BasicAlertDialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.85f),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.share_lyrics),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                shareLyricsAsText(
                                    context = context,
                                    payload = LyricsSharePayload(lyricsText, songTitle, artists),
                                    songId = mediaMetadata?.id,
                                )
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                shareDialogData = Triple(lyricsText, songTitle, artists)
                                showShareImageDialog = true
                                showShareDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.share),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
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
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }

    if (showShareImageDialog && shareDialogData != null) {
        val (lyricsText, songTitle, artists) = shareDialogData!!
        LyricsShareImageDialog(
            mediaMetadata = mediaMetadata,
            payload = LyricsSharePayload(lyricsText, songTitle, artists),
            onDismissRequest = { showShareImageDialog = false },
        )
    }
}

private fun ISyncedLine.lineText(): String = when (this) {
    is KaraokeLine -> syllables.joinToString("") { it.content }
    is SyncedLine -> content
    else -> ""
}

private suspend fun LazyListState.keepLyricLineVisible(index: Int) {
    val info = layoutInfo
    val viewportHeight = info.viewportEndOffset - info.viewportStartOffset
    if (viewportHeight <= 0) return

    val preferredCenter = info.viewportStartOffset + (viewportHeight * 0.46f).roundToInt()
    val safeStart = info.viewportStartOffset + (viewportHeight * 0.16f).roundToInt()
    val safeEnd = info.viewportEndOffset - (viewportHeight * 0.24f).roundToInt()
    val targetItem = info.visibleItemsInfo.firstOrNull { it.index == index }

    if (targetItem == null) {
        val firstVisibleIndex = info.visibleItemsInfo.firstOrNull()?.index ?: firstVisibleItemIndex
        val scrollOffset = -(viewportHeight * 0.42f).roundToInt()
        if (abs(index - firstVisibleIndex) > ACTIVE_LINE_FAR_SCROLL_THRESHOLD) {
            scrollToItem(index, scrollOffset)
        } else {
            animateScrollToItem(index, scrollOffset)
        }
        return
    }

    val itemStart = targetItem.offset
    val itemEnd = targetItem.offset + targetItem.size
    if (itemStart >= safeStart && itemEnd <= safeEnd) return

    val itemCenter = itemStart + targetItem.size / 2
    animateScrollBy(
        value = (itemCenter - preferredCenter).toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )
}

private fun SyncedLyrics.positionForStableLineFocus(time: Int): Int {
    if (lines.isEmpty()) return time
    val index = findLastStartedLineIndex(time)
    if (index < 0) return time

    val line = lines[index]
    if (time < line.end) return time

    return (line.end - 1).coerceAtLeast(line.start)
}

private fun SyncedLyrics.findLastStartedLineIndex(time: Int): Int {
    var low = 0
    var high = lines.lastIndex
    var result = -1

    while (low <= high) {
        val mid = low + (high - low) / 2
        if (lines[mid].start <= time) {
            result = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }

    return result
}

private fun buildSyncedLyrics(
    entries: List<LyricsEntry>,
    isTtml: Boolean,
    romanizationMap: Map<Int, List<String?>>,
): SyncedLyrics {
    if (entries.isEmpty()) return SyncedLyrics(emptyList())
    val lines = mutableListOf<ISyncedLine>()

    entries.forEachIndexed { index, entry ->
        if (entry.time < 0L) return@forEachIndexed
        if (entry.isInstrumental) return@forEachIndexed
        if (entry.text.isBlank() && entry.words.isNullOrEmpty()) return@forEachIndexed

        if (isTtml && entry.words != null) {
            val mainWords = entry.words!!.filter { !it.isBackground }
            val bgWords = entry.words!!.filter { it.isBackground }
            val alignment = when (entry.agent?.lowercase()) {
                "v2" -> KaraokeAlignment.End
                else -> KaraokeAlignment.Start
            }

            val wordsForMain = if (mainWords.isNotEmpty()) mainWords else entry.words!!
            val wordPhonetics = romanizationMap[index] ?: emptyList()
            val mainSyllables = wordsForMain.mapIndexed { wordIdx, word ->
                val start = (word.startTime * 1000).toInt()
                val end = (word.endTime * 1000).toInt().coerceAtLeast(start + 1)
                KaraokeSyllable(content = word.text, start = start, end = end, phonetic = wordPhonetics.getOrNull(wordIdx))
            }

            val lineStart = mainSyllables.first().start
            val lineEnd = mainSyllables.last().end
            if (lineEnd <= lineStart) return@forEachIndexed

            val accompanimentLines = if (mainWords.isNotEmpty() && bgWords.isNotEmpty()) {
                val bgSyllables = bgWords.map { word ->
                    val start = (word.startTime * 1000).toInt()
                    val end = (word.endTime * 1000).toInt().coerceAtLeast(start + 1)
                    KaraokeSyllable(content = word.text, start = start, end = end)
                }
                val bgStart = bgSyllables.first().start
                val bgEnd = bgSyllables.last().end
                if (bgEnd > bgStart) {
                    listOf(
                        KaraokeLine.AccompanimentKaraokeLine(
                            syllables = bgSyllables,
                            translation = null,
                            alignment = alignment,
                            start = bgStart,
                            end = bgEnd,
                            phonetic = null,
                        )
                    )
                } else {
                    null
                }
            } else {
                null
            }

            lines.add(
                KaraokeLine.MainKaraokeLine(
                    syllables = mainSyllables,
                    translation = null,
                    alignment = alignment,
                    start = lineStart,
                    end = lineEnd,
                    phonetic = null,
                    accompanimentLines = accompanimentLines,
                )
            )
        } else {
            val nextEntry = entries.getOrNull(index + 1)
            val lineEnd = if (nextEntry != null && nextEntry.time > entry.time) {
                val gap = nextEntry.time - entry.time
                if (gap > 3000L) {
                    minOf((nextEntry.time - 1L).toInt(), (entry.time + 4000L).toInt())
                        .coerceAtLeast(entry.time.toInt() + 1)
                } else {
                    (nextEntry.time - 1L).coerceAtLeast(entry.time + 1L).toInt()
                }
            } else {
                (entry.time + 4000L).toInt()
            }
            lines.add(
                SyncedLine(
                    content = entry.text,
                    translation = romanizationMap[index]?.firstOrNull(),
                    start = entry.time.toInt(),
                    end = lineEnd,
                )
            )
        }
    }

    return SyncedLyrics(lines = lines)
}

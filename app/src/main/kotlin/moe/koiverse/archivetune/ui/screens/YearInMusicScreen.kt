/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.screens

import android.content.Intent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.DisableBlurKey
import moe.koiverse.archivetune.db.entities.Album
import moe.koiverse.archivetune.db.entities.Artist
import moe.koiverse.archivetune.db.entities.SongWithStats
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.models.toMediaMetadata
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.menu.ArtistMenu
import moe.koiverse.archivetune.ui.menu.SongMenu
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.ComposeToImage
import moe.koiverse.archivetune.utils.joinByBullet
import moe.koiverse.archivetune.utils.makeTimeString
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.YearInMusicViewModel
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val NeonPink = Color(0xFFFF006E)
private val ElectricPurple = Color(0xFF8338EC)
private val VibrantBlue = Color(0xFF3A86FF)
private val NeonGreen = Color(0xFF06D6A0)
private val SunsetOrange = Color(0xFFFF6B35)
private val GoldenYellow = Color(0xFFFFBE0B)
private val DeepBlack = Color(0xFF0A0A0F)
private val RichBlack = Color(0xFF121218)
private val SoftWhite = Color(0xFFFAFAFA)
private val GlassWhite = Color(0x33FFFFFF)

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color,
    val velocity: Offset,
    val rotation: Float,
    val rotationSpeed: Float,
    val alpha: Float = 1f
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YearInMusicScreen(
    navController: NavController,
    viewModel: YearInMusicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val availableYears by viewModel.availableYears.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val topSongsStats by viewModel.topSongsStats.collectAsState()
    val topSongs by viewModel.topSongs.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val topAlbums by viewModel.topAlbums.collectAsState()
    val totalListeningTime by viewModel.totalListeningTime.collectAsState()
    val totalSongsPlayed by viewModel.totalSongsPlayed.collectAsState()

    var isGeneratingImage by remember { mutableStateOf(false) }
    var isShareCaptureMode by remember { mutableStateOf(false) }
    var shareBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isYearPickerOpen by remember { mutableStateOf(false) }
    var recapCurrentPage by remember { mutableIntStateOf(0) }
    var recapLastPage by remember { mutableIntStateOf(0) }

    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val shareBackgroundArgb = DeepBlack.toArgb()
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .onGloballyPositioned { coordinates ->
                shareBounds = coordinates.boundsInRoot()
            }
    ) {
        if (!disableBlur) {
            PremiumAnimatedBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(-1f)
            )
        }

        if (!isShareCaptureMode && !disableBlur) {
            FloatingParticles(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )
        }

        YearInMusicStoryPager(
            year = selectedYear,
            totalListeningTime = totalListeningTime,
            totalSongsPlayed = totalSongsPlayed,
            topSongsStats = topSongsStats,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            isPlaying = isPlaying,
            mediaMetadataId = mediaMetadata?.id,
            navController = navController,
            menuState = menuState,
            haptic = haptic,
            playerConnection = playerConnection,
            coroutineScope = coroutineScope,
            isShareCaptureMode = isShareCaptureMode,
            onPagerStateChanged = { current, last ->
                recapCurrentPage = current
                recapLastPage = last
            },
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                )
        )

        if (topSongsStats.isNotEmpty() || topArtists.isNotEmpty() || topAlbums.isNotEmpty()) {
            if (!isShareCaptureMode && recapCurrentPage == recapLastPage) {
                PremiumShareButton(
                    isGenerating = isGeneratingImage,
                    onClick = {
                        if (!isGeneratingImage) {
                            isGeneratingImage = true
                            coroutineScope.launch {
                                try {
                                    isShareCaptureMode = true
                                    awaitNextPreDraw(view)
                                    awaitNextPreDraw(view)

                                    val raw = ComposeToImage.captureViewBitmap(
                                        view = view,
                                        backgroundColor = shareBackgroundArgb,
                                    )
                                    val bounds = shareBounds
                                    val cropped = if (bounds != null) {
                                        ComposeToImage.cropBitmap(
                                            source = raw,
                                            left = bounds.left.toInt(),
                                            top = bounds.top.toInt(),
                                            width = bounds.width.toInt(),
                                            height = bounds.height.toInt(),
                                        )
                                    } else {
                                        raw
                                    }
                                    val fitted = ComposeToImage.fitBitmap(
                                        source = cropped,
                                        targetWidth = 1080,
                                        targetHeight = 1920,
                                        backgroundColor = shareBackgroundArgb,
                                    )

                                    val uri = ComposeToImage.saveBitmapAsFile(
                                        context,
                                        fitted,
                                        "ArchiveTune_YearInMusic_$selectedYear"
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.share_summary)
                                        )
                                    )
                                } finally {
                                    isShareCaptureMode = false
                                    isGeneratingImage = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                        )
                )
            }
        }

        if (!isShareCaptureMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                    )
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PulsingDot()
                            Text(
                                text = stringResource(R.string.year_in_music),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SoftWhite
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                tint = SoftWhite
                            )
                        }
                    },
                    actions = {
                        PremiumYearChip(
                            year = selectedYear,
                            onClick = { isYearPickerOpen = true }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }

        if (!isShareCaptureMode && isYearPickerOpen) {
            PremiumYearPickerDialog(
                availableYears = availableYears,
                selectedYear = selectedYear,
                onSelectYear = { year ->
                    viewModel.selectedYear.value = year
                    isYearPickerOpen = false
                },
                onDismiss = { isYearPickerOpen = false }
            )
        }
    }
}

private suspend fun awaitNextPreDraw(view: View) {
    suspendCancellableCoroutine { cont ->
        val vto = view.viewTreeObserver
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (vto.isAlive) vto.removeOnPreDrawListener(this)
                cont.resume(Unit)
                return true
            }
        }
        vto.addOnPreDrawListener(listener)
        cont.invokeOnCancellation {
            if (vto.isAlive) vto.removeOnPreDrawListener(listener)
        }
        view.invalidate()
    }
}

@Composable
private fun PremiumAnimatedBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bgTransition")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase3"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    DeepBlack,
                    Color(0xFF1A0A2E),
                    Color(0xFF0F0515),
                    DeepBlack
                )
            )
        )

        val center1 = Offset(
            x = w * (0.2f + 0.3f * sin(phase1 * 2 * PI.toFloat())),
            y = h * (0.15f + 0.15f * cos(phase1 * 2 * PI.toFloat()))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonPink.copy(alpha = 0.5f),
                    ElectricPurple.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = center1,
                radius = w * 0.8f
            )
        )

        val center2 = Offset(
            x = w * (0.8f - 0.25f * cos(phase2 * 2 * PI.toFloat())),
            y = h * (0.4f + 0.2f * sin(phase2 * 2 * PI.toFloat()))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VibrantBlue.copy(alpha = 0.45f),
                    ElectricPurple.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center2,
                radius = w * 0.9f
            )
        )

        val center3 = Offset(
            x = w * (0.5f + 0.2f * sin(phase3 * 2 * PI.toFloat())),
            y = h * (0.75f + 0.1f * cos(phase3 * 2 * PI.toFloat()))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonGreen.copy(alpha = 0.3f),
                    VibrantBlue.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = center3,
                radius = w * 0.65f
            )
        )

        val center4 = Offset(
            x = w * (0.9f - 0.15f * sin(phase1 * 2 * PI.toFloat())),
            y = h * (0.85f - 0.1f * cos(phase2 * 2 * PI.toFloat()))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SunsetOrange.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = center4,
                radius = w * 0.4f
            )
        )
    }
}

@Composable
private fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 25
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 1f,
                color = listOf(NeonPink, ElectricPurple, VibrantBlue, NeonGreen, GoldenYellow).random(),
                velocity = Offset(
                    (Random.nextFloat() - 0.5f) * 0.001f,
                    (Random.nextFloat() - 0.5f) * 0.001f
                ),
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 0.5f,
                alpha = Random.nextFloat() * 0.4f + 0.2f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val x = ((particle.x + particle.velocity.x * time + 1f) % 1f) * size.width
            val y = ((particle.y + particle.velocity.y * time + 1f) % 1f) * size.height
            val rotation = particle.rotation + particle.rotationSpeed * time

            rotate(rotation, pivot = Offset(x, y)) {
                drawCircle(
                    color = particle.color.copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingDot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .scale(scale)
            .alpha(alpha)
            .background(NeonPink, CircleShape)
    )
}

@Composable
private fun PremiumYearChip(
    year: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chipGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonPink.copy(alpha = glowAlpha * 0.5f),
                        ElectricPurple.copy(alpha = glowAlpha * 0.5f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NeonPink.copy(alpha = glowAlpha),
                        ElectricPurple.copy(alpha = glowAlpha)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SoftWhite
            )
            Icon(
                painter = painterResource(R.drawable.calendar_today),
                contentDescription = null,
                tint = SoftWhite.copy(alpha = 0.9f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PremiumShareButton(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shareBtn")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shareRotation"
    )

    Box(
        modifier = modifier
            .size(64.dp)
            .drawBehind {
                rotate(rotation) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                NeonPink,
                                ElectricPurple,
                                VibrantBlue,
                                NeonGreen,
                                GoldenYellow,
                                NeonPink
                            )
                        ),
                        radius = size.minDimension / 2 + 4.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            containerColor = RichBlack,
            contentColor = SoftWhite
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = NeonPink
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = stringResource(R.string.share_summary),
                    tint = SoftWhite
                )
            }
        }
    }
}

@Composable
private fun PremiumYearPickerDialog(
    availableYears: List<Int>,
    selectedYear: Int,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RichBlack,
        titleContentColor = SoftWhite,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NeonPink, CircleShape)
                )
                Text(
                    text = stringResource(R.string.year_in_music),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(availableYears) { year ->
                    val isSelected = year == selectedYear
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(NeonPink, ElectricPurple)
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(GlassWhite, GlassWhite.copy(alpha = 0.1f))
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = if (isSelected) Color.Transparent else GlassWhite,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectYear(year) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = year.toString(),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = SoftWhite,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dismiss),
                    color = NeonPink
                )
            }
        }
    )
}

@Composable
private fun PremiumStoryProgressIndicator(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val progress by animateFloatAsState(
                targetValue = when {
                    index < currentPage -> 1f
                    index == currentPage -> 1f
                    else -> 0f
                },
                animationSpec = tween(300),
                label = "progress"
            )
            val alpha by animateFloatAsState(
                targetValue = when {
                    index < currentPage -> 0.6f
                    index == currentPage -> 1f
                    else -> 0.2f
                },
                animationSpec = tween(300),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SoftWhite.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonPink, ElectricPurple)
                            )
                        )
                        .alpha(alpha)
                )
            }
        }
    }
}

@Composable
private fun PremiumStoryNavBar(
    canGoBack: Boolean,
    canGoNext: Boolean,
    pageLabel: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(RichBlack.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(GlassWhite, GlassWhite.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                onLongClick = {},
                enabled = canGoBack
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = if (canGoBack) SoftWhite else SoftWhite.copy(alpha = 0.3f)
                )
            }

            Text(
                text = pageLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = SoftWhite.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (canGoNext) {
                            Brush.linearGradient(colors = listOf(NeonPink, ElectricPurple))
                        } else {
                            Brush.linearGradient(colors = listOf(GlassWhite, GlassWhite))
                        }
                    )
                    .clickable(enabled = canGoNext, onClick = onNext)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftWhite
                    )
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = SoftWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun YearInMusicStoryPager(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSongsStats: List<SongWithStats>,
    topSongs: List<moe.koiverse.archivetune.db.entities.Song>,
    topArtists: List<Artist>,
    topAlbums: List<Album>,
    isPlaying: Boolean,
    mediaMetadataId: String?,
    navController: NavController,
    menuState: moe.koiverse.archivetune.ui.component.MenuState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    playerConnection: moe.koiverse.archivetune.playback.PlayerConnection,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    isShareCaptureMode: Boolean,
    onPagerStateChanged: (currentPage: Int, lastPage: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember(topSongsStats, topArtists, topAlbums) {
        buildList {
            add(YearInMusicStoryPage.Hero)
            if (topSongsStats.isNotEmpty()) add(YearInMusicStoryPage.TopSong)
            if (topArtists.isNotEmpty()) add(YearInMusicStoryPage.TopArtist)
            if (topAlbums.isNotEmpty()) add(YearInMusicStoryPage.TopAlbum)
            add(YearInMusicStoryPage.Summary)
        }
    }

    var currentPage by remember { mutableIntStateOf(0) }
    val lastPage = pages.lastIndex.coerceAtLeast(0)

    LaunchedEffect(lastPage) {
        currentPage = currentPage.coerceIn(0, lastPage)
    }

    LaunchedEffect(isShareCaptureMode, lastPage) {
        if (isShareCaptureMode) currentPage = lastPage
    }

    LaunchedEffect(currentPage, lastPage) {
        onPagerStateChanged(currentPage, lastPage)
    }

    fun navigateTo(page: Int) {
        currentPage = page.coerceIn(0, lastPage)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                slideInHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    initialOffsetX = { it * direction }
                ) + fadeIn(animationSpec = tween(200)) togetherWith
                    slideOutHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        targetOffsetX = { -it * direction }
                    ) + fadeOut(animationSpec = tween(150))
            },
            label = "yearInMusicPage"
        ) { pageIndex ->
            when (pages.getOrNull(pageIndex)) {
                YearInMusicStoryPage.Hero -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isShareCaptureMode && currentPage < lastPage) {
                                navigateTo(currentPage + 1)
                            }
                    ) {
                        PremiumHeroStoryCard(
                            year = year,
                            totalListeningTime = totalListeningTime,
                            totalSongsPlayed = totalSongsPlayed
                        )
                    }
                }

                YearInMusicStoryPage.TopSong -> {
                    val topSong = topSongsStats.firstOrNull()
                    val topSongEntity = topSongs.firstOrNull()
                    if (topSong != null) {
                        PremiumTopSongStoryCard(
                            song = topSong,
                            onClick = {
                                if (!isShareCaptureMode && currentPage < lastPage) navigateTo(currentPage + 1)
                            },
                            onLongClick = {
                                if (!isShareCaptureMode && topSongEntity != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = topSongEntity,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                YearInMusicStoryPage.TopArtist -> {
                    val topArtist = topArtists.firstOrNull()
                    if (topArtist != null) {
                        PremiumTopArtistStoryCard(
                            artist = topArtist,
                            onClick = { if (!isShareCaptureMode && currentPage < lastPage) navigateTo(currentPage + 1) },
                            onLongClick = {
                                if (!isShareCaptureMode) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        ArtistMenu(
                                            originalArtist = topArtist,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                YearInMusicStoryPage.TopAlbum -> {
                    val topAlbum = topAlbums.firstOrNull()
                    if (topAlbum != null) {
                        PremiumTopAlbumStoryCard(
                            album = topAlbum,
                            onClick = { if (!isShareCaptureMode && currentPage < lastPage) navigateTo(currentPage + 1) }
                        )
                    }
                }

                YearInMusicStoryPage.Summary -> {
                    PremiumSummaryStoryCard(
                        year = year,
                        totalListeningTime = totalListeningTime,
                        totalSongsPlayed = totalSongsPlayed,
                        topSong = topSongsStats.firstOrNull(),
                        topArtist = topArtists.firstOrNull(),
                        topAlbum = topAlbums.firstOrNull()
                    )
                }

                null -> Unit
            }
        }

        if (!isShareCaptureMode) {
            PremiumStoryProgressIndicator(
                totalPages = pages.size,
                currentPage = currentPage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                    .padding(top = 56.dp)
            )

            PremiumStoryNavBar(
                canGoBack = currentPage > 0,
                canGoNext = currentPage < lastPage,
                pageLabel = when (pages.getOrNull(currentPage)) {
                    YearInMusicStoryPage.Hero -> stringResource(R.string.year_in_music)
                    YearInMusicStoryPage.TopSong -> stringResource(R.string.top_songs)
                    YearInMusicStoryPage.TopArtist -> stringResource(R.string.top_artists)
                    YearInMusicStoryPage.TopAlbum -> stringResource(R.string.albums)
                    YearInMusicStoryPage.Summary -> stringResource(R.string.share_summary)
                    null -> ""
                },
                onBack = { navigateTo(currentPage - 1) },
                onNext = { navigateTo(currentPage + 1) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PremiumHeroStoryCard(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroGlow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowPhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val center = Offset(
                x = w * (0.3f + 0.4f * sin(glowPhase * 2 * PI.toFloat())),
                y = h * (0.3f + 0.2f * cos(glowPhase * 2 * PI.toFloat()))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPink.copy(alpha = 0.4f),
                        ElectricPurple.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.8f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.your_year_in_music, year),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = SoftWhite.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = year.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
                fontWeight = FontWeight.Black,
                color = SoftWhite
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassWhite)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(NeonPink, ElectricPurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.timer),
                                contentDescription = null,
                                tint = SoftWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.total_listening_time),
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftWhite.copy(alpha = 0.7f)
                            )
                            Text(
                                text = makeTimeString(totalListeningTime),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftWhite
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(VibrantBlue, NeonGreen)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = SoftWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.songs),
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftWhite.copy(alpha = 0.7f)
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.n_song,
                                    totalSongsPlayed.toInt(),
                                    totalSongsPlayed.toInt()
                                ) + " " + stringResource(R.string.played),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tap to continue →",
                style = MaterialTheme.typography.bodyMedium,
                color = SoftWhite.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun rememberShareSafeImageRequest(data: Any?): Any? {
    val context = LocalContext.current
    return remember(data, context) {
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .allowHardware(false)
                .build()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PremiumTopSongStoryCard(
    song: SongWithStats,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val imageModel = rememberShareSafeImageRequest(song.thumbnailUrl)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepBlack.copy(alpha = 0.3f),
                            DeepBlack.copy(alpha = 0.6f),
                            DeepBlack.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NeonPink, ElectricPurple)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "#1 " + stringResource(R.string.top_songs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
            }

            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(NeonPink, ElectricPurple)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SoftWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip(
                    icon = R.drawable.play,
                    value = pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                    color = NeonPink
                )
                StatChip(
                    icon = R.drawable.timer,
                    value = makeTimeString(song.timeListened),
                    color = VibrantBlue
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PremiumTopArtistStoryCard(
    artist: Artist,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val imageModel = rememberShareSafeImageRequest(artist.artist.thumbnailUrl)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.4f),
                            DeepBlack.copy(alpha = 0.85f),
                            DeepBlack.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(ElectricPurple, VibrantBlue)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "#1 " + stringResource(R.string.top_artists),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind {
                        rotate(0f) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        ElectricPurple,
                                        VibrantBlue,
                                        NeonGreen,
                                        ElectricPurple
                                    )
                                ),
                                radius = size.minDimension / 2 + 6.dp.toPx(),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    }
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = artist.artist.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SoftWhite,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip(
                    icon = R.drawable.play,
                    value = pluralStringResource(R.plurals.n_time, artist.songCount, artist.songCount),
                    color = ElectricPurple
                )
                StatChip(
                    icon = R.drawable.timer,
                    value = makeTimeString(artist.timeListened?.toLong()),
                    color = NeonGreen
                )
            }
        }
    }
}

@Composable
private fun PremiumTopAlbumStoryCard(
    album: Album,
    onClick: () -> Unit
) {
    val artistNames = album.artists.take(2).joinToString(" • ") { it.name }
    val imageModel = rememberShareSafeImageRequest(album.thumbnailUrl)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepBlack.copy(alpha = 0.2f),
                            DeepBlack.copy(alpha = 0.7f),
                            DeepBlack.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(SunsetOrange, GoldenYellow)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "#1 " + stringResource(R.string.albums),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlack
                )
            }

            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(SunsetOrange, GoldenYellow)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Text(
                text = album.album.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SoftWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (artistNames.isNotBlank()) {
                Text(
                    text = artistNames,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftWhite.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                album.songCountListened?.let {
                    StatChip(
                        icon = R.drawable.play,
                        value = pluralStringResource(R.plurals.n_time, it, it),
                        color = SunsetOrange
                    )
                }
                StatChip(
                    icon = R.drawable.timer,
                    value = makeTimeString(album.timeListened),
                    color = GoldenYellow
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun PremiumSummaryStoryCard(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSong: SongWithStats?,
    topArtist: Artist?,
    topAlbum: Album?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ElectricPurple.copy(alpha = 0.3f),
                        NeonPink.copy(alpha = 0.2f),
                        DeepBlack
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlassWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_icon_small),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ArchiveTune",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftWhite
                        )
                        Text(
                            text = joinByBullet(stringResource(R.string.year_in_music), year.toString()),
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.share_summary),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = SoftWhite
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassWhite)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.total_listening_time),
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftWhite.copy(alpha = 0.7f)
                            )
                            Text(
                                text = makeTimeString(totalListeningTime),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SoftWhite
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassWhite)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.songs),
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftWhite.copy(alpha = 0.7f)
                            )
                            Text(
                                text = totalSongsPlayed.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SoftWhite
                            )
                            Text(
                                text = stringResource(R.string.played),
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val hasAnyHighlight = topSong != null || topArtist != null || topAlbum != null

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(RichBlack.copy(alpha = 0.8f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(GlassWhite, GlassWhite.copy(alpha = 0.1f))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = stringResource(R.string.year_in_music),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftWhite
                        )

                        if (!hasAnyHighlight) {
                            Text(
                                text = stringResource(R.string.no_listening_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftWhite.copy(alpha = 0.6f)
                            )
                        } else {
                            topSong?.let {
                                SummaryHighlightRow(
                                    icon = R.drawable.ic_music,
                                    label = stringResource(R.string.top_songs),
                                    value = it.title,
                                    accentColor = NeonPink
                                )
                            }
                            topArtist?.let {
                                SummaryHighlightRow(
                                    icon = R.drawable.artist,
                                    label = stringResource(R.string.top_artists),
                                    value = it.artist.name,
                                    accentColor = ElectricPurple
                                )
                            }
                            topAlbum?.let {
                                SummaryHighlightRow(
                                    icon = R.drawable.album,
                                    label = stringResource(R.string.albums),
                                    value = it.album.title,
                                    accentColor = SunsetOrange
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = joinByBullet("ArchiveTune", year.toString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftWhite.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.share_summary),
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftWhite.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: Int,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = SoftWhite
            )
        }
    }
}

@Composable
private fun SummaryHighlightRow(
    icon: Int,
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SoftWhite.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = SoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private enum class YearInMusicStoryPage {
    Hero,
    TopSong,
    TopArtist,
    TopAlbum,
    Summary
}

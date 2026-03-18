package moe.koiverse.archivetune.ui.screens.musicrecognition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.shazamkit.Shazam
import moe.koiverse.archivetune.shazamkit.ShazamSignatureGenerator
import moe.koiverse.archivetune.shazamkit.models.RecognitionResult

const val MusicRecognitionRoute = "music_recognition"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicRecognitionScreen(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<MusicRecognitionState>(MusicRecognitionState.Ready) }
    var recognitionJob by remember { mutableStateOf<Job?>(null) }

    val strings =
        remember {
            MusicRecognitionStrings(
                signatureFailed = context.getString(R.string.music_recognition_signature_failed),
                noMatchFallback = context.getString(R.string.music_recognition_no_match),
                recognitionFailedFallback = context.getString(R.string.music_recognition_recognition_failed),
            )
        }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                recognitionJob?.cancel()
                recognitionJob =
                    scope.launch {
                        runRecognitionFlow(
                            strings = strings,
                            onState = { state = it },
                            onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onSearch = { query -> navController.navigate("search/${Uri.encode(query)}") },
                        )
                    }
            } else {
                state = MusicRecognitionState.PermissionRequired
            }
        }

    fun startOrRequestPermission() {
        val permission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (permission) {
            recognitionJob?.cancel()
            recognitionJob =
                scope.launch {
                    runRecognitionFlow(
                        strings = strings,
                        onState = { state = it },
                        onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onSearch = { query -> navController.navigate("search/${Uri.encode(query)}") },
                    )
                }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { recognitionJob?.cancel() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.music_recognition)) },
                navigationIcon = {
                    Surface(
                        modifier =
                            Modifier
                                .padding(horizontal = 8.dp)
                                .size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = navController::navigateUp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = stringResource(R.string.back_button_desc),
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val gradient =
            remember(primary, tertiary) {
                listOf(
                    primary.copy(alpha = 0.35f),
                    tertiary.copy(alpha = 0.18f),
                    Color.Transparent,
                )
            }

        val infinite = rememberInfiniteTransition(label = "")
        val drift by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
            label = "",
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = gradient,
                            center = Offset(0.5f + 0.15f * drift, 0.25f + 0.12f * (1f - drift)),
                            radius = 1100f,
                        ),
                    )
                    .padding(padding)
                    .statusBarsPadding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f))
                            .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 1.02f))
                    },
                    label = "",
                ) { target ->
                    when (target) {
                        is MusicRecognitionState.Success -> {
                            ResultHeader(
                                title = target.result.title,
                                subtitle = target.result.artist,
                                metadata = buildMetadata(target.result),
                            )
                        }
                        else -> {
                            IdleHeader()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                val isListening = state is MusicRecognitionState.Listening
                val isProcessing = state is MusicRecognitionState.Processing
                val isBusy = isListening || isProcessing

                ListeningOrb(
                    modifier = Modifier.size(260.dp),
                    isActive = isListening,
                    isProcessing = isProcessing,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        startOrRequestPermission()
                    },
                )

                Spacer(modifier = Modifier.height(26.dp))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(120))) },
                    label = "",
                ) { target ->
                    when (target) {
                        MusicRecognitionState.Ready -> {
                            StatusPill(
                                label = stringResource(R.string.music_recognition_tap_to_listen),
                                iconRes = R.drawable.mic,
                            )
                        }
                        MusicRecognitionState.Listening -> {
                            StatusPill(
                                label = stringResource(R.string.music_recognition_listening),
                                iconRes = R.drawable.listening,
                            )
                        }
                        MusicRecognitionState.Processing -> {
                            StatusPill(
                                label = stringResource(R.string.music_recognition_processing),
                                iconRes = R.drawable.cached,
                            )
                        }
                        MusicRecognitionState.PermissionRequired -> {
                            PermissionCard(onAllow = { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) })
                        }
                        is MusicRecognitionState.NoMatch -> {
                            FailureCard(
                                title = stringResource(R.string.music_recognition_no_match),
                                message = target.message,
                                actionLabel = stringResource(R.string.music_recognition_listen_again),
                                onAction = { startOrRequestPermission() },
                            )
                        }
                        is MusicRecognitionState.Error -> {
                            FailureCard(
                                title = stringResource(R.string.music_recognition_error),
                                message = target.message,
                                actionLabel = stringResource(R.string.music_recognition_listen_again),
                                onAction = { startOrRequestPermission() },
                            )
                        }
                        is MusicRecognitionState.Success -> {
                            SuccessActions(
                                result = target.result,
                                onSearch = {
                                    val query = "${target.result.title} ${target.result.artist}".trim()
                                    navController.navigate("search/${Uri.encode(query)}")
                                },
                                onListenAgain = { startOrRequestPermission() },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = state is MusicRecognitionState.Success,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(160)),
                ) {
                    val result = (state as? MusicRecognitionState.Success)?.result
                    if (result != null) {
                        ResultCard(result = result)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private sealed interface MusicRecognitionState {
    data object Ready : MusicRecognitionState
    data object Listening : MusicRecognitionState
    data object Processing : MusicRecognitionState
    data object PermissionRequired : MusicRecognitionState
    data class Success(val result: RecognitionResult) : MusicRecognitionState
    data class NoMatch(val message: String) : MusicRecognitionState
    data class Error(val message: String) : MusicRecognitionState
}

private data class MusicRecognitionStrings(
    val signatureFailed: String,
    val noMatchFallback: String,
    val recognitionFailedFallback: String,
)

private suspend fun runRecognitionFlow(
    strings: MusicRecognitionStrings,
    onState: (MusicRecognitionState) -> Unit,
    onHaptic: () -> Unit,
    onSearch: (String) -> Unit,
) {
    onHaptic()
    onState(MusicRecognitionState.Listening)

    val samples =
        withContext(Dispatchers.IO) {
            recordMicPcm16Mono(
                sampleRateHz = 16000,
                recordMs = 4200L,
            ).first
        }

    onState(MusicRecognitionState.Processing)

    val signature =
        withContext(Dispatchers.Default) {
            ShazamSignatureGenerator().apply {
                feedPcm16Mono(samples)
            }.nextSignatureOrNull()
        }

    if (signature == null) {
        onState(MusicRecognitionState.Error(strings.signatureFailed))
        return
    }

    val result =
        withContext(Dispatchers.IO) {
            Shazam.recognize(signature.uri, signature.sampleDurationMs)
        }

    result.fold(
        onSuccess = { onState(MusicRecognitionState.Success(it)) },
        onFailure = { e ->
            val msg = e.message?.trim().orEmpty()
            when {
                msg.contains("no match", ignoreCase = true) || msg.contains("404") -> {
                    onState(MusicRecognitionState.NoMatch(msg.ifEmpty { strings.noMatchFallback }))
                }
                else -> onState(MusicRecognitionState.Error(msg.ifEmpty { strings.recognitionFailedFallback }))
            }
        },
    )
}

private fun buildMetadata(result: RecognitionResult): String {
    val pieces =
        listOfNotNull(
            result.album?.takeIf { it.isNotBlank() },
            result.genre?.takeIf { it.isNotBlank() },
            result.releaseDate?.takeIf { it.isNotBlank() },
        )
    return pieces.joinToString(" • ")
}

private suspend fun recordMicPcm16Mono(
    sampleRateHz: Int,
    recordMs: Long,
): Pair<ShortArray, Int> = withContext(Dispatchers.IO) {
    val channel = AudioFormat.CHANNEL_IN_MONO
    val encoding = AudioFormat.ENCODING_PCM_16BIT
    val minBuffer = AudioRecord.getMinBufferSize(sampleRateHz, channel, encoding).coerceAtLeast(4096)
    val record =
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateHz,
            channel,
            encoding,
            minBuffer,
        )

    val totalSamples = ((recordMs / 1000.0) * sampleRateHz).toInt().coerceAtLeast(sampleRateHz)
    val output = ShortArray(totalSamples)
    val buffer = ShortArray(minBuffer / 2)

    try {
        record.startRecording()

        var written = 0
        while (written < output.size && isActive) {
            val read = record.read(buffer, 0, minOf(buffer.size, output.size - written))
            if (read > 0) {
                System.arraycopy(buffer, 0, output, written, read)
                written += read
            }
        }

        if (written <= 0) {
            ShortArray(0) to sampleRateHz
        } else {
            output.copyOf(written) to sampleRateHz
        }
    } finally {
        runCatching { record.stop() }
        runCatching { record.release() }
    }
}

@Composable
private fun IdleHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.music_recognition),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.music_recognition_tap_to_listen),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultHeader(
    title: String,
    subtitle: String,
    metadata: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (metadata.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            StatusPill(label = metadata, iconRes = R.drawable.info)
        }
    }
}

@Composable
private fun ListeningOrb(
    modifier: Modifier,
    isActive: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "")
    val ringProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1700, easing = LinearEasing)),
        label = "",
    )
    val ringProgress2 by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(animation = tween(2100, easing = LinearEasing)),
        label = "",
    )

    val orbScale by animateFloatAsState(
        targetValue = if (isActive) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "",
    )

    val baseColor =
        if (isProcessing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    val container =
        Brush.radialGradient(
            colors =
                listOf(
                    baseColor.copy(alpha = 0.42f),
                    baseColor.copy(alpha = 0.16f),
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                ),
        )

    Box(
        modifier =
            modifier
                .scale(orbScale)
                .clip(CircleShape)
                .background(container)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            if (isActive) {
                drawRing(center, r, ringProgress, baseColor)
                drawRing(center, r, ringProgress2, baseColor.copy(alpha = 0.85f))
            }

            val glow = baseColor.copy(alpha = if (isActive) 0.22f else 0.12f)
            drawCircle(glow, radius = r * 0.88f, center = center)
            drawCircle(Color.Black.copy(alpha = 0.06f), radius = r * 0.78f, center = center)
        }

        val icon =
            when {
                isProcessing -> R.drawable.cached
                isActive -> R.drawable.listening
                else -> R.drawable.mic
            }

        val iconAlpha by animateFloatAsState(
            targetValue = if (isProcessing) 0.9f else 1f,
            animationSpec = tween(180),
            label = "",
        )

        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(54.dp).alpha(iconAlpha),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRing(
    center: Offset,
    baseRadius: Float,
    progress: Float,
    color: Color,
) {
    val p = progress.coerceIn(0f, 1f)
    val radius = baseRadius * (0.62f + 0.55f * p)
    val alpha = (1f - p) * 0.55f
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
    )
}

@Composable
private fun StatusPill(
    label: String,
    iconRes: Int,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    onAllow: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.music_recognition_permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.music_recognition_permission_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAllow,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.music_recognition_permission_action))
            }
        }
    }
}

@Composable
private fun FailureCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SuccessActions(
    result: RecognitionResult,
    onSearch: () -> Unit,
    onListenAgain: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = onListenAgain,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.replay),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.music_recognition_listen_again),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }

            Button(
                onClick = onSearch,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.search),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }

        if (!result.shazamUrl.isNullOrBlank()) {
            FilledTonalButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(result.shazamUrl)),
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.music_recognition_open_shazam),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: RecognitionResult,
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            val cover = result.coverArtHqUrl ?: result.coverArtUrl
            val title = result.title
            val artist = result.artist
            val meta = buildMetadata(result)

            val primary = MaterialTheme.colorScheme.primary
            val tertiary = MaterialTheme.colorScheme.tertiary
            val heroColors =
                remember(primary, tertiary) {
                    listOf(
                        primary.copy(alpha = 0.28f),
                        tertiary.copy(alpha = 0.18f),
                        Color.Transparent,
                    )
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = heroColors,
                            ),
                        )
                        .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoverArt(
                        coverUrl = cover,
                        modifier = Modifier.size(96.dp),
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        FlowChips(
                            album = result.album,
                            genre = result.genre,
                            releaseDate = result.releaseDate,
                            isrc = result.isrc,
                        )
                    }

                    if (!result.shazamUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(result.shazamUrl)),
                                )
                            },
                            modifier = Modifier.heightIn(min = 40.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.link),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

            Column(modifier = Modifier.padding(16.dp)) {
                val lyrics = result.lyrics?.takeIf { it.isNotEmpty() }?.take(6)?.joinToString("\n")
                if (!lyrics.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.lyrics),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.music_recognition_lyrics_preview),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Text(
                            text = lyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                val label = result.label?.takeIf { it.isNotBlank() }
                if (label != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverArt(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model =
                    ImageRequest.Builder(context)
                        .data(coverUrl)
                        .allowHardware(false)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FlowChips(
    album: String?,
    genre: String?,
    releaseDate: String?,
    isrc: String?,
) {
    val items =
        remember(album, genre, releaseDate, isrc) {
            buildList {
                album?.takeIf { it.isNotBlank() }?.let { add(ChipData(R.drawable.album, it)) }
                genre?.takeIf { it.isNotBlank() }?.let { add(ChipData(R.drawable.info, it)) }
                releaseDate?.takeIf { it.isNotBlank() }?.let { add(ChipData(R.drawable.calendar_today, it)) }
                isrc?.takeIf { it.isNotBlank() }?.let { add(ChipData(R.drawable.link, it)) }
            }
        }

    if (items.isEmpty()) return

    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.take(2).forEach { chip ->
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = chip.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(chip.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = containerColor,
                        labelColor = labelColor,
                        leadingIconContentColor = labelColor,
                    ),
                border = null,
            )
        }
    }

    if (items.size > 2) {
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.drop(2).forEach { chip ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = containerColor,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(chip.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = labelColor,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = chip.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = labelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private data class ChipData(
    val iconRes: Int,
    val label: String,
)

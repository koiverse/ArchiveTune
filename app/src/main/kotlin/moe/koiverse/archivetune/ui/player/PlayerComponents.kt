/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import me.saket.squiggles.SquigglySlider
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.PlayerBackgroundStyle
import moe.koiverse.archivetune.constants.PlayerButtonsStyle
import moe.koiverse.archivetune.constants.PlayerDesignStyle
import moe.koiverse.archivetune.constants.PlayerHorizontalPadding
import moe.koiverse.archivetune.constants.SliderStyle
import moe.koiverse.archivetune.extensions.togglePlayPause
import moe.koiverse.archivetune.extensions.toggleRepeatMode
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.playback.PlayerConnection
import moe.koiverse.archivetune.ui.component.BottomSheetPageState
import moe.koiverse.archivetune.ui.component.BottomSheetState
import moe.koiverse.archivetune.ui.component.MenuState
import moe.koiverse.archivetune.ui.component.PlayerSliderTrack
import moe.koiverse.archivetune.ui.component.ResizableIconButton
import moe.koiverse.archivetune.ui.menu.PlayerMenu
import moe.koiverse.archivetune.ui.theme.PlayerBackgroundColorUtils
import moe.koiverse.archivetune.ui.theme.PlayerSliderColors
import moe.koiverse.archivetune.ui.utils.ShowMediaInfo
import moe.koiverse.archivetune.utils.makeTimeString

@Composable
fun PlayerTitleSection(
    mediaMetadata: MediaMetadata,
    textBackgroundColor: Color,
    navController: NavController,
    state: BottomSheetState,
    clipboardManager: ClipboardManager,
    context: Context
) {
    AnimatedContent(
        targetState = mediaMetadata.title,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "",
    ) { title ->
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textBackgroundColor,
            modifier =
            Modifier
                .basicMarquee()
                .combinedClickable(
                    enabled = true,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        if (mediaMetadata.album != null) {
                            navController.navigate("album/${mediaMetadata.album.id}")
                            state.collapseSoft()
                        }
                    },
                    onLongClick = {
                        val clip = ClipData.newPlainText("Copied Title", title)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Title", Toast.LENGTH_SHORT).show()
                    }
                ),
        )
    }

    Spacer(Modifier.height(6.dp))

    val annotatedString = buildAnnotatedString {
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val tag = "artist_${artist.id.orEmpty()}"
            pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
            withStyle(SpanStyle(color = textBackgroundColor, fontSize = 16.sp)) {
                append(artist.name)
            }
            pop()
            if (index != mediaMetadata.artists.lastIndex) append(", ")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee()
            .padding(end = 12.dp)
    ) {
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        var clickOffset by remember { mutableStateOf<Offset?>(null) }
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.titleMedium.copy(color = textBackgroundColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val tapPosition = event.changes.firstOrNull()?.position
                            if (tapPosition != null) {
                                clickOffset = tapPosition
                            }
                        }
                    }
                }
                .combinedClickable(
                    enabled = true,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        val tapPosition = clickOffset
                        val layout = layoutResult
                        if (tapPosition != null && layout != null) {
                            val offset = layout.getOffsetForPosition(tapPosition)
                            annotatedString.getStringAnnotations(offset, offset)
                                .firstOrNull()
                                ?.let { ann ->
                                    val artistId = ann.item
                                    if (artistId.isNotBlank()) {
                                        navController.navigate("artist/$artistId")
                                        state.collapseSoft()
                                    }
                                }
                        }
                    },
                    onLongClick = {
                        val clip = ClipData.newPlainText("Copied Artist", annotatedString)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Artist", Toast.LENGTH_SHORT).show()
                    }
                )
        )
    }
}

@Composable
fun PlayerTopActions(
    mediaMetadata: MediaMetadata,
    playerDesignStyle: PlayerDesignStyle,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    state: BottomSheetState,
    bottomSheetPageState: BottomSheetPageState,
    context: Context,
    currentSongLiked: Boolean
) {
    when (playerDesignStyle) {
        PlayerDesignStyle.V2 -> {
            val shareShape = RoundedCornerShape(
                topStart = 50.dp, bottomStart = 50.dp,
                topEnd = 10.dp, bottomEnd = 10.dp
            )

            val favShape = RoundedCornerShape(
                topStart = 10.dp, bottomStart = 10.dp,
                topEnd = 50.dp, bottomEnd = 50.dp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(shareShape)
                        .background(textButtonColor)
                        .clickable {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                ) {
                    Image(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(favShape)
                        .background(textButtonColor)
                        .clickable {
                            playerConnection.toggleLike()
                        }
                ) {
                    Image(
                        painter = painterResource(
                            if (currentSongLiked)
                                R.drawable.favorite
                            else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
            }
        }

        PlayerDesignStyle.V3, PlayerDesignStyle.V5 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { playerConnection.toggleLike() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSongLiked) R.drawable.favorite
                            else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (currentSongLiked)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        else textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        PlayerDesignStyle.V4 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    onClick = { playerConnection.toggleLike() },
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentSongLiked)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                    else textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(
                                if (currentSongLiked) R.drawable.favorite
                                else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            tint = if (currentSongLiked)
                                MaterialTheme.colorScheme.error
                            else textBackgroundColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // More menu button - cinematic glass card
                Surface(
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = {
                                    mediaMetadata.id.let {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(it)
                                        }
                                    }
                                },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        PlayerDesignStyle.V1 -> {
            Box(
                modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(textButtonColor)
                    .clickable {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
            ) {
                Image(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconButtonColor),
                    modifier =
                    Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(textButtonColor)
                    .clickable {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = {
                                    mediaMetadata.id.let {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(it)
                                        }
                                    }
                                },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
            ) {
                Image(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconButtonColor),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    sliderStyle: SliderStyle,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    textButtonColor: Color,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    when (sliderStyle) {
        SliderStyle.DEFAULT -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onValueChange(it.toLong()) },
                onValueChangeFinished = onValueChangeFinished,
                colors = PlayerSliderColors.defaultSliderColors(textButtonColor),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
            )
        }

        SliderStyle.SQUIGGLY -> {
            SquigglySlider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onValueChange(it.toLong()) },
                onValueChangeFinished = onValueChangeFinished,
                colors = PlayerSliderColors.squigglySliderColors(textButtonColor),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                squigglesSpec =
                SquigglySlider.SquigglesSpec(
                    amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                    strokeWidth = 3.dp,
                ),
            )
        }

        SliderStyle.SLIM -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onValueChange(it.toLong()) },
                onValueChangeFinished = onValueChangeFinished,
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = PlayerSliderColors.slimSliderColors(textButtonColor)
                    )
                },
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
            )
        }
    }
}

@Composable
fun PlayerTimeLabel(
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    textBackgroundColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding + 4.dp),
    ) {
        Text(
            text = makeTimeString(sliderPosition ?: position),
            style = MaterialTheme.typography.labelMedium,
            color = textBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
            style = MaterialTheme.typography.labelMedium,
            color = textBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PlayerPlaybackControls(
    playerDesignStyle: PlayerDesignStyle,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    playPauseRoundness: androidx.compose.ui.unit.Dp,
    playerConnection: PlayerConnection,
    currentSongLiked: Boolean
) {
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    when (playerDesignStyle) {
        PlayerDesignStyle.V2 -> {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val maxW = maxWidth
                val playButtonHeight = maxW / 6f
                val playButtonWidth = playButtonHeight * 1.6f
                val sideButtonHeight = playButtonHeight * 0.8f
                val sideButtonWidth = sideButtonHeight * 1.3f

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    FilledTonalIconButton(
                        onClick = playerConnection::seekToPrevious,
                        enabled = canSkipPrevious,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FilledIconButton(
                        onClick = {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier
                            .size(width = playButtonWidth, height = playButtonHeight)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(42.dp),
                                color = iconButtonColor,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FilledTonalIconButton(
                        onClick = playerConnection::seekToNext,
                        enabled = canSkipNext,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        PlayerDesignStyle.V3 -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (shuffleModeEnabled) 1f else 0.4f
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(textBackgroundColor.copy(alpha = 0.08f))
                            .clickable(enabled = canSkipPrevious) {
                                playerConnection.seekToPrevious()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 0.9f else 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(50))
                            .background(textBackgroundColor)
                            .clickable {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = icBackgroundColor,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                tint = icBackgroundColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(textBackgroundColor.copy(alpha = 0.08f))
                            .clickable(enabled = canSkipNext) {
                                playerConnection.seekToNext()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(alpha = if (canSkipNext) 0.9f else 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { playerConnection.player.toggleRepeatMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.4f else 1f
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        PlayerDesignStyle.V4 -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                val baseLarge = 56.dp
                val baseSmall = 46.dp
                val baseGap = 12.dp
                val baseLargeIcon = 28.dp
                val baseSmallIcon = 22.dp
                val baseLargeRadius = 18.dp
                val baseSmallRadius = 16.dp
                val centerSize = 88.dp
                val centerPadding = 40.dp
                val sideTotal = (maxWidth - centerSize - centerPadding) / 2f
                val scale =
                    ((sideTotal - baseGap) / (baseLarge + baseSmall)).coerceAtMost(1f).coerceAtLeast(0.6f)
                val large = baseLarge * scale
                val small = baseSmall * scale
                val gap = baseGap * scale
                val largeIcon = baseLargeIcon * scale
                val smallIcon = baseSmallIcon * scale
                val largeRadius = baseLargeRadius * scale
                val smallRadius = baseSmallRadius * scale

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                            },
                            shape = RoundedCornerShape(smallRadius),
                            color = textBackgroundColor.copy(
                                alpha = if (shuffleModeEnabled) 0.2f else 0.08f
                            ),
                            modifier = Modifier.size(small)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (shuffleModeEnabled) 1f else 0.6f
                                    ),
                                    modifier = Modifier.size(smallIcon)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(gap))

                        Surface(
                            onClick = { playerConnection.seekToPrevious() },
                            enabled = canSkipPrevious,
                            shape = RoundedCornerShape(largeRadius),
                            color = textBackgroundColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(large)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (canSkipPrevious) 1f else 0.4f
                                    ),
                                    modifier = Modifier.size(largeIcon)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        color = textBackgroundColor,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .size(88.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = icBackgroundColor,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        when {
                                            playbackState == STATE_ENDED -> R.drawable.replay
                                            isPlaying -> R.drawable.pause
                                            else -> R.drawable.play
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = icBackgroundColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { playerConnection.seekToNext() },
                            enabled = canSkipNext,
                            shape = RoundedCornerShape(largeRadius),
                            color = textBackgroundColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(large)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (canSkipNext) 1f else 0.4f
                                    ),
                                    modifier = Modifier.size(largeIcon)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(gap))

                        Surface(
                            onClick = { playerConnection.player.toggleRepeatMode() },
                            shape = RoundedCornerShape(smallRadius),
                            color = textBackgroundColor.copy(
                                alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 0.2f else 0.08f
                            ),
                            modifier = Modifier.size(small)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> R.drawable.repeat
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.6f else 1f
                                    ),
                                    modifier = Modifier.size(smallIcon)
                                )
                            }
                        }
                    }
                }
            }
        }

        PlayerDesignStyle.V1, PlayerDesignStyle.V5 -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        },
                        color = textBackgroundColor,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                        onClick = {
                            playerConnection.player.toggleRepeatMode()
                        },
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_previous,
                        enabled = canSkipPrevious,
                        color = textBackgroundColor,
                        modifier =
                        Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        onClick = playerConnection::seekToPrevious,
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier =
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(playPauseRoundness))
                        .background(textButtonColor)
                        .clickable {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                            color = iconButtonColor,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Image(
                            painter =
                            painterResource(
                                if (playbackState ==
                                    STATE_ENDED
                                ) {
                                    R.drawable.replay
                                } else if (isPlaying) {
                                    R.drawable.pause
                                } else {
                                    R.drawable.play
                                },
                            ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconButtonColor),
                            modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_next,
                        enabled = canSkipNext,
                        color = textBackgroundColor,
                        modifier =
                        Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        onClick = playerConnection::seekToNext,
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border,
                        color = if (currentSongLiked) MaterialTheme.colorScheme.error else textBackgroundColor,
                        modifier =
                        Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center),
                        onClick = playerConnection::toggleLike,
                    )
                }
            }
        }
    }
}

/**
 * Wrapper composable that combines all player control components.
 * This replaces the large inline controlsContent lambda in BottomSheetPlayer
 * to reduce JIT compilation overhead.
 */
@Composable
fun PlayerControlsContent(
    mediaMetadata: MediaMetadata,
    playerDesignStyle: PlayerDesignStyle,
    sliderStyle: SliderStyle,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    menuState: MenuState,
    bottomSheetPageState: BottomSheetPageState,
    clipboardManager: ClipboardManager,
    context: Context,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit
) {
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true

    val playPauseRoundness by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "playPauseRoundness",
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            PlayerTitleSection(
                mediaMetadata = mediaMetadata,
                textBackgroundColor = textBackgroundColor,
                navController = navController,
                state = state,
                clipboardManager = clipboardManager,
                context = context
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        PlayerTopActions(
            mediaMetadata = mediaMetadata,
            playerDesignStyle = playerDesignStyle,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            textBackgroundColor = textBackgroundColor,
            playerConnection = playerConnection,
            navController = navController,
            menuState = menuState,
            state = state,
            bottomSheetPageState = bottomSheetPageState,
            context = context,
            currentSongLiked = currentSongLiked
        )
    }

    Spacer(Modifier.height(12.dp))

    PlayerSlider(
        sliderStyle = sliderStyle,
        sliderPosition = sliderPosition,
        position = position,
        duration = duration,
        isPlaying = isPlaying,
        textButtonColor = textButtonColor,
        onValueChange = onSliderValueChange,
        onValueChangeFinished = onSliderValueChangeFinished
    )

    Spacer(Modifier.height(4.dp))

    PlayerTimeLabel(
        sliderPosition = sliderPosition,
        position = position,
        duration = duration,
        textBackgroundColor = textBackgroundColor
    )

    Spacer(Modifier.height(12.dp))

    PlayerPlaybackControls(
        playerDesignStyle = playerDesignStyle,
        playbackState = playbackState,
        isPlaying = isPlaying,
        isLoading = isLoading,
        repeatMode = repeatMode,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        textButtonColor = textButtonColor,
        iconButtonColor = iconButtonColor,
        textBackgroundColor = textBackgroundColor,
        icBackgroundColor = icBackgroundColor,
        playPauseRoundness = playPauseRoundness,
        playerConnection = playerConnection,
        currentSongLiked = currentSongLiked
    )
}
@Composable
fun PlayerBackground(
    playerBackground: PlayerBackgroundStyle,
    mediaMetadata: MediaMetadata?,
    gradientColors: List<Color>,
    disableBlur: Boolean,
    playerCustomImageUri: String,
    playerCustomBlur: Float,
    playerCustomContrast: Float,
    playerCustomBrightness: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR -> {
                AnimatedContent(
                    targetState = mediaMetadata?.thumbnailUrl,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { thumbnailUrl ->
                    if (thumbnailUrl != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = "Blurred background",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize().let {
                                    if (disableBlur) it else it.blur(radius = 60.dp)
                                }
                            )
                            val overlayStops = PlayerBackgroundColorUtils.buildBlurOverlayStops(gradientColors)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = overlayStops))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.08f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.GRADIENT -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val gradientColorStops = if (colors.size >= 3) {
                                arrayOf(
                                    0.0f to colors[0].copy(alpha = 0.92f), // Top: primary vibrant color
                                    0.5f to colors[1].copy(alpha = 0.75f), // Middle: darker variant
                                    1.0f to colors[2].copy(alpha = 0.65f)  // Bottom: black-ish
                                )
                            } else {
                                arrayOf(
                                    0.0f to colors[0].copy(alpha = 0.9f), // Top: primary color
                                    0.6f to colors[0].copy(alpha = 0.55f), // Middle: faded variant
                                    1.0f to Color.Black.copy(alpha = 0.7f) // Bottom: black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                            )
                            // Keep a gentle dark overlay to ensure text contrast on bright artwork
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.18f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.COLORING -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        val baseColor = PlayerBackgroundColorUtils.ensureComfortableColor(colors.first())
                        val gradientStops = PlayerBackgroundColorUtils.buildColoringStops(baseColor)
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.fillMaxSize().background(baseColor))
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientStops))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.BLUR_GRADIENT -> {
                AnimatedContent(
                    targetState = mediaMetadata?.thumbnailUrl,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { thumbnailUrl ->
                    if (thumbnailUrl != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = "Blurred background",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize().let {
                                    if (disableBlur) it else it.blur(radius = 65.dp)
                                }
                            )
                            val gradientColorStops =
                                PlayerBackgroundColorUtils.buildBlurGradientStops(gradientColors)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.05f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.CUSTOM -> {
                AnimatedContent(
                    targetState = playerCustomImageUri,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { uri ->
                    if (uri.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val blurPx = playerCustomBlur
                            val contrastVal = playerCustomContrast
                            val brightnessVal = playerCustomBrightness

                            val t = (1f - contrastVal) * 128f + (brightnessVal - 1f) * 255f
                            val matrix = floatArrayOf(
                                contrastVal, 0f, 0f, 0f, t,
                                0f, contrastVal, 0f, 0f, t,
                                0f, 0f, contrastVal, 0f, t,
                                0f, 0f, 0f, 1f, 0f,
                            )

                            val cm = ColorMatrix(matrix)

                            AsyncImage(
                                model = Uri.parse(uri),
                                contentDescription = "Custom background",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize().let {
                                    if (disableBlur) it else it.blur(radius = blurPx.dp)
                                },
                                colorFilter = ColorFilter.colorMatrix(cm)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.GLOW -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1200)) togetherWith fadeOut(tween(1200))
                    },
                    label = ""
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    val width = size.width
                                    val height = size.height

                                    // Use a dark base, but the gradients will cover most of it
                                    val baseColor = Color(0xFF050505)

                                    // Extract up to 6 colors
                                    val color1 = colors.getOrElse(0) { Color.DarkGray }
                                    val color2 = colors.getOrElse(1) { color1 }
                                    val color3 = colors.getOrElse(2) { color2 }
                                    val color4 = colors.getOrElse(3) { color1 }
                                    val color5 = colors.getOrElse(4) { color2 }
                                    val color6 = colors.getOrElse(5) { color3 }

                                    // Top-Left Large Glow (Primary)
                                    val brush1 = Brush.radialGradient(
                                        colors = listOf(
                                            color1.copy(alpha = 0.8f),
                                            color1.copy(alpha = 0.5f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.2f, height * 0.25f),
                                        radius = width * 1.2f
                                    )

                                    // Bottom-Right Large Glow (Secondary)
                                    val brush2 = Brush.radialGradient(
                                        colors = listOf(
                                            color2.copy(alpha = 0.75f),
                                            color2.copy(alpha = 0.45f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.85f, height * 0.8f),
                                        radius = width * 1.1f
                                    )

                                    // Top-Right Glow (Tertiary)
                                    val brush3 = Brush.radialGradient(
                                        colors = listOf(
                                            color3.copy(alpha = 0.7f),
                                            color3.copy(alpha = 0.4f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.9f, height * 0.15f),
                                        radius = width * 1.0f
                                    )
                                    
                                    // Bottom-Left (Quaternary)
                                    val brush4 = Brush.radialGradient(
                                        colors = listOf(
                                            color4.copy(alpha = 0.65f),
                                            color4.copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.1f, height * 0.9f),
                                        radius = width * 1.0f
                                    )
                                    
                                    // Top-Center (Quinary)
                                    val brush5 = Brush.radialGradient(
                                        colors = listOf(
                                            color5.copy(alpha = 0.6f),
                                            color5.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.5f, height * 0.1f),
                                        radius = width * 0.9f
                                    )
                                    
                                    // Bottom-Center (Senary)
                                    val brush6 = Brush.radialGradient(
                                        colors = listOf(
                                            color6.copy(alpha = 0.6f),
                                            color6.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.5f, height * 0.95f),
                                        radius = width * 0.9f
                                    )

                                    onDrawBehind {
                                        drawRect(color = baseColor)
                                        drawRect(brush = brush1)
                                        drawRect(brush = brush2)
                                        drawRect(brush = brush3)
                                        drawRect(brush = brush4)
                                        drawRect(brush = brush5)
                                        drawRect(brush = brush6)
                                    }
                                }
                        )
                    }
                }
            }

            PlayerBackgroundStyle.GLOW_ANIMATED -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1200)) togetherWith fadeOut(tween(1200))
                    },
                    label = "GlowAnimatedContent"
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        val infiniteTransition = rememberInfiniteTransition(label = "GlowAnimation")

                        val progress by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(20000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "glowProgress"
                        )

                        fun rotatedColorAt(index: Int): Color {
                            val size = colors.size
                            val idx = index.toFloat() + progress * size
                            val a = kotlin.math.floor(idx).toInt() % size
                            val b = (a + 1) % size
                            val frac = idx - kotlin.math.floor(idx)
                            return androidx.compose.ui.graphics.lerp(colors.getOrElse(a) { Color.DarkGray }, colors.getOrElse(b) { Color.DarkGray }, frac)
                        }

                        fun oscillate(min: Float, max: Float, phase: Float, speed: Float = 1f): Float {
                            val v = kotlin.math.sin(2f * kotlin.math.PI.toFloat() * (progress * speed + phase)).toFloat()
                            return min + (max - min) * ((v + 1f) * 0.5f)
                        }

                        val color1 = rotatedColorAt(0)
                        val color2 = rotatedColorAt(1)
                        val color3 = rotatedColorAt(2)
                        val color4 = rotatedColorAt(3)
                        val color5 = rotatedColorAt(4)
                        val color6 = rotatedColorAt(5)

                        val o1x = oscillate(0.0f, 1.0f, 0.00f, 1.0f)
                        val o1y = oscillate(0.0f, 0.5f, 0.07f, 1.1f)
                        val r1 = oscillate(0.8f, 1.6f, 0.12f, 0.95f)

                        val o2x = oscillate(1.0f, 0.0f, 0.2f, 1.05f)
                        val o2y = oscillate(0.5f, 1.0f, 0.25f, 0.9f)
                        val r2 = oscillate(0.7f, 1.5f, 0.18f, 1.02f)

                        val o3x = oscillate(0.2f, 0.8f, 0.33f, 0.98f)
                        val o3y = oscillate(0.8f, 0.2f, 0.36f, 1.01f)
                        val r3 = oscillate(0.6f, 1.4f, 0.29f, 0.97f)

                        val o4x = oscillate(0.3f, 0.7f, 0.44f, 1.03f)
                        val o4y = oscillate(0.2f, 0.8f, 0.41f, 0.99f)
                        val r4 = oscillate(0.9f, 1.7f, 0.47f, 1.0f)

                        val o5x = oscillate(0.4f, 0.6f, 0.55f, 0.95f)
                        val o5y = oscillate(0.0f, 1.0f, 0.51f, 1.04f)
                        val r5 = oscillate(0.7f, 1.5f, 0.58f, 1.06f)

                        val o6x = oscillate(0.0f, 1.0f, 0.66f, 1.08f)
                        val o6y = oscillate(0.5f, 0.7f, 0.62f, 0.96f)
                        val r6 = oscillate(0.8f, 1.8f, 0.69f, 1.01f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    val width = size.width
                                    val height = size.height
                                    val baseColor = Color(0xFF050505)

                                    val brush1 = Brush.radialGradient(
                                        colors = listOf(color1.copy(alpha = 0.85f), color1.copy(alpha = 0.5f), Color.Transparent),
                                        center = Offset(width * o1x, height * o1y),
                                        radius = width * r1
                                    )
                                    val brush2 = Brush.radialGradient(
                                        colors = listOf(color2.copy(alpha = 0.8f), color2.copy(alpha = 0.45f), Color.Transparent),
                                        center = Offset(width * o2x, height * o2y),
                                        radius = width * r2
                                    )
                                    val brush3 = Brush.radialGradient(
                                        colors = listOf(color3.copy(alpha = 0.75f), color3.copy(alpha = 0.4f), Color.Transparent),
                                        center = Offset(width * o3x, height * o3y),
                                        radius = width * r3
                                    )
                                    val brush4 = Brush.radialGradient(
                                        colors = listOf(color4.copy(alpha = 0.7f), color4.copy(alpha = 0.35f), Color.Transparent),
                                        center = Offset(width * o4x, height * o4y),
                                        radius = width * r4
                                    )
                                    val brush5 = Brush.radialGradient(
                                        colors = listOf(color5.copy(alpha = 0.65f), color5.copy(alpha = 0.3f), Color.Transparent),
                                        center = Offset(width * o5x, height * o5y),
                                        radius = width * r5
                                    )
                                    val brush6 = Brush.radialGradient(
                                        colors = listOf(color6.copy(alpha = 0.6f), color6.copy(alpha = 0.25f), Color.Transparent),
                                        center = Offset(width * o6x, height * o6y),
                                        radius = width * r6
                                    )

                                    onDrawBehind {
                                        drawRect(color = baseColor)
                                        drawRect(brush = brush1)
                                        drawRect(brush = brush2)
                                        drawRect(brush = brush3)
                                        drawRect(brush = brush4)
                                        drawRect(brush = brush5)
                                        drawRect(brush = brush6)
                                    }
                                }
                        )
                    }
                }
            }

            else -> {
                // DEFAULT or other modes - no background
            }
        }
    }
}

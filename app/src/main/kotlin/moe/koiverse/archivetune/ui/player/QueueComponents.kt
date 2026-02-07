/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.FormatEntity
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.ui.component.ActionPromptDialog
import moe.koiverse.archivetune.ui.component.BottomSheetPageState
import moe.koiverse.archivetune.ui.component.BottomSheetState
import moe.koiverse.archivetune.ui.component.MenuState
import moe.koiverse.archivetune.ui.component.bottomSheetDraggable
import moe.koiverse.archivetune.ui.menu.PlayerMenu
import moe.koiverse.archivetune.ui.utils.ShowMediaInfo
import moe.koiverse.archivetune.utils.makeTimeString
import kotlin.math.roundToInt

/**
 * Current Song Header shown at the top of the queue
 * Displays album art, song info, and control buttons
 */
@Composable
fun CurrentSongHeader(
    sheetState: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    locked: Boolean,
    songCount: Int,
    queueDuration: Int,
    similarContentEnabled: Boolean,
    backgroundColor: Color,
    onBackgroundColor: Color,
    onToggleLike: () -> Unit,
    onMenuClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onLockClick: () -> Unit,
    onSimilarContentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .bottomSheetDraggable(sheetState)
            .pointerInput(Unit) { detectTapGestures { } } // Block clicks while allowing drag
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(onBackgroundColor.copy(alpha = 0.3f))
            )
        }
        
        // Song info row with thumbnail, title, artist, and action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art thumbnail
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Song title and artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = mediaMetadata?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = onBackgroundColor
                )
                Text(
                    text = mediaMetadata?.artists?.joinToString(",") { artist -> artist.name } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = onBackgroundColor.copy(alpha = 0.7f)
                )
            }
            
            // Like button
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (mediaMetadata?.liked == true) R.drawable.favorite
                        else R.drawable.favorite_border
                    ),
                    contentDescription = stringResource(R.string.action_like),
                    tint = if (mediaMetadata?.liked == true) 
                        MaterialTheme.colorScheme.primary
                    else onBackgroundColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Lock button
            IconButton(
                onClick = onLockClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                    contentDescription = null,
                    tint = onBackgroundColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Menu button
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = onBackgroundColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Control buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (shuffleModeEnabled) onBackgroundColor.copy(alpha = 0.2f)
                        else onBackgroundColor.copy(alpha = 0.15f)
                    )
                    .clickable { onShuffleClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.action_shuffle_on),
                    tint = onBackgroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Repeat button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (repeatMode != Player.REPEAT_MODE_OFF) onBackgroundColor.copy(alpha = 0.2f)
                        else onBackgroundColor.copy(alpha = 0.15f)
                    )
                    .clickable { onRepeatClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = null,
                    tint = onBackgroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Infinity/Automix button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (similarContentEnabled) onBackgroundColor.copy(alpha = 0.2f)
                        else onBackgroundColor.copy(alpha = 0.15f)
                    )
                    .clickable { onSimilarContentClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.all_inclusive),
                    contentDescription = stringResource(R.string.similar_content),
                    tint = onBackgroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // "Continue Playing" text and stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.queue_continue_playing),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = onBackgroundColor
                )
                Text(
                    text = stringResource(R.string.queue_autoplaying_similar),
                    style = MaterialTheme.typography.bodySmall,
                    color = onBackgroundColor.copy(alpha = 0.6f)
                )
            }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.n_song,
                        songCount,
                        songCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBackgroundColor.copy(alpha = 0.8f)
                )
                Text(
                    text = makeTimeString(queueDuration * 1000L),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBackgroundColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Shared Sleep Timer Dialog component used in both Queue and Player.
 */
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onEndOfSong: () -> Unit,
    initialValue: Float = 30f
) {
    var sleepTimerValue by remember { mutableFloatStateOf(initialValue) }
    
    ActionPromptDialog(
        titleBar = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        },
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(sleepTimerValue.roundToInt())
        },
        onCancel = onDismiss,
        onReset = {
            sleepTimerValue = 30f
        },
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pluralStringResource(
                        R.plurals.minute,
                        sleepTimerValue.roundToInt(),
                        sleepTimerValue.roundToInt()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(Modifier.height(16.dp))

                Slider(
                    value = sleepTimerValue,
                    onValueChange = { sleepTimerValue = it },
                    valueRange = 5f..120f,
                    steps = (120 - 5) / 5 - 1,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(onClick = onEndOfSong) {
                    Text(stringResource(R.string.end_of_song))
                }
            }
        }
    )
}

/**
 * Codec information row displayed when showCodecOnPlayer is enabled.
 */
@Composable
fun CodecInfoRow(
    codec: String,
    bitrate: String,
    fileSize: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 30.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Text(
            text = buildString {
                append(codec)
                if (bitrate != "Unknown") {
                    append(" • ")
                    append(bitrate)
                }
                if (fileSize.isNotEmpty()) {
                    append(" • ")
                    append(fileSize)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * V2 Design Style collapsed queue content.
 */
@Composable
fun QueueCollapsedContentV2(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    repeatMode: Int,
    mediaMetadata: MediaMetadata?,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    onRepeatModeClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec =
                currentFormat.codecs
                    .takeIf { it.isNotBlank() }
                    ?: currentFormat.mimeType.substringAfter("/", missingDelimiterValue = currentFormat.mimeType).uppercase()

            val container =
                currentFormat.mimeType.substringAfter("/", missingDelimiterValue = currentFormat.mimeType).uppercase()

            val codecLabel =
                if (container.isNotBlank() && !codec.equals(container, ignoreCase = true)) {
                    "$codec ($container)"
                } else {
                    codec
                }

            val bitrate =
                if (currentFormat.bitrate > 0) {
                    "${currentFormat.bitrate / 1000} kbps"
                } else {
                    "Unknown"
                }

            val sampleRateText =
                currentFormat.sampleRate?.takeIf { it > 0 }?.let { sampleRate ->
                    val khz = (sampleRate / 100.0).roundToInt() / 10.0
                    "$khz kHz"
                }

            val fileSizeText =
                if (currentFormat.contentLength > 0) {
                    "${(currentFormat.contentLength / 1024.0 / 1024.0).roundToInt()} MB"
                } else {
                    ""
                }

            val extraText =
                listOfNotNull(sampleRateText, fileSizeText.takeIf { it.isNotBlank() })
                    .joinToString(separator = " • ")
            
            CodecInfoRow(
                codec = codecLabel,
                bitrate = bitrate,
                fileSize = extraText,
                textColor = textBackgroundColor.copy(alpha = 0.7f),
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 10.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    ),
                ),
        ) {
            val buttonSize = 42.dp
            val iconSize = 24.dp
            val borderColor = textBackgroundColor.copy(alpha = 0.35f)

            // Queue button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(
                        RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 10.dp,
                            bottomEnd = 10.dp
                        )
                    )
                    .border(
                        1.dp,
                        borderColor,
                        RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 10.dp,
                            bottomEnd = 10.dp
                        )
                    )
                    .clickable { onExpandQueue() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.queue_music),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = textBackgroundColor
                )
            }

            // Sleep timer button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onSleepTimerClick() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    label = "sleepTimer",
                    targetState = sleepTimerEnabled,
                ) { enabled ->
                    if (enabled) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            color = textBackgroundColor,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.bedtime),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = textBackgroundColor
                        )
                    }
                }
            }

            // Lyrics button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onShowLyrics() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.lyrics),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = textBackgroundColor
                )
            }

            // Repeat mode button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(
                        RoundedCornerShape(
                            topStart = 10.dp,
                            bottomStart = 10.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp
                        )
                    )
                    .border(
                        1.dp,
                        borderColor,
                        RoundedCornerShape(
                            topStart = 10.dp,
                            bottomStart = 10.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp
                        )
                    )
                    .clickable { onRepeatModeClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                    tint = textBackgroundColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Menu button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(textButtonColor)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.more_vert),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = iconButtonColor
                )
            }
        }
    }
}

/**
 * V3 Design Style collapsed queue content.
 */
@Composable
fun QueueCollapsedContentV3(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    repeatMode: Int,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    onRepeatModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec = currentFormat.mimeType.substringAfter("/").uppercase()
            val bitrate = "${currentFormat.bitrate / 1000} kbps"
            
            CodecInfoRow(
                codec = codec,
                bitrate = bitrate,
                fileSize = "",
                textColor = textBackgroundColor.copy(alpha = 0.5f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    ),
                ),
        ) {
            // Queue button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExpandQueue() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = textBackgroundColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(id = R.string.queue),
                        color = textBackgroundColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            // Sleep timer button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSleepTimerClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    label = "sleepTimer",
                    targetState = sleepTimerEnabled,
                ) { enabled ->
                    if (enabled) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            color = textBackgroundColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.bedtime),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = textBackgroundColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Lyrics button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onShowLyrics() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lyrics),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = textBackgroundColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(id = R.string.lyrics),
                        color = textBackgroundColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            // Repeat mode button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRepeatModeClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                    tint = textBackgroundColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * V1 Design Style collapsed queue content (text buttons).
 */
@Composable
fun QueueCollapsedContentV1(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec = currentFormat.mimeType.substringAfter("/").uppercase()
            val bitrate = "${currentFormat.bitrate / 1000} kbps"
            val fileSize = if (currentFormat.contentLength > 0) {
                "${(currentFormat.contentLength / 1024.0 / 1024.0).roundToInt()} MB"
            } else ""
            
            CodecInfoRow(
                codec = codec,
                bitrate = bitrate,
                fileSize = fileSize,
                textColor = textBackgroundColor.copy(alpha = 0.7f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 12.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
        ) {
            TextButton(
                onClick = onExpandQueue,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.queue),
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            TextButton(
                onClick = onSleepTimerClick,
                modifier = Modifier.weight(1.2f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.bedtime),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedContent(
                        label = "sleepTimer",
                        targetState = sleepTimerEnabled,
                    ) { enabled ->
                        if (enabled) {
                            Text(
                                text = makeTimeString(sleepTimerTimeLeft),
                                color = textBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.sleep_timer),
                                color = textBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onShowLyrics,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lyrics),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.lyrics),
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

/**
 * V4 Design Style collapsed queue content (pill buttons).
 */
@Composable
fun QueueCollapsedContentV4(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    mediaMetadata: MediaMetadata?,
    onExpandQueue: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowLyrics: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showCodecOnPlayer && currentFormat != null) {
            val codec = currentFormat.mimeType.substringAfter("/").uppercase()
            val bitrate = "${currentFormat.bitrate / 1000} kbps"
            val fileSize = if (currentFormat.contentLength > 0) {
                "${(currentFormat.contentLength / 1024.0 / 1024.0).roundToInt()} MB"
            } else ""
            
            CodecInfoRow(
                codec = codec,
                bitrate = bitrate,
                fileSize = fileSize,
                textColor = textBackgroundColor.copy(alpha = 0.6f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    ),
                ),
        ) {
            val buttonSize = 48.dp
            val iconSize = 22.dp

            // Queue button (pill)
            Box(
                modifier = Modifier
                    .height(buttonSize)
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(textBackgroundColor.copy(alpha = 0.1f))
                    .clickable { onExpandQueue() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.queue),
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Sleep timer button (circle)
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(
                        if (sleepTimerEnabled) textBackgroundColor.copy(alpha = 0.2f)
                        else textBackgroundColor.copy(alpha = 0.1f)
                    )
                    .clickable { onSleepTimerClick() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    label = "sleepTimer",
                    targetState = sleepTimerEnabled,
                ) { enabled ->
                    if (enabled) {
                        Text(
                            text = makeTimeString(sleepTimerTimeLeft),
                            color = textBackgroundColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.bedtime),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = textBackgroundColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Lyrics button (pill)
            Box(
                modifier = Modifier
                    .height(buttonSize)
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(textBackgroundColor.copy(alpha = 0.1f))
                    .clickable { onShowLyrics() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lyrics),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = textBackgroundColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.lyrics),
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Menu button (circle)
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(textButtonColor)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.more_vert),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = iconButtonColor
                )
            }
        }
    }
}

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.entities.FormatEntity
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.ui.component.ActionPromptDialog
import moe.koiverse.archivetune.ui.component.BottomSheetPageState
import moe.koiverse.archivetune.ui.component.BottomSheetState
import moe.koiverse.archivetune.ui.component.MenuState
import moe.koiverse.archivetune.ui.menu.PlayerMenu
import moe.koiverse.archivetune.ui.utils.ShowMediaInfo
import moe.koiverse.archivetune.utils.makeTimeString
import kotlin.math.roundToInt

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
            .padding(start = 30.dp, end = 30.dp, top = 8.dp, bottom = 0.dp)
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 12.dp)
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

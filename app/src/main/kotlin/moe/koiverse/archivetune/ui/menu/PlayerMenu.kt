/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.menu

import android.content.Intent
import android.content.res.Configuration
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ListItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import android.widget.Toast
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.models.WatchEndpoint
import moe.koiverse.archivetune.LocalDatabase
import moe.koiverse.archivetune.LocalDownloadUtil
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ArtistSeparatorsKey
import moe.koiverse.archivetune.constants.EqualizerBandLevelsMbKey
import moe.koiverse.archivetune.constants.EqualizerBassBoostEnabledKey
import moe.koiverse.archivetune.constants.EqualizerBassBoostStrengthKey
import moe.koiverse.archivetune.constants.EqualizerCustomProfilesJsonKey
import moe.koiverse.archivetune.constants.EqualizerEnabledKey
import moe.koiverse.archivetune.constants.EqualizerOutputGainEnabledKey
import moe.koiverse.archivetune.constants.EqualizerOutputGainMbKey
import moe.koiverse.archivetune.constants.EqualizerSelectedProfileIdKey
import moe.koiverse.archivetune.constants.EqualizerVirtualizerEnabledKey
import moe.koiverse.archivetune.constants.EqualizerVirtualizerStrengthKey
import moe.koiverse.archivetune.constants.ListItemHeight
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.playback.EqCapabilities
import moe.koiverse.archivetune.playback.EqProfile
import moe.koiverse.archivetune.playback.EqProfilesPayload
import moe.koiverse.archivetune.playback.EqualizerJson
import moe.koiverse.archivetune.playback.ExoDownloadService
import moe.koiverse.archivetune.playback.queues.YouTubeQueue
import moe.koiverse.archivetune.ui.component.BigSeekBar
import moe.koiverse.archivetune.ui.component.BottomSheetState
import moe.koiverse.archivetune.ui.component.ListDialog
import moe.koiverse.archivetune.ui.component.MenuSurfaceSection
import moe.koiverse.archivetune.ui.component.NewAction
import moe.koiverse.archivetune.ui.component.NewActionGrid
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import java.util.UUID

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: MediaMetadata.Artist?
    )

    val splitArtists = remember(artists, artistSeparators) {
        if (artistSeparators.isEmpty()) {
            artists.map { SplitArtist(it.name, it) }
        } else {
            val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
            artists.flatMap { artist ->
                val parts = artist.name.split(separatorRegex).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    parts.mapIndexed { index, name ->
                        SplitArtist(name, if (index == 0) artist else null)
                    }
                } else {
                    listOf(SplitArtist(artist.name, artist))
                }
            }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
        onAddComplete = { songCount, playlistNames ->
            val message = when {
                playlistNames.size == 1 -> context.getString(R.string.added_to_playlist, playlistNames.first())
                else -> context.getString(R.string.added_to_n_playlists, playlistNames.size)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(splitArtists.distinctBy { it.name }) { splitArtist ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = splitArtist.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        val thumbUrl = splitArtist.originalArtist?.thumbnailUrl
                        if (thumbUrl.isNullOrBlank()) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                splitArtist.originalArtist?.let { artist ->
                                    navController.navigate("artist/${artist.id}")
                                    showSelectArtistDialog = false
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                }
                            },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    var showEqualizerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showEqualizerDialog) {
        EqualizerDialog(
            onDismiss = { showEqualizerDialog = false },
            openSystemEqualizer = {
                val intent =
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(
                            AudioEffect.EXTRA_AUDIO_SESSION,
                            playerConnection.player.audioSessionId,
                        )
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                if (intent.resolveActivity(context.packageManager) != null) {
                    activityResultLauncher.launch(intent)
                }
            },
        )
    }

    val nowPlayingTitle =
        remember(mediaMetadata.title) {
            mediaMetadata.title.ifBlank { context.getString(R.string.no_title) }
        }

    val nowPlayingSubtitle =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.joinToString(separator = " • ") { it.name }
        }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            val thumb = mediaMetadata.thumbnailUrl
            if (thumb.isNullOrBlank()) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nowPlayingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
                if (nowPlayingSubtitle.isNotBlank()) {
                    Text(
                        text = nowPlayingSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }
        }
    }

    if (isQueueTrigger != true) {
        Spacer(modifier = Modifier.height(12.dp))

        PlayerVolumeCard(
            volume = playerVolume.value,
            onVolumeChange = { playerConnection.service.playerVolume.value = it },
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.start_radio),
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.starting_radio), Toast.LENGTH_SHORT).show()
                            playerConnection.startRadioSeamlessly()
                            onDismiss()
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.link),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.copy_link),
                        onClick = {
                            val clipboard =
                                context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip =
                                android.content.ClipData.newPlainText(
                                    context.getString(R.string.copy_link),
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                                )
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, R.string.link_copied, android.widget.Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (splitArtists.isNotEmpty() || mediaMetadata.album != null) {
            item {
                MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                    Column {
                        if (splitArtists.isNotEmpty()) {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.artist),
                                        contentDescription = null,
                                    )
                                },
                                modifier =
                                    Modifier.clickable {
                                        if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                                            navController.navigate("artist/${splitArtists[0].originalArtist!!.id}")
                                            playerBottomSheetState.collapseSoft()
                                            onDismiss()
                                        } else {
                                            showSelectArtistDialog = true
                                        }
                                    },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }

                        if (splitArtists.isNotEmpty() && mediaMetadata.album != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }

                        if (mediaMetadata.album != null) {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.view_album)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                    )
                                },
                                modifier =
                                    Modifier.clickable {
                                        navController.navigate("album/${mediaMetadata.album.id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        item {
            when (download?.state) {
                Download.STATE_COMPLETED -> {
                    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                        ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.remove_download),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.offline),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        modifier = Modifier.clickable {
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                mediaMetadata.id,
                                false,
                            )
                        }
                        , colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                        ListItem(
                        headlineContent = { Text(text = stringResource(R.string.downloading)) },
                        leadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        modifier = Modifier.clickable {
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                mediaMetadata.id,
                                false,
                            )
                        }
                        , colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
                else -> {
                    MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                        ListItem(
                        headlineContent = { Text(text = stringResource(R.string.action_download)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            database.transaction {
                                insert(mediaMetadata)
                            }
                            val downloadRequest =
                                DownloadRequest
                                    .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                    .setCustomCacheKey(mediaMetadata.id)
                                    .setData(mediaMetadata.title.toByteArray())
                                    .build()
                            DownloadService.sendAddDownload(
                                context,
                                ExoDownloadService::class.java,
                                downloadRequest,
                                false,
                            )
                        }
                        , colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
        item {
            MenuSurfaceSection(modifier = Modifier.padding(vertical = 6.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.details)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                            )
                        },
                        modifier =
                            Modifier.clickable {
                                onShowDetailsDialog()
                                onDismiss()
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    if (isQueueTrigger != true) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.equalizer)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.equalizer),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable { showEqualizerDialog = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )

                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.tempo_and_pitch)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.speed),
                                    contentDescription = null,
                                )
                            },
                            supportingContent = {
                                val playbackParameters by playerConnection.playbackParameters.collectAsState()
                                Text(
                                    text = "x${formatMultiplier(playbackParameters.speed)} • x${formatMultiplier(playbackParameters.pitch)}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.clickable { showPitchTempoDialog = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerVolumeCard(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )

                Text(
                    text = stringResource(R.string.volume),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = "${(volume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BigSeekBar(
                progressProvider = { volume },
                onProgressChange = onVolumeChange,
                modifier = Modifier.fillMaxWidth().height(36.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.minimum_volume),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.maximum_volume),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val initialSpeed = remember { playerConnection.player.playbackParameters.speed }
    val initialPitch = remember { playerConnection.player.playbackParameters.pitch }

    var tempo by remember {
        mutableFloatStateOf(initialSpeed.safeCoerceIn(TempoMin, TempoMax, fallback = 1f))
    }

    var pitch by remember {
        mutableFloatStateOf(initialPitch.safeCoerceIn(PitchMin, PitchMax, fallback = 1f))
    }

    var pitchMode by rememberSaveable {
        mutableStateOf(
            if (isPitchSemitoneAligned(pitch)) PitchMode.Semitones else PitchMode.Multiplier
        )
    }

    val applyPlaybackParameters: (Float, Float) -> Unit = { speed, pitchMultiplier ->
        playerConnection.player.playbackParameters =
            PlaybackParameters(
                speed.coerceIn(TempoMin, TempoMax),
                pitchMultiplier.coerceIn(PitchMin, PitchMax),
            )
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    pitch = 1f
                    applyPlaybackParameters(tempo, pitch)
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.speed),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )

                    Text(
                        text = stringResource(R.string.tempo),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = "x${formatMultiplier(tempo)}",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.End,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(
                        enabled = tempo > TempoMin,
                        onClick = {
                            tempo = (tempo - 0.01f).coerceIn(TempoMin, TempoMax).quantize(0.01f)
                            applyPlaybackParameters(tempo, pitch)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.remove),
                            contentDescription = null,
                        )
                    }

                    Slider(
                        value = multiplierToSlider(tempo),
                        onValueChange = { slider ->
                            val updated = sliderToMultiplier(slider).quantize(0.01f)
                            if (abs(updated - tempo) >= 0.005f) {
                                tempo = updated
                                applyPlaybackParameters(tempo, pitch)
                            }
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(),
                    )

                    IconButton(
                        enabled = tempo < TempoMax,
                        onClick = {
                            tempo = (tempo + 0.01f).coerceIn(TempoMin, TempoMax).quantize(0.01f)
                            applyPlaybackParameters(tempo, pitch)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    val presets = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                    presets.forEach { preset ->
                        val selected = abs(tempo - preset) < 0.005f
                        FilterChip(
                            selected = selected,
                            onClick = {
                                tempo = preset
                                applyPlaybackParameters(tempo, pitch)
                            },
                            label = { Text("x${formatMultiplier(preset)}") },
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.discover_tune),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )

                    Text(
                        text = stringResource(R.string.pitch),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text =
                        when (pitchMode) {
                            PitchMode.Semitones -> {
                                val semitones = pitchToSemitones(pitch)
                                "${if (semitones > 0) "+" else ""}$semitones"
                            }

                            PitchMode.Multiplier -> "x${formatMultiplier(pitch)}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.End,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    FilterChip(
                        selected = pitchMode == PitchMode.Semitones,
                        onClick = { pitchMode = PitchMode.Semitones },
                        label = { Text(stringResource(R.string.pitch_mode_semitones_short)) },
                    )
                    FilterChip(
                        selected = pitchMode == PitchMode.Multiplier,
                        onClick = { pitchMode = PitchMode.Multiplier },
                        label = { Text(stringResource(R.string.pitch_mode_multiplier_short)) },
                    )
                }

                when (pitchMode) {
                    PitchMode.Semitones -> {
                        val currentSemitones = pitchToSemitones(pitch)
                        Slider(
                            value = currentSemitones.toFloat(),
                            onValueChange = { slider ->
                                val semitones = slider.roundToInt().coerceIn(-12, 12)
                                val updated = semitonesToPitch(semitones)
                                if (abs(updated - pitch) >= 0.0005f) {
                                    pitch = updated
                                    applyPlaybackParameters(tempo, pitch)
                                }
                            },
                            valueRange = -12f..12f,
                            steps = 23,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(),
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            val presets = listOf(-12, -7, -5, 0, 5, 7, 12)
                            presets.forEach { preset ->
                                val selected = currentSemitones == preset
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        pitch = semitonesToPitch(preset)
                                        applyPlaybackParameters(tempo, pitch)
                                    },
                                    label = { Text("${if (preset > 0) "+" else ""}$preset") },
                                )
                            }
                        }
                    }

                    PitchMode.Multiplier -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            IconButton(
                                enabled = pitch > PitchMin,
                                onClick = {
                                    pitch = (pitch - 0.01f).coerceIn(PitchMin, PitchMax).quantize(0.01f)
                                    applyPlaybackParameters(tempo, pitch)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.remove),
                                    contentDescription = null,
                                )
                            }

                            Slider(
                                value = multiplierToSlider(pitch),
                                onValueChange = { slider ->
                                    val updated = sliderToMultiplier(slider).quantize(0.01f)
                                    if (abs(updated - pitch) >= 0.005f) {
                                        pitch = updated
                                        applyPlaybackParameters(tempo, pitch)
                                    }
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(),
                            )

                            IconButton(
                                enabled = pitch < PitchMax,
                                onClick = {
                                    pitch = (pitch + 0.01f).coerceIn(PitchMin, PitchMax).quantize(0.01f)
                                    applyPlaybackParameters(tempo, pitch)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            val presets = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                            presets.forEach { preset ->
                                val selected = abs(pitch - preset) < 0.005f
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        pitch = preset
                                        applyPlaybackParameters(tempo, pitch)
                                    },
                                    label = { Text("x${formatMultiplier(preset)}") },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

private enum class PitchMode {
    Semitones,
    Multiplier
}

private const val TempoMin = 0.25f
private const val TempoMax = 2f
private const val PitchMin = 0.25f
private const val PitchMax = 2f

private fun Float.safeCoerceIn(min: Float, max: Float, fallback: Float): Float {
    val safe = if (this.isFinite()) this else fallback
    return safe.coerceIn(min, max)
}

private fun Float.quantize(step: Float): Float {
    if (step <= 0f) return this
    return (round(this / step) * step).coerceAtLeast(0f)
}

private fun pitchToSemitones(pitch: Float): Int {
    val safePitch = pitch.safeCoerceIn(PitchMin, PitchMax, fallback = 1f).coerceAtLeast(0.0001f)
    return (12f * log2(safePitch)).roundToInt().coerceIn(-12, 12)
}

private fun semitonesToPitch(semitones: Int): Float {
    return 2f.pow(semitones.toFloat() / 12f).coerceIn(PitchMin, PitchMax)
}

private fun isPitchSemitoneAligned(pitch: Float): Boolean {
    val safePitch = pitch.safeCoerceIn(PitchMin, PitchMax, fallback = 1f).coerceAtLeast(0.0001f)
    val semitones = (12f * log2(safePitch)).roundToInt()
    val reconstructed = 2f.pow(semitones.toFloat() / 12f)
    return abs(reconstructed - pitch) < 0.0015f
}

private fun formatMultiplier(multiplier: Float): String {
    return String.format("%.2f", multiplier)
}

private fun sliderToMultiplier(slider: Float): Float {
    val t = slider.coerceIn(0f, 1f)
    val y = (t - 0.5f) * 2f
    val curve = 2.2f
    val absY = abs(y).pow(curve)
    val shaped = when {
        y > 0f -> absY
        y < 0f -> -absY
        else -> 0f
    }
    val exponent = if (y < 0f) 2f * shaped else shaped
    return 2f.pow(exponent).coerceIn(TempoMin, TempoMax)
}

private fun multiplierToSlider(multiplier: Float): Float {
    val m = multiplier.coerceIn(TempoMin, TempoMax)
    val log = log2(m)
    val curve = 2.2f
    val shaped = if (m < 1f) (log / 2f) else log
    val absShaped = abs(shaped).pow(1f / curve)
    val y = when {
        shaped > 0f -> absShaped
        shaped < 0f -> -absShaped
        else -> 0f
    }
    return (0.5f + y / 2f).coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerDialog(
    onDismiss: () -> Unit,
    openSystemEqualizer: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val eqCapabilities by playerConnection.service.eqCapabilities.collectAsState()

    val (eqEnabled, setEqEnabled) = rememberPreference(EqualizerEnabledKey, defaultValue = false)
    val (selectedProfileId, setSelectedProfileId) = rememberPreference(EqualizerSelectedProfileIdKey, defaultValue = "flat")
    val (bandLevelsRaw, setBandLevelsRaw) = rememberPreference(EqualizerBandLevelsMbKey, defaultValue = "")

    val (outputGainEnabled, setOutputGainEnabled) = rememberPreference(EqualizerOutputGainEnabledKey, defaultValue = false)
    val (outputGainMb, setOutputGainMb) = rememberPreference(EqualizerOutputGainMbKey, defaultValue = 0)

    val (bassBoostEnabled, setBassBoostEnabled) = rememberPreference(EqualizerBassBoostEnabledKey, defaultValue = false)
    val (bassBoostStrength, setBassBoostStrength) = rememberPreference(EqualizerBassBoostStrengthKey, defaultValue = 0)

    val (virtualizerEnabled, setVirtualizerEnabled) = rememberPreference(EqualizerVirtualizerEnabledKey, defaultValue = false)
    val (virtualizerStrength, setVirtualizerStrength) = rememberPreference(EqualizerVirtualizerStrengthKey, defaultValue = 0)

    val (customProfilesJson, setCustomProfilesJson) = rememberPreference(EqualizerCustomProfilesJsonKey, defaultValue = "")

    val caps = eqCapabilities
    val bandCount = caps?.bandCount ?: 0
    val minMb = caps?.minBandLevelMb ?: -1500
    val maxMb = caps?.maxBandLevelMb ?: 1500

    var outputGainLocal by rememberSaveable { mutableIntStateOf(outputGainMb) }
    LaunchedEffect(outputGainMb) { outputGainLocal = outputGainMb }

    var bassBoostStrengthLocal by rememberSaveable { mutableIntStateOf(bassBoostStrength) }
    LaunchedEffect(bassBoostStrength) { bassBoostStrengthLocal = bassBoostStrength }

    var virtualizerStrengthLocal by rememberSaveable { mutableIntStateOf(virtualizerStrength) }
    LaunchedEffect(virtualizerStrength) { virtualizerStrengthLocal = virtualizerStrength }

    var bandLevelsMb by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(bandLevelsRaw, bandCount) {
        bandLevelsMb = resampleLevelsByIndex(decodeBandLevelsMb(bandLevelsRaw), bandCount)
    }

    val profiles = remember(customProfilesJson) { decodeProfilesPayload(customProfilesJson).profiles }
    val activeProfileId = selectedProfileId.removePrefix("profile:").takeIf { selectedProfileId.startsWith("profile:") }
    val activeProfile = remember(profiles, activeProfileId) { profiles.firstOrNull { it.id == activeProfileId } }

    var showSaveProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showManageProfilesDialog by rememberSaveable { mutableStateOf(false) }
    var showImportProfilesDialog by rememberSaveable { mutableStateOf(false) }

    if (showSaveProfileDialog) {
        TextFieldDialog(
            title = { Text(text = stringResource(R.string.eq_save_profile)) },
            placeholder = { Text(text = stringResource(R.string.eq_profile_name)) },
            onDone = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) {
                    val newProfile =
                        EqProfile(
                            id = UUID.randomUUID().toString(),
                            name = trimmed,
                            bandCenterFreqHz = caps?.centerFreqHz.orEmpty(),
                            bandLevelsMb = bandLevelsMb,
                            outputGainMb = outputGainMb,
                            bassBoostStrength = bassBoostStrength,
                            virtualizerStrength = virtualizerStrength,
                        )

                    val updatedPayload =
                        EqProfilesPayload(
                            profiles =
                                (profiles + newProfile)
                                    .distinctBy { it.id }
                                    .sortedBy { it.name.lowercase() },
                        )

                    setCustomProfilesJson(encodeProfilesPayload(updatedPayload))
                    setSelectedProfileId("profile:${newProfile.id}")
                }
            },
            onDismiss = { showSaveProfileDialog = false },
        )
    }

    if (showImportProfilesDialog) {
        TextFieldDialog(
            title = { Text(text = stringResource(R.string.eq_import_profiles)) },
            placeholder = { Text(text = stringResource(R.string.eq_import_profiles_placeholder)) },
            singleLine = false,
            maxLines = 10,
            isInputValid = { it.trim().isNotBlank() },
            onDone = { raw ->
                val trimmed = raw.trim()
                val payload =
                    decodeProfilesPayload(trimmed).takeIf { it.profiles.isNotEmpty() }
                        ?: runCatching {
                            EqProfilesPayload(EqualizerJson.json.decodeFromString<List<EqProfile>>(trimmed))
                        }.getOrNull()
                        ?: EqProfilesPayload()

                if (payload.profiles.isEmpty()) {
                    Toast
                        .makeText(context, context.getString(R.string.eq_import_failed), Toast.LENGTH_SHORT)
                        .show()
                    return@TextFieldDialog
                }

                val existingIds = profiles.map { it.id }.toMutableSet()
                val normalizedImported =
                    payload.profiles
                        .map { p ->
                            val baseName = p.name.trim().ifBlank { context.getString(R.string.eq_imported_profile) }
                            val incomingId = p.id.trim()
                            val finalId =
                                if (incomingId.isBlank() || !existingIds.add(incomingId)) {
                                    generateSequence { UUID.randomUUID().toString() }
                                        .first { existingIds.add(it) }
                                } else {
                                    incomingId
                                }

                            p.copy(
                                id = finalId,
                                name = baseName,
                            )
                        }

                val updatedPayload =
                    EqProfilesPayload(
                        profiles =
                            (profiles + normalizedImported)
                                .distinctBy { it.id }
                                .sortedBy { it.name.lowercase() },
                    )

                setCustomProfilesJson(encodeProfilesPayload(updatedPayload))
                val firstImportedId = normalizedImported.firstOrNull()?.id
                if (firstImportedId != null) {
                    setSelectedProfileId("profile:$firstImportedId")
                }

                Toast
                    .makeText(
                        context,
                        context.getString(R.string.eq_import_success, normalizedImported.size),
                        Toast.LENGTH_SHORT,
                    ).show()
            },
            onDismiss = { showImportProfilesDialog = false },
        )
    }

    if (showManageProfilesDialog) {
        ListDialog(
            onDismiss = { showManageProfilesDialog = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = profiles,
                key = { it.id },
            ) { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                setEqEnabled(true)
                                setBandLevelsRaw(encodeBandLevelsMb(profile.bandLevelsMb))
                                setOutputGainMb(profile.outputGainMb)
                                setOutputGainEnabled(profile.outputGainMb != 0)
                                setBassBoostStrength(profile.bassBoostStrength)
                                setBassBoostEnabled(profile.bassBoostStrength != 0)
                                setVirtualizerStrength(profile.virtualizerStrength)
                                setVirtualizerEnabled(profile.virtualizerStrength != 0)
                                setSelectedProfileId("profile:${profile.id}")
                                showManageProfilesDialog = false
                            }.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.eq_custom_profile),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = {
                            val updatedPayload =
                                EqProfilesPayload(
                                    profiles = profiles.filterNot { it.id == profile.id },
                                )
                            setCustomProfilesJson(encodeProfilesPayload(updatedPayload))
                            if (selectedProfileId == "profile:${profile.id}") {
                                setSelectedProfileId("manual")
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.equalizer)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = {
                                setEqEnabled(it)
                                if (it && selectedProfileId.isBlank()) setSelectedProfileId("manual")
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(id = if (eqEnabled) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 24.dp),
                ) {
                    Spacer(Modifier.height(12.dp))

                    if (caps == null || bandCount <= 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp),
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.eq_waiting_for_audio_session),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = openSystemEqualizer) {
                                    Text(text = stringResource(R.string.eq_open_system_equalizer))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        return@Column
                    }

                    EqSection(
                        title = stringResource(R.string.eq_presets),
                        trailing = {
                            TextButton(onClick = openSystemEqualizer) {
                                Text(text = stringResource(R.string.eq_system))
                            }
                        },
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                        ) {
                            FilterChip(
                                selected = selectedProfileId == "flat",
                                onClick = {
                                    playerConnection.service.applyEqFlatPreset()
                                    setSelectedProfileId("flat")
                                },
                                label = { Text(text = stringResource(R.string.eq_flat)) },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                border = null,
                            )

                            Spacer(Modifier.width(8.dp))

                            caps.systemPresets.forEachIndexed { index, name ->
                                FilterChip(
                                    selected = selectedProfileId == "system:$index",
                                    onClick = {
                                        playerConnection.service.applySystemEqPreset(index)
                                        setSelectedProfileId("system:$index")
                                    },
                                    label = {
                                        Text(
                                            text = name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ),
                                    border = null,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    EqSection(
                        title = stringResource(R.string.eq_profiles),
                        trailing = {
                            TextButton(onClick = { showManageProfilesDialog = true }) {
                                Text(text = stringResource(R.string.eq_manage))
                            }
                        },
                    ) {
                        val subtitle =
                            when {
                                selectedProfileId == "flat" -> stringResource(R.string.eq_flat)
                                selectedProfileId.startsWith("system:") -> stringResource(R.string.eq_system_preset)
                                activeProfile != null -> activeProfile.name
                                else -> stringResource(R.string.eq_manual)
                            }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(R.string.eq_profile_hint),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { showSaveProfileDialog = true }) {
                                Text(text = stringResource(R.string.eq_save))
                            }
                            TextButton(onClick = { showImportProfilesDialog = true }) {
                                Text(text = stringResource(R.string.eq_import))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    EqSection(
                        title = stringResource(R.string.eq_bands),
                        trailing = {
                            TextButton(
                                onClick = {
                                    setSelectedProfileId("manual")
                                    setBandLevelsRaw(encodeBandLevelsMb(List(bandCount) { 0 }))
                                },
                            ) {
                                Text(text = stringResource(R.string.reset))
                            }
                        },
                    ) {
                        caps.centerFreqHz.forEachIndexed { band, hz ->
                            val label = formatHz(hz)
                            val value = bandLevelsMb.getOrNull(band) ?: 0
                            val valueDb = (value / 100f).coerceIn(-24f, 24f)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.width(64.dp),
                                )

                                Slider(
                                    value = value.toFloat().coerceIn(minMb.toFloat(), maxMb.toFloat()),
                                    onValueChange = { newValue ->
                                        val coerced = newValue.toInt().coerceIn(minMb, maxMb)
                                        bandLevelsMb =
                                            bandLevelsMb.toMutableList().apply {
                                                while (size < bandCount) add(0)
                                                set(band, coerced)
                                            }
                                    },
                                    onValueChangeFinished = {
                                        setSelectedProfileId("manual")
                                        setBandLevelsRaw(encodeBandLevelsMb(bandLevelsMb))
                                    },
                                    valueRange = minMb.toFloat()..maxMb.toFloat(),
                                    colors =
                                        SliderDefaults.colors(
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ),
                                    modifier = Modifier.weight(1f),
                                )

                                Text(
                                    text = formatDb(valueDb),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(64.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    EqSection(title = stringResource(R.string.eq_output_gain)) {
                        EqToggleSliderRow(
                            enabled = outputGainEnabled,
                            onEnabledChange = {
                                setSelectedProfileId("manual")
                                setOutputGainEnabled(it)
                            },
                            value = outputGainLocal,
                            onValueChange = { outputGainLocal = it },
                            valueRange = -1500..1500,
                            formatValue = { formatDb(it / 100f) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onValueChangeFinished = {
                                setSelectedProfileId("manual")
                                setOutputGainMb(outputGainLocal)
                            },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    EqSection(title = stringResource(R.string.eq_bass_boost)) {
                        EqToggleSliderRow(
                            enabled = bassBoostEnabled,
                            onEnabledChange = {
                                setSelectedProfileId("manual")
                                setBassBoostEnabled(it)
                            },
                            value = bassBoostStrengthLocal,
                            onValueChange = { bassBoostStrengthLocal = it },
                            valueRange = 0..1000,
                            formatValue = { "${it / 10}%" },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onValueChangeFinished = {
                                setSelectedProfileId("manual")
                                setBassBoostStrength(bassBoostStrengthLocal)
                            },
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    EqSection(title = stringResource(R.string.eq_virtualizer)) {
                        EqToggleSliderRow(
                            enabled = virtualizerEnabled,
                            onEnabledChange = {
                                setSelectedProfileId("manual")
                                setVirtualizerEnabled(it)
                            },
                            value = virtualizerStrengthLocal,
                            onValueChange = { virtualizerStrengthLocal = it },
                            valueRange = 0..1000,
                            formatValue = { "${it / 10}%" },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onValueChangeFinished = {
                                setSelectedProfileId("manual")
                                setVirtualizerStrength(virtualizerStrengthLocal)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqSection(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun EqToggleSliderRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    formatValue: (Int) -> String,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            thumbContent = {
                Icon(
                    painter = painterResource(id = if (enabled) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )

        Spacer(Modifier.width(12.dp))

        Slider(
            value = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat()),
            onValueChange = { onValueChange(it.toInt().coerceIn(valueRange.first, valueRange.last)) },
            onValueChangeFinished = { onValueChangeFinished?.invoke() },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            enabled = enabled,
            colors =
                SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = formatValue(value),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.width(72.dp),
        )
    }
}

private fun decodeBandLevelsMb(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { EqualizerJson.json.decodeFromString<List<Int>>(raw) }.getOrNull() ?: emptyList()
}

private fun encodeBandLevelsMb(levelsMb: List<Int>): String {
    return runCatching { EqualizerJson.json.encodeToString(levelsMb) }.getOrNull().orEmpty()
}

private fun decodeProfilesPayload(raw: String?): EqProfilesPayload {
    if (raw.isNullOrBlank()) return EqProfilesPayload()
    return runCatching { EqualizerJson.json.decodeFromString<EqProfilesPayload>(raw) }.getOrNull() ?: EqProfilesPayload()
}

private fun encodeProfilesPayload(payload: EqProfilesPayload): String {
    return runCatching { EqualizerJson.json.encodeToString(payload) }.getOrNull().orEmpty()
}

private fun resampleLevelsByIndex(levelsMb: List<Int>, targetCount: Int): List<Int> {
    if (targetCount <= 0) return emptyList()
    if (levelsMb.isEmpty()) return List(targetCount) { 0 }
    if (levelsMb.size == targetCount) return levelsMb
    if (targetCount == 1) return listOf(levelsMb.sum() / levelsMb.size)

    val lastIndex = levelsMb.lastIndex.toFloat().coerceAtLeast(1f)
    return List(targetCount) { i ->
        val pos = i.toFloat() * lastIndex / (targetCount - 1).toFloat()
        val lo = kotlin.math.floor(pos).toInt().coerceIn(0, levelsMb.lastIndex)
        val hi = kotlin.math.ceil(pos).toInt().coerceIn(0, levelsMb.lastIndex)
        val t = (pos - lo.toFloat()).coerceIn(0f, 1f)
        val a = levelsMb[lo]
        val b = levelsMb[hi]
        (a + ((b - a) * t)).toInt()
    }
}

private fun formatHz(hz: Int): String {
    if (hz <= 0) return ""
    return if (hz >= 1000) "${(hz / 1000f).let { round(it * 10f) / 10f }}k" else hz.toString()
}

private fun formatDb(db: Float): String {
    val rounded = round(db * 10f) / 10f
    return "${if (rounded > 0f) "+" else ""}$rounded dB"
}

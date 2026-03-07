/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.extensions.currentMetadata
import moe.koiverse.archivetune.extensions.getCurrentQueueIndex
import moe.koiverse.archivetune.extensions.getQueueWindows
import moe.koiverse.archivetune.extensions.metadata
import moe.koiverse.archivetune.playback.MusicService.MusicBinder
import moe.koiverse.archivetune.playback.queues.Queue
import moe.koiverse.archivetune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    scope: CoroutineScope,
) : Player.Listener {
    val service = binder.service
    val player = service.player

    val playbackState = MutableStateFlow(player.playbackState)
    private val playWhenReady = MutableStateFlow(player.playWhenReady)
    val playbackParameters = MutableStateFlow(player.playbackParameters)
    val isPlaying =
        combine(playbackState, playWhenReady) { playbackState, playWhenReady ->
            playWhenReady && playbackState != STATE_ENDED
        }.stateIn(
            scope,
            SharingStarted.Lazily,
            player.playWhenReady && player.playbackState != STATE_ENDED
        )
    val mediaMetadata = MutableStateFlow(player.currentMetadata)
    val currentSong =
        mediaMetadata.flatMapLatest {
            database.song(it?.id)
        }
    val currentLyrics = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.lyrics(mediaMetadata?.id)
    }
    val currentFormat =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)
    val waitingForNetworkConnection = service.waitingForNetworkConnection
    val queueRestoreCompleted = service.queueRestoreCompleted

    init {
        player.addListener(this)

        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        playbackParameters.value = player.playbackParameters
        val currentMeta = player.currentMetadata ?: service.currentMediaMetadata.value
        mediaMetadata.value = currentMeta
        queueTitle.value = service.queueTitle
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode
        
        if (currentMeta == null && player.mediaItemCount > 0) {
            val mediaItem = player.currentMediaItem
            if (mediaItem != null) {
                mediaMetadata.value = mediaItem.metadata
            }
        }
    }

    fun playQueue(queue: Queue) {
        service.playQueue(queue)
    }

    fun startRadioSeamlessly() {
        service.startRadioSeamlessly()
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        service.playNext(items)
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        service.addToQueue(items)
    }

    fun toggleLike() {
        service.toggleLike()
    }

    fun seekToNext() {
        val state = service.togetherSessionState.value as? moe.koiverse.archivetune.together.TogetherSessionState.Joined
        if (state?.role is moe.koiverse.archivetune.together.TogetherRole.Guest) {
            service.requestTogetherControl(moe.koiverse.archivetune.together.ControlAction.SkipNext)
            return
        }
        player.seekToNext()
        player.prepare()
        player.playWhenReady = true
        // Immediately restart the Discord presence updater so it picks up the new track without waiting
        if (moe.koiverse.archivetune.ui.screens.settings.DiscordPresenceManager.isRunning()) {
            try {
                moe.koiverse.archivetune.ui.screens.settings.DiscordPresenceManager.restart()
            } catch (_: Exception) {}
        }
    }

    fun seekToPrevious() {
        val state = service.togetherSessionState.value as? moe.koiverse.archivetune.together.TogetherSessionState.Joined
        if (state?.role is moe.koiverse.archivetune.together.TogetherRole.Guest) {
            service.requestTogetherControl(moe.koiverse.archivetune.together.ControlAction.SkipPrevious)
            return
        }
        player.seekToPrevious()
        player.prepare()
        player.playWhenReady = true
        // Immediately restart the Discord presence updater so it picks up the new track without waiting
        if (moe.koiverse.archivetune.ui.screens.settings.DiscordPresenceManager.isRunning()) {
            try {
                moe.koiverse.archivetune.ui.screens.settings.DiscordPresenceManager.restart()
            } catch (_: Exception) {}
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        error.value = player.playerError
    }

    override fun onPlayWhenReadyChanged(
        newPlayWhenReady: Boolean,
        reason: Int,
    ) {
        playWhenReady.value = newPlayWhenReady
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        this.playbackParameters.value = playbackParameters
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        val meta = mediaItem?.metadata ?: service.currentMediaMetadata.value
        mediaMetadata.value = meta
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        queueWindows.value = player.getQueueWindows()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        error.value = playbackError
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                    !window.isLive ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive &&
                    window.isDynamic ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    fun dispose() {
        player.removeListener(this)
    }
}

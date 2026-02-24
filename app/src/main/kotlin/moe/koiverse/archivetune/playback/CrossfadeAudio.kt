/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package moe.koiverse.archivetune.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.db.MusicDatabase
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

internal class CrossfadeAudio(
    private val player: ExoPlayer,
    private val database: MusicDatabase,
    private val crossfadeDurationMs: MutableStateFlow<Int>,
    private val playbackFadeFactor: MutableStateFlow<Float>,
    private val playerVolume: MutableStateFlow<Float>,
    private val audioFocusVolumeFactor: MutableStateFlow<Float>,
    private val audioNormalizationEnabled: MutableStateFlow<Boolean>,
    private val maxSafeGainFactor: Float,
    private val overlapPlayerFactory: () -> ExoPlayer,
) {
    private var loopJob: Job? = null

    private var overlapPlayer: ExoPlayer? = null
    private var overlapPrimedIndex: Int = C.INDEX_UNSET
    private var overlapPrimedMediaId: String? = null
    private var crossfadeActive = false
    private var crossfadeTargetIndex: Int = C.INDEX_UNSET
    private var crossfadeTargetMediaId: String? = null
    private var crossfadeStartElapsedMs: Long = 0L
    private var crossfadeActiveDurationMs: Int = 0
    private var overlapNormalizeFactor: Float = 1f

    private var handoffActive = false
    private var handoffStartElapsedMs: Long = 0L
    private var handoffDurationMs: Int = 0
    private var handoffTargetPositionMs: Long = 0L
    private var handoffLastSyncSeekElapsedMs: Long = 0L
    private var handoffSeekIssued = false
    private var handoffRampStarted = false
    private val handoffReseekMinIntervalMs = 180L
    private val handoffDriftCorrectionThresholdMs = 220L
    private val handoffRampStartDriftToleranceMs = 120L

    fun start(scope: CoroutineScope) {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch { runLoop() }
    }

    fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        handleMediaItemTransition(mediaItem, reason)
    }

    fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            stop(resetMainFade = true)
        }
    }

    fun stop(resetMainFade: Boolean) {
        stopOverlapCrossfade(resetMainFade = resetMainFade)
    }

    fun release() {
        loopJob?.cancel()
        loopJob = null
        stopOverlapCrossfade(resetMainFade = true)
        runCatching { overlapPlayer?.release() }
        overlapPlayer = null
    }

    private suspend fun runLoop() {
        while (kotlin.coroutines.coroutineContext.isActive) {
            val fadeMs = crossfadeDurationMs.value
            if (fadeMs <= 0) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(250)
                continue
            }

            if (handoffActive) {
                updateVolumes()
                delay(50)
                continue
            }

            if (!player.playWhenReady) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(150)
                continue
            }

            if (!crossfadeActive && (player.playbackState != Player.STATE_READY || !player.isPlaying)) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(150)
                continue
            }

            val durationMs = player.duration
            val positionMs = player.currentPosition.coerceAtLeast(0L)
            val nextIndex = player.nextMediaItemIndex

            if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(150)
                continue
            }

            if (!crossfadeActive && (nextIndex == C.INDEX_UNSET || durationMs <= 0 || durationMs == C.TIME_UNSET)) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(150)
                continue
            }

            if (crossfadeActive) {
                val targetId = crossfadeTargetMediaId
                val currentId = player.currentMediaItem?.mediaId
                val onTarget = !targetId.isNullOrBlank() && targetId == currentId

                if (!onTarget && (nextIndex == C.INDEX_UNSET || durationMs <= 0 || durationMs == C.TIME_UNSET)) {
                    stopOverlapCrossfade(resetMainFade = true)
                    delay(150)
                    continue
                }

                val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
                val tooFarFromEnd = !onTarget && remainingMs > fadeMs.toLong() + 2000L
                val nextChanged = !onTarget && crossfadeTargetIndex != C.INDEX_UNSET && nextIndex != crossfadeTargetIndex
                if (tooFarFromEnd || nextChanged) {
                    stopOverlapCrossfade(resetMainFade = true)
                    delay(100)
                    continue
                }

                updateVolumes()
                delay(50)
                continue
            }

            val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
            val preloadWindowMs = fadeMs.toLong() + 1200L

            if (remainingMs in 1L..preloadWindowMs) {
                primeOverlapForNext(nextIndex)
            } else {
                unprimeOverlap()
            }

            if (overlapPrimedIndex == nextIndex && remainingMs in 1L..fadeMs.toLong()) {
                beginOverlapCrossfade(fadeMs = fadeMs, remainingMs = remainingMs)
                delay(50)
                continue
            }

            if (playbackFadeFactor.value != 1f) playbackFadeFactor.value = 1f
            delay(100)
        }
    }

    private suspend fun primeOverlapForNext(nextIndex: Int) {
        val nextItem = player.getMediaItemAt(nextIndex)
        val nextMediaId = nextItem.mediaId
        if (overlapPrimedIndex == nextIndex && overlapPrimedMediaId == nextMediaId) return

        stopOverlapCrossfade(resetMainFade = false)

        val overlap = ensureOverlapPlayer()
        overlap.clearMediaItems()
        overlap.setMediaItem(nextItem)
        overlap.prepare()
        overlap.playWhenReady = true
        overlap.volume = 0f

        overlapNormalizeFactor = fetchNormalizeFactorForMediaId(nextMediaId)
        overlapPrimedIndex = nextIndex
        overlapPrimedMediaId = nextMediaId
    }

    private fun unprimeOverlap() {
        if (crossfadeActive) return
        if (overlapPrimedIndex == C.INDEX_UNSET && overlapPrimedMediaId == null) return
        stopOverlapCrossfade(resetMainFade = false)
    }

    private fun beginOverlapCrossfade(fadeMs: Int, remainingMs: Long) {
        if (overlapPlayer == null) return
        crossfadeActive = true
        crossfadeStartElapsedMs = android.os.SystemClock.elapsedRealtime()
        crossfadeActiveDurationMs = min(fadeMs.toLong(), remainingMs).toInt().coerceAtLeast(1)
        crossfadeTargetIndex = overlapPrimedIndex
        crossfadeTargetMediaId = overlapPrimedMediaId
    }

    private fun updateVolumes() {
        val overlap = overlapPlayer ?: run {
            stopOverlapCrossfade(resetMainFade = true)
            return
        }

        val baseOverlapVolume =
            (playerVolume.value * overlapNormalizeFactor * audioFocusVolumeFactor.value).coerceIn(0f, 1f)

        if (handoffActive) {
            val nowElapsedMs = android.os.SystemClock.elapsedRealtime()
            val overlapPositionMs = overlap.currentPosition.coerceAtLeast(0L)
            val mainPositionMs = player.currentPosition.coerceAtLeast(0L)
            val positionDriftMs = mainPositionMs - overlapPositionMs
            val shouldResyncMainToOverlap =
                !handoffSeekIssued ||
                    (
                        abs(positionDriftMs) > handoffDriftCorrectionThresholdMs &&
                            nowElapsedMs - handoffLastSyncSeekElapsedMs >= handoffReseekMinIntervalMs
                        )

            if (shouldResyncMainToOverlap) {
                handoffSeekIssued = true
                handoffTargetPositionMs = overlapPositionMs
                val currentIndex = player.currentMediaItemIndex
                if (currentIndex != C.INDEX_UNSET) {
                    player.seekTo(currentIndex, handoffTargetPositionMs)
                    player.prepare()
                    handoffLastSyncSeekElapsedMs = nowElapsedMs
                }
                playbackFadeFactor.value = 0f
                overlap.volume = baseOverlapVolume
                return
            }

            if (!handoffRampStarted) {
                val bufferedMs = player.totalBufferedDuration.coerceAtLeast(0L)
                val mainStable =
                    player.playbackState == Player.STATE_READY &&
                        player.isPlaying &&
                        bufferedMs >= 1200L &&
                        abs(positionDriftMs) <= handoffRampStartDriftToleranceMs

                if (!mainStable) {
                    playbackFadeFactor.value = 0f
                    overlap.volume = baseOverlapVolume
                    return
                }

                handoffRampStarted = true
                handoffStartElapsedMs = nowElapsedMs
            }

            val denom = handoffDurationMs.toLong().coerceAtLeast(1L)
            val elapsed = (nowElapsedMs - handoffStartElapsedMs).coerceAtLeast(0L)
            val t = (elapsed.toFloat() / denom.toFloat()).coerceIn(0f, 1f)

            playbackFadeFactor.value = t
            overlap.volume = (baseOverlapVolume * (1f - t)).coerceIn(0f, 1f)

            if (t >= 1f) {
                completeHandoffFromOverlap()
            }
            return
        }

        val denom = crossfadeActiveDurationMs.toLong().coerceAtLeast(1L)
        val elapsed = (android.os.SystemClock.elapsedRealtime() - crossfadeStartElapsedMs).coerceAtLeast(0L)
        val t = (elapsed.toFloat() / denom.toFloat()).coerceIn(0f, 1f)

        val mainFactor = (1f - t).coerceIn(0f, 1f)
        val overlapFactor = t

        playbackFadeFactor.value = mainFactor
        overlap.volume = (baseOverlapVolume * overlapFactor).coerceIn(0f, 1f)
    }

    private fun handleMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!crossfadeActive) return

        val targetId = crossfadeTargetMediaId
        val newId = mediaItem?.mediaId

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !targetId.isNullOrBlank() && targetId == newId) {
            beginHandoffFromOverlap()
            return
        }

        stopOverlapCrossfade(resetMainFade = true)
    }

    private fun beginHandoffFromOverlap() {
        val overlap = overlapPlayer ?: run {
            stopOverlapCrossfade(resetMainFade = true)
            return
        }

        val overlapPositionMs = overlap.currentPosition.coerceAtLeast(0L)

        handoffActive = true
        handoffSeekIssued = false
        handoffRampStarted = false
        handoffTargetPositionMs = overlapPositionMs
        handoffStartElapsedMs = android.os.SystemClock.elapsedRealtime()
        handoffLastSyncSeekElapsedMs = 0L
        handoffDurationMs = 450
        playbackFadeFactor.value = 0f
    }

    private fun completeHandoffFromOverlap() {
        val overlap = overlapPlayer ?: run {
            stopOverlapCrossfade(resetMainFade = true)
            return
        }

        runCatching {
            overlap.volume = 0f
            overlap.stop()
            overlap.clearMediaItems()
        }

        handoffActive = false
        handoffStartElapsedMs = 0L
        handoffDurationMs = 0
        handoffTargetPositionMs = 0L
        handoffLastSyncSeekElapsedMs = 0L
        handoffSeekIssued = false
        handoffRampStarted = false

        crossfadeActive = false
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeTargetMediaId = null
        crossfadeActiveDurationMs = 0
        overlapNormalizeFactor = 1f
        overlapPrimedIndex = C.INDEX_UNSET
        overlapPrimedMediaId = null

        playbackFadeFactor.value = 1f
    }

    private fun stopOverlapCrossfade(resetMainFade: Boolean) {
        crossfadeActive = false
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeTargetMediaId = null
        crossfadeActiveDurationMs = 0
        overlapNormalizeFactor = 1f
        overlapPrimedIndex = C.INDEX_UNSET
        overlapPrimedMediaId = null
        handoffActive = false
        handoffStartElapsedMs = 0L
        handoffDurationMs = 0
        handoffTargetPositionMs = 0L
        handoffLastSyncSeekElapsedMs = 0L
        handoffSeekIssued = false
        handoffRampStarted = false

        overlapPlayer?.let { overlap ->
            runCatching {
                overlap.volume = 0f
                overlap.stop()
                overlap.clearMediaItems()
            }
        }

        if (resetMainFade) {
            playbackFadeFactor.value = 1f
        }
    }

    private fun ensureOverlapPlayer(): ExoPlayer {
        val existing = overlapPlayer
        if (existing != null) return existing
        return overlapPlayerFactory().also { overlapPlayer = it }
    }

    private suspend fun fetchNormalizeFactorForMediaId(mediaId: String): Float {
        if (!audioNormalizationEnabled.value) return 1f

        val format =
            withContext(Dispatchers.IO) {
                database.format(mediaId).first()
            }

        val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb ?: return 1f
        var factor = 10f.pow((-loudness.toFloat()) / 20f)
        if (factor > 1f) factor = min(factor, maxSafeGainFactor)
        return factor
    }
}

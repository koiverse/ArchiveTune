package moe.koiverse.archivetune.playback

import androidx.media3.common.TrackGroup
import androidx.media3.common.Timeline
import androidx.media3.common.util.Clock
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.BandwidthMeter
import com.google.common.collect.ImmutableList

internal class SafeTrackSelectionFactory(
    private val delegate: ExoTrackSelection.Factory = SafeAdaptiveTrackSelectionFactory()
) : ExoTrackSelection.Factory by delegate

private class SafeAdaptiveTrackSelectionFactory : AdaptiveTrackSelection.Factory() {
    override fun createAdaptiveTrackSelection(
        group: TrackGroup,
        tracks: IntArray,
        type: Int,
        bandwidthMeter: BandwidthMeter,
        adaptationCheckpoints: ImmutableList<AdaptiveTrackSelection.AdaptationCheckpoint>
    ): AdaptiveTrackSelection {
        return SafeAdaptiveTrackSelection(
            group = group,
            tracks = tracks,
            type = type,
            bandwidthMeter = bandwidthMeter,
            adaptationCheckpoints = adaptationCheckpoints,
        )
    }
}

private class SafeAdaptiveTrackSelection(
    group: TrackGroup,
    tracks: IntArray,
    type: Int,
    bandwidthMeter: BandwidthMeter,
    adaptationCheckpoints: ImmutableList<AdaptiveTrackSelection.AdaptationCheckpoint>,
) : AdaptiveTrackSelection(
    group,
    tracks,
    type,
    bandwidthMeter,
    AdaptiveTrackSelection.DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS.toLong(),
    AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS.toLong(),
    AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS.toLong(),
    AdaptiveTrackSelection.DEFAULT_MAX_WIDTH_TO_DISCARD,
    AdaptiveTrackSelection.DEFAULT_MAX_HEIGHT_TO_DISCARD,
    AdaptiveTrackSelection.DEFAULT_BANDWIDTH_FRACTION,
    AdaptiveTrackSelection.DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE,
    adaptationCheckpoints,
    Clock.DEFAULT,
) {
    override fun isTrackExcluded(index: Int, nowMs: Long): Boolean {
        if (index < 0 || index >= length()) {
            return false
        }
        return super.isTrackExcluded(index, nowMs)
    }

    override fun excludeTrack(index: Int, exclusionDurationMs: Long): Boolean {
        if (index < 0 || index >= length()) {
            return false
        }
        return super.excludeTrack(index, exclusionDurationMs)
    }
}

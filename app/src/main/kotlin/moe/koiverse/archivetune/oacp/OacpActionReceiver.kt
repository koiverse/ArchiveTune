package moe.koiverse.archivetune.oacp

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import moe.koiverse.archivetune.constants.MediaSessionConstants
import moe.koiverse.archivetune.playback.MusicService
import org.oacp.android.OacpParams
import org.oacp.android.OacpReceiver
import org.oacp.android.OacpResult

class OacpActionReceiver : OacpReceiver() {

    override fun onAction(
        context: Context,
        action: String,
        params: OacpParams,
        requestId: String?
    ): OacpResult? {
        return when {
            action.endsWith(".ACTION_PAUSE_MUSIC") -> {
                withMediaController(context) { controller ->
                    controller.pause()
                }
                OacpResult.success("Paused")
            }

            action.endsWith(".ACTION_NEXT_TRACK") -> {
                withMediaController(context) { controller ->
                    controller.seekToNext()
                }
                OacpResult.success("Skipped to next track")
            }

            action.endsWith(".ACTION_PREVIOUS_TRACK") -> {
                withMediaController(context) { controller ->
                    controller.seekToPrevious()
                }
                OacpResult.success("Going to previous track")
            }

            action.endsWith(".ACTION_TOGGLE_SHUFFLE") -> {
                withMediaController(context) { controller ->
                    controller.sendCustomCommand(
                        MediaSessionConstants.CommandToggleShuffle,
                        android.os.Bundle.EMPTY
                    )
                }
                OacpResult.success("Toggled shuffle")
            }

            action.endsWith(".ACTION_TOGGLE_REPEAT") -> {
                withMediaController(context) { controller ->
                    controller.sendCustomCommand(
                        MediaSessionConstants.CommandToggleRepeatMode,
                        android.os.Bundle.EMPTY
                    )
                }
                OacpResult.success("Toggled repeat mode")
            }

            action.endsWith(".ACTION_TOGGLE_LIKE") -> {
                withMediaController(context) { controller ->
                    controller.sendCustomCommand(
                        MediaSessionConstants.CommandToggleLike,
                        android.os.Bundle.EMPTY
                    )
                }
                OacpResult.success("Toggled like")
            }

            else -> null
        }
    }

    private fun withMediaController(context: Context, block: (MediaController) -> Unit) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        Futures.addCallback(
            future,
            object : com.google.common.util.concurrent.FutureCallback<MediaController> {
                override fun onSuccess(controller: MediaController) {
                    block(controller)
                }

                override fun onFailure(t: Throwable) {
                    // Service not running — nothing to control
                }
            },
            MoreExecutors.directExecutor()
        )
    }
}

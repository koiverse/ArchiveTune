/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import moe.koiverse.archivetune.playback.MusicService

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("MusicWidgetActions", "PlayPauseAction triggered")
        sendWidgetAction(context, ACTION_PLAY_PAUSE)
    }
}

class SkipNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("MusicWidgetActions", "SkipNextAction triggered")
        sendWidgetAction(context, ACTION_SKIP_NEXT)
    }
}

class SkipPrevAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("MusicWidgetActions", "SkipPrevAction triggered")
        sendWidgetAction(context, ACTION_SKIP_PREV)
    }
}

// ── Helper function using custom broadcast intents ────────────────────────────

private const val ACTION_PLAY_PAUSE = "moe.koiverse.archivetune.WIDGET_PLAY_PAUSE"
private const val ACTION_SKIP_NEXT = "moe.koiverse.archivetune.WIDGET_SKIP_NEXT"
private const val ACTION_SKIP_PREV = "moe.koiverse.archivetune.WIDGET_SKIP_PREV"

private fun sendWidgetAction(context: Context, action: String) {
    val intent = Intent(action).apply {
        component = ComponentName(context, MusicService::class.java)
    }
    try {
        context.startService(intent)
        Log.d("MusicWidgetActions", "Sent widget action: $action")
    } catch (e: Exception) {
        Log.e("MusicWidgetActions", "Failed to send widget action: $action", e)
    }
}

// Made with Bob

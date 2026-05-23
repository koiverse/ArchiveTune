/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.playback.MusicService
import java.util.concurrent.TimeUnit

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MusicWidget()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Try to get current state from the service when widget is first added
        scope.launch {
            tryUpdateFromService(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Also try to update when widget is updated
        scope.launch {
            tryUpdateFromService(context)
        }
    }

    private suspend fun tryUpdateFromService(context: Context) {
        try {
            val token = SessionToken(
                context,
                ComponentName(context, MusicService::class.java)
            )
            val future = MediaController.Builder(context, token).buildAsync()
            val controller = kotlinx.coroutines.withContext(Dispatchers.IO) {
                future.get(2, TimeUnit.SECONDS)
            }
            
            // If we got a controller, the service is running
            // Get the service instance and trigger widget update
            val serviceIntent = android.content.Intent(context, MusicService::class.java)
            context.bindService(
                serviceIntent,
                object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: android.os.IBinder?) {
                        val service = (binder as? MusicService.MusicBinder)?.service
                        service?.updateWidget()
                        context.unbindService(this)
                    }
                    override fun onServiceDisconnected(name: ComponentName?) {}
                },
                Context.BIND_AUTO_CREATE
            )
            
            controller.release()
        } catch (e: Exception) {
            // Service not running or no media loaded - widget will show default state
        }
    }
}

// Made with Bob

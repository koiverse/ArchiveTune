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
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.playback.MusicService
import java.util.concurrent.TimeUnit

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MusicWidget()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scope.launch {
            tryUpdateFromService(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scope.launch {
            tryUpdateFromService(context)
        }
    }

    private suspend fun tryUpdateFromService(context: Context) {
        try {
            val token = SessionToken(
                context,
                ComponentName(context, MusicService::class.java),
            )
            val future = MediaController.Builder(context, token).buildAsync()
            val controller = withContext(Dispatchers.IO) {
                future.get(2, TimeUnit.SECONDS)
            }

            try {
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
                    Context.BIND_AUTO_CREATE,
                )
            } finally {
                controller.release()
            }
        } catch (e: Exception) {
        }
    }
}

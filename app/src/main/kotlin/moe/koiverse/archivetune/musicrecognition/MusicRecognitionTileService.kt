/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.musicrecognition

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import moe.koiverse.archivetune.MainActivity
import moe.koiverse.archivetune.R

class MusicRecognitionTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val launchIntent =
            Intent(this, MainActivity::class.java).apply {
                action = ACTION_MUSIC_RECOGNITION
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent)
        }
    }

    private fun updateTile() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.music_recognition)
            icon = Icon.createWithResource(this@MusicRecognitionTileService, R.drawable.mic)
            updateTile()
        }
    }
}

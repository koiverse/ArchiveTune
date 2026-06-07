/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object MusicWidgetKeys {
    val TRACK_TITLE = stringPreferencesKey("widget_track_title")
    val TRACK_ARTIST = stringPreferencesKey("widget_track_artist")
    val ART_PATH = stringPreferencesKey("widget_art_path")
    val IS_PLAYING = booleanPreferencesKey("widget_is_playing")
    val IS_AVAILABLE = booleanPreferencesKey("widget_is_available")
    val DOMINANT_COLOR = intPreferencesKey("widget_dominant_color")
    val PLAYBACK_POSITION = floatPreferencesKey("widget_position")
}

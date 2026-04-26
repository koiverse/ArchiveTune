/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.lyrics.LyricsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.paxsenix.PaxsenixLyrics
import moe.koiverse.archivetune.paxsenix.models.PaxsenixStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ContentSettingsViewModel @Inject constructor(
    private val lyricsHelper: LyricsHelper,
    private val database: MusicDatabase,
) : ViewModel() {

    private val _paxsenixStats = MutableStateFlow<PaxsenixStats?>(null)
    val paxsenixStats = _paxsenixStats.asStateFlow()

    fun fetchPaxsenixStats() {
        viewModelScope.launch(Dispatchers.IO) {
            PaxsenixLyrics.getStats().onSuccess {
                _paxsenixStats.value = it
            }
        }
    }

    fun clearLyricsCache() {
        viewModelScope.launch(Dispatchers.IO) {
            lyricsHelper.clearCache()
            database.query {
                clearAllLyrics()
            }
        }
    }
}

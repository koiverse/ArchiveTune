/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.Cache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.di.DownloadCache
import moe.koiverse.archivetune.di.PlayerCache
import moe.koiverse.archivetune.utils.AudioExporter
import moe.koiverse.archivetune.utils.ExportConfig
import moe.koiverse.archivetune.utils.ExportResult
import javax.inject.Inject

enum class ExportSource { ALL, DOWNLOAD, CACHE }

data class ExportableSong(
    val song: Song,
    val source: SongSource,
    val isSelected: Boolean = false,
    val fileSize: Long = 0L,
) {
    enum class SongSource { DOWNLOAD, CACHE }
}

enum class ExportState { IDLE, EXPORTING, COMPLETE }

data class ExportProgress(
    val state: ExportState = ExportState.IDLE,
    val total: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val currentSongName: String = "",
    val currentBytesWritten: Long = 0,
    val currentTotalBytes: Long = 0,
    val results: List<ExportResult> = emptyList(),
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @DownloadCache private val downloadCache: Cache,
    @PlayerCache private val playerCache: Cache,
) : ViewModel() {

    private val _exportableSongs = MutableStateFlow<List<ExportableSong>>(emptyList())
    val exportableSongs: StateFlow<List<ExportableSong>> = _exportableSongs.asStateFlow()

    private val _exportConfig = MutableStateFlow(ExportConfig())
    val exportConfig: StateFlow<ExportConfig> = _exportConfig.asStateFlow()

    private val _exportProgress = MutableStateFlow(ExportProgress())
    val exportProgress: StateFlow<ExportProgress> = _exportProgress.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sourceFilter = MutableStateFlow(ExportSource.ALL)
    val sourceFilter: StateFlow<ExportSource> = _sourceFilter.asStateFlow()

    private var exportJob: Job? = null
    private var allSongs: List<ExportableSong> = emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshSongList()
                delay(2000)
            }
        }
    }

    private suspend fun refreshSongList() {
        val downloadedIds = downloadCache.keys.toSet()
        val cachedIds = playerCache.keys.toSet()
        val pureCacheIds = cachedIds.subtract(downloadedIds)

        val allIds = downloadedIds + pureCacheIds
        if (allIds.isEmpty()) {
            allSongs = emptyList()
            applyFilters()
            return
        }

        val songs = database.getSongsByIds(allIds.toList())

        val exportable = songs.mapNotNull { song ->
            val format = song.format ?: return@mapNotNull null
            val contentLength = format.contentLength
            if (contentLength <= 0) return@mapNotNull null

            val isDownloaded = song.song.id in downloadedIds &&
                downloadCache.isCached(song.song.id, 0, contentLength)
            val isCached = song.song.id in pureCacheIds &&
                playerCache.isCached(song.song.id, 0, contentLength)

            if (!isDownloaded && !isCached) return@mapNotNull null

            val source = if (isDownloaded) ExportableSong.SongSource.DOWNLOAD
            else ExportableSong.SongSource.CACHE

            val existing = allSongs.find { it.song.id == song.id }

            ExportableSong(
                song = song,
                source = source,
                isSelected = existing?.isSelected ?: false,
                fileSize = contentLength,
            )
        }

        allSongs = exportable
        applyFilters()
    }

    private fun applyFilters() {
        val query = _searchQuery.value.lowercase()
        val source = _sourceFilter.value

        _exportableSongs.value = allSongs.filter { item ->
            val matchesSource = when (source) {
                ExportSource.ALL -> true
                ExportSource.DOWNLOAD -> item.source == ExportableSong.SongSource.DOWNLOAD
                ExportSource.CACHE -> item.source == ExportableSong.SongSource.CACHE
            }

            val matchesQuery = query.isBlank() ||
                item.song.song.title.lowercase().contains(query) ||
                item.song.artists.any { it.name.lowercase().contains(query) } ||
                (item.song.song.albumName?.lowercase()?.contains(query) == true)

            matchesSource && matchesQuery
        }.sortedByDescending { it.song.song.dateDownload }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setSourceFilter(source: ExportSource) {
        _sourceFilter.value = source
        applyFilters()
    }

    fun toggleSelection(songId: String) {
        allSongs = allSongs.map {
            if (it.song.id == songId) it.copy(isSelected = !it.isSelected) else it
        }
        applyFilters()
    }

    fun selectAll() {
        val visibleIds = _exportableSongs.value.map { it.song.id }.toSet()
        allSongs = allSongs.map {
            if (it.song.id in visibleIds) it.copy(isSelected = true) else it
        }
        applyFilters()
    }

    fun deselectAll() {
        allSongs = allSongs.map { it.copy(isSelected = false) }
        applyFilters()
    }

    val selectedCount: Int
        get() = allSongs.count { it.isSelected }

    val selectedTotalSize: Long
        get() = allSongs.filter { it.isSelected }.sumOf { it.fileSize }

    fun updateConfig(config: ExportConfig) {
        _exportConfig.value = config
    }

    fun startExport() {
        val selected = allSongs.filter { it.isSelected }
        if (selected.isEmpty()) return

        exportJob?.cancel()
        exportJob = viewModelScope.launch(Dispatchers.IO) {
            val config = _exportConfig.value
            val results = mutableListOf<ExportResult>()

            _exportProgress.value = ExportProgress(
                state = ExportState.EXPORTING,
                total = selected.size,
            )

            for ((index, item) in selected.withIndex()) {
                if (!isActive) break

                _exportProgress.value = _exportProgress.value.copy(
                    currentSongName = item.song.song.title,
                    currentBytesWritten = 0,
                    currentTotalBytes = item.fileSize,
                )

                val result = AudioExporter.exportSong(
                    context = context,
                    song = item.song,
                    downloadCache = downloadCache,
                    playerCache = playerCache,
                    config = config,
                ) { bytesWritten, totalBytes ->
                    _exportProgress.value = _exportProgress.value.copy(
                        currentBytesWritten = bytesWritten,
                        currentTotalBytes = totalBytes,
                    )
                }

                results.add(result)

                val successCount = results.count { it is ExportResult.Success }
                val failCount = results.count { it is ExportResult.Failed }
                val skipCount = results.count { it is ExportResult.Skipped }

                _exportProgress.value = _exportProgress.value.copy(
                    completed = index + 1,
                    failed = failCount,
                    skipped = skipCount,
                    results = results.toList(),
                )
            }

            val successCount = results.count { it is ExportResult.Success }
            val failCount = results.count { it is ExportResult.Failed }
            val skipCount = results.count { it is ExportResult.Skipped }

            _exportProgress.value = ExportProgress(
                state = ExportState.COMPLETE,
                total = selected.size,
                completed = successCount,
                failed = failCount,
                skipped = skipCount,
                results = results.toList(),
            )
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _exportProgress.value = _exportProgress.value.copy(
            state = ExportState.COMPLETE,
        )
    }

    fun resetExport() {
        _exportProgress.value = ExportProgress()
    }
}

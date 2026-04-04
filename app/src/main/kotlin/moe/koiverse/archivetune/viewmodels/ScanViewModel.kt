package moe.koiverse.archivetune.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import moe.koiverse.archivetune.constants.ExcludedScanFoldersKey
import moe.koiverse.archivetune.constants.LastScanTimeKey
import moe.koiverse.archivetune.constants.LocalScanCompletedKey
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.utils.LocalMediaScanner
import moe.koiverse.archivetune.utils.ScanResult
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.get
import javax.inject.Inject

sealed interface ScanState {
    data object Idle : ScanState
    data object PermissionRequired : ScanState
    data class Scanning(val progress: Int, val total: Int) : ScanState
    data class Complete(val result: ScanResult) : ScanState
    data class Error(val message: String) : ScanState
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    private val scanner = LocalMediaScanner(context, database)

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val localSongCount: StateFlow<Int> = database.localSongCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _lastScanTime = MutableStateFlow(context.dataStore.get(LastScanTimeKey, 0L))
    val lastScanTime: StateFlow<Long> = _lastScanTime.asStateFlow()

    private val _excludedFolders = MutableStateFlow(loadExcludedFolders())
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun checkPermissionAndSetState() {
        if (!hasAudioPermission()) {
            _scanState.value = ScanState.PermissionRequired
        } else if (_scanState.value is ScanState.PermissionRequired) {
            _scanState.value = ScanState.Idle
        }
    }

    fun startMediaStoreScan() {
        if (_scanState.value is ScanState.Scanning) return
        viewModelScope.launch(Dispatchers.IO) {
            _scanState.value = ScanState.Scanning(0, 0)
            try {
                val result = scanner.scanMediaStore(
                    excludedFolders = _excludedFolders.value,
                    onProgress = { current, total ->
                        _scanState.value = ScanState.Scanning(current, total)
                    },
                )
                val now = System.currentTimeMillis()
                context.dataStore.edit { it[LastScanTimeKey] = now }
                context.dataStore.edit { it[LocalScanCompletedKey] = true }
                _lastScanTime.value = now
                _scanState.value = ScanState.Complete(result)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun startSafFolderScan(treeUri: Uri) {
        if (_scanState.value is ScanState.Scanning) return
        viewModelScope.launch(Dispatchers.IO) {
            _scanState.value = ScanState.Scanning(0, 0)
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                val result = scanner.scanSafFolder(
                    treeUri = treeUri,
                    excludedFolders = _excludedFolders.value,
                    onProgress = { current, total ->
                        _scanState.value = ScanState.Scanning(current, total)
                    },
                )
                val now = System.currentTimeMillis()
                context.dataStore.edit { it[LastScanTimeKey] = now }
                context.dataStore.edit { it[LocalScanCompletedKey] = true }
                _lastScanTime.value = now
                _scanState.value = ScanState.Complete(result)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addExcludedFolder(path: String) {
        val updated = _excludedFolders.value + path
        _excludedFolders.value = updated
        saveExcludedFolders(updated)
    }

    fun removeExcludedFolder(path: String) {
        val updated = _excludedFolders.value - path
        _excludedFolders.value = updated
        saveExcludedFolders(updated)
    }

    fun resetState() {
        if (_scanState.value !is ScanState.Scanning) {
            _scanState.value = ScanState.Idle
        }
    }

    private fun loadExcludedFolders(): Set<String> {
        val raw = context.dataStore.get(ExcludedScanFoldersKey, "")
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    private fun saveExcludedFolders(folders: Set<String>) {
        viewModelScope.launch {
            context.dataStore.edit { it[ExcludedScanFoldersKey] = folders.joinToString("\n") }
        }
    }
}

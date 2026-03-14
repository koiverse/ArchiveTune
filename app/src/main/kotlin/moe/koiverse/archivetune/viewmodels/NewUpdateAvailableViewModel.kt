package moe.koiverse.archivetune.viewmodels

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.utils.Updater
import moe.koiverse.archivetune.utils.ReleaseInfo
import javax.inject.Inject

@HiltViewModel
class NewUpdateAvailableViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val appName = context.getString(R.string.app_name)

    private val _latestVersion = MutableStateFlow<ReleaseInfo?>(null)
    val latestVersion = _latestVersion.asStateFlow()

    private val _downloadProgress = MutableStateFlow(-1f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadManager.STATUS_PENDING)
    val downloadState = _downloadState.asStateFlow()

    private val _installIntent = MutableStateFlow<Intent?>(null)
    val installIntent = _installIntent.asStateFlow()

    private val _onDownloadDone = MutableSharedFlow<Intent>()
    val onDownloadDone = _onDownloadDone.asSharedFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            Updater.getLatestReleaseInfo().onSuccess { releaseInfo ->
                _latestVersion.update { releaseInfo }
            }.onFailure { exception ->
                _error.update { exception.message }
            }
        }
    }

    fun startDownload() {
        val version = _latestVersion.value ?: return
        _isDownloading.update { true }
        _error.update { null }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = Updater.getLatestDownloadUrl(version).toUri()
                val request = DownloadManager.Request(url)
                    .setTitle("$appName v${version.name}")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.lastPathSegment)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setMimeType("application/vnd.android.package-archive")
                val downloadId = downloadManager.enqueue(request)
                observeDownload(downloadId)
            } catch (e: Exception) {
                _error.update { e.message }
                _isDownloading.update { false }
            }
        }
    }

    fun onDownloadComplete(intent: Intent) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
            if (downloadId == 0L) {
                return@launch
            }
            @Suppress("DEPRECATION")
            val installerIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            installerIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            installerIntent.setDataAndType(
                downloadManager.getUriForDownloadedFile(downloadId),
                downloadManager.getMimeTypeForDownloadedFile(downloadId),
            )
            installerIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            _installIntent.update { installerIntent }
            _onDownloadDone.emit(installerIntent)
        }
    }

    private suspend fun observeDownload(id: Long) {
        val query = DownloadManager.Query()
        query.setFilterById(id)
        while (currentCoroutineContext().isActive) {
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                    )
                    val bytesTotal = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                    )
                    _downloadProgress.update { bytesDownloaded.toFloat() / bytesTotal }
                    val state = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    _downloadState.update { state }
                    if (state == DownloadManager.STATUS_SUCCESSFUL || state == DownloadManager.STATUS_FAILED) {
                        _isDownloading.update { false }
                        if (state == DownloadManager.STATUS_FAILED) {
                            _error.update { context.getString(R.string.download_failed) }
                        }
                        return
                    }
                }
            }
            delay(100)
        }
    }
}

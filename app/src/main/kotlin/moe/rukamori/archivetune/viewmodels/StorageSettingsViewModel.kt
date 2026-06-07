/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.viewmodels

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.storage.ObserveStorageFoldersUseCase
import moe.rukamori.archivetune.storage.ResetStorageFolderUseCase
import moe.rukamori.archivetune.storage.SetStorageFolderUseCase
import moe.rukamori.archivetune.storage.StorageFolderSelection
import moe.rukamori.archivetune.storage.StorageFolderUpdateResult
import javax.inject.Inject

sealed interface StorageSettingsScreenState {
    data object Loading : StorageSettingsScreenState
    data class Success(val model: StorageSettingsUiModel) : StorageSettingsScreenState
    data object Empty : StorageSettingsScreenState
    data class Error(val messageResId: Int) : StorageSettingsScreenState
}

@Immutable
data class StorageSettingsUiModel(
    val folder: StorageFolderUiModel,
)

@Immutable
data class StorageFolderUiModel(
    val displayName: String,
    val isCustom: Boolean,
)

@Immutable
data class StorageSettingsEffect(
    val messageResId: Int,
    val restartApp: Boolean,
)

@HiltViewModel
class StorageSettingsViewModel
@Inject
constructor(
    observeStorageFolders: ObserveStorageFoldersUseCase,
    private val setStorageFolder: SetStorageFolderUseCase,
    private val resetStorageFolder: ResetStorageFolderUseCase,
) : ViewModel() {
    private val _effects = MutableSharedFlow<StorageSettingsEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    val state: StateFlow<StorageSettingsScreenState> =
        observeStorageFolders()
            .map<StorageFolderSelection, StorageSettingsScreenState> { selection ->
                StorageSettingsScreenState.Success(
                    StorageSettingsUiModel(
                        folder = selection.toUiModel(),
                    ),
                )
            }
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                emit(StorageSettingsScreenState.Error(R.string.error_unknown))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StorageSettingsScreenState.Loading,
            )

    fun selectFolder(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = withContext(NonCancellable + Dispatchers.IO) {
                setStorageFolder(uri)
            }
            val messageResId = when (result) {
                StorageFolderUpdateResult.Success -> R.string.storage_folder_selected_restart
                StorageFolderUpdateResult.InvalidTree -> R.string.storage_folder_invalid
                StorageFolderUpdateResult.UnsupportedProvider -> R.string.storage_folder_unsupported
                StorageFolderUpdateResult.NotWritable -> R.string.storage_folder_not_writable
            }
            _effects.emit(
                StorageSettingsEffect(
                    messageResId = messageResId,
                    restartApp = result == StorageFolderUpdateResult.Success,
                ),
            )
        }
    }

    fun resetFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = withContext(NonCancellable + Dispatchers.IO) {
                resetStorageFolder()
            }
            val messageResId = when (result) {
                StorageFolderUpdateResult.Success -> R.string.storage_folder_reset_restart
                else -> R.string.storage_folder_not_writable
            }
            _effects.emit(
                StorageSettingsEffect(
                    messageResId = messageResId,
                    restartApp = result == StorageFolderUpdateResult.Success,
                ),
            )
        }
    }

    private fun StorageFolderSelection.toUiModel(): StorageFolderUiModel =
        StorageFolderUiModel(
            displayName = displayName,
            isCustom = isCustom,
        )
}

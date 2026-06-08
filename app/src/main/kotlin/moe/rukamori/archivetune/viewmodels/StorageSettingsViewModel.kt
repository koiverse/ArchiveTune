/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.viewmodels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.storage.ObserveStorageFoldersUseCase
import moe.rukamori.archivetune.storage.SetStorageFolderUseCase
import moe.rukamori.archivetune.storage.StorageFolderSelection
import moe.rukamori.archivetune.storage.StorageFolderUpdateResult
import moe.rukamori.archivetune.storage.StorageLocationKind
import moe.rukamori.archivetune.storage.StorageLocationOption
import moe.rukamori.archivetune.storage.StorageLocationOptions
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
    val storageOptions: StorageLocationUiOptions,
    val picker: StorageLocationPickerUiModel,
)

@Immutable
data class StorageFolderUiModel(
    val selectedOptionId: String,
    val kind: StorageLocationKind,
    val volumeLabel: String?,
    val availableBytes: Long,
)

@Immutable
data class StorageLocationUiOptions(
    private val values: List<StorageLocationUiModel>,
) {
    val size: Int get() = values.size

    operator fun get(index: Int): StorageLocationUiModel = values[index]

    fun firstOrNull(predicate: (StorageLocationUiModel) -> Boolean): StorageLocationUiModel? =
        values.firstOrNull(predicate)

    fun forEach(action: (StorageLocationUiModel) -> Unit) {
        values.forEach(action)
    }
}

@Immutable
data class StorageLocationUiModel(
    val id: String,
    val kind: StorageLocationKind,
    val volumeLabel: String?,
    val availableBytes: Long,
    val isSelected: Boolean,
)

@Immutable
data class StorageLocationPickerUiModel(
    val visible: Boolean = false,
    val selectedOptionId: String? = null,
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
) : ViewModel() {
    private val _effects = MutableSharedFlow<StorageSettingsEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()
    private val pickerState = MutableStateFlow(StorageLocationPickerUiModel())

    val state: StateFlow<StorageSettingsScreenState> =
        combine(
            observeStorageFolders(),
            pickerState,
        ) { selection, picker ->
            val selectedOptionId = picker.selectedOptionId
                ?.takeIf { optionId ->
                    selection.options.firstOrNull { option -> option.id == optionId } != null
                }
                ?: selection.selectedOption.id
            val normalizedPicker = picker.copy(selectedOptionId = selectedOptionId)
            selection to normalizedPicker
        }
            .map<Pair<StorageFolderSelection, StorageLocationPickerUiModel>, StorageSettingsScreenState> { (selection, picker) ->
                StorageSettingsScreenState.Success(
                    StorageSettingsUiModel(
                        folder = selection.toUiModel(),
                        storageOptions = selection.options.toUiOptions(),
                        picker = picker,
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

    fun openStorageLocationPicker() {
        val selectedOptionId = (state.value as? StorageSettingsScreenState.Success)
            ?.model
            ?.folder
            ?.selectedOptionId
            ?: return
        pickerState.value = StorageLocationPickerUiModel(
            visible = true,
            selectedOptionId = selectedOptionId,
        )
    }

    fun chooseStorageLocation(optionId: String) {
        pickerState.update { picker ->
            picker.copy(selectedOptionId = optionId)
        }
    }

    fun dismissStorageLocationPicker() {
        pickerState.update { picker ->
            picker.copy(visible = false)
        }
    }

    fun applyStorageLocationSelection() {
        val model = (state.value as? StorageSettingsScreenState.Success)?.model ?: return
        val optionId = model.picker.selectedOptionId ?: model.folder.selectedOptionId
        pickerState.update { picker ->
            picker.copy(visible = false)
        }
        selectStorageLocation(optionId)
    }

    private fun selectStorageLocation(optionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = withContext(NonCancellable + Dispatchers.IO) {
                setStorageFolder(optionId)
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

    private fun StorageFolderSelection.toUiModel(): StorageFolderUiModel =
        StorageFolderUiModel(
            selectedOptionId = selectedOption.id,
            kind = selectedOption.kind,
            volumeLabel = selectedOption.volumeLabel,
            availableBytes = selectedOption.availableBytes,
        )

    private fun StorageLocationOptions.toUiOptions(): StorageLocationUiOptions {
        val items = mutableListOf<StorageLocationUiModel>()
        forEach { option ->
            items += option.toUiModel()
        }
        return StorageLocationUiOptions(items)
    }

    private fun StorageLocationOption.toUiModel(): StorageLocationUiModel =
        StorageLocationUiModel(
            id = id,
            kind = kind,
            volumeLabel = volumeLabel,
            availableBytes = availableBytes,
            isSelected = isSelected,
        )
}

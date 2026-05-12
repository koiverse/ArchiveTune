/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.screens.settings

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AppIconKey
import moe.koiverse.archivetune.utils.AppIconManager
import moe.koiverse.archivetune.utils.PreferenceStore
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.getAsync
import javax.inject.Inject

@Immutable
data class AppIconItem(
    val aliasSuffix: String,
    val displayName: String,
    val author: String?,
    val assetPath: String?,
)

sealed interface IconChangerUiState {
    data object Loading : IconChangerUiState

    @Immutable
    data class Success(
        val icons: List<AppIconItem>,
        val currentAlias: String,
        val pendingSelection: AppIconItem?,
    ) : IconChangerUiState
}

@HiltViewModel
class IconChangerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<IconChangerUiState>(IconChangerUiState.Loading)
    val uiState: StateFlow<IconChangerUiState> = _uiState.asStateFlow()

    init {
        loadIcons()
    }

    private fun loadIcons() {
        viewModelScope.launch(Dispatchers.IO) {
            val assetFiles = runCatching {
                context.assets.list("AppIcon")
                    ?.filter { it.endsWith(".png") }
                    ?: emptyList()
            }.getOrDefault(emptyList())

            val assetItems = assetFiles.sortedBy { it }.map { filename ->
                val nameWithoutExt = filename.removeSuffix(".png")
                val parts = nameWithoutExt.split("_", limit = 2)
                AppIconItem(
                    aliasSuffix = parts[0],
                    displayName = parts[0],
                    author = parts.getOrNull(1),
                    assetPath = filename,
                )
            }

            val savedAlias = context.dataStore.getAsync(AppIconKey)
                ?: AppIconManager.resolveActiveAlias(context)

            val defaultLabel = context.getString(R.string.icon_change_default_name)
            val allIcons = buildList {
                add(
                    AppIconItem(
                        aliasSuffix = "Default",
                        displayName = defaultLabel,
                        author = null,
                        assetPath = null,
                    )
                )
                addAll(assetItems)
            }

            _uiState.value = IconChangerUiState.Success(
                icons = allIcons,
                currentAlias = savedAlias,
                pendingSelection = null,
            )
        }
    }

    fun setPendingSelection(item: AppIconItem) {
        val current = _uiState.value as? IconChangerUiState.Success ?: return
        _uiState.value = current.copy(pendingSelection = item)
    }

    fun clearPendingSelection() {
        val current = _uiState.value as? IconChangerUiState.Success ?: return
        _uiState.value = current.copy(pendingSelection = null)
    }

    fun confirmIconChange(item: AppIconItem) {
        val current = _uiState.value as? IconChangerUiState.Success ?: return
        _uiState.value = current.copy(pendingSelection = null, currentAlias = item.aliasSuffix)
        PreferenceStore.launchEdit(context.dataStore) {
            this[AppIconKey] = item.aliasSuffix
        }
        viewModelScope.launch(Dispatchers.IO) {
            AppIconManager.switchIcon(context, item.aliasSuffix)
        }
    }
}

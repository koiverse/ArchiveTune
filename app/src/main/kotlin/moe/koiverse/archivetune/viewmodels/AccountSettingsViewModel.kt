/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.viewmodels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.utils.RemoteHistorySyncManager
import moe.koiverse.archivetune.utils.RemoteHistorySyncResult

sealed interface AccountHistorySyncUiState {
    @Immutable
    data object Idle : AccountHistorySyncUiState

    @Immutable
    data class Running(
        val completed: Int,
        val total: Int,
    ) : AccountHistorySyncUiState

    @Immutable
    data class Finished(
        val result: RemoteHistorySyncResult,
    ) : AccountHistorySyncUiState
}

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val remoteHistorySyncManager: RemoteHistorySyncManager,
) : ViewModel() {
    private val _historySyncUiState = MutableStateFlow<AccountHistorySyncUiState>(AccountHistorySyncUiState.Idle)
    val historySyncUiState: StateFlow<AccountHistorySyncUiState> = _historySyncUiState.asStateFlow()

    fun forceSyncLocalHistory() {
        if (_historySyncUiState.value is AccountHistorySyncUiState.Running) return

        viewModelScope.launch {
            val result = remoteHistorySyncManager.forceSyncLocalHistory { completed, total ->
                _historySyncUiState.value = AccountHistorySyncUiState.Running(
                    completed = completed,
                    total = total,
                )
            }

            _historySyncUiState.value = AccountHistorySyncUiState.Finished(result)
        }
    }
}
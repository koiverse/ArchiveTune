/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.spotifyimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.utils.reportException
import javax.inject.Inject

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(
    private val repository: SpotifyImportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpotifyImportUiState(isLoading = true))
    val uiState: StateFlow<SpotifyImportUiState> = _uiState.asStateFlow()

    private var sources: List<SpotifyImportSource> = emptyList()

    init {
        restoreSession()
    }

    fun restoreSession() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.restoreSession() }
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            isAuthenticated = session.isAuthenticated,
                            accountName = session.accountName,
                            accountAvatarUrl = session.accountAvatarUrl,
                            isLoading = false,
                        )
                    }
                    if (session.isAuthenticated) {
                        loadSources()
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    reportException(error)
                    _uiState.update {
                        it.copy(
                            isAuthenticated = false,
                            isLoading = false,
                            errorMessage = error.message,
                        )
                    }
                }
        }
    }

    fun connectWithCookies(
        spDc: String,
        spKey: String,
    ) {
        if (spDc.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.connectWithCookies(spDc = spDc, spKey = spKey) }
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            accountName = session.accountName,
                            accountAvatarUrl = session.accountAvatarUrl,
                            isLoading = false,
                        )
                    }
                    loadSources()
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    reportException(error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                        )
                    }
                }
        }
    }

    fun loadSources() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadSources() }
                .onSuccess { loadedSources ->
                    sources = loadedSources
                    val selectedIds = loadedSources.mapTo(LinkedHashSet()) { it.id }
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            sources = loadedSources.map(SpotifyImportSource::toUi),
                            selectedSourceIds = selectedIds,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    reportException(error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                        )
                    }
                }
        }
    }

    fun toggleSource(sourceId: String) {
        _uiState.update { state ->
            val selected =
                if (sourceId in state.selectedSourceIds) {
                    state.selectedSourceIds - sourceId
                } else {
                    state.selectedSourceIds + sourceId
                }
            state.copy(selectedSourceIds = selected)
        }
    }

    fun selectAllSources() {
        _uiState.update { state ->
            state.copy(selectedSourceIds = state.sources.mapTo(LinkedHashSet()) { it.id })
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedSourceIds = emptySet()) }
    }

    fun logout() {
        if (uiState.value.progress != null) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.logout() }
                .onSuccess {
                    sources = emptyList()
                    _uiState.update { SpotifyImportUiState() }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    reportException(error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                        )
                    }
                }
        }
    }

    fun importSelectedSources() {
        val selectedIds = uiState.value.selectedSourceIds
        if (selectedIds.isEmpty() || uiState.value.progress != null) return
        val selectedSources = sources.filter { it.id in selectedIds }
        if (selectedSources.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(summary = null, errorMessage = null) }
            runCatching {
                repository.importSources(selectedSources) { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
            }.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        progress = null,
                        summary = summary,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                reportException(error)
                _uiState.update {
                    it.copy(
                        progress = null,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun dismissSummary() {
        _uiState.update { it.copy(summary = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

private fun SpotifyImportSource.toUi(): SpotifyImportSourceUi =
    SpotifyImportSourceUi(
        id = id,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        trackCount = trackCount,
        type = type,
    )

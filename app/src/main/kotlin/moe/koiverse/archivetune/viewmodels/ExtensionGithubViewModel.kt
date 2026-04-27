package moe.koiverse.archivetune.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.extensions.system.ExtensionManager
import moe.koiverse.archivetune.extensions.system.GithubExtensionRepository
import moe.koiverse.archivetune.extensions.system.GithubExtensionSource
import moe.koiverse.archivetune.extensions.system.GithubExtensionStore
import javax.inject.Inject

sealed interface GithubImportState {
    data object Idle : GithubImportState
    data object Validating : GithubImportState
    data object Downloading : GithubImportState
    data class Success(val extensionId: String, val tag: String) : GithubImportState
    data class Error(val message: String) : GithubImportState
}

sealed interface GithubUpdateState {
    data object Idle : GithubUpdateState
    data object Checking : GithubUpdateState
    data class Updating(val extensionId: String) : GithubUpdateState
    data class Done(val updated: Int, val failed: Int) : GithubUpdateState
    data class Error(val message: String) : GithubUpdateState
}

@HiltViewModel
class ExtensionGithubViewModel @Inject constructor(
    application: Application,
    private val extensionManager: ExtensionManager,
) : AndroidViewModel(application) {

    private val repo = GithubExtensionRepository(application)
    private val store = GithubExtensionStore(application)

    val sources: StateFlow<List<GithubExtensionSource>> = store.sourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow<GithubImportState>(GithubImportState.Idle)
    val importState: StateFlow<GithubImportState> = _importState.asStateFlow()

    private val _updateState = MutableStateFlow<GithubUpdateState>(GithubUpdateState.Idle)
    val updateState: StateFlow<GithubUpdateState> = _updateState.asStateFlow()

    fun resetImportState() {
        _importState.value = GithubImportState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = GithubUpdateState.Idle
    }

    /**
     * Parses input and returns (owner, repo) or null if invalid.
     */
    fun parseInput(input: String): Pair<String, String>? = repo.parseOwnerRepo(input)

    /**
     * Installs an extension from a GitHub URL or owner/repo string.
     */
    fun installFromGithub(input: String, assetPattern: String? = null) {
        viewModelScope.launch {
            _importState.value = GithubImportState.Validating
            val parsed = repo.parseOwnerRepo(input)
            if (parsed == null) {
                _importState.value = GithubImportState.Error("Invalid GitHub URL or owner/repo format")
                return@launch
            }
            val (owner, repoName) = parsed
            _importState.value = GithubImportState.Downloading
            val result = repo.installFromGithub(owner, repoName, assetPattern, extensionManager)
            result.fold(
                onSuccess = { (id, release) ->
                    // Persist the source for future update tracking
                    store.upsert(
                        GithubExtensionSource(
                            extensionId = id,
                            owner = owner,
                            repo = repoName,
                            assetPattern = assetPattern,
                            installedTag = release.tagName,
                            latestTag = release.tagName,
                            updateAvailable = false,
                            lastCheckedAt = System.currentTimeMillis(),
                        )
                    )
                    _importState.value = GithubImportState.Success(id, release.tagName)
                },
                onFailure = { e ->
                    _importState.value = GithubImportState.Error(e.message ?: "Unknown error")
                }
            )
        }
    }

    /**
     * Checks all tracked sources for updates.
     */
    fun checkAllUpdates() {
        viewModelScope.launch {
            _updateState.value = GithubUpdateState.Checking
            val current = store.getAll()
            val updated = current.map { source ->
                repo.checkForUpdate(source)
            }
            updated.forEach { store.upsert(it) }
            _updateState.value = GithubUpdateState.Idle
        }
    }

    /**
     * Updates a single extension from its tracked GitHub source.
     */
    fun updateExtension(extensionId: String) {
        viewModelScope.launch {
            val source = store.getAll().firstOrNull { it.extensionId == extensionId }
                ?: run {
                    _updateState.value = GithubUpdateState.Error("No GitHub source tracked for $extensionId")
                    return@launch
                }
            _updateState.value = GithubUpdateState.Updating(extensionId)
            val result = repo.installFromGithub(source.owner, source.repo, source.assetPattern, extensionManager)
            result.fold(
                onSuccess = { (_, release) ->
                    store.upsert(
                        source.copy(
                            installedTag = release.tagName,
                            latestTag = release.tagName,
                            updateAvailable = false,
                            lastCheckedAt = System.currentTimeMillis(),
                        )
                    )
                    _updateState.value = GithubUpdateState.Done(updated = 1, failed = 0)
                },
                onFailure = { e ->
                    _updateState.value = GithubUpdateState.Error(e.message ?: "Update failed")
                }
            )
        }
    }

    /**
     * Updates all extensions that have updates available.
     */
    fun updateAllAvailable() {
        viewModelScope.launch {
            val toUpdate = store.getAll().filter { it.updateAvailable }
            if (toUpdate.isEmpty()) {
                _updateState.value = GithubUpdateState.Done(0, 0)
                return@launch
            }
            var updated = 0
            var failed = 0
            toUpdate.forEach { source ->
                _updateState.value = GithubUpdateState.Updating(source.extensionId)
                val result = repo.installFromGithub(source.owner, source.repo, source.assetPattern, extensionManager)
                result.fold(
                    onSuccess = { (_, release) ->
                        store.upsert(
                            source.copy(
                                installedTag = release.tagName,
                                latestTag = release.tagName,
                                updateAvailable = false,
                                lastCheckedAt = System.currentTimeMillis(),
                            )
                        )
                        updated++
                    },
                    onFailure = { failed++ }
                )
            }
            _updateState.value = GithubUpdateState.Done(updated, failed)
        }
    }

    /**
     * Removes GitHub tracking for an extension (called when extension is deleted).
     */
    fun removeTracking(extensionId: String) {
        viewModelScope.launch {
            store.remove(extensionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.close()
    }
}

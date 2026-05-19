/*
 * ArchiveTune (2026)
 * Â© Chartreux Westia â€” github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ai.AiServiceConfig
import moe.koiverse.archivetune.ai.AiTextService
import moe.koiverse.archivetune.ai.AiUserMixGenerator
import moe.koiverse.archivetune.ai.AiUserMixJson
import moe.koiverse.archivetune.constants.AiApiKeyKey
import moe.koiverse.archivetune.constants.AiApiValidationStatus
import moe.koiverse.archivetune.constants.AiApiValidationStatusKey
import moe.koiverse.archivetune.constants.AiCustomEndpointKey
import moe.koiverse.archivetune.constants.AiProvider
import moe.koiverse.archivetune.constants.AiProviderKey
import moe.koiverse.archivetune.constants.AiUserMixJsonKey
import moe.koiverse.archivetune.constants.AiUserMixUpdatedAtKey
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.extensions.toEnum
import moe.koiverse.archivetune.utils.dataStore

data class AiIntegrationActionState(
    val isTesting: Boolean = false,
    val isRebuildingMix: Boolean = false,
)

@HiltViewModel
class AiIntegrationSettingsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    private val _actionState = MutableStateFlow(AiIntegrationActionState())
    val actionState: StateFlow<AiIntegrationActionState> = _actionState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun testApi() {
        if (_actionState.value.isTesting) return
        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = _actionState.value.copy(isTesting = true)
            try {
                AiTextService.test(readConfig())
                context.dataStore.edit { prefs ->
                    prefs[AiApiValidationStatusKey] = AiApiValidationStatus.SUCCESS.name
                }
                _events.emit(context.getString(R.string.ai_api_connected))
            } catch (e: Exception) {
                context.dataStore.edit { prefs ->
                    prefs[AiApiValidationStatusKey] = AiApiValidationStatus.FAILED.name
                }
                _events.emit(e.localizedMessage ?: context.getString(R.string.ai_api_test_failed))
            } finally {
                _actionState.value = _actionState.value.copy(isTesting = false)
            }
        }
    }

    fun rebuildMix(count: Int) {
        if (_actionState.value.isRebuildingMix) return
        viewModelScope.launch(Dispatchers.IO) {
            _actionState.value = _actionState.value.copy(isRebuildingMix = true)
            try {
                val mixes = AiUserMixGenerator(database).generate(
                    config = readConfig(),
                    count = count,
                )
                context.dataStore.edit { prefs ->
                    prefs[AiUserMixJsonKey] = AiUserMixJson.encode(mixes)
                    prefs[AiUserMixUpdatedAtKey] = System.currentTimeMillis()
                    prefs[AiApiValidationStatusKey] = AiApiValidationStatus.SUCCESS.name
                }
                _events.emit(context.getString(R.string.ai_mix_rebuilt))
            } catch (e: Exception) {
                context.dataStore.edit { prefs ->
                    prefs[AiApiValidationStatusKey] = AiApiValidationStatus.FAILED.name
                }
                _events.emit(e.localizedMessage ?: context.getString(R.string.ai_mix_rebuild_failed))
            } finally {
                _actionState.value = _actionState.value.copy(isRebuildingMix = false)
            }
        }
    }

    fun removeMix() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs.remove(AiUserMixJsonKey)
                prefs.remove(AiUserMixUpdatedAtKey)
            }
            _events.emit(context.getString(R.string.ai_mix_removed))
        }
    }

    private suspend fun readConfig(): AiServiceConfig {
        val prefs = context.dataStore.data.first()
        return AiServiceConfig(
            provider = prefs[AiProviderKey].toEnum(AiProvider.NONE),
            apiKey = prefs[AiApiKeyKey].orEmpty(),
            customEndpoint = prefs[AiCustomEndpointKey].orEmpty(),
        )
    }
}

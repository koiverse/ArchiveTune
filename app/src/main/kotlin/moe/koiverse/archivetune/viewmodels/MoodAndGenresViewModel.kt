/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import moe.koiverse.archivetune.ai.AiUserMix
import moe.koiverse.archivetune.ai.AiUserMixJson
import moe.koiverse.archivetune.constants.AiUserMixJsonKey
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.innertube.pages.MoodAndGenres
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.utils.dataStore
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres.Item>?>(null)
    val aiUserMixes = MutableStateFlow<List<AiUserMix>>(emptyList())

    init {
        viewModelScope.launch {
            YouTube
                .explore()
                .onSuccess {
                    moodAndGenres.value = it.moodAndGenres
                }.onFailure {
                    reportException(it)
                }
        }

        viewModelScope.launch {
            context.dataStore.data
                .map { preferences -> AiUserMixJson.decode(preferences[AiUserMixJsonKey].orEmpty()) }
                .distinctUntilChanged()
                .collect { aiUserMixes.value = it }
        }
    }
}

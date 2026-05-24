/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.constants.NewsLastReadDateKey
import moe.koiverse.archivetune.models.NewsItem
import moe.koiverse.archivetune.repository.NewsRepository
import moe.koiverse.archivetune.utils.dataStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

sealed interface NewsUiState {
    data object Loading : NewsUiState
    data class Success(val items: List<NewsItem>) : NewsUiState
    data object Empty : NewsUiState
    data class Error(val message: String) : NewsUiState
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private val _rawItems = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _loadState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)

    val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NewsUiState> = combine(_loadState, searchQuery, _rawItems) { loadState, query, items ->
        when (loadState) {
            is NewsUiState.Loading -> NewsUiState.Loading
            is NewsUiState.Error -> loadState
            is NewsUiState.Empty -> NewsUiState.Empty
            is NewsUiState.Success -> {
                if (query.isBlank()) {
                    loadState
                } else {
                    val q = query.trim().lowercase()
                    val filtered = items.filter { item ->
                        item.title.lowercase().contains(q) ||
                            item.description.lowercase().contains(q) ||
                            item.author.lowercase().contains(q)
                    }
                    if (filtered.isEmpty()) NewsUiState.Empty else NewsUiState.Success(filtered)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsUiState.Loading)

    val hasUnreadNews: StateFlow<Boolean> = combine(
        _rawItems,
        context.dataStore.data.map { prefs -> prefs[NewsLastReadDateKey] ?: "" },
    ) { items, lastReadDate ->
        if (items.isEmpty()) return@combine false
        val latestDate = items.mapNotNull { parseDate(it.date) }.maxOrNull() ?: return@combine false
        val storedDate = if (lastReadDate.isBlank()) null else parseDate(lastReadDate)
        storedDate == null || latestDate.isAfter(storedDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        fetchNews()
    }

    fun fetchNews() {
        viewModelScope.launch {
            _loadState.value = NewsUiState.Loading
            runCatching {
                val items = repository.fetchNews()
                    .sortedByDescending { parseDate(it.date) }
                items
            }.onSuccess { items ->
                _rawItems.value = items
                _loadState.value = if (items.isEmpty()) NewsUiState.Empty else NewsUiState.Success(items)
            }.onFailure { error ->
                _loadState.value = NewsUiState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun markAllRead() {
        val latestDate = _rawItems.value
            .mapNotNull { parseDate(it.date) }
            .maxOrNull()
            ?.format(dateFormatter)
            ?: return
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[NewsLastReadDateKey] = latestDate
            }
        }
    }

    private fun parseDate(dateStr: String): LocalDateTime? = try {
        LocalDateTime.parse(dateStr.trim(), dateFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

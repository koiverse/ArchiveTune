@file:OptIn(ExperimentalCoroutinesApi::class)

package moe.koiverse.archivetune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import moe.koiverse.archivetune.constants.LocalSongSortDescendingKey
import moe.koiverse.archivetune.constants.LocalSongSortTypeKey
import moe.koiverse.archivetune.constants.SongSortType
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.extensions.toEnum
import moe.koiverse.archivetune.utils.dataStore
import javax.inject.Inject

@HiltViewModel
class LocalSongViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {

    val allLocalSongs =
        context.dataStore.data
            .map {
                Pair(
                    it[LocalSongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                    it[LocalSongSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.localSongs(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.constants.statToPeriod
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.ListeningSummary
import moe.koiverse.archivetune.ui.screens.OptionStats
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel
@Inject
constructor(
    val database: MusicDatabase,
) : ViewModel() {
    val selectedOption = MutableStateFlow(OptionStats.CONTINUOUS)
    val indexChips = MutableStateFlow(0)

    fun onOptionSelected(option: OptionStats) {
        selectedOption.value = option
        indexChips.value = 0
    }

    fun onChipIndexChanged(index: Int) {
        indexChips.value = index
    }

    private fun periodPair() = combine(selectedOption, indexChips) { opt, idx -> Pair(opt, idx) }

    private fun toTimestamp(selection: OptionStats, t: Int): Long =
        if (selection == OptionStats.CONTINUOUS || t == 0) {
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
        } else {
            statToPeriod(selection, t - 1)
        }

    val mostPlayedSongsStats =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.mostPlayedSongsStats(
                    fromTimeStamp = statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedSongs =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.mostPlayedSongs(
                    fromTimeStamp = statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedArtists =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.mostPlayedArtists(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp = toTimestamp(selection, t),
                ).map { artists -> artists.filter { it.artist.isYouTubeArtist } }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedAlbums =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.mostPlayedAlbums(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val listeningByHour =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.listeningByHour(
                    fromTimestamp = statToPeriod(selection, t),
                    toTimestamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val listeningByDayOfWeek =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.listeningByDayOfWeek(
                    fromTimestamp = statToPeriod(selection, t),
                    toTimestamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val listeningSummary =
        combine(mostPlayedSongsStats, mostPlayedArtists, mostPlayedAlbums) { songs, artists, albums ->
            ListeningSummary(
                totalPlayCount = songs.sumOf { it.songCountListened },
                totalTimeListened = songs.sumOf { it.timeListened ?: 0L },
                uniqueSongsCount = songs.size,
                uniqueArtistsCount = artists.size,
                uniqueAlbumsCount = albums.size,
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, ListeningSummary(0, 0L, 0, 0, 0))

    val firstEvent =
        database
            .firstEvent()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            mostPlayedArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
        viewModelScope.launch {
            mostPlayedAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
class YearInMusicViewModel
@Inject
constructor(
    val database: MusicDatabase,
) : ViewModel() {

    val selectedYear = MutableStateFlow(LocalDateTime.now().year)

    val firstEvent = database
        .firstEvent()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val availableYears = firstEvent.map { event ->
        val startYear = event?.event?.timestamp?.year ?: LocalDateTime.now().year
        val currentYear = LocalDateTime.now().year
        (currentYear downTo startYear).toList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf(LocalDateTime.now().year))

    private fun getYearStartTimestamp(year: Int): Long {
        return LocalDateTime.of(year, 1, 1, 0, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }

    private fun getYearEndTimestamp(year: Int): Long {
        return LocalDateTime.of(year, 12, 31, 23, 59, 59)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }

    val topSongsStats = selectedYear.flatMapLatest { year ->
        database.mostPlayedSongsStats(
            fromTimeStamp = getYearStartTimestamp(year),
            limit = 5,
            toTimeStamp = getYearEndTimestamp(year)
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topSongs = selectedYear.flatMapLatest { year ->
        database.mostPlayedSongs(
            fromTimeStamp = getYearStartTimestamp(year),
            limit = 5,
            toTimeStamp = getYearEndTimestamp(year)
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topArtists = selectedYear.flatMapLatest { year ->
        database.mostPlayedArtists(
            fromTimeStamp = getYearStartTimestamp(year),
            limit = 5,
            toTimeStamp = getYearEndTimestamp(year)
        ).map { artists ->
            artists.filter { it.artist.isYouTubeArtist }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topAlbums = selectedYear.flatMapLatest { year ->
        database.mostPlayedAlbums(
            fromTimeStamp = getYearStartTimestamp(year),
            limit = 5,
            toTimeStamp = getYearEndTimestamp(year)
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalListeningTime = topSongsStats.map { songs ->
        songs.sumOf { stat -> stat.timeListened ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalSongsPlayed = topSongsStats.map { songs ->
        songs.sumOf { stat -> stat.songCountListened.toLong() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    init {
        viewModelScope.launch {
            topArtists.collect { artists ->
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
            topAlbums.collect { albums ->
                albums
                    .filter { it.album.songCount == 0 }
                    .forEach { album ->
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

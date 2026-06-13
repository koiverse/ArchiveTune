/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.constants.HideExplicitKey
import moe.rukamori.archivetune.constants.SongSortType
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.extensions.filterExplicit
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.models.toMediaMetadata
import moe.rukamori.archivetune.utils.dataStore

private const val LibraryTopMixCandidateLimit = 80

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LibraryTopMixRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    fun observeRecentTracks(): Flow<List<MediaMetadata>> =
        hideExplicitEnabled()
            .flatMapLatest { hideExplicit ->
                database
                    .recentSongs(LibraryTopMixCandidateLimit)
                    .map { songs -> songs.filterExplicit(hideExplicit).map { it.toMediaMetadata() } }
            }
            .flowOn(Dispatchers.IO)

    fun observeLikedTracks(): Flow<List<MediaMetadata>> =
        hideExplicitEnabled()
            .flatMapLatest { hideExplicit ->
                database
                    .likedSongsByCreateDateAsc()
                    .map { songs ->
                        songs
                            .filterExplicit(hideExplicit)
                            .asReversed()
                            .take(LibraryTopMixCandidateLimit)
                            .map { it.toMediaMetadata() }
                    }
            }
            .flowOn(Dispatchers.IO)

    fun observeMostPlayedTracks(): Flow<List<MediaMetadata>> =
        hideExplicitEnabled()
            .flatMapLatest { hideExplicit ->
                database
                    .songs(SongSortType.PLAY_TIME, descending = true, filterVideo = true)
                    .map { songs ->
                        songs
                            .filterExplicit(hideExplicit)
                            .take(LibraryTopMixCandidateLimit)
                            .map { it.toMediaMetadata() }
                    }
            }
            .flowOn(Dispatchers.IO)

    fun observeLibraryTracks(): Flow<List<MediaMetadata>> =
        hideExplicitEnabled()
            .flatMapLatest { hideExplicit ->
                database
                    .songs(SongSortType.CREATE_DATE, descending = true, filterVideo = true)
                    .map { songs ->
                        songs
                            .filterExplicit(hideExplicit)
                            .take(LibraryTopMixCandidateLimit)
                            .map { it.toMediaMetadata() }
                    }
            }
            .flowOn(Dispatchers.IO)

    private fun hideExplicitEnabled(): Flow<Boolean> =
        context.dataStore.data
            .map { preferences -> preferences[HideExplicitKey] ?: false }
            .distinctUntilChanged()
}

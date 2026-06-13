/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.library

import com.google.common.collect.ImmutableList
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.repository.LibraryTopMixRepository

private const val LibraryTopMixTrackLimit = 50
private const val LibraryTopMixPreviewLimit = 3

class ObserveLibraryTopMixesUseCase
@Inject
constructor(
    private val repository: LibraryTopMixRepository,
) {
    operator fun invoke(): Flow<List<LibraryTopMix>> =
        combine(
            repository.observeRecentTracks(),
            repository.observeLikedTracks(),
            repository.observeMostPlayedTracks(),
            repository.observeLibraryTracks(),
        ) { recentTracks, likedTracks, mostPlayedTracks, libraryTracks ->
            listOfNotNull(
                buildMix(
                    id = LibraryTopMixId.DAILY,
                    primaryTracks = recentTracks,
                    secondaryTracks = likedTracks,
                    fallbackTracks = libraryTracks,
                ),
                buildMix(
                    id = LibraryTopMixId.CHILL,
                    primaryTracks = likedTracks,
                    secondaryTracks = recentTracks.asReversed(),
                    fallbackTracks = libraryTracks.asReversed(),
                ),
                buildMix(
                    id = LibraryTopMixId.FOCUS,
                    primaryTracks = mostPlayedTracks,
                    secondaryTracks = recentTracks,
                    fallbackTracks = libraryTracks,
                ),
            )
        }.flowOn(Dispatchers.Default)

    private fun buildMix(
        id: LibraryTopMixId,
        primaryTracks: List<MediaMetadata>,
        secondaryTracks: List<MediaMetadata>,
        fallbackTracks: List<MediaMetadata>,
    ): LibraryTopMix? {
        val tracks = distinctTracks(primaryTracks, secondaryTracks, fallbackTracks)
            .take(LibraryTopMixTrackLimit)

        if (tracks.isEmpty()) return null

        return LibraryTopMix(
            id = id,
            tracks = ImmutableList.copyOf(tracks),
            previewArtworkUrls = ImmutableList.copyOf(
                tracks
                    .asSequence()
                    .mapNotNull { it.thumbnailUrl }
                    .distinct()
                    .take(LibraryTopMixPreviewLimit)
                    .toList(),
            ),
        )
    }

    private fun distinctTracks(vararg groups: List<MediaMetadata>): List<MediaMetadata> {
        val seen = LinkedHashSet<String>()
        return groups
            .asSequence()
            .flatMap { it.asSequence() }
            .filter { seen.add(it.id) }
            .toList()
    }
}

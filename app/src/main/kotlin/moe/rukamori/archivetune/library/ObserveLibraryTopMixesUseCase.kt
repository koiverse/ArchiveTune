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
import java.time.LocalDate
import kotlin.random.Random

private const val LibraryTopMixTrackLimit = 50
private const val LibraryTopMixPreviewLimit = 3
private const val ArtistAffinityScore = 6
private const val AlbumAffinityScore = 3

class ObserveLibraryTopMixesUseCase
@Inject
constructor(
    private val repository: LibraryTopMixRepository,
) {
    operator fun invoke(): Flow<List<LibraryTopMix>> =
        combine(
            repository.observeRecentTracks(),
            repository.observeLikedTracks(),
            repository.observeListenedTracks(),
            repository.observeLibraryTracks(),
        ) { recentTracks, likedTracks, listenedLibraryTracks, libraryTracks ->
            val listenedTracks = distinctTracks(recentTracks, listenedLibraryTracks)
            val excludedTrackIds = (listenedTracks + likedTracks).mapTo(LinkedHashSet()) { it.id }
            val candidates = scoreLibraryCandidates(
                libraryTracks = libraryTracks,
                listenedTracks = listenedTracks,
                excludedTrackIds = excludedTrackIds,
            )

            listOfNotNull(
                buildMix(
                    id = LibraryTopMixId.DAILY,
                    candidates = candidates,
                ),
                buildMix(
                    id = LibraryTopMixId.CHILL,
                    candidates = candidates,
                ),
                buildMix(
                    id = LibraryTopMixId.FOCUS,
                    candidates = candidates,
                    minimumScore = ArtistAffinityScore,
                ),
            )
        }.flowOn(Dispatchers.Default)

    private fun buildMix(
        id: LibraryTopMixId,
        candidates: List<ScoredTrack>,
        minimumScore: Int = 0,
    ): LibraryTopMix? {
        val eligibleCandidates = candidates
            .filter { it.score >= minimumScore }
            .ifEmpty { candidates }

        val tracks = randomizedByAffinity(
            id = id,
            candidates = eligibleCandidates,
        )
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

    private fun scoreLibraryCandidates(
        libraryTracks: List<MediaMetadata>,
        listenedTracks: List<MediaMetadata>,
        excludedTrackIds: Set<String>,
    ): List<ScoredTrack> {
        val affinity = listenedTracks.toAffinityProfile()
        return libraryTracks
            .asSequence()
            .filterNot { it.id in excludedTrackIds }
            .map { track ->
                ScoredTrack(
                    track = track,
                    score = track.affinityScore(affinity),
                )
            }
            .toList()
    }

    private fun randomizedByAffinity(
        id: LibraryTopMixId,
        candidates: List<ScoredTrack>,
    ): List<MediaMetadata> {
        val random = Random(seedFor(id, candidates))
        return candidates
            .groupBy { it.score }
            .toSortedMap(compareByDescending<Int> { it })
            .values
            .flatMap { group -> group.shuffled(random) }
            .map { it.track }
    }

    private fun distinctTracks(vararg groups: List<MediaMetadata>): List<MediaMetadata> {
        val seen = LinkedHashSet<String>()
        return groups
            .asSequence()
            .flatMap { it.asSequence() }
            .filter { seen.add(it.id) }
            .toList()
    }

    private fun List<MediaMetadata>.toAffinityProfile(): AffinityProfile =
        AffinityProfile(
            artistIds = asSequence()
                .flatMap { track -> track.artists.asSequence() }
                .mapNotNull { artist -> artist.id }
                .toSet(),
            artistNames = asSequence()
                .flatMap { track -> track.artists.asSequence() }
                .map { artist -> artist.name.normalizedForAffinity() }
                .filter { it.isNotBlank() }
                .toSet(),
            albumIds = asSequence()
                .mapNotNull { track -> track.album?.id }
                .toSet(),
            albumTitles = asSequence()
                .map { track -> track.album?.title.orEmpty().normalizedForAffinity() }
                .filter { it.isNotBlank() }
                .toSet(),
        )

    private fun MediaMetadata.affinityScore(affinity: AffinityProfile): Int {
        val artistMatches = artists.any { artist ->
            artist.id?.let { it in affinity.artistIds } == true ||
                artist.name.normalizedForAffinity() in affinity.artistNames
        }
        val albumMatches = album?.let { album ->
            album.id in affinity.albumIds || album.title.normalizedForAffinity() in affinity.albumTitles
        } ?: false

        return (if (artistMatches) ArtistAffinityScore else 0) +
            (if (albumMatches) AlbumAffinityScore else 0)
    }

    private fun seedFor(
        id: LibraryTopMixId,
        candidates: List<ScoredTrack>,
    ): Int {
        val daySeed = LocalDate.now().toEpochDay()
        val candidateSeed = candidates.fold(17) { seed, candidate ->
            seed * 31 + candidate.track.id.hashCode()
        }
        return (daySeed * 31 + id.ordinal * 997 + candidateSeed).toInt()
    }

    private fun String.normalizedForAffinity(): String = trim().lowercase()

    private data class ScoredTrack(
        val track: MediaMetadata,
        val score: Int,
    )

    private data class AffinityProfile(
        val artistIds: Set<String>,
        val artistNames: Set<String>,
        val albumIds: Set<String>,
        val albumTitles: Set<String>,
    )
}

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.innertube.pages

import moe.koiverse.archivetune.innertube.models.Album
import moe.koiverse.archivetune.innertube.models.AlbumItem
import moe.koiverse.archivetune.innertube.models.Artist
import moe.koiverse.archivetune.innertube.models.MusicResponsiveHeaderRenderer
import moe.koiverse.archivetune.innertube.models.MusicResponsiveListItemRenderer
import moe.koiverse.archivetune.innertube.models.SectionListRenderer
import moe.koiverse.archivetune.innertube.models.getContinuation
import moe.koiverse.archivetune.innertube.models.SongItem
import moe.koiverse.archivetune.innertube.models.getItems
import moe.koiverse.archivetune.innertube.models.oddElements
import moe.koiverse.archivetune.innertube.models.response.BrowseResponse
import moe.koiverse.archivetune.innertube.models.splitBySeparator
import moe.koiverse.archivetune.innertube.utils.parseTime

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
) {
    companion object {
        fun getPlaylistId(response: BrowseResponse): String? {
            response.microformat?.microformatDataRenderer?.urlCanonical
                ?.substringAfter("list=", missingDelimiterValue = "")
                ?.substringBefore('&')
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }

            getHeader(response)?.buttons?.forEach { button ->
                button.musicPlayButtonRenderer
                    ?.playNavigationEndpoint
                    ?.anyWatchEndpoint
                    ?.playlistId
                    ?.let { return it }
            }

            response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.forEach { button ->
                button.buttonRenderer?.navigationEndpoint?.anyWatchEndpoint?.playlistId?.let { return it }
            }

            return null
        }

        fun getTitle(response: BrowseResponse): String? {
            val title = getHeader(response)?.title
                ?: response.header?.musicDetailHeaderRenderer?.title
                ?: response.header?.musicHeaderRenderer?.title
            return title?.runs?.firstOrNull()?.text
        }

        fun getYear(response: BrowseResponse): Int? {
            val subtitle = getHeader(response)?.subtitle
                ?: response.header?.musicDetailHeaderRenderer?.subtitle
                ?: response.header?.musicHeaderRenderer?.subtitle
            return subtitle?.runs?.lastOrNull()?.text?.toIntOrNull()
        }

        fun getThumbnail(response: BrowseResponse): String? {
            return response.background?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: getHeader(response)?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicVisualHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
        }

        fun getArtists(response: BrowseResponse): List<Artist> {
            val artists = getHeader(response)?.straplineTextOne?.runs?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: response.header?.musicHeaderRenderer?.straplineTextOne?.runs?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: emptyList()

            return artists
        }

        private fun getHeader(response: BrowseResponse): MusicResponsiveHeaderRenderer? {
            for (section in getSectionContents(response)) {
                section.musicResponsiveHeaderRenderer?.let { return it }
                section.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer?.let { return it }
            }
            return null
        }

        fun getSongRenderers(response: BrowseResponse): List<MusicResponsiveListItemRenderer> {
            for (section in getSectionContents(response)) {
                section.musicShelfRenderer?.contents?.getItems()?.takeIf(::hasTrackCandidates)?.let { return it }
                section.musicPlaylistShelfRenderer?.contents?.getItems()?.takeIf(::hasTrackCandidates)?.let { return it }
                section.itemSectionRenderer?.contents?.forEach { content ->
                    content.musicShelfRenderer?.contents?.getItems()?.takeIf(::hasTrackCandidates)?.let { return it }
                }
            }
            return emptyList()
        }

        fun getSongs(response: BrowseResponse, album: AlbumItem? = null): List<SongItem> =
            getSongRenderers(response).mapNotNull { getSong(it, album) }

        fun getSongContinuation(response: BrowseResponse): String? {
            for (section in getSectionContents(response)) {
                section.musicShelfRenderer?.let { shelf ->
                    shelf.contents?.getItems()?.takeIf(::hasTrackCandidates)?.let {
                        shelf.contents?.getContinuation()?.let { continuation -> return continuation }
                        shelf.continuations?.getContinuation()?.let { continuation -> return continuation }
                    }
                }
                section.musicPlaylistShelfRenderer?.let { shelf ->
                    shelf.contents.getItems().takeIf(::hasTrackCandidates)?.let {
                        shelf.contents.getContinuation()?.let { continuation -> return continuation }
                        shelf.continuations?.getContinuation()?.let { continuation -> return continuation }
                    }
                }
                section.itemSectionRenderer?.contents?.forEach { content ->
                    content.musicShelfRenderer?.let { shelf ->
                        shelf.contents?.getItems()?.takeIf(::hasTrackCandidates)?.let {
                            shelf.contents?.getContinuation()?.let { continuation -> return continuation }
                            shelf.continuations?.getContinuation()?.let { continuation -> return continuation }
                        }
                    }
                }
            }
            return null
        }

        fun getContinuationSongRenderers(response: BrowseResponse): List<MusicResponsiveListItemRenderer> {
            response.onResponseReceivedActions
                ?.flatMap { it.appendContinuationItemsAction?.continuationItems.orEmpty() }
                ?.getItems()
                ?.takeIf(::hasTrackCandidates)
                ?.let { return it }

            response.continuationContents?.musicPlaylistShelfContinuation?.contents
                ?.getItems()
                ?.takeIf(::hasTrackCandidates)
                ?.let { return it }

            response.continuationContents?.musicShelfContinuation?.contents
                ?.getItems()
                ?.takeIf(::hasTrackCandidates)
                ?.let { return it }

            return emptyList()
        }

        fun getContinuationSongs(response: BrowseResponse, album: AlbumItem? = null): List<SongItem> =
            getContinuationSongRenderers(response).mapNotNull { getSong(it, album) }

        fun getNextSongContinuation(response: BrowseResponse): String? {
            response.onResponseReceivedActions
                ?.flatMap { it.appendContinuationItemsAction?.continuationItems.orEmpty() }
                ?.getContinuation()
                ?.let { return it }

            response.continuationContents?.musicPlaylistShelfContinuation?.contents?.getContinuation()?.let { return it }
            response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation()?.let { return it }
            response.continuationContents?.musicShelfContinuation?.contents?.getContinuation()?.let { return it }
            response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation()?.let { return it }

            return null
        }

        fun getSong(renderer: MusicResponsiveListItemRenderer, album: AlbumItem? = null): SongItem? {
            val title = renderer.flexColumns.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.text
                ?: PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text
                ?: return null

            val duration = findDuration(renderer) ?: return null
            val videoId = renderer.playlistItemData?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                ?: return null
            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: renderer.thumbnail?.musicAnimatedThumbnailRenderer?.backupRenderer?.getThumbnailUrl()
                ?: album?.thumbnail
                ?: return null
            val songAlbum = album?.let {
                Album(it.title, it.browseId)
            } ?: findAlbumRun(renderer)?.let { run ->
                run.navigationEndpoint?.browseEndpoint?.browseId?.let { browseId ->
                    Album(
                        name = run.text,
                        id = browseId
                    )
                }
            }

            return SongItem(
                id = videoId,
                title = title,
                artists = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST").map{
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }.ifEmpty {
                    // Fallback to album artists if no artists found in song data
                    album?.artists ?: emptyList()
                },
                album = songAlbum,
                duration = duration,
                thumbnail = thumbnail,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint
                    ?: renderer.navigationEndpoint?.watchEndpoint
            )
        }

        private fun getSectionContents(response: BrowseResponse): List<SectionListRenderer.Content> {
            val contents = mutableListOf<SectionListRenderer.Content>()

            response.contents?.sectionListRenderer?.contents?.let(contents::addAll)
            response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.let(contents::addAll)
            response.contents?.twoColumnBrowseResultsRenderer?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.let(contents::addAll)
            response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents
                ?.sectionListRenderer
                ?.contents
                ?.let(contents::addAll)

            return contents
        }

        private fun findDuration(renderer: MusicResponsiveListItemRenderer): Int? {
            renderer.fixedColumns?.forEach { column ->
                column.musicResponsiveListItemFlexColumnRenderer.text?.runs?.forEach { run ->
                    run.text.parseTime()?.let { return it }
                }
            }

            renderer.flexColumns.forEach { column ->
                column.musicResponsiveListItemFlexColumnRenderer.text?.runs?.forEach { run ->
                    run.text.parseTime()?.let { return it }
                }
            }

            return null
        }

        private fun findAlbumRun(renderer: MusicResponsiveListItemRenderer) =
            renderer.flexColumns
                .asSequence()
                .flatMap { column ->
                    column.musicResponsiveListItemFlexColumnRenderer.text?.runs.orEmpty().asSequence()
                }.firstOrNull { run ->
                    run.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseEndpointContextSupportedConfigs
                        ?.browseEndpointContextMusicConfig
                        ?.pageType
                        ?.contains("ALBUM") == true
                }

        private fun hasTrackCandidates(items: List<MusicResponsiveListItemRenderer>) =
            items.any(::isTrackCandidate)

        private fun isTrackCandidate(renderer: MusicResponsiveListItemRenderer): Boolean {
            if (renderer.playlistItemData?.videoId != null) return true
            if (renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId != null) {
                return true
            }
            if (renderer.navigationEndpoint?.watchEndpoint?.videoId != null) return true
            return findDuration(renderer) != null
        }
    }
}

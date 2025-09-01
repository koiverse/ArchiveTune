package moe.koiverse.archivetune.innertube.pages

import moe.koiverse.archivetune.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)

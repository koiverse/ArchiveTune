/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


package moe.koiverse.archivetune.localmedia

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.AlbumArtistMap
import moe.koiverse.archivetune.db.entities.AlbumEntity
import moe.koiverse.archivetune.db.entities.ArtistEntity
import moe.koiverse.archivetune.db.entities.FormatEntity
import moe.koiverse.archivetune.db.entities.SongAlbumMap
import moe.koiverse.archivetune.db.entities.SongArtistMap
import moe.koiverse.archivetune.db.entities.SongEntity
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class LocalSongScanSummary(
    val scannedSongs: Int,
    val removedSongs: Int,
)

class LocalSongScanner
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    suspend fun scanDevice(): LocalSongScanSummary = withContext(Dispatchers.IO) {
        val snapshot = queryTracks()
        database.withTransaction {
            val existingLocalIds = localSongIds()
            val scannedIds = snapshot.tracks.map(LocalTrackRecord::id)
            val scannedIdSet = scannedIds.toSet()
            val removedIds = existingLocalIds.filterNot(scannedIdSet::contains)

            if (scannedIds.isEmpty()) {
                clearLocalSongs()
            } else {
                removedIds.chunked(SqlBatchSize).forEach(::deleteSongsByIds)
            }

            val existingSongs = loadSongs(scannedIds)
            val existingArtists = loadArtists(snapshot.artists.map(LocalArtistRecord::id))
            val existingAlbums = loadAlbums(snapshot.albums.map(LocalAlbumRecord::id))

            snapshot.artists.forEach { artist ->
                val existingArtist = existingArtists[artist.id]
                upsert(
                    ArtistEntity(
                        id = artist.id,
                        name = artist.name,
                        thumbnailUrl = existingArtist?.thumbnailUrl,
                        channelId = null,
                        lastUpdateTime = existingArtist?.lastUpdateTime ?: LocalDateTime.now(),
                        bookmarkedAt = existingArtist?.bookmarkedAt,
                        isLocal = true,
                    ),
                )
            }

            snapshot.albums.forEach { album ->
                val existingAlbum = existingAlbums[album.id]
                upsert(
                    AlbumEntity(
                        id = album.id,
                        playlistId = null,
                        title = album.title,
                        year = album.year ?: existingAlbum?.year,
                        thumbnailUrl = album.thumbnailUrl ?: existingAlbum?.thumbnailUrl,
                        themeColor = existingAlbum?.themeColor,
                        songCount = album.songCount,
                        duration = album.duration,
                        explicit = false,
                        lastUpdateTime = LocalDateTime.now(),
                        bookmarkedAt = existingAlbum?.bookmarkedAt,
                        likedDate = existingAlbum?.likedDate,
                        inLibrary = existingAlbum?.inLibrary,
                        isLocal = true,
                    ),
                )
            }

            snapshot.albums.map(LocalAlbumRecord::id).distinct().chunked(SqlBatchSize).forEach(::deleteAlbumArtistMapsByAlbumIds)
            snapshot.albums.forEach { album ->
                album.artistIds.forEachIndexed { index, artistId ->
                    insert(
                        AlbumArtistMap(
                            albumId = album.id,
                            artistId = artistId,
                            order = index,
                        ),
                    )
                }
            }

            snapshot.tracks.forEach { track ->
                val existingSong = existingSongs[track.id]?.song
                upsert(
                    SongEntity(
                        id = track.id,
                        title = track.title,
                        duration = track.durationSeconds,
                        thumbnailUrl = track.thumbnailUrl ?: existingSong?.thumbnailUrl,
                        albumId = track.albumId,
                        albumName = track.albumName,
                        explicit = existingSong?.explicit ?: false,
                        year = track.year ?: existingSong?.year,
                        date = existingSong?.date,
                        dateModified = track.dateModified ?: existingSong?.dateModified,
                        liked = existingSong?.liked ?: false,
                        likedDate = existingSong?.likedDate,
                        totalPlayTime = existingSong?.totalPlayTime ?: 0L,
                        inLibrary = null,
                        dateDownload = existingSong?.dateDownload,
                        isLocal = true,
                    ),
                )
                upsert(
                    FormatEntity(
                        id = track.id,
                        itag = -1,
                        mimeType = track.mimeType,
                        codecs = "",
                        bitrate = 0,
                        sampleRate = null,
                        contentLength = track.sizeBytes,
                        loudnessDb = null,
                        perceptualLoudnessDb = null,
                        playbackUrl = null,
                    ),
                )
                deleteSongArtistMaps(track.id)
                track.artists.forEachIndexed { index, artist ->
                    insert(
                        SongArtistMap(
                            songId = track.id,
                            artistId = artist.id,
                            position = index,
                        ),
                    )
                }
                deleteSongAlbumMaps(track.id)
                track.albumId?.let { albumId ->
                    insert(
                        SongAlbumMap(
                            songId = track.id,
                            albumId = albumId,
                            index = 0,
                        ),
                    )
                }
            }

            pruneLocalAlbums()
            pruneLocalArtists()
            pruneFormats()
            prunePlayCounts()

            LocalSongScanSummary(
                scannedSongs = snapshot.tracks.size,
                removedSongs = removedIds.size,
            )
        }
    }

    private suspend fun loadSongs(ids: List<String>) =
        ids.chunked(SqlBatchSize)
            .flatMap(database::getSongsByIds)
            .associateBy { it.id }

    private suspend fun loadArtists(ids: List<String>) =
        ids.distinct().chunked(SqlBatchSize)
            .flatMap(database::getArtistEntitiesByIds)
            .associateBy { it.id }

    private suspend fun loadAlbums(ids: List<String>) =
        ids.distinct().chunked(SqlBatchSize)
            .flatMap(database::getAlbumEntitiesByIds)
            .associateBy { it.id }

    private fun queryTracks(): LocalScanSnapshot {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val selection = buildList {
            add("${MediaStore.Audio.Media.SIZE} > 0")
            add("${MediaStore.Audio.Media.DURATION} > 0")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add("${MediaStore.MediaColumns.IS_PENDING} = 0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add("${MediaStore.MediaColumns.IS_TRASH} = 0")
            }
        }.joinToString(" AND ")

        val unknownArtist = context.getString(R.string.unknown_artist)
        val unknownTitle = context.getString(R.string.unknown)
        val tracks = mutableListOf<LocalTrackRecord>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC, ${MediaStore.Audio.Media._ID} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val yearIndex = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idIndex)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
                val artistValue = normalizeArtistName(cursor.getString(artistIndex), unknownArtist)
                val splitArtists = splitArtistNames(artistValue).ifEmpty { listOf(unknownArtist) }
                val mediaStoreArtistId = cursor.getLongOrNull(artistIdIndex)
                val artists = splitArtists.mapIndexed { index, name ->
                    LocalArtistRecord(
                        id = buildArtistId(mediaStoreArtistId, name, index, splitArtists.size),
                        name = name,
                    )
                }
                val mediaStoreAlbumId = cursor.getLongOrNull(albumIdIndex)
                val albumName = normalizeAlbumName(cursor.getString(albumIndex))
                val title = normalizeTitle(
                    title = cursor.getString(titleIndex),
                    displayName = cursor.getString(displayNameIndex),
                    fallback = unknownTitle,
                )
                tracks += LocalTrackRecord(
                    id = contentUri.toString(),
                    title = title,
                    artists = artists,
                    albumId = albumName?.let {
                        buildAlbumId(
                            mediaStoreAlbumId = mediaStoreAlbumId,
                            albumName = it,
                            primaryArtistId = artists.firstOrNull()?.id,
                        )
                    },
                    albumName = albumName,
                    durationSeconds = (cursor.getLong(durationIndex).coerceAtLeast(0L) / 1000L)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt(),
                    year = cursor.getIntOrNull(yearIndex)?.takeIf { it > 0 },
                    dateModified = cursor.getLong(dateModifiedIndex)
                        .takeIf { it > 0L }
                        ?.let { LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()) },
                    sizeBytes = cursor.getLong(sizeIndex).coerceAtLeast(0L),
                    mimeType = cursor.getString(mimeTypeIndex)?.takeIf(String::isNotBlank) ?: "audio/*",
                    thumbnailUrl = mediaStoreAlbumId
                        ?.takeIf { it > 0L }
                        ?.let { ContentUris.withAppendedId(AlbumArtUri, it).toString() },
                )
            }
        }

        val albums = tracks
            .filter { !it.albumId.isNullOrBlank() && !it.albumName.isNullOrBlank() }
            .groupBy { it.albumId!! }
            .map { (albumId, albumTracks) ->
                LocalAlbumRecord(
                    id = albumId,
                    title = albumTracks.first().albumName.orEmpty(),
                    year = albumTracks.mapNotNull(LocalTrackRecord::year).maxOrNull(),
                    thumbnailUrl = albumTracks.mapNotNull(LocalTrackRecord::thumbnailUrl).firstOrNull(),
                    songCount = albumTracks.size,
                    duration = albumTracks.sumOf(LocalTrackRecord::durationSeconds),
                    artistIds = albumTracks.flatMap { track -> track.artists.map(LocalArtistRecord::id) }.distinct(),
                )
            }

        return LocalScanSnapshot(
            tracks = tracks,
            artists = tracks.flatMap(LocalTrackRecord::artists).distinctBy(LocalArtistRecord::id),
            albums = albums,
        )
    }

    private fun normalizeTitle(title: String?, displayName: String?, fallback: String): String {
        return title?.trim()?.takeIf { it.isNotBlank() }
            ?: displayName?.substringBeforeLast('.')?.trim()?.takeIf { it.isNotBlank() }
            ?: fallback
    }

    private fun normalizeArtistName(rawArtist: String?, fallback: String): String {
        val normalized = rawArtist?.trim()?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
        return normalized ?: fallback
    }

    private fun normalizeAlbumName(rawAlbum: String?): String? {
        return rawAlbum?.trim()?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
    }

    private fun splitArtistNames(rawArtist: String): List<String> {
        return rawArtist
            .split(ArtistSeparators)
            .map(String::trim)
            .filter(String::isNotBlank)
            .ifEmpty { listOf(rawArtist) }
    }

    private fun buildArtistId(
        mediaStoreArtistId: Long?,
        artistName: String,
        index: Int,
        totalArtists: Int,
    ): String {
        val stableId = mediaStoreArtistId?.takeIf { it > 0L }
        return if (stableId != null && totalArtists == 1) {
            "LOCAL_ARTIST_$stableId"
        } else {
            "LOCAL_ARTIST_${stableHash("$artistName|$index")}" 
        }
    }

    private fun buildAlbumId(
        mediaStoreAlbumId: Long?,
        albumName: String,
        primaryArtistId: String?,
    ): String {
        val stableId = mediaStoreAlbumId?.takeIf { it > 0L }
        return if (stableId != null) {
            "LOCAL_ALBUM_$stableId"
        } else {
            "LOCAL_ALBUM_${stableHash("$albumName|$primaryArtistId")}" 
        }
    }

    private fun stableHash(source: String): String {
        return UUID.nameUUIDFromBytes(source.toByteArray(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "")
    }

    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex) else null
    }

    private fun android.database.Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getInt(columnIndex) else null
    }

    private data class LocalScanSnapshot(
        val tracks: List<LocalTrackRecord>,
        val artists: List<LocalArtistRecord>,
        val albums: List<LocalAlbumRecord>,
    )

    private data class LocalTrackRecord(
        val id: String,
        val title: String,
        val artists: List<LocalArtistRecord>,
        val albumId: String?,
        val albumName: String?,
        val durationSeconds: Int,
        val year: Int?,
        val dateModified: LocalDateTime?,
        val sizeBytes: Long,
        val mimeType: String,
        val thumbnailUrl: String?,
    )

    private data class LocalArtistRecord(
        val id: String,
        val name: String,
    )

    private data class LocalAlbumRecord(
        val id: String,
        val title: String,
        val year: Int?,
        val thumbnailUrl: String?,
        val songCount: Int,
        val duration: Int,
        val artistIds: List<String>,
    )

    private companion object {
        val AlbumArtUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val ArtistSeparators = Regex("[,;/&]")
        const val SqlBatchSize = 900
    }
}
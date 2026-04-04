package moe.koiverse.archivetune.utils

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.AlbumArtistMap
import moe.koiverse.archivetune.db.entities.AlbumEntity
import moe.koiverse.archivetune.db.entities.ArtistEntity
import moe.koiverse.archivetune.db.entities.SongAlbumMap
import moe.koiverse.archivetune.db.entities.SongArtistMap
import moe.koiverse.archivetune.db.entities.SongEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs

data class ScanResult(
    val imported: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val totalFound: Int = 0,
)

class LocalMediaScanner(
    private val context: Context,
    private val database: MusicDatabase,
) {
    fun interface ProgressCallback {
        fun onProgress(current: Int, total: Int)
    }

    fun scanMediaStore(
        excludedFolders: Set<String>,
        onProgress: ProgressCallback? = null,
    ): ScanResult {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder,
        ) ?: return ScanResult()

        val rows = mutableListOf<MediaStoreRow>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateModIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val mimeIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dataIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val yearIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (c.moveToNext()) {
                val filePath = c.getString(dataIdx) ?: continue
                if (excludedFolders.any { filePath.startsWith(it) }) continue

                rows.add(
                    MediaStoreRow(
                        mediaStoreId = c.getLong(idIdx),
                        title = c.getString(titleIdx) ?: "Unknown",
                        artist = c.getString(artistIdx)?.takeIf { it != "<unknown>" },
                        album = c.getString(albumIdx)?.takeIf { it != "<unknown>" },
                        albumId = c.getLong(albumIdIdx),
                        durationMs = c.getLong(durationIdx),
                        dateModified = c.getLong(dateModIdx),
                        mimeType = c.getString(mimeIdx),
                        filePath = filePath,
                        size = c.getLong(sizeIdx),
                        year = c.getInt(yearIdx).takeIf { it > 0 },
                        trackNumber = c.getInt(trackIdx),
                    )
                )
            }
        }

        return persistRows(rows, onProgress)
    }

    fun scanSafFolder(
        treeUri: Uri,
        excludedFolders: Set<String>,
        onProgress: ProgressCallback? = null,
    ): ScanResult {
        val docFile = DocumentFile.fromTreeUri(context, treeUri) ?: return ScanResult()
        val audioFiles = collectAudioFiles(docFile, excludedFolders)
        val rows = audioFiles.mapNotNull { file -> extractSafMetadata(file) }
        return persistRows(rows, onProgress)
    }

    private fun collectAudioFiles(
        dir: DocumentFile,
        excludedFolders: Set<String>,
    ): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                val path = file.uri.path ?: ""
                if (excludedFolders.none { path.contains(it) }) {
                    result.addAll(collectAudioFiles(file, excludedFolders))
                }
            } else if (file.isFile && file.type?.startsWith("audio/") == true) {
                result.add(file)
            }
        }
        return result
    }

    private fun extractSafMetadata(file: DocumentFile): MediaStoreRow? {
        val uri = file.uri
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.name?.substringBeforeLast('.') ?: return null
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.toIntOrNull()
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.split("/")?.firstOrNull()?.toIntOrNull() ?: 0

            if (durationMs < 10_000) return null

            val docId = try { DocumentsContract.getDocumentId(uri) } catch (_: Exception) { uri.toString() }
            val stableId = abs(docId.hashCode().toLong())

            MediaStoreRow(
                mediaStoreId = stableId,
                title = title,
                artist = artist,
                album = album,
                albumId = album?.lowercase()?.hashCode()?.toLong() ?: 0L,
                durationMs = durationMs,
                dateModified = file.lastModified() / 1000,
                mimeType = file.type,
                filePath = uri.toString(),
                size = file.length(),
                year = year,
                trackNumber = trackNumber,
                contentUri = uri.toString(),
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun persistRows(
        rows: List<MediaStoreRow>,
        onProgress: ProgressCallback?,
    ): ScanResult {
        val total = rows.size
        var imported = 0
        var updated = 0
        var skipped = 0
        var failed = 0

        val validUris = mutableListOf<String>()

        database.transaction {
            rows.forEachIndexed { index, row ->
                onProgress?.onProgress(index + 1, total)
                try {
                    val songId = "local_${row.mediaStoreId}"
                    val contentUri = row.contentUri ?: ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        row.mediaStoreId,
                    ).toString()

                    validUris.add(contentUri)

                    val existingSong = localSongByUri(contentUri)
                    if (existingSong != null) {
                        val existingModified = existingSong.dateModified
                            ?.toEpochSecond(ZoneOffset.UTC) ?: 0L
                        if (existingModified == row.dateModified) {
                            skipped++
                            return@forEachIndexed
                        }
                    }

                    val artistName = row.artist ?: "Unknown Artist"
                    val artistId = "local_artist_${artistName.lowercase().hashCode()}"
                    val albumName = row.album
                    val albumId = albumName?.let { "local_album_${it.lowercase().hashCode()}" }

                    val albumArtUri = if (row.albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            row.albumId,
                        ).toString()
                    } else {
                        null
                    }

                    val dateModified = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(row.dateModified),
                        ZoneOffset.UTC,
                    )

                    val songEntity = SongEntity(
                        id = songId,
                        title = row.title,
                        duration = (row.durationMs / 1000).toInt(),
                        thumbnailUrl = albumArtUri,
                        albumId = albumId,
                        albumName = albumName,
                        year = row.year,
                        dateModified = dateModified,
                        isLocal = true,
                        localUri = contentUri,
                        inLibrary = LocalDateTime.now(),
                    )

                    if (existingSong != null) {
                        upsert(songEntity.copy(
                            totalPlayTime = existingSong.totalPlayTime,
                            liked = existingSong.liked,
                            likedDate = existingSong.likedDate,
                            inLibrary = existingSong.inLibrary ?: LocalDateTime.now(),
                        ))
                        updated++
                    } else {
                        upsert(songEntity)
                        imported++
                    }

                    insert(ArtistEntity(id = artistId, name = artistName, isLocal = true))
                    insert(SongArtistMap(songId = songId, artistId = artistId, position = 0))

                    if (albumId != null && albumName != null) {
                        insert(
                            AlbumEntity(
                                id = albumId,
                                title = albumName,
                                year = row.year,
                                thumbnailUrl = albumArtUri,
                                songCount = 0,
                                duration = 0,
                                isLocal = true,
                            )
                        )
                        insert(SongAlbumMap(songId = songId, albumId = albumId, index = row.trackNumber))
                        insert(AlbumArtistMap(albumId = albumId, artistId = artistId, order = 0))
                    }
                } catch (_: Exception) {
                    failed++
                }
            }

            if (validUris.isNotEmpty()) {
                validUris.chunked(500).forEach { chunk ->
                    deleteStaleLocalSongs(chunk)
                }
            }
        }

        return ScanResult(
            imported = imported,
            updated = updated,
            skipped = skipped,
            failed = failed,
            totalFound = total,
        )
    }

    private data class MediaStoreRow(
        val mediaStoreId: Long,
        val title: String,
        val artist: String?,
        val album: String?,
        val albumId: Long,
        val durationMs: Long,
        val dateModified: Long,
        val mimeType: String?,
        val filePath: String,
        val size: Long,
        val year: Int?,
        val trackNumber: Int,
        val contentUri: String? = null,
    )
}

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.utils

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import moe.koiverse.archivetune.db.entities.FormatEntity
import moe.koiverse.archivetune.db.entities.Song
import java.io.File
import java.io.OutputStream
import java.net.URL

enum class ExportDestination { MEDIA_STORE, SAF }

enum class FilenameTemplate {
    TITLE_ARTIST,
    ARTIST_TITLE,
    TITLE_ONLY,
    ARTIST_ALBUM_TITLE;

    fun format(title: String, artist: String, album: String): String {
        val safeTitle = sanitize(title)
        val safeArtist = sanitize(artist)
        val safeAlbum = sanitize(album)
        return when (this) {
            TITLE_ARTIST -> "$safeTitle — $safeArtist"
            ARTIST_TITLE -> "$safeArtist — $safeTitle"
            TITLE_ONLY -> safeTitle
            ARTIST_ALBUM_TITLE -> "$safeArtist — $safeAlbum — $safeTitle"
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
}

enum class DuplicateHandling { SKIP, OVERWRITE, RENAME }

data class ExportConfig(
    val destination: ExportDestination = ExportDestination.MEDIA_STORE,
    val safUri: Uri? = null,
    val filenameTemplate: FilenameTemplate = FilenameTemplate.TITLE_ARTIST,
    val duplicateHandling: DuplicateHandling = DuplicateHandling.SKIP,
    val embedMetadata: Boolean = true,
    val includeCoverArt: Boolean = true,
)

sealed class ExportResult {
    data class Success(val songId: String, val path: String) : ExportResult()
    data class Failed(val songId: String, val reason: String) : ExportResult()
    data class Skipped(val songId: String, val reason: String) : ExportResult()
}

object AudioExporter {

    private const val BUFFER_SIZE = 8192
    private const val RELATIVE_PATH = "Music/ArchiveTune"
    private const val EXPORT_EXTENSION = ".mp3"
    private const val EXPORT_MIME_TYPE = "audio/mpeg"

    fun fileExtension(format: FormatEntity): String = EXPORT_EXTENSION

    private fun sourceExtension(format: FormatEntity): String = when {
        format.mimeType.contains("mp4") -> ".m4a"
        format.mimeType.contains("webm") -> ".webm"
        format.mimeType.contains("ogg") -> ".ogg"
        else -> ".bin"
    }

    private fun exportBitrateKbps(format: FormatEntity): Int =
        (format.bitrate / 1000).coerceIn(128, 320)

    fun exportSong(
        context: Context,
        song: Song,
        downloadCache: Cache,
        playerCache: Cache,
        config: ExportConfig,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): ExportResult {
        val format = song.format
            ?: return ExportResult.Failed(song.id, "No format info")

        val contentLength = format.contentLength
        if (contentLength <= 0) return ExportResult.Failed(song.id, "Invalid content length")

        val isInDownload = downloadCache.isCached(song.id, 0, contentLength)
        val isInPlayer = playerCache.isCached(song.id, 0, contentLength)
        if (!isInDownload && !isInPlayer) return ExportResult.Failed(song.id, "Cache incomplete")

        val artistName = song.artists.firstOrNull()?.name ?: "Unknown Artist"
        val albumName = song.song.albumName ?: "Unknown Album"
        val extension = fileExtension(format)
        val baseName = config.filenameTemplate.format(song.song.title, artistName, albumName)
        val fileName = "$baseName$extension"

        val tempDir = context.cacheDir.resolve("song_exports").apply { mkdirs() }
        val inputFile = tempDir.resolve("${song.id}_${System.nanoTime()}${sourceExtension(format)}")
        val outputFile = tempDir.resolve("${song.id}_${System.nanoTime()}$EXPORT_EXTENSION")
        val artworkFile = if (config.embedMetadata && config.includeCoverArt) {
            downloadArtworkFile(song, tempDir)
        } else {
            null
        }

        return try {
            inputFile.outputStream().use { outputStream ->
                writeCacheToStream(song.id, downloadCache, playerCache, outputStream, contentLength, onProgress)
            }

            val transcodeResult = transcodeToMp3(
                inputFile = inputFile,
                outputFile = outputFile,
                song = song,
                artworkFile = artworkFile,
                bitrateKbps = exportBitrateKbps(format),
                embedMetadata = config.embedMetadata,
            )
            if (transcodeResult != null) {
                return ExportResult.Failed(song.id, transcodeResult)
            }

            when (config.destination) {
                ExportDestination.MEDIA_STORE -> exportToMediaStore(
                    context = context,
                    song = song,
                    config = config,
                    fileName = fileName,
                    baseName = baseName,
                    exportedFile = outputFile,
                )

                ExportDestination.SAF -> exportToSaf(
                    context = context,
                    song = song,
                    config = config,
                    fileName = fileName,
                    baseName = baseName,
                    exportedFile = outputFile,
                )
            }
        } catch (e: Exception) {
            ExportResult.Failed(song.id, e.message ?: "Unknown error")
        } finally {
            inputFile.delete()
            outputFile.delete()
            artworkFile?.delete()
        }
    }

    private fun exportToMediaStore(
        context: Context,
        song: Song,
        config: ExportConfig,
        fileName: String,
        baseName: String,
        exportedFile: File,
    ): ExportResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val existingUri = findExistingMediaStore(context, fileName)

            when (config.duplicateHandling) {
                DuplicateHandling.SKIP -> {
                    if (existingUri != null) return ExportResult.Skipped(song.id, "Already exists")
                }
                DuplicateHandling.OVERWRITE -> {
                    existingUri?.let { context.contentResolver.delete(it, null, null) }
                }
                DuplicateHandling.RENAME -> {}
            }

            val actualFileName = if (config.duplicateHandling == DuplicateHandling.RENAME && existingUri != null) {
                generateUniqueName(context, baseName, EXPORT_EXTENSION)
            } else {
                fileName
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, actualFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, EXPORT_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.Audio.AudioColumns.TITLE, song.song.title)
                put(MediaStore.Audio.AudioColumns.ARTIST, song.artists.firstOrNull()?.name ?: "Unknown Artist")
                put(MediaStore.Audio.AudioColumns.ALBUM, song.song.albumName ?: "")
                put(MediaStore.Audio.AudioColumns.DURATION, song.song.duration * 1000L)
                song.song.year?.let { put(MediaStore.Audio.AudioColumns.YEAR, it) }
                put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return ExportResult.Failed(song.id, "Failed to create MediaStore entry")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                exportedFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return ExportResult.Failed(song.id, "Failed to open output stream")

            return ExportResult.Success(song.id, "$RELATIVE_PATH/$actualFileName")
        } else {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val archiveDir = File(musicDir, "ArchiveTune")
            archiveDir.mkdirs()

            val existingFile = File(archiveDir, fileName)
            when (config.duplicateHandling) {
                DuplicateHandling.SKIP -> {
                    if (existingFile.exists()) return ExportResult.Skipped(song.id, "Already exists")
                }
                DuplicateHandling.OVERWRITE -> {
                    if (existingFile.exists()) existingFile.delete()
                }
                DuplicateHandling.RENAME -> {}
            }

            val actualFile = if (config.duplicateHandling == DuplicateHandling.RENAME && existingFile.exists()) {
                generateUniqueFile(archiveDir, baseName, EXPORT_EXTENSION)
            } else {
                existingFile
            }

            exportedFile.copyTo(actualFile, overwrite = true)

            return ExportResult.Success(song.id, actualFile.absolutePath)
        }
    }

    private fun exportToSaf(
        context: Context,
        song: Song,
        config: ExportConfig,
        fileName: String,
        baseName: String,
        exportedFile: File,
    ): ExportResult {
        val treeUri = config.safUri
            ?: return ExportResult.Failed(song.id, "No destination folder selected")

        val parentDocument = DocumentFile.fromTreeUri(context, treeUri)
            ?: return ExportResult.Failed(song.id, "Invalid destination folder")

        val existing = parentDocument.findFile(fileName)
        when (config.duplicateHandling) {
            DuplicateHandling.SKIP -> {
                if (existing != null) return ExportResult.Skipped(song.id, "Already exists")
            }
            DuplicateHandling.OVERWRITE -> {
                existing?.delete()
            }
            DuplicateHandling.RENAME -> {}
        }

        val actualFileName = if (config.duplicateHandling == DuplicateHandling.RENAME && existing != null) {
            generateUniqueSafName(parentDocument, baseName, EXPORT_EXTENSION)
        } else {
            fileName
        }

        val docFile = parentDocument.createFile(EXPORT_MIME_TYPE, actualFileName)
            ?: return ExportResult.Failed(song.id, "Failed to create file")

        context.contentResolver.openOutputStream(docFile.uri)?.use { outputStream ->
            exportedFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: return ExportResult.Failed(song.id, "Failed to open output stream")

        return ExportResult.Success(song.id, docFile.name ?: actualFileName)
    }

    private fun writeCacheToStream(
        songId: String,
        downloadCache: Cache,
        playerCache: Cache,
        outputStream: OutputStream,
        contentLength: Long,
        onProgress: (Long, Long) -> Unit,
    ) {
        val cache = if (downloadCache.isCached(songId, 0, contentLength)) downloadCache else playerCache
        val dataSource = CacheDataSource(cache, null)

        try {
            val dataSpec = DataSpec.Builder()
                .setUri("https://placeholder.invalid/$songId")
                .setPosition(0)
                .setLength(contentLength)
                .setKey(songId)
                .build()

            dataSource.open(dataSpec)
            val buffer = ByteArray(BUFFER_SIZE)
            var totalWritten = 0L

            while (true) {
                val bytesRead = dataSource.read(buffer, 0, buffer.size)
                if (bytesRead == -1) break
                outputStream.write(buffer, 0, bytesRead)
                totalWritten += bytesRead
                onProgress(totalWritten, contentLength)
            }
        } finally {
            dataSource.close()
        }
    }

    private fun transcodeToMp3(
        inputFile: File,
        outputFile: File,
        song: Song,
        artworkFile: File?,
        bitrateKbps: Int,
        embedMetadata: Boolean,
    ): String? {
        val command = buildString {
            append("-y -hide_banner -loglevel error ")
            append("-i ${quoteForFfmpeg(inputFile.absolutePath)} ")
            if (artworkFile != null && embedMetadata) {
                append("-i ${quoteForFfmpeg(artworkFile.absolutePath)} ")
            }
            append("-map 0:a:0 ")
            if (artworkFile != null && embedMetadata) {
                append("-map 1:v:0 -c:v mjpeg -disposition:v:0 attached_pic ")
                append("-metadata:s:v title=${quoteForFfmpeg("Album cover")} ")
                append("-metadata:s:v comment=${quoteForFfmpeg("Cover (front)")} ")
            }
            append("-c:a libmp3lame -b:a ${bitrateKbps}k -id3v2_version 3 -write_id3v1 1 ")
            if (embedMetadata) {
                append(metadataArguments(song))
            }
            append(quoteForFfmpeg(outputFile.absolutePath))
        }

        val session = FFmpegKit.execute(command)
        return if (ReturnCode.isSuccess(session.returnCode)) {
            null
        } else {
            val details = session.allLogsAsString.trim().lineSequence().lastOrNull().orEmpty()
            if (details.isBlank()) "FFmpeg failed to create MP3" else details
        }
    }

    private fun metadataArguments(song: Song): String {
        val artist = song.artists.firstOrNull()?.name ?: "Unknown Artist"
        val albumArtist = song.artists.joinToString(", ") { it.name }.ifBlank { artist }
        val builder = StringBuilder()
        builder.append("-metadata title=${quoteForFfmpeg(song.song.title)} ")
        builder.append("-metadata artist=${quoteForFfmpeg(artist)} ")
        builder.append("-metadata album=${quoteForFfmpeg(song.song.albumName ?: "Unknown Album")} ")
        builder.append("-metadata album_artist=${quoteForFfmpeg(albumArtist)} ")
        builder.append("-metadata comment=${quoteForFfmpeg("Exported from ArchiveTune")} ")
        song.song.year?.let {
            builder.append("-metadata date=${quoteForFfmpeg(it.toString())} ")
        }
        return builder.toString()
    }

    private fun downloadArtworkFile(song: Song, tempDir: File): File? {
        val artworkUrl = song.song.thumbnailUrl ?: return null
        return runCatching {
            val artworkFile = tempDir.resolve("${song.id}_${System.nanoTime()}.jpg")
            val imageBytes = URL(
                artworkUrl
                    .replace("w60-h60", "w544-h544")
                    .replace("w120-h120", "w544-h544")
            ).readBytes()
            artworkFile.writeBytes(imageBytes)
            artworkFile
        }.getOrNull()
    }

    private fun quoteForFfmpeg(value: String): String =
        "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

    private fun findExistingMediaStore(context: Context, fileName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("$RELATIVE_PATH/", fileName)

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        return null
    }

    private fun generateUniqueName(context: Context, baseName: String, extension: String): String {
        var counter = 1
        var candidate = "$baseName ($counter)$extension"
        while (findExistingMediaStore(context, candidate) != null) {
            counter++
            candidate = "$baseName ($counter)$extension"
        }
        return candidate
    }

    private fun generateUniqueFile(directory: File, baseName: String, extension: String): File {
        var counter = 1
        var candidate = File(directory, "$baseName ($counter)$extension")
        while (candidate.exists()) {
            counter++
            candidate = File(directory, "$baseName ($counter)$extension")
        }
        return candidate
    }

    private fun generateUniqueSafName(parent: DocumentFile, baseName: String, extension: String): String {
        var counter = 1
        var candidate = "$baseName ($counter)$extension"
        while (parent.findFile(candidate) != null) {
            counter++
            candidate = "$baseName ($counter)$extension"
        }
        return candidate
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.utils

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
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
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

data class ExportMetadata(
    val lyrics: String? = null,
)

sealed class ExportResult {
    data class Success(val songId: String, val path: String) : ExportResult()
    data class Failed(val songId: String, val reason: String) : ExportResult()
    data class Skipped(val songId: String, val reason: String) : ExportResult()
}

object AudioExporter {

    private const val BUFFER_SIZE = 8192
    private const val RELATIVE_PATH = "Music/ArchiveTune"

    fun fileExtension(format: FormatEntity): String = ".mp3"

    private fun sourceFileExtension(format: FormatEntity): String = when {
        format.mimeType.contains("mp4") -> ".m4a"
        format.mimeType.contains("webm") -> ".ogg"
        format.mimeType.contains("ogg") -> ".ogg"
        else -> ".${format.codecs.substringBefore(',').substringBefore(' ').trim().ifBlank { "bin" }}"
    }

    fun mimeTypeForExport(format: FormatEntity): String = "audio/mpeg"

    private fun mediaStoreMimeType(format: FormatEntity): String = "audio/mpeg"

    private fun safMimeType(format: FormatEntity): String = "audio/mpeg"

    fun exportSong(
        context: Context,
        song: Song,
        downloadCache: Cache,
        playerCache: Cache,
        config: ExportConfig,
        metadata: ExportMetadata = ExportMetadata(),
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

        return try {
            when (config.destination) {
                ExportDestination.MEDIA_STORE -> exportToMediaStore(
                    context, song, metadata, downloadCache, playerCache, config, format,
                    fileName, baseName, extension, contentLength, onProgress
                )
                ExportDestination.SAF -> exportToSaf(
                    context, song, metadata, downloadCache, playerCache, config, format,
                    fileName, baseName, extension, contentLength, onProgress
                )
            }
        } catch (e: Exception) {
            ExportResult.Failed(song.id, e.message ?: "Unknown error")
        }
    }

    private fun exportToMediaStore(
        context: Context,
        song: Song,
        metadata: ExportMetadata,
        downloadCache: Cache,
        playerCache: Cache,
        config: ExportConfig,
        format: FormatEntity,
        fileName: String,
        baseName: String,
        extension: String,
        contentLength: Long,
        onProgress: (Long, Long) -> Unit,
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
                generateUniqueName(context, baseName, extension)
            } else {
                fileName
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, actualFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mediaStoreMimeType(format))
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
                writeCacheToStream(song.id, downloadCache, playerCache, format, outputStream, contentLength, onProgress)
            } ?: return ExportResult.Failed(song.id, "Failed to open output stream")

            if (config.embedMetadata && canTagFormat(format)) {
                embedMetadataViaMediaStore(context, uri, song, metadata, format, config)
            }

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
                generateUniqueFile(archiveDir, baseName, extension)
            } else {
                existingFile
            }

            actualFile.outputStream().use { outputStream ->
                writeCacheToStream(song.id, downloadCache, playerCache, format, outputStream, contentLength, onProgress)
            }

            if (config.embedMetadata && canTagFormat(format)) {
                val taggedFile = retaggedTempFile(context, format)
                try {
                    actualFile.inputStream().use { input ->
                        taggedFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    embedMetadataToFile(taggedFile, song, metadata, config)
                    taggedFile.inputStream().use { input ->
                        actualFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } finally {
                    taggedFile.delete()
                }
            }

            return ExportResult.Success(song.id, actualFile.absolutePath)
        }
    }

    private fun exportToSaf(
        context: Context,
        song: Song,
        metadata: ExportMetadata,
        downloadCache: Cache,
        playerCache: Cache,
        config: ExportConfig,
        format: FormatEntity,
        fileName: String,
        baseName: String,
        extension: String,
        contentLength: Long,
        onProgress: (Long, Long) -> Unit,
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
            generateUniqueSafName(parentDocument, baseName, extension)
        } else {
            fileName
        }

        val mimeType = safMimeType(format)
        val docFile = parentDocument.createFile(mimeType, actualFileName.removeSuffix(extension))
            ?: return ExportResult.Failed(song.id, "Failed to create file")

        context.contentResolver.openOutputStream(docFile.uri)?.use { outputStream ->
            writeCacheToStream(song.id, downloadCache, playerCache, format, outputStream, contentLength, onProgress)
        } ?: return ExportResult.Failed(song.id, "Failed to open output stream")

        if (config.embedMetadata && canTagFormat(format)) {
            val tempFile = retaggedTempFile(context, format)
            try {
                context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                embedMetadataToFile(tempFile, song, metadata, config)
                tempFile.inputStream().use { input ->
                    context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                tempFile.delete()
            }
        }

        return ExportResult.Success(song.id, docFile.name ?: actualFileName)
    }

    private fun writeCacheToStream(
        songId: String,
        downloadCache: Cache,
        playerCache: Cache,
        format: FormatEntity,
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

    private fun canTagFormat(format: FormatEntity): Boolean =
        format.mimeType.contains("mp4") || format.mimeType.contains("ogg")

    private fun embedMetadataToFile(
        file: File,
        song: Song,
        metadata: ExportMetadata,
        config: ExportConfig,
    ) {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault
        val primaryArtist = song.artists.firstOrNull()?.name
        val allArtists = song.artists.joinToString(", ") { it.name }.ifBlank { null }
        val albumTitle = song.song.albumName ?: song.album?.title
        val year = song.song.year ?: song.album?.year
        val recordingDate = song.song.date?.toLocalDate()?.toString()
        val lyrics = metadata.lyrics?.takeIf { it.isNotBlank() }

        setTextField(tag, FieldKey.TITLE, song.song.title)
        setTextField(tag, FieldKey.ARTIST, primaryArtist)
        setTextField(tag, FieldKey.ALBUM, albumTitle)
        setTextField(tag, FieldKey.ALBUM_ARTIST, allArtists)
        setTextField(tag, FieldKey.YEAR, year?.toString())
        setTextField(tag, FieldKey.DATE, recordingDate ?: year?.toString())
        setTextField(tag, FieldKey.LYRICS, lyrics)
        setTextField(tag, FieldKey.COMMENT, buildComment(song))

        if (config.includeCoverArt) {
            embedArtwork(tag, song)
        }

        audioFile.commit()
    }

    private fun embedMetadataViaMediaStore(
        context: Context,
        uri: Uri,
        song: Song,
        metadata: ExportMetadata,
        format: FormatEntity,
        config: ExportConfig,
    ) {
        val tempFile = retaggedTempFile(context, format)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            embedMetadataToFile(tempFile, song, metadata, config)
            tempFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun retaggedTempFile(context: Context, format: FormatEntity): File =
        File(context.cacheDir, "export_tag_temp_${System.nanoTime()}${sourceFileExtension(format)}")

    private fun buildComment(song: Song): String = buildList {
        add("Exported from ArchiveTune")
        if (song.song.explicit) {
            add("Explicit")
        }
    }.joinToString(" • ")

    private fun setTextField(tag: Tag, fieldKey: FieldKey, value: String?) {
        value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                runCatching {
                    tag.setField(fieldKey, it)
                }
            }
    }

    private fun embedArtwork(tag: Tag, song: Song) {
        val artworkUrl = resolveArtworkUrl(song) ?: return
        runCatching {
            val imageBytes = URL(artworkUrl).readBytes()
            val artwork = ArtworkFactory.getNew().apply {
                binaryData = imageBytes
                mimeType = "image/jpeg"
            }
            tag.setField(artwork)
        }
    }

    private fun resolveArtworkUrl(song: Song): String? {
        val artworkSource = song.song.thumbnailUrl
            ?: song.album?.thumbnailUrl
            ?: song.artists.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
            ?: return null

        return artworkSource
            .replace("w60-h60", "w1200-h1200")
            .replace("w120-h120", "w1200-h1200")
            .replace("w544-h544", "w1200-h1200")
    }

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

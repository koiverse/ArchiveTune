/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import android.content.Context
import androidx.media3.datasource.cache.Cache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.constants.AudioExportFormat
import moe.rukamori.archivetune.constants.AudioExportFormatKey
import moe.rukamori.archivetune.constants.SaveThumbnailKey
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.di.DownloadCache
import moe.rukamori.archivetune.storage.AudioExportRepository
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.utils.PreferenceStore
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFileExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadCache private val downloadCache: Cache,
    private val database: MusicDatabase,
    private val exportRepository: AudioExportRepository,
    private val metadataWriter: AudioMetadataWriter,
    private val thumbnailDownloader: ThumbnailDownloader,
) {
    private val exportLock = Any()
    private val exportedMarker = mutableSetOf<String>()

    suspend fun exportCompletedDownloads(): Int = withContext(Dispatchers.IO) {
        val songs = database.allSongs().first()
        var count = 0
        for (song in songs) {
            if (song.song.id in exportedMarker) continue
            if (exportSingleSong(song) != null) count++
        }
        count
    }

    suspend fun exportSingleSong(song: Song): File? = withContext(Dispatchers.IO) {
        val mediaId = song.song.id

        if (!isFullyCached(mediaId)) return@withContext null

        synchronized(exportLock) {
            if (mediaId in exportedMarker) return@withContext null
            exportedMarker.add(mediaId)
        }

        runCatching {
            val exportDir = exportRepository.ensureExportDirectory().getOrThrow()
            val targetFile = buildTargetFile(exportDir, song)

            if (targetFile.exists() && targetFile.length() > 0) {
                return@runCatching targetFile
            }

            val totalCached = downloadCache.getCachedLength(mediaId, 0L, Long.MAX_VALUE)
            if (totalCached <= 0) {
                synchronized(exportLock) { exportedMarker.remove(mediaId) }
                return@runCatching error("No cached data for $mediaId")
            }

            copyFromCache(mediaId, targetFile)
            writeMetadataAndThumbnail(targetFile, song)

            targetFile
        }.onFailure {
            synchronized(exportLock) { exportedMarker.remove(mediaId) }
        }.getOrNull()
    }

    suspend fun exportSingleSongById(songId: String): File? = withContext(Dispatchers.IO) {
        val song = database.getSongById(songId)
        if (song == null) return@withContext null
        exportSingleSong(song)
    }

    fun isExported(mediaId: String): Boolean = mediaId in exportedMarker

    fun markAsExported(mediaId: String) {
        exportedMarker.add(mediaId)
    }

    fun clearExportedMarkers() {
        exportedMarker.clear()
    }

    private fun isFullyCached(mediaId: String): Boolean {
        if (mediaId !in downloadCache.keys) return false
        return downloadCache.getCachedLength(mediaId, 0L, Long.MAX_VALUE) > 0
    }

    private fun buildTargetFile(exportDir: File, song: Song): File {
        val format = resolveExportFormat()
        val extension = extensionForFormat(format, song.format?.mimeType)
        val baseName = buildSafeFileName(song)
        val targetFile = File(exportDir, "$baseName.$extension")

        if (!targetFile.exists()) return targetFile

        var counter = 1
        while (true) {
            val numbered = File(exportDir, "$baseName ($counter).$extension")
            if (!numbered.exists()) return numbered
            counter++
        }
    }

    private fun resolveExportFormat(): AudioExportFormat {
        return PreferenceStore.get(AudioExportFormatKey)
            ?.let { name -> runCatching { AudioExportFormat.valueOf(name) }.getOrNull() }
            ?: AudioExportFormat.SOURCE
    }

    private fun extensionForFormat(format: AudioExportFormat, sourceMimeType: String?): String = when (format) {
        AudioExportFormat.SOURCE -> sourceMimeToExtension(sourceMimeType) ?: "opus"
        AudioExportFormat.OPUS -> "opus"
        AudioExportFormat.M4A -> "m4a"
        AudioExportFormat.MP3 -> "mp3"
        AudioExportFormat.FLAC -> "flac"
    }

    private fun sourceMimeToExtension(mimeType: String?): String? {
        if (mimeType == null) return null
        return when {
            mimeType.contains("audio/webm") || mimeType.contains("audio/opus") -> "opus"
            mimeType.contains("audio/mp4") || mimeType.contains("audio/x-m4a") -> "m4a"
            mimeType.contains("audio/mpeg") || mimeType.contains("audio/mp3") -> "mp3"
            mimeType.contains("audio/flac") -> "flac"
            mimeType.contains("audio/ogg") -> "ogg"
            mimeType.contains("audio/wav") -> "wav"
            mimeType.contains("video/webm") -> "webm"
            mimeType.contains("video/mp4") -> "m4a"
            else -> null
        }
    }

    private fun buildSafeFileName(song: Song): String {
        val artistName = song.artists.firstOrNull()?.name ?: "Unknown Artist"
        val title = song.song.title

        val parts = mutableListOf(artistName, title)
        if (!song.song.albumName.isNullOrBlank()) {
            parts.add(song.song.albumName)
        }

        val raw = parts.joinToString(" - ")
        val sanitized = raw.replace(Regex("""[\\/:*?"<>|]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (sanitized.length > MAX_FILENAME_LENGTH) {
            sanitized.take(MAX_FILENAME_LENGTH).trim()
        } else {
            sanitized
        }
    }

    private fun copyFromCache(mediaId: String, targetFile: File) {
        targetFile.parentFile?.mkdirs()

        val cachedSpans = downloadCache.getCachedSpans(mediaId)
        if (cachedSpans.isEmpty()) return

        FileOutputStream(targetFile).use { outputStream ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)

            for (span in cachedSpans) {
                if (span.isHoleSpan) continue
                if (span.length <= 0) continue

                RandomAccessFile(span.file, "r").use { raf ->
                    raf.seek(span.position)
                    var remaining = span.length
                    while (remaining > 0) {
                        val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                        val bytesRead = raf.read(buffer, 0, toRead)
                        if (bytesRead < 0) break
                        outputStream.write(buffer, 0, bytesRead)
                        remaining -= bytesRead
                    }
                }
            }
        }
    }

    private suspend fun writeMetadataAndThumbnail(audioFile: File, song: Song) {
        val saveThumbnail = PreferenceStore.get(SaveThumbnailKey) ?: false

        val coverArtBytes = if (saveThumbnail) {
            val thumbnailUrl = song.song.thumbnailUrl
            if (!thumbnailUrl.isNullOrBlank()) {
                val thumbFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.jpg")
                if (!thumbFile.exists()) {
                    thumbnailDownloader.downloadThumbnail(thumbnailUrl, thumbFile)
                        .onFailure { /* thumbnail download failed silently */ }
                }
                if (thumbFile.exists()) thumbFile.readBytes() else null
            } else null
        } else null

        metadataWriter.writeMetadata(
            audioFile = audioFile,
            metadata = AudioMetadataWriter.Metadata(
                title = song.song.title,
                artists = song.artists,
                album = song.song.albumName,
                year = song.song.year,
                coverArtBytes = coverArtBytes,
            ),
        )
    }

    companion object {
        private const val MAX_FILENAME_LENGTH = 200
        private const val COPY_BUFFER_SIZE = 64 * 1024
    }
}

/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.entities.ArtistEntity
import moe.rukamori.archivetune.db.entities.SongEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMetadataWriter @Inject constructor() {

    data class Metadata(
        val title: String,
        val artists: List<ArtistEntity>,
        val album: String?,
        val year: Int?,
        val coverArtBytes: ByteArray?,
    )

    suspend fun writeMetadata(
        audioFile: File,
        metadata: Metadata,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val extension = audioFile.extension.lowercase()
            when {
                extension == "mp3" -> writeMp3Metadata(audioFile, metadata)
                extension in listOf("m4a", "mp4") -> writeM4aMetadata(audioFile, metadata)
                extension in listOf("ogg", "opus", "webm") -> writeOggMetadata(audioFile, metadata)
            }
        }
    }

    private fun writeMp3Metadata(file: File, metadata: Metadata) {
        val artistName = metadata.artists.firstOrNull()?.name ?: "Unknown Artist"
        val albumName = metadata.album ?: "Unknown Album"

        val id3 = buildId3v2Tag(metadata.title, artistName, albumName, metadata.year, metadata.coverArtBytes)
        if (id3 == null) return

        val tempFile = File(file.parentFile, "${file.name}.tmp")
        try {
            RandomAccessFile(file, "r").use { raf ->
                val existingId3Size = detectExistingId3Size(raf)
                raf.seek(existingId3Size.toLong())

                FileChannel.open(tempFile.toPath(), java.nio.file.StandardOpenOption.CREATE_NEW,
                    java.nio.file.StandardOpenOption.WRITE).use { outChannel ->
                    outChannel.write(ByteBuffer.wrap(id3))
                    raf.channel.transferTo(raf.filePointer, raf.length() - raf.filePointer, outChannel)
                }
            }
            tempFile.renameTo(file)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun detectExistingId3Size(raf: RandomAccessFile): Int {
        return try {
            if (raf.length() < 10) return 0
            val header = ByteArray(10)
            raf.readFully(header)
            if (String(header, 0, 3, Charsets.ISO_8859_1) == "ID3") {
                val size = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F)
                size + 10
            } else {
                raf.seek(0)
                0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun buildId3v2Tag(
        title: String,
        artist: String,
        album: String,
        year: Int?,
        coverArt: ByteArray?,
    ): ByteArray? {
        val frames = ByteArrayOutputStream()

        writeId3TextFrame(frames, "TIT2", title)
        writeId3TextFrame(frames, "TPE1", artist)
        writeId3TextFrame(frames, "TALB", album)
        if (year != null) writeId3TextFrame(frames, "TYER", year.toString())

        if (coverArt != null) {
            writeId3ApicFrame(frames, coverArt)
        }

        val frameBytes = frames.toByteArray()
        val tagSize = frameBytes.size
        val header = ByteArray(10)

        val id3Header = "ID3".toByteArray(Charsets.ISO_8859_1)
        System.arraycopy(id3Header, 0, header, 0, 3)

        header[3] = 3
        header[4] = 0

        header[6] = ((tagSize shr 21) and 0x7F).toByte()
        header[7] = ((tagSize shr 14) and 0x7F).toByte()
        header[8] = ((tagSize shr 7) and 0x7F).toByte()
        header[9] = (tagSize and 0x7F).toByte()

        return ByteArrayOutputStream().apply {
            write(header)
            write(frameBytes)
        }.toByteArray()
    }

    private fun writeId3TextFrame(os: ByteArrayOutputStream, frameId: String, text: String) {
        val encoding: Byte = 3 // UTF-8
        val textBytes = text.encodeToByteArray()
        val frameData = ByteArrayOutputStream()
        frameData.write(encoding.toInt())
        frameData.write(textBytes)

        val data = frameData.toByteArray()
        writeId3FrameHeader(os, frameId, data.size)
        os.write(data)
    }

    private fun writeId3ApicFrame(os: ByteArrayOutputStream, imageData: ByteArray) {
        val mimeType = detectImageMimeType(imageData)
        val mimeBytes = mimeType.encodeToByteArray()
        val encoding: Byte = 0 // ISO-8859-1

        val frameData = ByteArrayOutputStream()
        frameData.write(encoding.toInt())
        frameData.write(mimeBytes)
        frameData.write(0)
        frameData.write(3) // front cover
        frameData.write(0)
        frameData.write(imageData)

        val data = frameData.toByteArray()
        writeId3FrameHeader(os, "APIC", data.size)
        os.write(data)
    }

    private fun writeId3FrameHeader(os: ByteArrayOutputStream, frameId: String, size: Int) {
        val frameIdBytes = frameId.encodeToByteArray()
        os.write(frameIdBytes)
        val sizeBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(size).array()
        os.write(sizeBytes)
        os.write(0) // no flags
        os.write(0) // no flags
    }

    private fun writeM4aMetadata(file: File, metadata: Metadata) {
        val artistName = metadata.artists.firstOrNull()?.name ?: "Unknown Artist"
        val albumName = metadata.album ?: "Unknown Album"

        writeM4aMetadataAtoms(file, metadata.title, artistName, albumName, metadata.year, metadata.coverArtBytes)
    }

    private fun writeM4aMetadataAtoms(
        file: File,
        title: String,
        artist: String,
        album: String,
        year: Int?,
        coverArt: ByteArray?,
    ) {
        try {
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            val raf = RandomAccessFile(file, "r")
            val originalBytes = raf.use { it.readBytes() }
            val originalSize = originalBytes.size

            val metaAtom = buildM4aMetadataAtom(title, artist, album, year, coverArt)
            val newFileSize = originalSize + metaAtom.size

            val output = ByteArrayOutputStream()
            output.write(originalBytes)
            output.write(metaAtom)

            tempFile.writeBytes(output.toByteArray())
            if (tempFile.length() == newFileSize.toLong()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
            }
        } catch (_: Exception) {
        }
    }

    private fun buildM4aMetadataAtom(
        title: String,
        artist: String,
        album: String,
        year: Int?,
        coverArt: ByteArray?,
    ): ByteArray {
        // Build an MP4 metadata (ilist) atom
        val ilist = ByteArrayOutputStream()

        addM4aTextAtom(ilist, "\u00A9nam", title)
        addM4aTextAtom(ilist, "\u00A9ART", artist)
        addM4aTextAtom(ilist, "\u00A9alb", album)
        if (year != null) addM4aTextAtom(ilist, "\u00A9day", year.toString())
        if (coverArt != null) addM4aCoverArtAtom(ilist, coverArt)

        val ilistBytes = ilist.toByteArray()

        // Build meta atom
        val metaData = ByteArrayOutputStream()
        metaData.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array()) // version + flags (0)
        metaData.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(ilistBytes.size + 8).array()) // ilist atom size
        metaData.write("ilist".encodeToByteArray())
        metaData.write(ilistBytes)

        val metaBytes = metaData.toByteArray()
        val metaAtom = ByteArrayOutputStream()
        metaAtom.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(metaBytes.size + 8).array())
        metaAtom.write("meta".encodeToByteArray())
        metaAtom.write(metaBytes)

        val metaAtomBytes = metaAtom.toByteArray()

        // Build the parent udta atom
        val udta = ByteArrayOutputStream()
        udta.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(metaAtomBytes.size + 8).array())
        udta.write("udta".encodeToByteArray())
        udta.write(metaAtomBytes)

        return udta.toByteArray()
    }

    private fun addM4aTextAtom(ilist: ByteArrayOutputStream, name: String, value: String) {
        val textBytes = value.encodeToByteArray()
        // data atom: size(4) + "data"(4) + type(4) + locale(4) + value
        val dataPayload = ByteArrayOutputStream()
        dataPayload.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array()) // type indicator = 0 (UTF-8 text)
        dataPayload.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array()) // locale = 0
        dataPayload.write(textBytes)

        val dataBytes = dataPayload.toByteArray()
        val dataAtom = ByteArrayOutputStream()
        dataAtom.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(dataBytes.size + 8).array())
        dataAtom.write("data".encodeToByteArray())
        dataAtom.write(dataBytes)

        val dataAtomBytes = dataAtom.toByteArray()

        // mean atom
        val meanBytes = "com.apple.iTunes".encodeToByteArray()
        val meanAtom = ByteArrayOutputStream()
        meanAtom.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(meanBytes.size + 8).array())
        meanAtom.write("mean".encodeToByteArray())
        meanAtom.write(meanBytes)

        // name atom
        val nameBytes = name.encodeToByteArray()
        val nameAtom = ByteArrayOutputStream()
        nameAtom.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(nameBytes.size + 8).array())
        nameAtom.write("name".encodeToByteArray())
        nameAtom.write(nameBytes)

        val meanAtomBytes = meanAtom.toByteArray()
        val nameAtomBytes = nameAtom.toByteArray()

        // Build the item atom
        val itemData = ByteArrayOutputStream()
        itemData.write(meanAtomBytes)
        itemData.write(nameAtomBytes)
        itemData.write(dataAtomBytes)

        val itemBytes = itemData.toByteArray()
        ilist.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(itemBytes.size + 8).array())
        ilist.write(name.encodeToByteArray())
        ilist.write(itemBytes)
    }

    private fun addM4aCoverArtAtom(ilist: ByteArrayOutputStream, imageData: ByteArray) {
        val mimeType = detectImageMimeType(imageData)
        val typeIndicator = if (mimeType == "image/jpeg") 13 else 14 // 13=JPEG, 14=PNG

        val dataPayload = ByteArrayOutputStream()
        dataPayload.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(typeIndicator).array())
        dataPayload.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array()) // locale
        dataPayload.write(imageData)

        val dataBytes = dataPayload.toByteArray()
        val dataAtom = ByteArrayOutputStream()
        dataAtom.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(dataBytes.size + 8).array())
        dataAtom.write("data".encodeToByteArray())
        dataAtom.write(dataBytes)

        val dataAtomBytes = dataAtom.toByteArray()

        ilist.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(dataAtomBytes.size + 8).array())
        ilist.write("covr".encodeToByteArray())
        ilist.write(dataAtomBytes)
    }

    private fun writeOggMetadata(file: File, metadata: Metadata) {
    }

    private fun detectImageMimeType(bytes: ByteArray): String = when {
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte() -> "image/jpeg"

        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte() -> "image/png"

        else -> "image/jpeg"
    }
}

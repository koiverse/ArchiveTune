/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.player

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.serializer
import moe.rukamori.archivetune.canvas.models.CanvasArtwork
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.storage.StorageFolderKind
import moe.rukamori.archivetune.storage.StorageLocationRepository
import moe.rukamori.archivetune.utils.StreamClientUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit

object CanvasArtworkPlaybackCache {
    private const val defaultMaxSize = 256
    private const val PERSIST_FILE = "canvas_artwork_cache.json"
    private const val PERSIST_DEBOUNCE_MS = 2_000L
    private const val DOWNLOAD_BUFFER_SIZE_BYTES = 64 * 1024

    private val map = LinkedHashMap<String, CanvasCacheEntry>(defaultMaxSize, 0.75f, true)
    @Volatile private var maxSize = defaultMaxSize
    @Volatile private var cacheDirectory: File? = null
    @Volatile private var cacheFile: File? = null

    private val persistScope = CoroutineScope(Dispatchers.IO)
    private var persistJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .proxy(YouTube.streamOkHttpProxy)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(3, TimeUnit.MINUTES)
            .addInterceptor { chain ->
                val request = chain.request()
                if (!request.url.isYouTubeMediaHost()) {
                    return@addInterceptor chain.proceed(
                        request.newBuilder()
                            .header("User-Agent", CanvasDownloadUserAgent)
                            .build(),
                    )
                }
                val requestProfile = StreamClientUtils.resolveRequestProfile(request.url)
                chain.proceed(
                    StreamClientUtils.applyRequestProfile(
                        request.newBuilder(),
                        requestProfile,
                    ).build(),
                )
            }
            .build()
    }

    fun init(context: Context) {
        val directory = StorageLocationRepository.cacheDirectory(context, StorageFolderKind.CANVAS_CACHE)
        cacheDirectory = directory
        cacheFile = directory.resolve(PERSIST_FILE)
        loadFromDisk()
    }

    @Synchronized
    fun get(mediaId: String): CanvasArtwork? {
        if (maxSize <= 0 || mediaId.isBlank()) return null
        val entry = map[mediaId] ?: return null
        val playable = entry.toPlayableArtwork(cacheDirectory ?: return null)
        if (playable == null) {
            map.remove(mediaId)
            schedulePersist()
            return null
        }
        map[mediaId] = entry.copy(lastAccessedAtMs = System.currentTimeMillis())
        schedulePersist()
        return playable
    }

    suspend fun put(mediaId: String, artwork: CanvasArtwork): CanvasArtwork =
        withContext(Dispatchers.IO) {
            if (maxSize <= 0 || mediaId.isBlank()) return@withContext artwork
            val directory = cacheDirectory ?: return@withContext artwork
            directory.mkdirs()

            val current = synchronized(this@CanvasArtworkPlaybackCache) { map[mediaId] }
            val regularFileName = cacheCanvasVideo(
                directory = directory,
                mediaId = mediaId,
                variant = CanvasVideoVariant.Regular,
                url = artwork.downloadableRegularUrl(),
                currentFileName = current?.regularFileName,
            )
            persistEntry(
                directory = directory,
                entry = CanvasCacheEntry(
                    mediaId = mediaId,
                    artwork = artwork,
                    regularFileName = regularFileName,
                    verticalFileName = current?.verticalFileName,
                    createdAtMs = current?.createdAtMs ?: System.currentTimeMillis(),
                    lastAccessedAtMs = System.currentTimeMillis(),
                ),
            )
            val verticalFileName = cacheCanvasVideo(
                directory = directory,
                mediaId = mediaId,
                variant = CanvasVideoVariant.Vertical,
                url = artwork.downloadableVerticalUrl(),
                currentFileName = current?.verticalFileName,
            )

            val now = System.currentTimeMillis()
            val entry = CanvasCacheEntry(
                mediaId = mediaId,
                artwork = artwork,
                regularFileName = regularFileName,
                verticalFileName = verticalFileName,
                createdAtMs = current?.createdAtMs ?: now,
                lastAccessedAtMs = now,
            )

            persistEntry(directory = directory, entry = entry)

            entry.toPlayableArtwork(directory) ?: artwork
        }

    @Synchronized
    fun size(): Int = map.count { (_, entry) ->
        entry.hasPlayableFile(cacheDirectory)
    }

    @Synchronized
    fun byteSize(): Long {
        val directory = cacheDirectory ?: return 0L
        return map.values.sumOf { entry -> entry.byteSize(directory) }
    }

    @Synchronized
    fun clear() {
        clearFilesLocked()
        map.clear()
        schedulePersist()
    }

    fun clearAndPersist(): Boolean {
        synchronized(this) {
            clearFilesLocked()
            map.clear()
            persistJob?.cancel()
        }
        return writeToDisk()
    }

    @Synchronized
    fun setMaxSize(value: Int) {
        maxSize = value.coerceAtLeast(0)
        val directory = cacheDirectory
        if (maxSize == 0) {
            clearFilesLocked()
            map.clear()
            schedulePersist()
            return
        }
        if (directory != null) {
            trimLocked(directory)
        } else {
            trimMetadataLocked()
        }
        schedulePersist()
    }

    @Synchronized
    private fun loadFromDisk() {
        val file = cacheFile ?: return
        if (!file.exists()) return
        try {
            val raw = file.readText()
            if (raw.isBlank()) return
            val restored = decodeEntries(raw)
            map.clear()
            restored
                .filter { entry -> entry.mediaId.isNotBlank() }
                .forEach { entry -> map[entry.mediaId] = entry }
            cacheDirectory?.let(::trimLocked) ?: trimMetadataLocked()
            Timber.d("Canvas cache restored: ${map.size} entries from disk")
        } catch (error: Exception) {
            Timber.e(error, "Failed to restore canvas cache from disk")
            runCatching { file.delete() }
        }
    }

    private fun decodeEntries(raw: String): List<CanvasCacheEntry> =
        runCatching {
            json.decodeFromString(ListSerializer(CanvasCacheEntry.serializer()), raw)
        }.getOrElse {
            val legacy = json.decodeFromString(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    CanvasArtwork.serializer(),
                ),
                raw,
            )
            val now = System.currentTimeMillis()
            legacy.map { (mediaId, artwork) ->
                CanvasCacheEntry(
                    mediaId = mediaId,
                    artwork = artwork,
                    regularFileName = null,
                    verticalFileName = null,
                    createdAtMs = now,
                    lastAccessedAtMs = now,
                )
            }
        }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = persistScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            writeToDisk()
        }
    }

    private fun persistEntry(
        directory: File,
        entry: CanvasCacheEntry,
    ) {
        synchronized(this@CanvasArtworkPlaybackCache) {
            map[entry.mediaId] = entry
            trimLocked(directory)
            schedulePersist()
        }
    }

    private fun writeToDisk(): Boolean {
        val file = cacheFile ?: return true
        return try {
            val snapshot: List<CanvasCacheEntry>
            synchronized(this@CanvasArtworkPlaybackCache) {
                snapshot = map.values.toList()
            }
            val raw = json.encodeToString(ListSerializer(CanvasCacheEntry.serializer()), snapshot)
            file.parentFile?.mkdirs()
            file.writeText(raw)
            true
        } catch (error: Exception) {
            Timber.e(error, "Failed to persist canvas cache to disk")
            false
        }
    }

    private suspend fun cacheCanvasVideo(
        directory: File,
        mediaId: String,
        variant: CanvasVideoVariant,
        url: String?,
        currentFileName: String?,
    ): String? {
        if (url.isNullOrBlank()) return currentFileName?.takeIf { directory.resolve(it).isUsableFile() }
        val fileName = canvasFileName(mediaId, variant, url)
        val target = directory.resolve(fileName)
        if (target.isUsableFile()) return fileName
        currentFileName
            ?.takeIf { it != fileName }
            ?.let { oldName -> runCatching { directory.resolve(oldName).delete() } }

        val partial = directory.resolve("$fileName.part")
        return try {
            downloadToFile(url = url, target = partial)
            if (partial.length() <= 0L) throw IOException("Downloaded empty canvas video")
            if (target.exists() && !target.delete()) throw IOException("Failed to replace existing canvas video")
            if (!partial.renameTo(target)) throw IOException("Failed to commit canvas video")
            fileName
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Timber.w(error, "Failed to cache canvas video")
            runCatching { partial.delete() }
            currentFileName?.takeIf { directory.resolve(it).isUsableFile() }
        }
    }

    private suspend fun downloadToFile(url: String, target: File) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Canvas request failed: HTTP ${response.code}")
            val body = response.body ?: throw IOException("Canvas response body is empty")
            val contentType = body.contentType()?.toString()?.lowercase(Locale.ROOT).orEmpty()
            if (
                contentType.contains("mpegurl") ||
                contentType.contains("m3u8") ||
                contentType.startsWith("text/") ||
                contentType.startsWith("image/") ||
                contentType.contains("json")
            ) {
                throw IOException("Canvas response is not a downloadable video: $contentType")
            }
            target.parentFile?.mkdirs()
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE_BYTES)
                    while (true) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private fun trimLocked(directory: File) {
        trimMetadataLocked()
        val activeFiles = map.values.flatMap { entry ->
            listOfNotNull(entry.regularFileName, entry.verticalFileName)
        }.toSet()
        directory.listFiles()
            ?.filter { file -> file.isFile && file.name.endsWith(".mp4") && file.name !in activeFiles }
            ?.forEach { file -> runCatching { file.delete() } }
    }

    private fun trimMetadataLocked() {
        while (map.size > maxSize) {
            val iterator = map.entries.iterator()
            if (!iterator.hasNext()) break
            val entry = iterator.next().value
            iterator.remove()
            cacheDirectory?.let { directory ->
                runCatching { entry.regularFileName?.let { directory.resolve(it).delete() } }
                runCatching { entry.verticalFileName?.let { directory.resolve(it).delete() } }
            }
        }
    }

    private fun clearFilesLocked() {
        val directory = cacheDirectory ?: return
        map.values.forEach { entry ->
            runCatching { entry.regularFileName?.let { directory.resolve(it).delete() } }
            runCatching { entry.verticalFileName?.let { directory.resolve(it).delete() } }
        }
        directory.listFiles()
            ?.filter { file -> file.isFile && (file.name.endsWith(".mp4") || file.name.endsWith(".part")) }
            ?.forEach { file -> runCatching { file.delete() } }
    }

    private fun canvasFileName(
        mediaId: String,
        variant: CanvasVideoVariant,
        url: String,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$mediaId|${variant.cacheKey}|$url".toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
        return "${variant.cacheKey}-$digest.mp4"
    }

    private fun CanvasArtwork.downloadableRegularUrl(): String? =
        videoUrl.takeIfDownloadableVideo() ?: animated.takeIfDownloadableVideo()

    private fun CanvasArtwork.downloadableVerticalUrl(): String? =
        videoUrlVertical.takeIfDownloadableVideo() ?: animatedVertical.takeIfDownloadableVideo()

    private fun String?.takeIfDownloadableVideo(): String? =
        this
            ?.trim()
            ?.takeIf { value ->
                val normalized = value.lowercase(Locale.ROOT)
                value.isNotBlank() &&
                    !normalized.contains(".m3u8") &&
                    !normalized.contains("application/x-mpegurl") &&
                    (normalized.startsWith("http://") || normalized.startsWith("https://"))
            }

    @Serializable
    private data class CanvasCacheEntry(
        val mediaId: String,
        val artwork: CanvasArtwork,
        val regularFileName: String? = null,
        val verticalFileName: String? = null,
        val createdAtMs: Long,
        val lastAccessedAtMs: Long,
    ) {
        fun hasPlayableFile(directory: File?): Boolean {
            directory ?: return false
            return regularFileName?.let { directory.resolve(it).isUsableFile() } == true ||
                verticalFileName?.let { directory.resolve(it).isUsableFile() } == true
        }

        fun byteSize(directory: File): Long =
            listOfNotNull(regularFileName, verticalFileName)
                .sumOf { fileName ->
                    directory.resolve(fileName)
                        .takeIf { file -> file.isUsableFile() }
                        ?.length()
                        ?: 0L
                }

        fun toPlayableArtwork(directory: File): CanvasArtwork? {
            val regularUri = regularFileName
                ?.let(directory::resolve)
                ?.takeIf { file -> file.isUsableFile() }
                ?.let { file -> Uri.fromFile(file).toString() }
            val verticalUri = verticalFileName
                ?.let(directory::resolve)
                ?.takeIf { file -> file.isUsableFile() }
                ?.let { file -> Uri.fromFile(file).toString() }
            if (regularUri == null && verticalUri == null) return null
            return artwork.copy(
                animated = regularUri,
                videoUrl = regularUri,
                animatedVertical = verticalUri,
                videoUrlVertical = verticalUri,
            )
        }
    }

    private enum class CanvasVideoVariant(
        val cacheKey: String,
    ) {
        Regular(cacheKey = "regular"),
        Vertical(cacheKey = "vertical"),
    }
}

private fun okhttp3.HttpUrl.isYouTubeMediaHost(): Boolean {
    val normalizedHost = host.lowercase(Locale.ROOT)
    return normalizedHost.endsWith("googlevideo.com") ||
        normalizedHost.endsWith("googleusercontent.com") ||
        normalizedHost.endsWith("youtube.com") ||
        normalizedHost.endsWith("youtube-nocookie.com") ||
        normalizedHost.endsWith("ytimg.com")
}

private fun File.isUsableFile(): Boolean = isFile && length() > 0L

private const val CanvasDownloadUserAgent =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"

/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.media3.datasource.cache.Cache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.constants.StorageFolderDisplayNameKey
import moe.rukamori.archivetune.constants.StorageFolderPathKey
import moe.rukamori.archivetune.constants.StorageFolderTreeUriKey
import moe.rukamori.archivetune.di.DownloadCache
import moe.rukamori.archivetune.di.PlayerCache
import moe.rukamori.archivetune.utils.PreferenceStore
import moe.rukamori.archivetune.utils.dataStore
import java.io.File
import javax.inject.Inject

enum class StorageFolderKind(
    val defaultDirectoryName: String,
) {
    SONG_CACHE(defaultDirectoryName = "exoplayer"),
    DOWNLOADS(defaultDirectoryName = "download"),
    IMAGE_CACHE(defaultDirectoryName = "coil"),
    CANVAS_CACHE(defaultDirectoryName = "canvas"),
    ARTWORK_CACHE(defaultDirectoryName = "artwork"),
}

@Immutable
data class StorageFolderSelection(
    val displayName: String,
    val isCustom: Boolean,
)

sealed interface StorageFolderUpdateResult {
    data object Success : StorageFolderUpdateResult
    data object InvalidTree : StorageFolderUpdateResult
    data object UnsupportedProvider : StorageFolderUpdateResult
    data object NotWritable : StorageFolderUpdateResult
}

object StorageRestartScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var restartJob: Job? = null

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(AppRestartDelayMillis)
            appContext.restartApp()
        }
    }
}

class ObserveStorageFoldersUseCase
@Inject
constructor(
    private val repository: StorageLocationRepository,
) {
    operator fun invoke(): Flow<StorageFolderSelection> = repository.selection
}

class SetStorageFolderUseCase
@Inject
constructor(
    private val repository: StorageLocationRepository,
) {
    suspend operator fun invoke(uri: Uri): StorageFolderUpdateResult =
        repository.setFolderAndMoveCache(uri)
}

class ResetStorageFolderUseCase
@Inject
constructor(
    private val repository: StorageLocationRepository,
) {
    suspend operator fun invoke(): StorageFolderUpdateResult =
        repository.resetFolderAndMoveCache()
}

class StorageLocationRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @PlayerCache private val playerCache: Cache,
    @DownloadCache private val downloadCache: Cache,
) {
    val selection: Flow<StorageFolderSelection> =
        context.dataStore.data.map { preferences ->
            preferences.selectionFor(context.defaultStorageRootDirectory())
        }

    suspend fun setFolderAndMoveCache(uri: Uri): StorageFolderUpdateResult = withContext(Dispatchers.IO) {
        if (!DocumentsContract.isTreeUri(uri)) return@withContext StorageFolderUpdateResult.InvalidTree

        val directory = resolveTreeDirectory(uri) ?: return@withContext StorageFolderUpdateResult.UnsupportedProvider
        val targetRoot = directory.canonicalFile
        if (!targetRoot.ensureStorageRoot()) return@withContext StorageFolderUpdateResult.NotWritable

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val permissionPersisted = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }.isSuccess
        if (!permissionPersisted) return@withContext StorageFolderUpdateResult.NotWritable

        val preferencesSnapshot = context.dataStore.data.first()
        val previousUri = preferencesSnapshot[StorageFolderTreeUriKey]
        val movedCache = moveAllCacheDirectories(preferencesSnapshot) { kind ->
            targetRoot.resolve(kind.defaultDirectoryName)
        }
        if (!movedCache) {
            releasePersistedPermission(uri.toString(), replacementUri = null)
            return@withContext StorageFolderUpdateResult.NotWritable
        }
        context.dataStore.edit { preferences ->
            preferences[StorageFolderTreeUriKey] = uri.toString()
            preferences[StorageFolderPathKey] = targetRoot.canonicalPath
            preferences[StorageFolderDisplayNameKey] = targetRoot.displayPath()
        }
        context.storageLocationPreferences()
            .edit()
            .putString(StorageRootPathMirrorKey, targetRoot.canonicalPath)
            .apply()
        releasePersistedPermission(previousUri, uri.toString())
        StorageRestartScheduler.schedule(context)
        StorageFolderUpdateResult.Success
    }

    suspend fun resetFolderAndMoveCache(): StorageFolderUpdateResult = withContext(Dispatchers.IO) {
        val preferencesSnapshot = context.dataStore.data.first()
        val previousUri = preferencesSnapshot[StorageFolderTreeUriKey]
        val movedCache = moveAllCacheDirectories(preferencesSnapshot) { kind ->
            context.defaultCacheDirectory(kind)
        }
        if (!movedCache) {
            return@withContext StorageFolderUpdateResult.NotWritable
        }
        context.dataStore.edit { preferences ->
            preferences.remove(StorageFolderTreeUriKey)
            preferences.remove(StorageFolderPathKey)
            preferences.remove(StorageFolderDisplayNameKey)
        }
        context.storageLocationPreferences()
            .edit()
            .remove(StorageRootPathMirrorKey)
            .apply()
        releasePersistedPermission(previousUri, replacementUri = null)
        StorageRestartScheduler.schedule(context)
        StorageFolderUpdateResult.Success
    }

    private fun Preferences.selectionFor(
        defaultDirectory: File,
    ): StorageFolderSelection {
        val selectedPath = this[StorageFolderPathKey]?.takeIf(String::isNotBlank)
        val selectedUri = this[StorageFolderTreeUriKey]?.takeIf(String::isNotBlank)
        val selectedName = this[StorageFolderDisplayNameKey]?.takeIf(String::isNotBlank)
        return StorageFolderSelection(
            displayName = selectedName ?: defaultDirectory.displayPath(),
            isCustom = selectedPath != null && selectedUri != null,
        )
    }

    private fun releasePersistedPermission(previousUri: String?, replacementUri: String?) {
        if (previousUri.isNullOrBlank() || previousUri == replacementUri) return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                previousUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    private fun moveAllCacheDirectories(
        preferences: Preferences,
        targetDirectory: (StorageFolderKind) -> File,
    ): Boolean =
        runCatching {
            releaseCachesForMigration()
            StorageFolderKind.entries.forEach { kind ->
                moveCacheDirectory(
                    source = activeCacheDirectory(preferences, kind),
                    target = targetDirectory(kind),
                )
            }
            moveLegacyCanvasFile(targetDirectory(StorageFolderKind.CANVAS_CACHE))
            moveLegacyArtworkFile(targetDirectory(StorageFolderKind.ARTWORK_CACHE))
        }.isSuccess

    private fun activeCacheDirectory(preferences: Preferences, kind: StorageFolderKind): File {
        val configuredPath = preferences[StorageFolderPathKey]?.takeIf(String::isNotBlank)
        val configuredDirectory = configuredPath?.let(::File)?.resolve(kind.defaultDirectoryName)
        return configuredDirectory ?: context.defaultCacheDirectory(kind)
    }

    private fun releaseCachesForMigration() {
        runCatching { playerCache.release() }
        runCatching { downloadCache.release() }
    }

    private fun moveCacheDirectory(source: File, target: File) {
        val canonicalSource = source.canonicalFile
        val canonicalTarget = target.canonicalFile
        if (canonicalSource == canonicalTarget) {
            canonicalTarget.ensureWritableDirectory()
            return
        }
        if (!canonicalSource.exists()) {
            canonicalTarget.ensureWritableDirectory()
            return
        }
        canonicalTarget.deleteRecursively()
        canonicalTarget.parentFile?.mkdirs()
        canonicalSource.copyRecursively(canonicalTarget, overwrite = true)
        canonicalSource.deleteRecursively()
    }

    private fun moveLegacyCanvasFile(targetDirectory: File) {
        moveLegacyFile(
            source = context.filesDir.resolve(CanvasArtworkCacheFileName),
            target = targetDirectory.resolve(CanvasArtworkCacheFileName),
        )
    }

    private fun moveLegacyArtworkFile(targetDirectory: File) {
        moveLegacyFile(
            source = context.filesDir.resolve(SavedArtworkCacheFileName),
            target = targetDirectory.resolve(SavedArtworkCacheFileName),
        )
    }

    private fun moveLegacyFile(source: File, target: File) {
        val canonicalSource = source.canonicalFile
        val canonicalTarget = target.canonicalFile
        if (canonicalSource == canonicalTarget || !canonicalSource.exists()) return
        canonicalTarget.parentFile?.mkdirs()
        canonicalSource.copyTo(canonicalTarget, overwrite = true)
        canonicalSource.delete()
    }

    companion object {
        fun cacheDirectory(context: Context, kind: StorageFolderKind): File {
            val configuredPath = context.storageLocationPreferences()
                .getString(StorageRootPathMirrorKey, null)
                ?.takeIf(String::isNotBlank)
                ?: PreferenceStore.get(StorageFolderPathKey)?.takeIf(String::isNotBlank)
            val configuredRootDirectory = configuredPath?.let(::File)
            val configuredDirectory = configuredRootDirectory?.resolve(kind.defaultDirectoryName)
            return configuredDirectory
                ?.takeIf { it.ensureWritableDirectory() }
                ?: context.defaultCacheDirectory(kind)
        }

        fun cacheFile(context: Context, kind: StorageFolderKind, fileName: String): File =
            cacheDirectory(context, kind).resolve(fileName)
    }
}

private fun Context.defaultCacheDirectory(kind: StorageFolderKind): File =
    when (kind) {
        StorageFolderKind.IMAGE_CACHE -> cacheDir.resolve(kind.defaultDirectoryName)
        else -> filesDir.resolve(kind.defaultDirectoryName)
    }

private fun Context.defaultStorageRootDirectory(): File =
    filesDir

private fun Context.restartApp() {
    packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(launchIntent)
    }
    kotlin.system.exitProcess(0)
}

private fun Context.storageLocationPreferences() =
    getSharedPreferences(StorageLocationPreferencesName, Context.MODE_PRIVATE)

private fun resolveTreeDirectory(uri: Uri): File? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
    if (documentId.startsWith("raw:", ignoreCase = true)) {
        return File(documentId.removePrefix("raw:"))
    }

    val volumeId = documentId.substringBefore(':', missingDelimiterValue = documentId)
    val relativePath = documentId.substringAfter(':', missingDelimiterValue = "")
    val root = when {
        volumeId.equals("primary", ignoreCase = true) -> Environment.getExternalStorageDirectory()
        volumeId.equals("home", ignoreCase = true) -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        volumeId.isNotBlank() && volumeId.all { it.isLetterOrDigit() || it == '-' } -> File("/storage", volumeId)
        else -> return null
    }
    return if (relativePath.isBlank()) root else File(root, relativePath)
}

private fun File.ensureWritableDirectory(): Boolean =
    runCatching {
        if (exists() && !isDirectory) return@runCatching false
        if (!exists() && !mkdirs()) return@runCatching false
        val probe = File(this, ".archivetune-storage-probe")
        probe.writeText("ok")
        probe.delete()
    }.isSuccess

private fun File.ensureStorageRoot(): Boolean =
    ensureWritableDirectory() &&
        StorageFolderKind.entries.all { kind ->
            resolve(kind.defaultDirectoryName).ensureWritableDirectory()
        }

private fun File.displayPath(): String =
    absolutePath
        .replace('\\', '/')
        .replace(Regex("/+"), "/")

private const val StorageLocationPreferencesName = "storage_locations"
private const val StorageRootPathMirrorKey = "storage_root_path"
private const val AppRestartDelayMillis = 3_000L
private const val CanvasArtworkCacheFileName = "canvas_artwork_cache.json"
private const val SavedArtworkCacheFileName = "archivetune_saved_artworks.json"

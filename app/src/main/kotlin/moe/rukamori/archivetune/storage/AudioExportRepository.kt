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
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.constants.AudioExportPathKey
import moe.rukamori.archivetune.constants.AudioExportUriKey
import moe.rukamori.archivetune.utils.PreferenceStore
import moe.rukamori.archivetune.utils.dataStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class AudioExportRepository
@Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun resolveExportDirectory(): File {
        val configuredPath = PreferenceStore.get(AudioExportPathKey)
            ?.takeIf(String::isNotBlank)
        val configuredUri = PreferenceStore.get(AudioExportUriKey)
            ?.takeIf(String::isNotBlank)

        configuredPath?.let { path ->
            val file = File(path)
            if (file.exists() || file.mkdirs()) return file
        }

        configuredUri?.let { uriString ->
            val uri = uriString.toUri()
            val realPath = resolveSafUriToPath(uri)
            if (realPath != null && (realPath.exists() || realPath.mkdirs())) return realPath
        }

        return defaultExportDirectory()
    }

    fun resolveExportDirectorySafe(): File {
        val dir = resolveExportDirectory()
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isExportDirectoryConfigured(): Boolean =
        PreferenceStore.get(AudioExportPathKey)?.isNotBlank() == true ||
            PreferenceStore.get(AudioExportUriKey)?.isNotBlank() == true

    fun getExportDirectoryDisplayPath(): String {
        val path = PreferenceStore.get(AudioExportPathKey)?.takeIf(String::isNotBlank)
        val uri = PreferenceStore.get(AudioExportUriKey)?.takeIf(String::isNotBlank)
        return path ?: uri ?: context.getString(
            moe.rukamori.archivetune.R.string.audio_export_not_configured
        )
    }

    suspend fun setExportDirectory(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val docFile = DocumentFile.fromTreeUri(context, uri)
                ?: return@runCatching error("Invalid URI")

            if (!docFile.exists()) {
                val treeDocId = DocumentsContract.getTreeDocumentId(docFile.uri)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(docFile.uri, treeDocId)
                val createdUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    docFile.name ?: return@runCatching error("Cannot get directory name"),
                )
                if (createdUri == null) {
                    return@runCatching error("Cannot create directory")
                }
            }

            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }

            val realPath = resolveSafUriToPath(uri)
                ?: return@runCatching error("Cannot resolve directory path")

            if (!realPath.exists() && !realPath.mkdirs()) {
                return@runCatching error("Cannot create directory")
            }

            context.dataStore.edit {
                it[AudioExportUriKey] = uri.toString()
                it[AudioExportPathKey] = realPath.absolutePath
            }

            realPath
        }
    }

    suspend fun resetToDefault() = withContext(Dispatchers.IO) {
        context.dataStore.edit {
            it.remove(AudioExportUriKey)
            it.remove(AudioExportPathKey)
        }
    }

    suspend fun ensureExportDirectory(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = resolveExportDirectory()
            if (!dir.exists() && !dir.mkdirs()) {
                return@runCatching error("Cannot create export directory")
            }
            dir
        }
    }

    private fun defaultExportDirectory(): File {
        val musicDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MUSIC,
        )
        val dir = File(musicDir, EXPORT_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun resolveSafUriToPath(uri: Uri): File? {
        if (!DocumentsContract.isTreeUri(uri)) return null
        val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }
            .getOrNull()
            ?: return null

        val relativePath = treeDocumentId.substringAfter(':', "")
        if (relativePath.isBlank()) {
            return Environment.getExternalStorageDirectory()
        }

        val segments = treeDocumentId.split(":")
        val storageType = segments.firstOrNull() ?: return null

        return when {
            storageType.equals("primary", ignoreCase = true) -> {
                File(Environment.getExternalStorageDirectory(), relativePath)
            }
            else -> {
                File("/storage/$storageType", relativePath)
            }
        }
    }

    companion object {
        private const val EXPORT_DIR_NAME = "ArchiveTune"
    }
}

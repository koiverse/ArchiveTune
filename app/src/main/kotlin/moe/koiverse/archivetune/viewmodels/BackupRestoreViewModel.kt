package moe.koiverse.archivetune.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import moe.koiverse.archivetune.MainActivity
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.InternalDatabase
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.ArtistEntity
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.db.entities.SongEntity
import moe.koiverse.archivetune.extensions.div
import moe.koiverse.archivetune.extensions.tryOrNull
import moe.koiverse.archivetune.extensions.zipInputStream
import moe.koiverse.archivetune.extensions.zipOutputStream
import moe.koiverse.archivetune.playback.MusicService
import moe.koiverse.archivetune.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess
import moe.koiverse.archivetune.utils.dataStore

import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {

    fun backup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.applicationContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.buffered().zipOutputStream().use { zipStream ->
                        // 1. Backup Settings to XML
                        zipStream.putNextEntry(ZipEntry(SETTINGS_XML_FILENAME))
                        writeSettingsToXml(context, zipStream)
                        zipStream.closeEntry()

                        database.checkpoint()

                        val dbFile = context.getDatabasePath(InternalDatabase.DB_NAME)
                        val dbFiles = listOf(
                            dbFile,
                            dbFile.resolveSibling("${InternalDatabase.DB_NAME}-wal"),
                            dbFile.resolveSibling("${InternalDatabase.DB_NAME}-shm"),
                            dbFile.resolveSibling("${InternalDatabase.DB_NAME}-journal"),
                        )
                        dbFiles.filter { it.exists() }.forEach { file ->
                            zipStream.putNextEntry(ZipEntry(file.name))
                            FileInputStream(file).use { input ->
                                input.copyTo(zipStream)
                            }
                            zipStream.closeEntry()
                        }
                    }
                } ?: throw IllegalStateException("Failed to open output stream")
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
                }
            }.onFailure { exception ->
                reportException(exception)
                withContext(Dispatchers.Main) {
                    val msg = exception.message ?: context.getString(R.string.backup_create_failed)
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun restore(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Verify file first
                context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                    var hasSettings = false
                    var hasDb = false
                    stream.zipInputStream().use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            when (entry.name) {
                                SETTINGS_XML_FILENAME, SETTINGS_FILENAME -> hasSettings = true
                                InternalDatabase.DB_NAME -> hasDb = true
                            }
                            entry = zip.nextEntry
                        }
                    }
                    if (!hasDb) throw IllegalStateException("Backup missing database")
                }

                // Perform restore
                context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.zipInputStream().use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            when (entry.name) {
                                SETTINGS_XML_FILENAME -> {
                                    // Parse XML and update DataStore
                                    restoreSettingsFromXml(context, zip)
                                }
                                SETTINGS_FILENAME -> {
                                    // Legacy binary restore
                                    val settingsDir = context.filesDir / "datastore"
                                    if (!settingsDir.exists()) settingsDir.mkdirs()
                                    (settingsDir / SETTINGS_FILENAME).outputStream().use { out ->
                                        zip.copyTo(out)
                                    }
                                }
                                InternalDatabase.DB_NAME,
                                "${InternalDatabase.DB_NAME}-wal",
                                "${InternalDatabase.DB_NAME}-shm",
                                "${InternalDatabase.DB_NAME}-journal" -> {
                                    database.checkpoint()
                                    database.close()
                                    val dbFile = context.getDatabasePath(entry.name)
                                    if (dbFile.exists()) {
                                        dbFile.delete()
                                    }
                                    FileOutputStream(dbFile).use { out ->
                                        zip.copyTo(out)
                                    }
                                }
                            }
                            entry = zip.nextEntry
                        }
                    }
                }

                // Restart logic
                withContext(Dispatchers.Main) {
                   Toast.makeText(context, "Restore successful", Toast.LENGTH_SHORT).show()
                }
                
                // Cleanup
                try { context.stopService(Intent(context, MusicService::class.java)) } catch (_: Exception) {}
                try { context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete() } catch (_: Exception) {}
                
             }.onSuccess {
                 context.startActivity(Intent(context, MainActivity::class.java))
                 exitProcess(0)
             }.onFailure { e ->
                reportException(e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message ?: context.getString(R.string.restore_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun writeSettingsToXml(context: Context, outputStream: java.io.OutputStream) {
        val prefs = context.dataStore.data.first().asMap()
        val serializer = android.util.Xml.newSerializer()
        serializer.setOutput(outputStream, "UTF-8")
        serializer.startDocument("UTF-8", true)
        serializer.startTag(null, "ArchiveTuneBackup")
        serializer.startTag(null, "Settings")

        for ((key, value) in prefs) {
            val tagName = when (value) {
                is Boolean -> "boolean"
                is Int -> "int"
                is Long -> "long"
                is Float -> "float"
                is String -> "string"
                is Set<*> -> "string-set"
                else -> null
            }
            if (tagName != null) {
                serializer.startTag(null, tagName)
                serializer.attribute(null, "name", key.name)
                if (value is Set<*>) {
                    value.forEach { item ->
                        serializer.startTag(null, "item")
                        serializer.text(item.toString())
                        serializer.endTag(null, "item")
                    }
                } else {
                    serializer.attribute(null, "value", value.toString())
                }
                serializer.endTag(null, tagName)
            }
        }

        serializer.endTag(null, "Settings")
        serializer.endTag(null, "ArchiveTuneBackup")
        serializer.endDocument()
        serializer.flush()
    }

    private suspend fun restoreSettingsFromXml(context: Context, inputStream: java.io.InputStream) {
        // Read full content to avoid ZipInputStream issues with XmlPullParser
        val content = inputStream.bufferedReader().use { it.readText() }
        if (content.isEmpty()) return

        val parser = android.util.Xml.newPullParser()
        parser.setInput(java.io.StringReader(content))
        
        var eventType = parser.eventType
        // Track restored count for debugging/verification (could be logged or toasted)
        var restoredCount = 0
        
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                val name = parser.name
                val keyName = parser.getAttributeValue(null, "name")
                
                if (keyName != null) {
                    when (name) {
                        "boolean" -> {
                            val value = parser.getAttributeValue(null, "value")?.toBoolean()
                            if (value != null) {
                                context.dataStore.edit { it[androidx.datastore.preferences.core.booleanPreferencesKey(keyName)] = value }
                                restoredCount++
                            }
                        }
                        "int" -> {
                            val value = parser.getAttributeValue(null, "value")?.toIntOrNull()
                            if (value != null) {
                                context.dataStore.edit { it[androidx.datastore.preferences.core.intPreferencesKey(keyName)] = value }
                                restoredCount++
                            }
                        }
                        "long" -> {
                            val value = parser.getAttributeValue(null, "value")?.toLongOrNull()
                            if (value != null) {
                                context.dataStore.edit { it[androidx.datastore.preferences.core.longPreferencesKey(keyName)] = value }
                                restoredCount++
                            }
                        }
                        "float" -> {
                            val value = parser.getAttributeValue(null, "value")?.toFloatOrNull()
                            if (value != null) {
                                context.dataStore.edit { it[androidx.datastore.preferences.core.floatPreferencesKey(keyName)] = value }
                                restoredCount++
                            }
                        }
                        "string" -> {
                            val value = parser.getAttributeValue(null, "value")
                            if (value != null) {
                                context.dataStore.edit { it[androidx.datastore.preferences.core.stringPreferencesKey(keyName)] = value }
                                restoredCount++
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    // Keep existing import logic
    fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        val songs = arrayListOf<Song>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                fun parseCsvLine(line: String): List<String> {
                    val result = mutableListOf<String>()
                    var i = 0
                    val n = line.length
                    while (i < n) {
                        if (line[i] == '"') {
                            i++
                            val sb = StringBuilder()
                            while (i < n) {
                                if (line[i] == '"') {
                                    if (i + 1 < n && line[i + 1] == '"') {
                                        sb.append('"')
                                        i += 2
                                        continue
                                    } else {
                                        i++
                                        break
                                    }
                                }
                                sb.append(line[i])
                                i++
                            }
                            while (i < n && line[i] != ',') i++
                            if (i < n && line[i] == ',') i++
                            result.add(sb.toString())
                        } else {
                            val start = i
                            while (i < n && line[i] != ',') i++
                            result.add(line.substring(start, i).trim())
                            if (i < n && line[i] == ',') i++
                        }
                    }
                    return result
                }

                val lines = stream.bufferedReader().readLines()
                val cleaned = lines.map { it.trim() }.filter { it.isNotEmpty() }
                val dataLines = if (cleaned.isNotEmpty() && cleaned.first().lowercase().contains("title") && cleaned.first().lowercase().contains("artist")) {
                    cleaned.drop(1)
                } else cleaned

                dataLines.forEach { line ->
                    val parts = parseCsvLine(line)
                    if (parts.size < 2) return@forEach
                    val title = parts[0].trim().trim('\uFEFF')
                    val artistStr = parts[1].trim()
                    if (title.isEmpty()) return@forEach

                    val artists = artistStr.split(";").map { it.trim() }.filter { it.isNotEmpty() }.map {
                        ArtistEntity(
                            id = "",
                            name = it,
                        )
                    }

                    val mockSong = Song(
                        song = SongEntity(
                            id = "",
                            title = title,
                        ),
                        artists = if (artists.isEmpty()) listOf(ArtistEntity("", "")) else artists,
                    )
                    songs.add(mockSong)
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    fun loadM3UOnline(
        context: Context,
        uri: Uri,
    ): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                if (lines.first().startsWith("#EXTM3U")) {
                    lines.forEachIndexed { _, rawLine ->
                        if (rawLine.startsWith("#EXTINF:")) {
                            // maybe later write this to be more efficient
                            val artists =
                                rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                            val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title,
                                ),
                                artists = artists.map { ArtistEntity("", it) },
                            )
                            songs.add(mockSong)

                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"
        const val SETTINGS_XML_FILENAME = "settings.xml"
    }
}

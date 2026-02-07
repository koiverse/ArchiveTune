/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlin.math.roundToInt
import org.xmlpull.v1.XmlPullParser

data class BackupRestoreProgressUi(
    val title: String,
    val step: String,
    val percent: Int,
    val indeterminate: Boolean,
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    private val _backupRestoreProgress = MutableStateFlow<BackupRestoreProgressUi?>(null)
    val backupRestoreProgress: StateFlow<BackupRestoreProgressUi?> = _backupRestoreProgress.asStateFlow()

    private fun emitProgress(
        title: String,
        step: String,
        percent: Int,
        indeterminate: Boolean,
    ) {
        _backupRestoreProgress.value =
            BackupRestoreProgressUi(
                title = title,
                step = step,
                percent = percent.coerceIn(0, 100),
                indeterminate = indeterminate,
            )
    }

    fun backup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val title = context.getString(R.string.backup_in_progress)
            try {
                val dbFile = context.getDatabasePath(InternalDatabase.DB_NAME)
                val dbFiles =
                    listOf(
                        dbFile,
                        dbFile.resolveSibling("${InternalDatabase.DB_NAME}-wal"),
                        dbFile.resolveSibling("${InternalDatabase.DB_NAME}-shm"),
                        dbFile.resolveSibling("${InternalDatabase.DB_NAME}-journal"),
                    ).filter { it.exists() }

                val totalUnits = 2 + dbFiles.size
                val unitSpan = 100f / totalUnits.coerceAtLeast(1)
                var completedUnits = 0
                var lastPercent = -1
                var lastStep = ""

                fun emit(step: String, unitFraction: Float = 0f, indeterminate: Boolean = false) {
                    val p =
                        ((completedUnits + unitFraction.coerceIn(0f, 1f)) * unitSpan)
                            .roundToInt()
                            .coerceIn(0, 100)
                    if (p != lastPercent || step != lastStep) {
                        lastPercent = p
                        lastStep = step
                        emitProgress(
                            title = title,
                            step = step,
                            percent = p,
                            indeterminate = indeterminate,
                        )
                    }
                }

                context.applicationContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.buffered().zipOutputStream().use { zipStream ->
                        emit(context.getString(R.string.backup_step_export_settings), indeterminate = true)
                        zipStream.putNextEntry(ZipEntry(SETTINGS_XML_FILENAME))
                        writeSettingsToXml(context, zipStream)
                        zipStream.closeEntry()
                        completedUnits++

                        emit(context.getString(R.string.backup_step_checkpoint_database), indeterminate = true)
                        database.awaitIdle()
                        database.checkpoint()
                        completedUnits++

                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        dbFiles.forEach { file ->
                            val fileSize = file.length().coerceAtLeast(1L)
                            var bytesCopied = 0L
                            emit(
                                context.getString(R.string.backup_step_copying_file, file.name),
                                unitFraction = 0f,
                                indeterminate = false,
                            )
                            zipStream.putNextEntry(ZipEntry(file.name))
                            FileInputStream(file).use { input ->
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    zipStream.write(buffer, 0, read)
                                    bytesCopied += read
                                    emit(
                                        context.getString(R.string.backup_step_copying_file, file.name),
                                        unitFraction = bytesCopied.toFloat() / fileSize.toFloat(),
                                        indeterminate = false,
                                    )
                                }
                            }
                            zipStream.closeEntry()
                            completedUnits++
                        }
                    }
                } ?: throw IllegalStateException("Failed to open output stream")

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
                }
            } catch (exception: Exception) {
                reportException(exception)
                withContext(Dispatchers.Main) {
                    val msg = exception.message ?: context.getString(R.string.backup_create_failed)
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } finally {
                _backupRestoreProgress.value = null
            }
        }
    }

    fun restore(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val title = context.getString(R.string.restore_in_progress)
            try {
                emitProgress(
                    title = title,
                    step = context.getString(R.string.restore_step_verifying),
                    percent = 0,
                    indeterminate = true,
                )

                val entryNames = ArrayList<String>()
                var hasDb = false
                context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.zipInputStream().use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            entryNames.add(entry.name)
                            if (entry.name == InternalDatabase.DB_NAME) hasDb = true
                            entry = zip.nextEntry
                        }
                    }
                }
                if (!hasDb) throw IllegalStateException("Backup missing database")

                val restoreEntries =
                    entryNames.filter { name ->
                        name == SETTINGS_XML_FILENAME ||
                            name == SETTINGS_FILENAME ||
                            name == InternalDatabase.DB_NAME ||
                            name == "${InternalDatabase.DB_NAME}-wal" ||
                            name == "${InternalDatabase.DB_NAME}-shm" ||
                            name == "${InternalDatabase.DB_NAME}-journal"
                    }

                val totalUnits = 1 + 1 + restoreEntries.size
                val unitSpan = 100f / totalUnits.coerceAtLeast(1)
                var completedUnits = 0

                fun emit(step: String, indeterminate: Boolean) {
                    val p = (completedUnits * unitSpan).roundToInt().coerceIn(0, 100)
                    emitProgress(title = title, step = step, percent = p, indeterminate = indeterminate)
                }

                completedUnits++
                emit(context.getString(R.string.restore_step_stopping_playback), indeterminate = true)
                runCatching { context.stopService(Intent(context, MusicService::class.java)) }
                runCatching { database.awaitIdle() }
                runCatching { database.checkpoint() }
                runCatching { database.close() }
                completedUnits++

                context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.zipInputStream().use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (name !in restoreEntries) {
                                entry = zip.nextEntry
                                continue
                            }
                            when (name) {
                                SETTINGS_XML_FILENAME -> {
                                    emit(context.getString(R.string.restore_step_restoring_settings), indeterminate = true)
                                    restoreSettingsFromXml(context, zip)
                                }
                                SETTINGS_FILENAME -> {
                                    emit(context.getString(R.string.restore_step_restoring_settings), indeterminate = true)
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
                                    emit(context.getString(R.string.restore_step_restoring_file, name), indeterminate = true)
                                    val dbFile = context.getDatabasePath(name)
                                    if (dbFile.exists()) {
                                        dbFile.delete()
                                    }
                                    FileOutputStream(dbFile).use { out ->
                                        zip.copyTo(out)
                                    }
                                }
                            }
                            completedUnits++
                            entry = zip.nextEntry
                        }
                    }
                }

                emitProgress(
                    title = title,
                    step = context.getString(R.string.restore_step_restarting),
                    percent = 100,
                    indeterminate = true,
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.restore_success, Toast.LENGTH_SHORT).show()
                }

                try { context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete() } catch (_: Exception) {}

                _backupRestoreProgress.value = null
                context.startActivity(Intent(context, MainActivity::class.java))
                exitProcess(0)
            } catch (e: Exception) {
                reportException(e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message ?: context.getString(R.string.restore_failed), Toast.LENGTH_LONG).show()
                }
            } finally {
                _backupRestoreProgress.value = null
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
        val content = inputStream.readBytes().toString(Charsets.UTF_8)
        if (content.isBlank()) return

        val parser = android.util.Xml.newPullParser()
        parser.setInput(StringReader(content))

        var eventType = parser.eventType
        val booleans = LinkedHashMap<String, Boolean>()
        val ints = LinkedHashMap<String, Int>()
        val longs = LinkedHashMap<String, Long>()
        val floats = LinkedHashMap<String, Float>()
        val strings = LinkedHashMap<String, String>()
        val stringSets = LinkedHashMap<String, Set<String>>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val name = parser.name
                val keyName = parser.getAttributeValue(null, "name")

                if (keyName != null) {
                    when (name) {
                        "boolean" -> {
                            val value = parser.getAttributeValue(null, "value")?.toBoolean()
                            if (value != null) {
                                booleans[keyName] = value
                            }
                        }
                        "int" -> {
                            val value = parser.getAttributeValue(null, "value")?.toIntOrNull()
                            if (value != null) {
                                ints[keyName] = value
                            }
                        }
                        "long" -> {
                            val value = parser.getAttributeValue(null, "value")?.toLongOrNull()
                            if (value != null) {
                                longs[keyName] = value
                            }
                        }
                        "float" -> {
                            val value = parser.getAttributeValue(null, "value")?.toFloatOrNull()
                            if (value != null) {
                                floats[keyName] = value
                            }
                        }
                        "string" -> {
                            val value = parser.getAttributeValue(null, "value")
                            if (value != null) {
                                strings[keyName] = value
                            }
                        }
                        "string-set" -> {
                            val values = LinkedHashSet<String>()
                            while (true) {
                                val next = parser.next()
                                if (next == XmlPullParser.START_TAG && parser.name == "item") {
                                    values.add(parser.nextText())
                                    continue
                                }
                                if (next == XmlPullParser.END_TAG && parser.name == "string-set") {
                                    break
                                }
                                if (next == XmlPullParser.END_DOCUMENT) {
                                    break
                                }
                            }
                            stringSets[keyName] = values
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (
            booleans.isEmpty() &&
            ints.isEmpty() &&
            longs.isEmpty() &&
            floats.isEmpty() &&
            strings.isEmpty() &&
            stringSets.isEmpty()
        ) {
            return
        }

        context.dataStore.edit { prefs ->
            booleans.forEach { (k, v) -> prefs[booleanPreferencesKey(k)] = v }
            ints.forEach { (k, v) -> prefs[intPreferencesKey(k)] = v }
            longs.forEach { (k, v) -> prefs[longPreferencesKey(k)] = v }
            floats.forEach { (k, v) -> prefs[floatPreferencesKey(k)] = v }
            strings.forEach { (k, v) -> prefs[stringPreferencesKey(k)] = v }
            stringSets.forEach { (k, v) -> prefs[stringSetPreferencesKey(k)] = v }
        }
    }
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

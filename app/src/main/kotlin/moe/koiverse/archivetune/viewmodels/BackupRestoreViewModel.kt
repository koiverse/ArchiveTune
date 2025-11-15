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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    fun backup(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.buffered().zipOutputStream().use { zipStream ->
                    // Backup settings file
                    val settingsFile = context.filesDir / "datastore" / SETTINGS_FILENAME
                    if (settingsFile.exists()) {
                        settingsFile.inputStream().buffered().use { inputStream ->
                            zipStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                            inputStream.copyTo(zipStream)
                            zipStream.closeEntry()
                        }
                    }
                    
                    // Ensure database is properly checkpointed before backup
                    runBlocking(Dispatchers.IO) {
                        database.checkpoint()
                    }
                    
                    // Backup database file
                    FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                        zipStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                        inputStream.copyTo(zipStream)
                        zipStream.closeEntry()
                    }
                }
            } ?: throw IllegalStateException("Failed to create backup file")
        }.onSuccess {
            Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
        }.onFailure { exception ->
            reportException(exception)
            
            val errorMessage = when {
                exception is IllegalStateException -> exception.message ?: context.getString(R.string.backup_create_failed)
                exception.message?.contains("FileNotFoundException") == true -> "Database file not found"
                exception.message?.contains("IOException") == true -> "Failed to write backup file"
                else -> context.getString(R.string.backup_create_failed)
            }
            
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun restore(context: Context, uri: Uri) {
        runCatching {
            // Validate that the file can be opened
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                // First pass: validate the backup file structure
                var hasSettings = false
                var hasDatabase = false
                
                stream.zipInputStream().use { inputStream ->
                    var entry = tryOrNull { inputStream.nextEntry }
                    while (entry != null) {
                        when (entry.name) {
                            SETTINGS_FILENAME -> hasSettings = true
                            InternalDatabase.DB_NAME -> hasDatabase = true
                        }
                        entry = tryOrNull { inputStream.nextEntry }
                    }
                }
                
                // Validate backup file contains required components
                if (!hasDatabase) {
                    throw IllegalStateException("Invalid backup file: missing database")
                }
            }
            
            // Second pass: actually restore the data
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                stream.zipInputStream().use { inputStream ->
                    var entry = tryOrNull { inputStream.nextEntry }
                    while (entry != null) {
                        when (entry.name) {
                            SETTINGS_FILENAME -> {
                                (context.filesDir / "datastore" / SETTINGS_FILENAME).outputStream()
                                    .use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                            }

                            InternalDatabase.DB_NAME -> {
                                // Ensure database is properly checkpointed and closed
                                runBlocking(Dispatchers.IO) {
                                    database.checkpoint()
                                }
                                database.close()
                                
                                // Restore database file
                                FileOutputStream(database.openHelper.writableDatabase.path).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        entry = tryOrNull { inputStream.nextEntry }
                    }
                }
            } ?: throw IllegalStateException("Failed to open backup file")
            
            // Clean up and restart
            context.stopService(Intent(context, MusicService::class.java))
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            context.startActivity(Intent(context, MainActivity::class.java))
            exitProcess(0)
        }.onFailure { exception ->
            reportException(exception)
            
            // Provide more specific error messages
            val errorMessage = when {
                exception is IllegalStateException -> exception.message ?: context.getString(R.string.restore_failed)
                exception.message?.contains("ZipException") == true -> "Invalid backup file format"
                exception.message?.contains("FileNotFoundException") == true -> "Backup file not found"
                else -> context.getString(R.string.restore_failed)
            }
            
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        val songs = arrayListOf<Song>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                lines.forEachIndexed { _, line ->
                    val parts = line.split(",").map { it.trim() }
                    val title = parts[0]
                    val artistStr = parts[1]

                    val artists = artistStr.split(";").map { it.trim() }.map {
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
                        artists = artists,
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
    }
}

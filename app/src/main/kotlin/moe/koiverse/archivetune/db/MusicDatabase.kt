package moe.koiverse.archivetune.db

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import moe.koiverse.archivetune.db.entities.AlbumArtistMap
import moe.koiverse.archivetune.db.entities.AlbumEntity
import moe.koiverse.archivetune.db.entities.ArtistEntity
import moe.koiverse.archivetune.db.entities.Event
import moe.koiverse.archivetune.db.entities.FormatEntity
import moe.koiverse.archivetune.db.entities.LyricsEntity
import moe.koiverse.archivetune.db.entities.PlaylistEntity
import moe.koiverse.archivetune.db.entities.PlayCountEntity
import moe.koiverse.archivetune.db.entities.PlaylistSongMap
import moe.koiverse.archivetune.db.entities.PlaylistSongMapPreview
import moe.koiverse.archivetune.db.entities.RelatedSongMap
import moe.koiverse.archivetune.db.entities.SearchHistory
import moe.koiverse.archivetune.db.entities.SetVideoIdEntity
import moe.koiverse.archivetune.db.entities.SongAlbumMap
import moe.koiverse.archivetune.db.entities.SongArtistMap
import moe.koiverse.archivetune.db.entities.SongEntity
import moe.koiverse.archivetune.db.entities.SortedSongAlbumMap
import moe.koiverse.archivetune.db.entities.SortedSongArtistMap
import moe.koiverse.archivetune.extensions.toSQLiteQuery
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

private const val TAG = "MusicDatabase"
private const val CURRENT_VERSION = 25

class MusicDatabase(
    private val delegate: InternalDatabase,
) : DatabaseDao by delegate.dao {
    val openHelper: SupportSQLiteOpenHelper
        get() = delegate.openHelper

    fun query(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            queryExecutor.execute {
                block(this@MusicDatabase)
            }
        }

    fun transaction(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            transactionExecutor.execute {
                runInTransaction {
                    block(this@MusicDatabase)
                }
            }
        }

    fun close() = delegate.close()
}

@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistSongMap::class,
        SearchHistory::class,
        FormatEntity::class,
        LyricsEntity::class,
        Event::class,
        RelatedSongMap::class,
        SetVideoIdEntity::class,
        PlayCountEntity::class
    ],
    views = [
        SortedSongArtistMap::class,
        SortedSongAlbumMap::class,
        PlaylistSongMapPreview::class,
    ],
    version = CURRENT_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = Migration5To6::class),
        AutoMigration(from = 6, to = 7, spec = Migration6To7::class),
        AutoMigration(from = 7, to = 8, spec = Migration7To8::class),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = Migration9To10::class),
        AutoMigration(from = 10, to = 11, spec = Migration10To11::class),
        AutoMigration(from = 11, to = 12, spec = Migration11To12::class),
        AutoMigration(from = 12, to = 13, spec = Migration12To13::class),
        AutoMigration(from = 13, to = 14, spec = Migration13To14::class),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17, spec = Migration16To17::class),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19, spec = Migration18To19::class),
        AutoMigration(from = 19, to = 20, spec = Migration19To20::class),
        AutoMigration(from = 20, to = 21, spec = Migration20To21::class),
        AutoMigration(from = 21, to = 22),
    ],
)
@TypeConverters(Converters::class)
abstract class InternalDatabase : RoomDatabase() {
    abstract val dao: DatabaseDao

    companion object {
        const val DB_NAME = "song.db"

        fun newInstance(context: Context): MusicDatabase =
            MusicDatabase(
                delegate = Room
                    .databaseBuilder(context, InternalDatabase::class.java, DB_NAME)
                    .addMigrations(
                        MIGRATION_1_2,
                        // Universal migrations - handle any version to current
                        UniversalMigration(21, CURRENT_VERSION),
                        UniversalMigration(22, CURRENT_VERSION),
                        UniversalMigration(23, CURRENT_VERSION),
                        UniversalMigration(24, CURRENT_VERSION),
                    )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .setTransactionExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                    .setQueryExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                    .build(),
            )
    }
}

private class DatabaseCallback : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            try {
                db.query("PRAGMA busy_timeout = 60000").close()
                db.query("PRAGMA cache_size = -16000").close()
                db.query("PRAGMA wal_autocheckpoint = 1000").close()
                db.query("PRAGMA synchronous = NORMAL").close()
                db.query("PRAGMA temp_store = MEMORY").close()
                db.query("PRAGMA mmap_size = 268435456").close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set PRAGMA settings", e)
            }
        }
    }
}

// =============================================================================
// UNIVERSAL MIGRATION - Handles schema upgrade to current version
// =============================================================================

/**
 * Universal migration that properly handles schema changes for any source version.
 * Recreates tables with correct schema when needed to fix default value issues.
 */
private class UniversalMigration(startVersion: Int, endVersion: Int) : Migration(startVersion, endVersion) {
    
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Running universal migration from $startVersion to $endVersion")
        
        try {
            db.execSQL("PRAGMA foreign_keys=OFF")
            
            migrateSongTable(db)
            migratePlaylistTable(db)
            migrateFormatTable(db)
            migrateArtistTable(db)
            migrateAlbumTable(db)
            
            db.execSQL("PRAGMA foreign_keys=ON")
            Log.i(TAG, "Migration completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            throw e
        }
    }
    
    private fun migrateSongTable(db: SupportSQLiteDatabase) {
        val columns = getColumns(db, "song")
        if (columns.isEmpty()) return
        
        val needsRecreation = !columns.containsKey("isLocal") || 
            !columns.containsKey("explicit") ||
            columns["isLocal"]?.defaultValue !in listOf("0", "'0'") ||
            columns["explicit"]?.defaultValue !in listOf("0", "'0'")
        
        if (needsRecreation) {
            val existing = columns.keys
            db.execSQL("ALTER TABLE song RENAME TO _song_old")
            
            db.execSQL("""
                CREATE TABLE song (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    duration INTEGER NOT NULL,
                    thumbnailUrl TEXT,
                    albumId TEXT,
                    albumName TEXT,
                    explicit INTEGER NOT NULL DEFAULT 0,
                    year INTEGER,
                    date INTEGER,
                    dateModified INTEGER,
                    liked INTEGER NOT NULL,
                    likedDate INTEGER,
                    totalPlayTime INTEGER NOT NULL,
                    inLibrary INTEGER,
                    dateDownload INTEGER,
                    isLocal INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_song_albumId ON song (albumId)")
            
            val cols = listOf("id", "title", "duration", "thumbnailUrl", "albumId", "albumName",
                "explicit", "year", "date", "dateModified", "liked", "likedDate", 
                "totalPlayTime", "inLibrary", "dateDownload", "isLocal")
            
            val select = cols.map { c ->
                when {
                    c !in existing && c in listOf("explicit", "isLocal", "liked") -> "0"
                    c !in existing && c == "totalPlayTime" -> "0"
                    c !in existing && c == "duration" -> "-1"
                    c !in existing -> "NULL"
                    c in listOf("explicit", "isLocal") -> "COALESCE($c, 0)"
                    else -> c
                }
            }
            
            db.execSQL("INSERT INTO song (${cols.joinToString()}) SELECT ${select.joinToString()} FROM _song_old")
            db.execSQL("DROP TABLE _song_old")
        }
        
        db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
        db.execSQL("UPDATE song SET isLocal = 0 WHERE isLocal IS NULL")
    }
    
    private fun migratePlaylistTable(db: SupportSQLiteDatabase) {
        val columns = getColumns(db, "playlist")
        if (columns.isEmpty()) return
        
        val needsRecreation = !columns.containsKey("isAutoSync") || 
            !columns.containsKey("isLocal") ||
            columns["isLocal"]?.defaultValue !in listOf("0", "'0'") ||
            columns["isEditable"]?.defaultValue !in listOf("1", "'1'", "true", "'true'")
        
        if (needsRecreation) {
            val existing = columns.keys
            db.execSQL("ALTER TABLE playlist RENAME TO _playlist_old")
            
            db.execSQL("""
                CREATE TABLE playlist (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    browseId TEXT,
                    createdAt INTEGER,
                    lastUpdateTime INTEGER,
                    isEditable INTEGER NOT NULL DEFAULT 1,
                    bookmarkedAt INTEGER,
                    remoteSongCount INTEGER,
                    playEndpointParams TEXT,
                    thumbnailUrl TEXT,
                    shuffleEndpointParams TEXT,
                    radioEndpointParams TEXT,
                    isLocal INTEGER NOT NULL DEFAULT 0,
                    isAutoSync INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            
            val cols = listOf("id", "name", "browseId", "createdAt", "lastUpdateTime",
                "isEditable", "bookmarkedAt", "remoteSongCount", "playEndpointParams",
                "thumbnailUrl", "shuffleEndpointParams", "radioEndpointParams", "isLocal", "isAutoSync")
            
            val select = cols.map { c ->
                when {
                    c !in existing && c == "isEditable" -> "1"
                    c !in existing && c in listOf("isLocal", "isAutoSync") -> "0"
                    c !in existing -> "NULL"
                    c == "isEditable" -> "COALESCE($c, 1)"
                    c in listOf("isLocal", "isAutoSync") -> "COALESCE($c, 0)"
                    else -> c
                }
            }
            
            db.execSQL("INSERT INTO playlist (${cols.joinToString()}) SELECT ${select.joinToString()} FROM _playlist_old")
            db.execSQL("DROP TABLE _playlist_old")
        }
        
        db.execSQL("UPDATE playlist SET isAutoSync = 0 WHERE isAutoSync IS NULL")
        db.execSQL("UPDATE playlist SET isLocal = 0 WHERE isLocal IS NULL")
        db.execSQL("UPDATE playlist SET isEditable = 1 WHERE isEditable IS NULL")
    }
    
    private fun migrateFormatTable(db: SupportSQLiteDatabase) {
        val columns = getColumns(db, "format")
        if (columns.isEmpty()) return
        
        if (!columns.containsKey("perceptualLoudnessDb")) {
            db.execSQL("ALTER TABLE format ADD COLUMN perceptualLoudnessDb REAL DEFAULT NULL")
        }
    }
    
    private fun migrateArtistTable(db: SupportSQLiteDatabase) {
        val columns = getColumns(db, "artist")
        if (columns.isEmpty()) return
        
        if (!columns.containsKey("isLocal")) {
            db.execSQL("ALTER TABLE artist ADD COLUMN isLocal INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL("UPDATE artist SET isLocal = 0 WHERE isLocal IS NULL")
    }
    
    private fun migrateAlbumTable(db: SupportSQLiteDatabase) {
        val columns = getColumns(db, "album")
        if (columns.isEmpty()) return
        
        if (!columns.containsKey("isLocal")) {
            db.execSQL("ALTER TABLE album ADD COLUMN isLocal INTEGER NOT NULL DEFAULT 0")
        }
        if (!columns.containsKey("explicit")) {
            db.execSQL("ALTER TABLE album ADD COLUMN explicit INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL("UPDATE album SET isLocal = 0 WHERE isLocal IS NULL")
        db.execSQL("UPDATE album SET explicit = 0 WHERE explicit IS NULL")
    }
    
    private data class ColInfo(val defaultValue: String?)
    
    private fun getColumns(db: SupportSQLiteDatabase, table: String): Map<String, ColInfo> {
        val cols = mutableMapOf<String, ColInfo>()
        try {
            db.query("PRAGMA table_info($table)").use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                val defaultIdx = cursor.getColumnIndex("dflt_value")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)
                    val default = if (cursor.isNull(defaultIdx)) null else cursor.getString(defaultIdx)
                    cols[name] = ColInfo(default)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading table info: $table", e)
        }
        return cols
    }
}

// =============================================================================
// LEGACY MIGRATION v1 -> v2 (Major schema rewrite, must be kept)
// =============================================================================

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        data class OldSong(val id: String, val title: String, val duration: Int, val liked: Boolean,
            val totalPlayTime: Long, val downloadState: Int, val createDate: LocalDateTime, val modifyDate: LocalDateTime)

        val converters = Converters()
        val artistMap = mutableMapOf<Int, String>()
        val artists = mutableListOf<ArtistEntity>()
        
        db.query("SELECT * FROM artist".toSQLiteQuery()).use { cursor ->
            while (cursor.moveToNext()) {
                val oldId = cursor.getInt(0)
                val newId = ArtistEntity.generateArtistId()
                artistMap[oldId] = newId
                artists.add(ArtistEntity(id = newId, name = cursor.getString(1)))
            }
        }

        val playlistMap = mutableMapOf<Int, String>()
        val playlists = mutableListOf<PlaylistEntity>()
        db.query("SELECT * FROM playlist".toSQLiteQuery()).use { cursor ->
            while (cursor.moveToNext()) {
                val oldId = cursor.getInt(0)
                val newId = PlaylistEntity.generatePlaylistId()
                playlistMap[oldId] = newId
                playlists.add(PlaylistEntity(id = newId, name = cursor.getString(1)))
            }
        }
        
        val playlistSongMaps = mutableListOf<PlaylistSongMap>()
        db.query("SELECT * FROM playlist_song".toSQLiteQuery()).use { cursor ->
            while (cursor.moveToNext()) {
                playlistSongMaps.add(PlaylistSongMap(
                    playlistId = playlistMap[cursor.getInt(1)]!!,
                    songId = cursor.getString(2),
                    position = cursor.getInt(3),
                ))
            }
        }
        playlistSongMaps.sortBy { it.position }
        
        val songs = mutableListOf<OldSong>()
        val songArtistMaps = mutableListOf<SongArtistMap>()
        db.query("SELECT * FROM song".toSQLiteQuery()).use { cursor ->
            while (cursor.moveToNext()) {
                val songId = cursor.getString(0)
                songs.add(OldSong(
                    id = songId,
                    title = cursor.getString(1),
                    duration = cursor.getInt(3),
                    liked = cursor.getInt(4) == 1,
                    totalPlayTime = 0,
                    downloadState = 0,
                    createDate = Instant.ofEpochMilli(Date(cursor.getLong(8)).time).atZone(ZoneOffset.UTC).toLocalDateTime(),
                    modifyDate = Instant.ofEpochMilli(Date(cursor.getLong(9)).time).atZone(ZoneOffset.UTC).toLocalDateTime(),
                ))
                songArtistMaps.add(SongArtistMap(songId = songId, artistId = artistMap[cursor.getInt(2)]!!, position = 0))
            }
        }
        
        // Drop old tables and create new schema
        db.execSQL("DROP TABLE IF EXISTS song")
        db.execSQL("DROP TABLE IF EXISTS artist")
        db.execSQL("DROP TABLE IF EXISTS playlist")
        db.execSQL("DROP TABLE IF EXISTS playlist_song")
        
        db.execSQL("CREATE TABLE IF NOT EXISTS `song` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL, `thumbnailUrl` TEXT, `albumId` TEXT, `albumName` TEXT, `liked` INTEGER NOT NULL, `totalPlayTime` INTEGER NOT NULL, `isTrash` INTEGER NOT NULL, `download_state` INTEGER NOT NULL, `create_date` INTEGER NOT NULL, `modify_date` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `artist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `thumbnailUrl` TEXT, `bannerUrl` TEXT, `description` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `album` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `year` INTEGER, `thumbnailUrl` TEXT, `songCount` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT, `authorId` TEXT, `year` INTEGER, `thumbnailUrl` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `song_artist_map` (`songId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`songId`, `artistId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_songId` ON `song_artist_map` (`songId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_artistId` ON `song_artist_map` (`artistId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `song_album_map` (`songId` TEXT NOT NULL, `albumId` TEXT NOT NULL, `index` INTEGER, PRIMARY KEY(`songId`, `albumId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_songId` ON `song_album_map` (`songId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_albumId` ON `song_album_map` (`albumId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `album_artist_map` (`albumId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`albumId`, `artistId`), FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_albumId` ON `album_artist_map` (`albumId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_artistId` ON `album_artist_map` (`artistId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_song_map` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` TEXT NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_playlistId` ON `playlist_song_map` (`playlistId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_songId` ON `playlist_song_map` (`songId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `download` (`id` INTEGER NOT NULL, `songId` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
        db.execSQL("CREATE VIEW `sorted_song_artist_map` AS SELECT * FROM song_artist_map ORDER BY position")
        db.execSQL("CREATE VIEW `playlist_song_map_preview` AS SELECT * FROM playlist_song_map WHERE position <= 3 ORDER BY position")
        
        // Insert data
        artists.forEach { db.insert("artist", SQLiteDatabase.CONFLICT_ABORT, contentValuesOf(
            "id" to it.id, "name" to it.name,
            "createDate" to converters.dateToTimestamp(it.lastUpdateTime),
            "lastUpdateTime" to converters.dateToTimestamp(it.lastUpdateTime))) }
        
        songs.forEach { db.insert("song", SQLiteDatabase.CONFLICT_ABORT, contentValuesOf(
            "id" to it.id, "title" to it.title, "duration" to it.duration, "liked" to it.liked,
            "totalPlayTime" to it.totalPlayTime, "isTrash" to false, "download_state" to it.downloadState,
            "create_date" to converters.dateToTimestamp(it.createDate),
            "modify_date" to converters.dateToTimestamp(it.modifyDate))) }
        
        songArtistMaps.forEach { db.insert("song_artist_map", SQLiteDatabase.CONFLICT_ABORT, contentValuesOf(
            "songId" to it.songId, "artistId" to it.artistId, "position" to it.position)) }
        
        playlists.forEach { db.insert("playlist", SQLiteDatabase.CONFLICT_ABORT, contentValuesOf(
            "id" to it.id, "name" to it.name,
            "createDate" to converters.dateToTimestamp(LocalDateTime.now()),
            "lastUpdateTime" to converters.dateToTimestamp(LocalDateTime.now()))) }
        
        playlistSongMaps.forEach { db.insert("playlist_song_map", SQLiteDatabase.CONFLICT_ABORT, contentValuesOf(
            "playlistId" to it.playlistId, "songId" to it.songId, "position" to it.position)) }
    }
}

// =============================================================================
// AUTO MIGRATION SPECS (Required by Room's AutoMigration annotations)
// =============================================================================

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isTrash"),
    DeleteColumn(tableName = "playlist", columnName = "author"),
    DeleteColumn(tableName = "playlist", columnName = "authorId"),
    DeleteColumn(tableName = "playlist", columnName = "year"),
    DeleteColumn(tableName = "playlist", columnName = "thumbnailUrl"),
    DeleteColumn(tableName = "playlist", columnName = "createDate"),
    DeleteColumn(tableName = "playlist", columnName = "lastUpdateTime"),
)
@RenameColumn.Entries(
    RenameColumn(tableName = "song", fromColumnName = "download_state", toColumnName = "downloadState"),
    RenameColumn(tableName = "song", fromColumnName = "create_date", toColumnName = "createDate"),
    RenameColumn(tableName = "song", fromColumnName = "modify_date", toColumnName = "modifyDate"),
)
class Migration5To6 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id FROM playlist WHERE id NOT LIKE 'LP%'").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL("UPDATE playlist SET browseId = '${cursor.getString(0)}' WHERE id = '${cursor.getString(0)}'")
            }
        }
    }
}

class Migration6To7 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id, createDate FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL("UPDATE song SET inLibrary = ${cursor.getLong(1)} WHERE id = '${cursor.getString(0)}'")
            }
        }
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "createDate"),
    DeleteColumn(tableName = "song", columnName = "modifyDate"),
)
class Migration7To8 : AutoMigrationSpec

@DeleteTable.Entries(DeleteTable(tableName = "download"))
class Migration9To10 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "downloadState"),
    DeleteColumn(tableName = "artist", columnName = "bannerUrl"),
    DeleteColumn(tableName = "artist", columnName = "description"),
    DeleteColumn(tableName = "artist", columnName = "createDate"),
)
class Migration10To11 : AutoMigrationSpec

@DeleteColumn.Entries(DeleteColumn(tableName = "album", columnName = "createDate"))
class Migration11To12 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE album SET bookmarkedAt = lastUpdateTime")
        db.query("SELECT DISTINCT albumId, albumName FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                val albumId = cursor.getString(0)
                val albumName = cursor.getString(1)
                db.insert("album", SQLiteDatabase.CONFLICT_IGNORE, contentValuesOf(
                    "id" to albumId, "title" to albumName, "songCount" to 0, "duration" to 0, "lastUpdateTime" to 0))
            }
        }
        db.query("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
    }
}

class Migration12To13 : AutoMigrationSpec

class Migration13To14 : AutoMigrationSpec {
    @SuppressLint("Range")
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val now = Converters().dateToTimestamp(LocalDateTime.now())
        db.execSQL("UPDATE playlist SET createdAt = '$now'")
        db.execSQL("UPDATE playlist SET lastUpdateTime = '$now'")
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isLocal"),
    DeleteColumn(tableName = "song", columnName = "localPath"),
    DeleteColumn(tableName = "artist", columnName = "isLocal"),
    DeleteColumn(tableName = "playlist", columnName = "isLocal"),
)
class Migration16To17 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE playlist SET bookmarkedAt = lastUpdateTime")
        db.execSQL("UPDATE playlist SET isEditable = 1 WHERE browseId IS NOT NULL")
    }
}

class Migration18To19 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

class Migration19To20 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

@DeleteColumn.Entries(DeleteColumn(tableName = "song", columnName = "artistName"))
class Migration20To21 : AutoMigrationSpec

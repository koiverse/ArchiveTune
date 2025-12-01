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
    version = 25,
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
        
        /**
         * Creates all manual migrations for database upgrades.
         * This includes both incremental and skip migrations for robustness.
         */
        private fun createMigrations(): Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_21_22,
            MIGRATION_22_23, 
            MIGRATION_23_24,
            MIGRATION_22_24,  // Direct migration path for users upgrading from v22 to v24
            MIGRATION_21_24,  // Direct migration path for users upgrading from v21 to v24
            MIGRATION_24_25,  // Add perceptualLoudnessDb column for audio normalization
            // Skip migrations directly to version 25 for various starting points
            MIGRATION_21_25,  // Direct 21 -> 25
            MIGRATION_22_25,  // Direct 22 -> 25
            MIGRATION_23_25,  // Direct 23 -> 25
            // Universal catch-all migrations to handle edge cases
            MIGRATION_ANY_TO_25,  // Handles any version < 25 to 25
        )

        fun newInstance(context: Context): MusicDatabase =
            MusicDatabase(
                delegate =
                Room
                    .databaseBuilder(context, InternalDatabase::class.java, DB_NAME)
                    .addMigrations(*createMigrations())
                    .addCallback(DatabaseSchemaValidationCallback())
                    .fallbackToDestructiveMigration()
                    .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .setTransactionExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                    .setQueryExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                    .build(),
            )
    }
}

/**
 * Callback to validate and repair database schema after opening.
 * This ensures that any schema inconsistencies are fixed before Room validates the schema.
 */
private class DatabaseSchemaValidationCallback : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        try {
            // Set PRAGMA optimizations
            db.query("PRAGMA busy_timeout = 60000").close()
            db.query("PRAGMA cache_size = -16000").close()
            db.query("PRAGMA wal_autocheckpoint = 1000").close()
            db.query("PRAGMA synchronous = NORMAL").close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set PRAGMA settings", e)
        }
    }
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            data class OldSongEntity(
                val id: String,
                val title: String,
                val duration: Int = -1, // in seconds
                val thumbnailUrl: String? = null,
                val albumId: String? = null,
                val albumName: String? = null,
                val liked: Boolean = false,
                val totalPlayTime: Long = 0, // in milliseconds
                val downloadState: Int = 0,
                val createDate: LocalDateTime = LocalDateTime.now(),
                val modifyDate: LocalDateTime = LocalDateTime.now(),
            )

            val converters = Converters()
            val artistMap = mutableMapOf<Int, String>()
            val artists = mutableListOf<ArtistEntity>()
            db.query("SELECT * FROM artist".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = ArtistEntity.generateArtistId()
                    artistMap[oldId] = newId
                    artists.add(
                        ArtistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }

            val playlistMap = mutableMapOf<Int, String>()
            val playlists = mutableListOf<PlaylistEntity>()
            db.query("SELECT * FROM playlist".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = PlaylistEntity.generatePlaylistId()
                    playlistMap[oldId] = newId
                    playlists.add(
                        PlaylistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }
            val playlistSongMaps = mutableListOf<PlaylistSongMap>()
            db.query("SELECT * FROM playlist_song".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    playlistSongMaps.add(
                        PlaylistSongMap(
                            playlistId = playlistMap[cursor.getInt(1)]!!,
                            songId = cursor.getString(2),
                            position = cursor.getInt(3),
                        ),
                    )
                }
            }
            // ensure we have continuous playlist song position
            playlistSongMaps.sortBy { it.position }
            val playlistSongCount = mutableMapOf<String, Int>()
            playlistSongMaps.map { map ->
                if (map.playlistId !in playlistSongCount) playlistSongCount[map.playlistId] = 0
                map.copy(position = playlistSongCount[map.playlistId]!!).also {
                    playlistSongCount[map.playlistId] = playlistSongCount[map.playlistId]!! + 1
                }
            }
            val songs = mutableListOf<OldSongEntity>()
            val songArtistMaps = mutableListOf<SongArtistMap>()
            db.query("SELECT * FROM song".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val songId = cursor.getString(0)
                    songs.add(
                        OldSongEntity(
                            id = songId,
                            title = cursor.getString(1),
                            duration = cursor.getInt(3),
                            liked = cursor.getInt(4) == 1,
                            createDate = Instant.ofEpochMilli(Date(cursor.getLong(8)).time)
                                .atZone(ZoneOffset.UTC).toLocalDateTime(),
                            modifyDate = Instant.ofEpochMilli(Date(cursor.getLong(9)).time)
                                .atZone(ZoneOffset.UTC).toLocalDateTime(),
                        ),
                    )
                    songArtistMaps.add(
                        SongArtistMap(
                            songId = songId,
                            artistId = artistMap[cursor.getInt(2)]!!,
                            position = 0,
                        ),
                    )
                }
            }
            db.execSQL("DROP TABLE IF EXISTS song")
            db.execSQL("DROP TABLE IF EXISTS artist")
            db.execSQL("DROP TABLE IF EXISTS playlist")
            db.execSQL("DROP TABLE IF EXISTS playlist_song")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL, `thumbnailUrl` TEXT, `albumId` TEXT, `albumName` TEXT, `liked` INTEGER NOT NULL, `totalPlayTime` INTEGER NOT NULL, `isTrash` INTEGER NOT NULL, `download_state` INTEGER NOT NULL, `create_date` INTEGER NOT NULL, `modify_date` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `artist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `thumbnailUrl` TEXT, `bannerUrl` TEXT, `description` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `album` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `year` INTEGER, `thumbnailUrl` TEXT, `songCount` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT, `authorId` TEXT, `year` INTEGER, `thumbnailUrl` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_artist_map` (`songId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`songId`, `artistId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_songId` ON `song_artist_map` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_artistId` ON `song_artist_map` (`artistId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_album_map` (`songId` TEXT NOT NULL, `albumId` TEXT NOT NULL, `index` INTEGER, PRIMARY KEY(`songId`, `albumId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_songId` ON `song_album_map` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_albumId` ON `song_album_map` (`albumId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `album_artist_map` (`albumId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`albumId`, `artistId`), FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_albumId` ON `album_artist_map` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_artistId` ON `album_artist_map` (`artistId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist_song_map` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` TEXT NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_playlistId` ON `playlist_song_map` (`playlistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_songId` ON `playlist_song_map` (`songId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `download` (`id` INTEGER NOT NULL, `songId` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
            db.execSQL("CREATE VIEW `sorted_song_artist_map` AS SELECT * FROM song_artist_map ORDER BY position")
            db.execSQL(
                "CREATE VIEW `playlist_song_map_preview` AS SELECT * FROM playlist_song_map WHERE position <= 3 ORDER BY position",
            )
            artists.forEach { artist ->
                db.insert(
                    "artist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to artist.id,
                        "name" to artist.name,
                        "createDate" to converters.dateToTimestamp(artist.lastUpdateTime),
                        "lastUpdateTime" to converters.dateToTimestamp(artist.lastUpdateTime),
                    ),
                )
            }
            songs.forEach { song ->
                db.insert(
                    "song",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to song.id,
                        "title" to song.title,
                        "duration" to song.duration,
                        "liked" to song.liked,
                        "totalPlayTime" to song.totalPlayTime,
                        "isTrash" to false,
                        "download_state" to song.downloadState,
                        "create_date" to converters.dateToTimestamp(song.createDate),
                        "modify_date" to converters.dateToTimestamp(song.modifyDate),
                    ),
                )
            }
            songArtistMaps.forEach { songArtistMap ->
                db.insert(
                    "song_artist_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "songId" to songArtistMap.songId,
                        "artistId" to songArtistMap.artistId,
                        "position" to songArtistMap.position,
                    ),
                )
            }
            playlists.forEach { playlist ->
                db.insert(
                    "playlist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to playlist.id,
                        "name" to playlist.name,
                        "createDate" to converters.dateToTimestamp(LocalDateTime.now()),
                        "lastUpdateTime" to converters.dateToTimestamp(LocalDateTime.now()),
                    ),
                )
            }
            playlistSongMaps.forEach { playlistSongMap ->
                db.insert(
                    "playlist_song_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "playlistId" to playlistSongMap.playlistId,
                        "songId" to playlistSongMap.songId,
                        "position" to playlistSongMap.position,
                    ),
                )
            }
        }
    }

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
    RenameColumn(
        tableName = "song",
        fromColumnName = "download_state",
        toColumnName = "downloadState"
    ),
    RenameColumn(tableName = "song", fromColumnName = "create_date", toColumnName = "createDate"),
    RenameColumn(tableName = "song", fromColumnName = "modify_date", toColumnName = "modifyDate"),
)
class Migration5To6 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id FROM playlist WHERE id NOT LIKE 'LP%'").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE playlist SET browseId = '${cursor.getString(0)}' WHERE id = '${cursor.getString(0)}'"
                )
            }
        }
    }
}

// Migration from version 21 to 22
// Version 21→22 was supposed to use AutoMigration, but we need manual migration for safety
val MIGRATION_21_22 =
    object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // This was an AutoMigration, so schema should be compatible
            // Just ensure boolean columns have proper values
            db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE song SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE artist SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE album SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE album SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE playlist SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE playlist SET isEditable = 1 WHERE isEditable IS NULL")
        }
    }

val MIGRATION_22_23 =
    object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Ensure all boolean columns have proper default values (0 for false, 1 for true)
            // This fixes issues where Room expects consistent default value representations
            
            // Fix song table
            db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE song SET isLocal = 0 WHERE isLocal IS NULL")
            
            // Fix artist table
            db.execSQL("UPDATE artist SET isLocal = 0 WHERE isLocal IS NULL")
            
            // Fix album table  
            db.execSQL("UPDATE album SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE album SET isLocal = 0 WHERE isLocal IS NULL")
            
            // Fix playlist table
            db.execSQL("UPDATE playlist SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE playlist SET isEditable = 1 WHERE isEditable IS NULL")
        }
    }

val MIGRATION_23_24 =
    object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add isAutoSync column to playlist table. Stored as INTEGER (0/1) with default 0 (false).
            // Check if column already exists to handle various upgrade paths
            var columnExists = false
            db.query("PRAGMA table_info(playlist)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "isAutoSync") {
                        columnExists = true
                        break
                    }
                }
            }
            
            if (!columnExists) {
                // Add the column with default value
                db.execSQL("ALTER TABLE playlist ADD COLUMN isAutoSync INTEGER NOT NULL DEFAULT 0")
            }
            
            // Ensure all existing rows have the default value set
            db.execSQL("UPDATE playlist SET isAutoSync = 0 WHERE isAutoSync IS NULL OR isAutoSync NOT IN (0, 1)")
        }
    }

// Direct migration from 22 to 24 for users upgrading directly
// This combines the changes from both 22→23 and 23→24
val MIGRATION_22_24 =
    object : Migration(22, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Fix all boolean columns to ensure they have proper default values (from 22→23)
            db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE song SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE artist SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE album SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE album SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE playlist SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE playlist SET isEditable = 1 WHERE isEditable IS NULL")
            
            // Add isAutoSync column if it doesn't exist (from 23→24)
            var columnExists = false
            db.query("PRAGMA table_info(playlist)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "isAutoSync") {
                        columnExists = true
                        break
                    }
                }
            }
            
            if (!columnExists) {
                db.execSQL("ALTER TABLE playlist ADD COLUMN isAutoSync INTEGER NOT NULL DEFAULT 0")
            }
            
            // Ensure all rows have proper values
            db.execSQL("UPDATE playlist SET isAutoSync = 0 WHERE isAutoSync IS NULL OR isAutoSync NOT IN (0, 1)")
        }
    }

// Direct migration from 21 to 24 for users who might skip intermediate versions
val MIGRATION_21_24 =
    object : Migration(21, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Combine all changes from 21→22→23→24
            
            // Ensure all boolean columns have proper default values
            db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE song SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE artist SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE album SET explicit = 0 WHERE explicit IS NULL")
            db.execSQL("UPDATE album SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE playlist SET isLocal = 0 WHERE isLocal IS NULL")
            db.execSQL("UPDATE playlist SET isEditable = 1 WHERE isEditable IS NULL")
            
            // Add isAutoSync column if it doesn't exist
            var columnExists = false
            db.query("PRAGMA table_info(playlist)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "isAutoSync") {
                        columnExists = true
                        break
                    }
                }
            }
            
            if (!columnExists) {
                db.execSQL("ALTER TABLE playlist ADD COLUMN isAutoSync INTEGER NOT NULL DEFAULT 0")
            }
            
            // Ensure all rows have proper values
            db.execSQL("UPDATE playlist SET isAutoSync = 0 WHERE isAutoSync IS NULL OR isAutoSync NOT IN (0, 1)")
        }
    }

// Direct migration from version 21 to 25 - combines all intermediate migrations
val MIGRATION_21_25 =
    object : Migration(21, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Log.i(TAG, "Running migration 21 -> 25")
            migrateToVersion25(db)
        }
    }

// Direct migration from version 22 to 25
val MIGRATION_22_25 =
    object : Migration(22, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Log.i(TAG, "Running migration 22 -> 25")
            migrateToVersion25(db)
        }
    }

// Direct migration from version 23 to 25
val MIGRATION_23_25 =
    object : Migration(23, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Log.i(TAG, "Running migration 23 -> 25")
            migrateToVersion25(db)
        }
    }

/**
 * Common migration logic to upgrade any schema to version 25.
 * This function handles all the necessary schema changes in a robust way.
 */
private fun migrateToVersion25(db: SupportSQLiteDatabase) {
    try {
        db.execSQL("PRAGMA foreign_keys=OFF")
        
        // 1. Fix song table - ensure isLocal has correct default
        fixSongTableSchema(db)
        
        // 2. Fix playlist table - add isAutoSync, fix isLocal
        fixPlaylistTableSchema(db)
        
        // 3. Fix format table - add perceptualLoudnessDb
        addColumnIfMissing(db, "format", "perceptualLoudnessDb", "REAL DEFAULT NULL")
        
        // 4. Fix artist table - ensure isLocal exists with default
        addColumnIfMissing(db, "artist", "isLocal", "INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE artist SET isLocal = 0 WHERE isLocal IS NULL")
        
        // 5. Fix album table - ensure isLocal and explicit exist with defaults
        addColumnIfMissing(db, "album", "isLocal", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "album", "explicit", "INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE album SET isLocal = 0 WHERE isLocal IS NULL")
        db.execSQL("UPDATE album SET explicit = 0 WHERE explicit IS NULL")
        
        db.execSQL("PRAGMA foreign_keys=ON")
    } catch (e: Exception) {
        Log.e(TAG, "Error in migrateToVersion25", e)
        throw e
    }
}

private fun fixSongTableSchema(db: SupportSQLiteDatabase) {
    // Check if song table needs recreation due to incorrect default values
    var needsRecreation = false
    var hasIsLocal = false
    var hasExplicit = false
    
    db.query("PRAGMA table_info(song)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        val defaultIndex = cursor.getColumnIndex("dflt_value")
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(nameIndex)
            val defaultValue = if (cursor.isNull(defaultIndex)) null else cursor.getString(defaultIndex)
            
            when (columnName) {
                "isLocal" -> {
                    hasIsLocal = true
                    if (defaultValue !in listOf("0", "'0'")) {
                        needsRecreation = true
                    }
                }
                "explicit" -> {
                    hasExplicit = true
                    if (defaultValue !in listOf("0", "'0'")) {
                        needsRecreation = true
                    }
                }
            }
        }
    }
    
    if (!hasIsLocal || !hasExplicit) {
        needsRecreation = true
    }
    
    if (needsRecreation) {
        recreateSongTableV25(db)
    }
    
    // Ensure all NULL values are fixed
    db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    db.execSQL("UPDATE song SET isLocal = 0 WHERE isLocal IS NULL")
}

private fun recreateSongTableV25(db: SupportSQLiteDatabase) {
    Log.i(TAG, "Recreating song table for version 25")
    
    // Get list of existing columns to preserve data
    val existingColumns = mutableSetOf<String>()
    db.query("PRAGMA table_info(song)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            existingColumns.add(cursor.getString(nameIndex))
        }
    }
    
    if (existingColumns.isEmpty()) return
    
    db.execSQL("ALTER TABLE song RENAME TO song_old_v25")
    
    db.execSQL("""
        CREATE TABLE `song` (
            `id` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `duration` INTEGER NOT NULL,
            `thumbnailUrl` TEXT,
            `albumId` TEXT,
            `albumName` TEXT,
            `explicit` INTEGER NOT NULL DEFAULT 0,
            `year` INTEGER,
            `date` INTEGER,
            `dateModified` INTEGER,
            `liked` INTEGER NOT NULL,
            `likedDate` INTEGER,
            `totalPlayTime` INTEGER NOT NULL,
            `inLibrary` INTEGER,
            `dateDownload` INTEGER,
            `isLocal` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`id`)
        )
    """.trimIndent())
    
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
    
    // Build INSERT with COALESCE for missing/null columns
    val targetColumns = listOf("id", "title", "duration", "thumbnailUrl", "albumId", 
        "albumName", "explicit", "year", "date", "dateModified", "liked", 
        "likedDate", "totalPlayTime", "inLibrary", "dateDownload", "isLocal")
    
    val selectExpressions = targetColumns.map { col ->
        when {
            col !in existingColumns && col == "explicit" -> "0"
            col !in existingColumns && col == "isLocal" -> "0"
            col !in existingColumns && col == "liked" -> "0"
            col !in existingColumns && col == "totalPlayTime" -> "0"
            col !in existingColumns && col == "duration" -> "-1"
            col !in existingColumns -> "NULL"
            col == "explicit" -> "COALESCE($col, 0)"
            col == "isLocal" -> "COALESCE($col, 0)"
            else -> col
        }
    }
    
    db.execSQL("""
        INSERT INTO song (${targetColumns.joinToString(", ")})
        SELECT ${selectExpressions.joinToString(", ")}
        FROM song_old_v25
    """.trimIndent())
    
    db.execSQL("DROP TABLE song_old_v25")
}

private fun fixPlaylistTableSchema(db: SupportSQLiteDatabase) {
    // Check for missing columns and wrong defaults
    var needsRecreation = false
    var hasIsAutoSync = false
    var hasIsLocal = false
    var hasIsEditable = false
    
    db.query("PRAGMA table_info(playlist)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        val defaultIndex = cursor.getColumnIndex("dflt_value")
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(nameIndex)
            val defaultValue = if (cursor.isNull(defaultIndex)) null else cursor.getString(defaultIndex)
            
            when (columnName) {
                "isAutoSync" -> hasIsAutoSync = true
                "isLocal" -> {
                    hasIsLocal = true
                    if (defaultValue !in listOf("0", "'0'")) {
                        needsRecreation = true
                    }
                }
                "isEditable" -> {
                    hasIsEditable = true
                    // Check if default is not 1 or true
                    if (defaultValue !in listOf("1", "'1'", "true", "'true'")) {
                        needsRecreation = true
                    }
                }
            }
        }
    }
    
    // If we're missing critical columns, we need to recreate
    if (!hasIsAutoSync || !hasIsLocal) {
        needsRecreation = true
    }
    
    if (needsRecreation) {
        recreatePlaylistTableV25(db)
    } else {
        // Just add missing column if needed
        if (!hasIsAutoSync) {
            db.execSQL("ALTER TABLE playlist ADD COLUMN isAutoSync INTEGER NOT NULL DEFAULT 0")
        }
    }
    
    // Ensure all values are proper
    db.execSQL("UPDATE playlist SET isAutoSync = 0 WHERE isAutoSync IS NULL")
    db.execSQL("UPDATE playlist SET isLocal = 0 WHERE isLocal IS NULL")
    db.execSQL("UPDATE playlist SET isEditable = 1 WHERE isEditable IS NULL")
}

private fun recreatePlaylistTableV25(db: SupportSQLiteDatabase) {
    Log.i(TAG, "Recreating playlist table for version 25")
    
    val existingColumns = mutableSetOf<String>()
    db.query("PRAGMA table_info(playlist)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            existingColumns.add(cursor.getString(nameIndex))
        }
    }
    
    if (existingColumns.isEmpty()) return
    
    db.execSQL("ALTER TABLE playlist RENAME TO playlist_old_v25")
    
    db.execSQL("""
        CREATE TABLE `playlist` (
            `id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `browseId` TEXT,
            `createdAt` INTEGER,
            `lastUpdateTime` INTEGER,
            `isEditable` INTEGER NOT NULL DEFAULT 1,
            `bookmarkedAt` INTEGER,
            `remoteSongCount` INTEGER,
            `playEndpointParams` TEXT,
            `thumbnailUrl` TEXT,
            `shuffleEndpointParams` TEXT,
            `radioEndpointParams` TEXT,
            `isLocal` INTEGER NOT NULL DEFAULT 0,
            `isAutoSync` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`id`)
        )
    """.trimIndent())
    
    val targetColumns = listOf("id", "name", "browseId", "createdAt", "lastUpdateTime",
        "isEditable", "bookmarkedAt", "remoteSongCount", "playEndpointParams",
        "thumbnailUrl", "shuffleEndpointParams", "radioEndpointParams", "isLocal", "isAutoSync")
    
    val selectExpressions = targetColumns.map { col ->
        when {
            col !in existingColumns && col == "isEditable" -> "1"
            col !in existingColumns && col == "isLocal" -> "0"
            col !in existingColumns && col == "isAutoSync" -> "0"
            col !in existingColumns -> "NULL"
            col == "isEditable" -> "COALESCE($col, 1)"
            col == "isLocal" -> "COALESCE($col, 0)"
            col == "isAutoSync" -> "COALESCE($col, 0)"
            else -> col
        }
    }
    
    db.execSQL("""
        INSERT INTO playlist (${targetColumns.joinToString(", ")})
        SELECT ${selectExpressions.joinToString(", ")}
        FROM playlist_old_v25
    """.trimIndent())
    
    db.execSQL("DROP TABLE playlist_old_v25")
}

private fun addColumnIfMissing(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
    var columnExists = false
    db.query("PRAGMA table_info($table)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) {
                columnExists = true
                break
            }
        }
    }
    
    if (!columnExists) {
        try {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            Log.i(TAG, "Added column $column to table $table")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add column $column to $table", e)
        }
    }
}

class Migration6To7 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id, createDate FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE song SET inLibrary = ${cursor.getLong(1)} WHERE id = '${
                        cursor.getString(
                            0
                        )
                    }'"
                )
            }
        }
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "createDate"),
    DeleteColumn(tableName = "song", columnName = "modifyDate"),
)
class Migration7To8 : AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(tableName = "download"),
)
class Migration9To10 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "downloadState"),
    DeleteColumn(tableName = "artist", columnName = "bannerUrl"),
    DeleteColumn(tableName = "artist", columnName = "description"),
    DeleteColumn(tableName = "artist", columnName = "createDate"),
)
class Migration10To11 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "album", columnName = "createDate"),
)
class Migration11To12 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE album SET bookmarkedAt = lastUpdateTime")
        db.query("SELECT DISTINCT albumId, albumName FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                val albumId = cursor.getString(0)
                val albumName = cursor.getString(1)
                db.insert(
                    table = "album",
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
                    values =
                    contentValuesOf(
                        "id" to albumId,
                        "title" to albumName,
                        "songCount" to 0,
                        "duration" to 0,
                        "lastUpdateTime" to 0,
                    ),
                )
            }
        }
        db.query("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
    }
}

class Migration12To13 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
    }
}

class Migration13To14 : AutoMigrationSpec {
    @SuppressLint("Range")
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE playlist SET createdAt = '${Converters().dateToTimestamp(LocalDateTime.now())}'")
        db.execSQL(
            "UPDATE playlist SET lastUpdateTime = '${
                Converters().dateToTimestamp(
                    LocalDateTime.now()
                )
            }'"
        )
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
        // Add explicit column
        db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

class Migration19To20 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        // Add explicit column
        db.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "song",
        columnName = "artistName"
    )
)
class Migration20To21 : AutoMigrationSpec

val MIGRATION_24_25 =
    object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add perceptualLoudnessDb column to format table for improved audio normalization
            var columnExists = false
            db.query("PRAGMA table_info(format)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "perceptualLoudnessDb") {
                        columnExists = true
                        break
                    }
                }
            }
            
            if (!columnExists) {
                // Add the column allowing NULL values (since existing rows won't have this data)
                db.execSQL("ALTER TABLE format ADD COLUMN perceptualLoudnessDb REAL DEFAULT NULL")
            }

            var requiresSongTableRewrite = false
            db.query("PRAGMA table_info(song)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val defaultIndex = cursor.getColumnIndex("dflt_value")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "isLocal") {
                        val defaultValue = cursor.getString(defaultIndex)
                        if (cursor.isNull(defaultIndex) || defaultValue !in setOf("0", "'0'")) {
                            requiresSongTableRewrite = true
                            break
                        }
                    }
                }
            }

            if (requiresSongTableRewrite) {
                db.execSQL("PRAGMA foreign_keys=OFF")
                db.execSQL("ALTER TABLE song RENAME TO song_old")
                db.execSQL(
                    """
                    CREATE TABLE `song` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `thumbnailUrl` TEXT,
                        `albumId` TEXT,
                        `albumName` TEXT,
                        `explicit` INTEGER NOT NULL DEFAULT 0,
                        `year` INTEGER,
                        `date` INTEGER,
                        `dateModified` INTEGER,
                        `liked` INTEGER NOT NULL,
                        `likedDate` INTEGER,
                        `totalPlayTime` INTEGER NOT NULL,
                        `inLibrary` INTEGER,
                        `dateDownload` INTEGER,
                        `isLocal` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """
                        .trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
                db.execSQL(
                    """
                    INSERT INTO song (
                        id,
                        title,
                        duration,
                        thumbnailUrl,
                        albumId,
                        albumName,
                        explicit,
                        year,
                        date,
                        dateModified,
                        liked,
                        likedDate,
                        totalPlayTime,
                        inLibrary,
                        dateDownload,
                        isLocal
                    )
                    SELECT
                        id,
                        title,
                        duration,
                        thumbnailUrl,
                        albumId,
                        albumName,
                        explicit,
                        year,
                        date,
                        dateModified,
                        liked,
                        likedDate,
                        totalPlayTime,
                        inLibrary,
                        dateDownload,
                        COALESCE(isLocal, 0)
                    FROM song_old
                    """
                        .trimIndent()
                )
                db.execSQL("DROP TABLE song_old")
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
    }

/**
 * Universal migration that handles ANY version upgrade to version 25.
 * This is a safety net that ensures schema compatibility when other migrations fail.
 * 
 * The approach is to:
 * 1. Check each table for missing columns and add them
 * 2. Fix column default values by recreating tables if necessary
 * 3. Ensure all NOT NULL constraints have proper default values
 */
private val MIGRATION_ANY_TO_25 = object : Migration(1, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Running universal migration to version 25")
        
        try {
            db.execSQL("PRAGMA foreign_keys=OFF")
            
            // Migrate song table to correct schema
            migrateSongTable(db)
            
            // Migrate playlist table to correct schema  
            migratePlaylistTable(db)
            
            // Migrate format table to add perceptualLoudnessDb
            migrateFormatTable(db)
            
            // Migrate artist table
            migrateArtistTable(db)
            
            // Migrate album table
            migrateAlbumTable(db)
            
            db.execSQL("PRAGMA foreign_keys=ON")
            
            Log.i(TAG, "Universal migration to version 25 completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during universal migration", e)
            throw e
        }
    }
    
    private fun migrateSongTable(db: SupportSQLiteDatabase) {
        // Check if song table exists and has all required columns
        val songColumns = getTableColumns(db, "song")
        
        if (songColumns.isEmpty()) {
            // Table doesn't exist, will be created by Room
            return
        }
        
        // Check for missing columns and incorrect defaults
        val requiredColumns = mapOf(
            "id" to "TEXT NOT NULL",
            "title" to "TEXT NOT NULL", 
            "duration" to "INTEGER NOT NULL",
            "thumbnailUrl" to "TEXT",
            "albumId" to "TEXT",
            "albumName" to "TEXT",
            "explicit" to "INTEGER NOT NULL DEFAULT 0",
            "year" to "INTEGER",
            "date" to "INTEGER",
            "dateModified" to "INTEGER",
            "liked" to "INTEGER NOT NULL",
            "likedDate" to "INTEGER",
            "totalPlayTime" to "INTEGER NOT NULL",
            "inLibrary" to "INTEGER",
            "dateDownload" to "INTEGER",
            "isLocal" to "INTEGER NOT NULL DEFAULT 0"
        )
        
        // Check if we need to recreate the table
        var needsRecreation = false
        for ((column, _) in requiredColumns) {
            if (!songColumns.containsKey(column)) {
                needsRecreation = true
                break
            }
        }
        
        // Check if isLocal has wrong default value
        val isLocalInfo = songColumns["isLocal"]
        if (isLocalInfo != null && isLocalInfo.defaultValue !in listOf("0", "'0'", null)) {
            needsRecreation = true
        }
        
        // Check if explicit has wrong default value
        val explicitInfo = songColumns["explicit"]
        if (explicitInfo != null && explicitInfo.defaultValue !in listOf("0", "'0'", null)) {
            needsRecreation = true
        }
        
        if (needsRecreation) {
            recreateSongTable(db)
        } else {
            // Just add missing columns
            for ((column, _) in requiredColumns) {
                if (!songColumns.containsKey(column)) {
                    addColumnIfNotExists(db, "song", column, getColumnSql(column))
                }
            }
        }
    }
    
    private fun recreateSongTable(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Recreating song table with correct schema")
        
        // Get existing columns
        val existingColumns = getTableColumns(db, "song").keys
        
        db.execSQL("ALTER TABLE song RENAME TO song_backup")
        
        db.execSQL("""
            CREATE TABLE `song` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `duration` INTEGER NOT NULL,
                `thumbnailUrl` TEXT,
                `albumId` TEXT,
                `albumName` TEXT,
                `explicit` INTEGER NOT NULL DEFAULT 0,
                `year` INTEGER,
                `date` INTEGER,
                `dateModified` INTEGER,
                `liked` INTEGER NOT NULL,
                `likedDate` INTEGER,
                `totalPlayTime` INTEGER NOT NULL,
                `inLibrary` INTEGER,
                `dateDownload` INTEGER,
                `isLocal` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
        
        // Build column list for INSERT based on what exists in backup
        val targetColumns = listOf("id", "title", "duration", "thumbnailUrl", "albumId", 
            "albumName", "explicit", "year", "date", "dateModified", "liked", 
            "likedDate", "totalPlayTime", "inLibrary", "dateDownload", "isLocal")
        
        val selectColumns = targetColumns.map { col ->
            when {
                col in existingColumns -> col
                col == "explicit" -> "0"
                col == "isLocal" -> "0"
                col == "liked" -> "0"
                col == "totalPlayTime" -> "0"
                col == "duration" -> "-1"
                else -> "NULL"
            }
        }
        
        db.execSQL("""
            INSERT INTO song (${targetColumns.joinToString(", ")})
            SELECT ${selectColumns.joinToString(", ")}
            FROM song_backup
        """.trimIndent())
        
        db.execSQL("DROP TABLE song_backup")
    }
    
    private fun migratePlaylistTable(db: SupportSQLiteDatabase) {
        val playlistColumns = getTableColumns(db, "playlist")
        
        if (playlistColumns.isEmpty()) return
        
        // Add isAutoSync column if missing
        if (!playlistColumns.containsKey("isAutoSync")) {
            db.execSQL("ALTER TABLE playlist ADD COLUMN isAutoSync INTEGER NOT NULL DEFAULT 0")
        }
        
        // Check if we need to recreate for isEditable default value fix
        val isEditableInfo = playlistColumns["isEditable"]
        val needsRecreation = isEditableInfo != null && 
            isEditableInfo.defaultValue !in listOf("1", "'1'", "true", "'true'")
        
        if (needsRecreation || !playlistColumns.containsKey("isLocal")) {
            recreatePlaylistTable(db)
        }
        
        // Ensure proper values
        db.execSQL("UPDATE playlist SET isAutoSync = 0 WHERE isAutoSync IS NULL")
        db.execSQL("UPDATE playlist SET isEditable = 1 WHERE isEditable IS NULL")
        db.execSQL("UPDATE playlist SET isLocal = 0 WHERE isLocal IS NULL")
    }
    
    private fun recreatePlaylistTable(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Recreating playlist table with correct schema")
        
        val existingColumns = getTableColumns(db, "playlist").keys
        
        db.execSQL("ALTER TABLE playlist RENAME TO playlist_backup")
        
        db.execSQL("""
            CREATE TABLE `playlist` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `browseId` TEXT,
                `createdAt` INTEGER,
                `lastUpdateTime` INTEGER,
                `isEditable` INTEGER NOT NULL DEFAULT 1,
                `bookmarkedAt` INTEGER,
                `remoteSongCount` INTEGER,
                `playEndpointParams` TEXT,
                `thumbnailUrl` TEXT,
                `shuffleEndpointParams` TEXT,
                `radioEndpointParams` TEXT,
                `isLocal` INTEGER NOT NULL DEFAULT 0,
                `isAutoSync` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        
        val targetColumns = listOf("id", "name", "browseId", "createdAt", "lastUpdateTime",
            "isEditable", "bookmarkedAt", "remoteSongCount", "playEndpointParams",
            "thumbnailUrl", "shuffleEndpointParams", "radioEndpointParams", "isLocal", "isAutoSync")
        
        val selectColumns = targetColumns.map { col ->
            when {
                col in existingColumns -> col
                col == "isEditable" -> "1"
                col == "isLocal" -> "0"
                col == "isAutoSync" -> "0"
                else -> "NULL"
            }
        }
        
        db.execSQL("""
            INSERT INTO playlist (${targetColumns.joinToString(", ")})
            SELECT ${selectColumns.joinToString(", ")}
            FROM playlist_backup
        """.trimIndent())
        
        db.execSQL("DROP TABLE playlist_backup")
    }
    
    private fun migrateFormatTable(db: SupportSQLiteDatabase) {
        val formatColumns = getTableColumns(db, "format")
        
        if (formatColumns.isEmpty()) return
        
        if (!formatColumns.containsKey("perceptualLoudnessDb")) {
            db.execSQL("ALTER TABLE format ADD COLUMN perceptualLoudnessDb REAL DEFAULT NULL")
        }
    }
    
    private fun migrateArtistTable(db: SupportSQLiteDatabase) {
        val artistColumns = getTableColumns(db, "artist")
        
        if (artistColumns.isEmpty()) return
        
        if (!artistColumns.containsKey("isLocal")) {
            db.execSQL("ALTER TABLE artist ADD COLUMN isLocal INTEGER NOT NULL DEFAULT 0")
        }
        
        db.execSQL("UPDATE artist SET isLocal = 0 WHERE isLocal IS NULL")
    }
    
    private fun migrateAlbumTable(db: SupportSQLiteDatabase) {
        val albumColumns = getTableColumns(db, "album")
        
        if (albumColumns.isEmpty()) return
        
        if (!albumColumns.containsKey("isLocal")) {
            db.execSQL("ALTER TABLE album ADD COLUMN isLocal INTEGER NOT NULL DEFAULT 0")
        }
        if (!albumColumns.containsKey("explicit")) {
            db.execSQL("ALTER TABLE album ADD COLUMN explicit INTEGER NOT NULL DEFAULT 0")
        }
        
        db.execSQL("UPDATE album SET isLocal = 0 WHERE isLocal IS NULL")
        db.execSQL("UPDATE album SET explicit = 0 WHERE explicit IS NULL")
    }
    
    private data class ColumnInfo(val type: String, val notNull: Boolean, val defaultValue: String?)
    
    private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): Map<String, ColumnInfo> {
        val columns = mutableMapOf<String, ColumnInfo>()
        try {
            db.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val typeIndex = cursor.getColumnIndex("type")
                val notNullIndex = cursor.getColumnIndex("notnull")
                val defaultIndex = cursor.getColumnIndex("dflt_value")
                
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    val type = cursor.getString(typeIndex)
                    val notNull = cursor.getInt(notNullIndex) == 1
                    val default = if (cursor.isNull(defaultIndex)) null else cursor.getString(defaultIndex)
                    columns[name] = ColumnInfo(type, notNull, default)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting columns for table $tableName", e)
        }
        return columns
    }
    
    private fun addColumnIfNotExists(db: SupportSQLiteDatabase, table: String, column: String, sql: String) {
        val columns = getTableColumns(db, table)
        if (!columns.containsKey(column)) {
            try {
                db.execSQL("ALTER TABLE $table ADD COLUMN $sql")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding column $column to $table", e)
            }
        }
    }
    
    private fun getColumnSql(column: String): String = when(column) {
        "explicit" -> "explicit INTEGER NOT NULL DEFAULT 0"
        "isLocal" -> "isLocal INTEGER NOT NULL DEFAULT 0"
        "isAutoSync" -> "isAutoSync INTEGER NOT NULL DEFAULT 0"
        "perceptualLoudnessDb" -> "perceptualLoudnessDb REAL DEFAULT NULL"
        else -> "$column TEXT"
    }
}

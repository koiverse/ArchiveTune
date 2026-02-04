# ArchiveTune Style Guide

This style guide provides comprehensive coding conventions for the ArchiveTune Android application. All code contributions should follow these guidelines to maintain consistency and quality across the codebase.

---

## 1. Kotlin Coding Conventions

### 1.1 Naming Conventions

```kotlin
// ✅ CORRECT - PascalCase for classes
class HomeViewModel : ViewModel()
class MusicDatabase : RoomDatabase()
interface LyricsProvider

// ✅ CORRECT - camelCase for functions and properties
fun getPlaylists(): List<Playlist>
fun isUserLoggedIn(): Boolean
var isLoading: Boolean = false

// ✅ CORRECT - SCREAMING_SNAKE_CASE for constants
companion object {
    const val MAX_RETRY_COUNT = 3
    const val DEFAULT_TIMEOUT_MS = 30_000L
}

// ❌ INCORRECT
class homeViewModel // Wrong case
fun GetPlaylists() // Wrong case
const val maxRetryCount = 3 // Wrong case style
```

### 1.2 Package Structure

```
moe.koiverse.archivetune/
├── MainActivity.kt
├── App.kt
├── constants/           # Constants and enums
│   ├── PreferenceKeys.kt
│   └── Dimensions.kt
├── db/                  # Room database
│   ├── MusicDatabase.kt
│   ├── DatabaseDao.kt
│   ├── Converters.kt
│   └── entities/        # Room entities
├── extensions/          # Extension functions
│   ├── ContextExt.kt
│   ├── StringExt.kt
│   └── CoroutineExt.kt
├── models/              # Data transfer objects
│   ├── MediaMetadata.kt
│   └── PersistPlayerState.kt
├── utils/               # Utility classes
│   ├── NetworkUtils.kt
│   ├── DiscordRPC.kt
│   └── ScrobbleManager.kt
├── viewmodels/          # ViewModels
│   ├── HomeViewModel.kt
│   ├── PlayerViewModel.kt
│   └── LibraryViewModels.kt
├── lyrics/              # Lyrics providers
│   ├── LyricsProvider.kt
│   ├── KuGouLyricsProvider.kt
│   └── LrcLibLyricsProvider.kt
└── services/            # Background services
    └── AudioService.kt
```

### 1.3 Data Classes

```kotlin
// ✅ CORRECT - Use data classes for holding data
data class SongEntity(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val artworkUrl: String?
)

// ✅ CORRECT - Use copy() for modified instances
fun updateSongTitle(song: SongEntity, newTitle: String): SongEntity {
    return song.copy(title = newTitle)
}

// ✅ CORRECT - Use sealed classes for restricted hierarchies
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// ✅ CORRECT - Use enum classes for fixed sets
enum class LibraryFilter {
    ALL,
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS
}
```

### 1.4 Extension Functions

```kotlin
// ✅ CORRECT - Organize extensions logically in dedicated files
// ContextExt.kt
fun Context.isUserLoggedIn(): Boolean {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    return "SAPISID" in parseCookieString(cookie)
}

// StringExt.kt
fun String.capitalizeWords(): String {
    return split("_").joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
}

// ListExt.kt
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in indices) this[index] else null
}
```

---

## 2. Jetpack Compose Guidelines

### 2.1 Composables Naming

```kotlin
// ✅ CORRECT - Composables are PascalCase functions
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToPlayer: () -> Unit
) { /* ... */ }

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit
) { /* ... */ }

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }
```

### 2.2 State Hoisting

```kotlin
// ✅ CORRECT - Hoist state to the lowest common ancestor
@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }

// ✅ CORRECT - Use rememberSaveable for configuration changes
@Composable
fun rememberPlayerState(): PlayerState {
    return rememberSaveable { PlayerState() }
}

@Composable
fun rememberVisibleSongs(
    songs: List<Song>,
    visibleRange: IntRange
): List<Song> {
    val visibleSongs by remember(songs, visibleRange) {
        derivedStateOf { songs.subList(visibleRange.first, visibleRange.last) }
    }
    return visibleSongs
}
```

### 2.3 StateFlow Usage

```kotlin
// ✅ CORRECT - ViewModel with StateFlow for UI state
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val songs = repository.getSongs()
                _uiState.value = _uiState.value.copy(
                    songs = songs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}
```

### 2.4 Material3 Components

```kotlin
// ✅ CORRECT - Use Material3 theming consistently
@Composable
fun ArchiveTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) dynamicDarkColorScheme() else dynamicLightColorScheme()
    val materialColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(
        LocalColors provides materialColorScheme
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}

// ✅ CORRECT - Use Scaffold for screen layout
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ArchiveTune") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = navigationItems,
                selectedIndex = selectedIndex,
                onItemClick = { /* ... */ }
            )
        }
    ) { paddingValues ->
        HomeContent(
            uiState = uiState,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

---

## 3. Room Database Conventions

### 3.1 Entity Naming

```kotlin
// ✅ CORRECT - Entity class naming
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "artist_id") val artistId: String,
    @ColumnInfo(name = "album_id") val albumId: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ✅ CORRECT - DAO naming with @Dao annotation
@Dao
interface MusicDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)
}

// ✅ CORRECT - Use @ForeignKey for relationships
@Entity(
    tableName = "album_songs",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("album_id")]
)
data class AlbumSongMap(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "album_id") val albumId: String,
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "track_number") val trackNumber: Int
)
```

### 3.2 Type Converters

```kotlin
// ✅ CORRECT - Use @TypeConverter for complex types
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}
```

---

## 4. Dependency Injection (Hilt)

### 4.1 Hilt ViewModel

```kotlin
// ✅ CORRECT - Use @HiltViewModel annotation
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val scrobbleManager: ScrobbleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Hilt constructor injection for dependencies
    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        // Initialization logic
    }
}
```

### 4.2 Hilt Modules

```kotlin
// ✅ CORRECT - Use @Module and @InstallIn for DI modules
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(
        @ApplicationContext context: Context
    ): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "archivetune.db"
        )
            // For production, implement proper migrations
            // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // For development/testing only:
            // .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMusicDao(database: MusicDatabase): MusicDao {
        return database.musicDao()
    }
}

// ✅ CORRECT - Use @Binds for interface implementations
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        musicRepositoryImpl: MusicRepositoryImpl
    ): MusicRepository
}
```

---

## 5. Coroutines and Flow

### 5.1 Structured Concurrency

```kotlin
// ✅ CORRECT - Use viewModelScope for ViewModel coroutines
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getSongs().collect { songs ->
                _uiState.value = _uiState.value.copy(
                    songs = songs,
                    isLoading = false
                )
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                repository.refreshSongs()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

// ✅ CORRECT - Use withContext for blocking operations
private suspend fun loadFromDatabase(): List<Song> {
    return withContext(Dispatchers.IO) {
        musicDao.getAllSongs().first()
    }
}

// ✅ CORRECT - Use SharedFlow for one-time events
private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

private fun navigateToPlayer() {
    viewModelScope.launch {
        _navigationEvent.emit(NavigationEvent.ToPlayer)
    }
}
```

### 5.2 Flow Collection

```kotlin
// ✅ CORRECT - Use Flow for reactive data streams
fun getSongs(): Flow<List<Song>> {
    return musicDao.getAllSongs()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { e ->
            emit(emptyList())
            log("Error: Failed to load songs", e)
        }
}

// ✅ CORRECT - Combine multiple flows
val combinedState: StateFlow<CombinedState> = combine(
    userRepository.getUser(),
    musicRepository.getCurrentSong()
) { user, song ->
    CombinedState(user = user, currentSong = song)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = CombinedState()
)
```

---

## 6. Error Handling

### 6.1 Result Sealed Class

```kotlin
// ✅ CORRECT - Use sealed class for error handling
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }

    companion object {
        suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e)
            }
        }
    }
}

// ✅ CORRECT - Handle Result in ViewModel
fun loadData() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        when (val result = repository.fetchData()) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    data = result.data,
                    isLoading = false,
                    error = null
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
            is Result.Loading -> { /* handled above */ }
        }
    }
}
```

---

## 7. Logging Conventions

### 7.1 Timber Logging

```kotlin
// ✅ CORRECT - Use Timber for logging
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    init {
        Timber.d("HomeViewModel initialized")
    }

    fun loadSongs() {
        Timber.d("Loading songs from repository")
        viewModelScope.launch {
            try {
                val songs = repository.getSongs()
                Timber.d("Loaded ${songs.size} songs")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load songs")
            }
        }
    }
}

// ❌ INCORRECT - Never log sensitive information
fun logUserCredentials(email: String, password: String) {
    Timber.d("User credentials: $email:$password") // ❌ NEVER DO THIS
}
```

---

## 8. Testing Guidelines

### 8.1 Unit Tests

```kotlin
// ✅ CORRECT - ViewModel unit test
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeMusicRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeMusicRepository()
        viewModel = HomeViewModel(repository)
    }

    @Test
    fun `initial state should be loading`() = runTest {
        assertEquals(true, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadSongs should update state with songs`() = runTest {
        val testSongs = listOf(
            SongEntity("1", "Test Song", "Test Artist", "Test Album", 180000, null)
        )
        repository.setSongs(testSongs)

        viewModel.loadSongs()

        advanceUntilIdle()
        assertEquals(testSongs.size, viewModel.uiState.value.songs.size)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

---

## 9. Build Commands

### 9.1 Required Quality Gates

Before submitting a Pull Request, run the following commands:

```bash
# Run lint checks
./gradlew lintDebug

# Check code formatting
./gradlew ktlintCheck

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build release APK (with R8 optimization)
./gradlew assembleRelease
```

---

## 10. Additional Guidelines

### 10.1 Import Organization

```kotlin
// ✅ CORRECT - Organize imports in this order:
package moe.koiverse.archivetune

// Kotlin imports
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Android imports
import android.content.Context
import android.os.Bundle

// AndroidX imports
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme

// Compose imports
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

// Third-party imports
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow

// Project imports
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.viewmodels.HomeViewModel
import moe.koiverse.archivetune.extensions.isUserLoggedIn
```

### 10.2 API Keys and Secrets

```kotlin
// ✅ CORRECT - Store API keys in local.properties or environment variables
// local.properties
LASTFM_API_KEY=your_api_key_here
LASTFM_SECRET=your_secret_here

// Build.gradle.kts
val lastfmApiKey = localProperties.getProperty("LASTFM_API_KEY")
    ?: System.getenv("LASTFM_API_KEY")
    ?: ""

// ❌ INCORRECT - Never hardcode API keys
const val LASTFM_API_KEY = "abc123secret" // ❌ NEVER DO THIS
```

---

## 11. AI Agent Code Review Rules

### 11.1 Fix All Issues Requirement

When AI agents are used to fix code issues, they must address **ALL** identified problems. Partial fixes are not acceptable.

**AI agents must fix:**
1. ✅ All lint warnings and errors
2. ✅ All compiler warnings and errors
3. ✅ All code smells and anti-patterns
4. ✅ All formatting violations (ktlint)
5. ✅ All null safety issues
6. ✅ All performance concerns
7. ✅ All security vulnerabilities
8. ✅ All accessibility issues
9. ✅ All memory leak potential
10. ✅ All threading/concurrency issues

**Prompt for AI Agents to Fix All Issues:**
```
Fix ALL issues in the code. This includes:
- All lint warnings and errors
- All compiler warnings and errors  
- All code smells and anti-patterns
- All formatting violations (ktlint)
- All null safety issues
- All performance concerns
- All security vulnerabilities
- All accessibility issues

Do not leave any known issues unfixed. Apply all necessary changes to resolve every identified problem.
```

### 11.2 Code Review Guidelines for AI Agents

When reviewing code, AI agents should:
- Check for compliance with this styleguide
- Verify architectural layer boundaries
- Ensure proper use of Compose patterns
- Validate error handling completeness
- Confirm coroutine usage correctness
- Review Room database operations
- Assess Hilt dependency injection patterns
- Validate testing coverage

---

## 12. References

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose)
- [Room Database Guidelines](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [ArchiveTune CONTRIBUTING.md](CONTRIBUTING.md)

---

*Last updated: February 2025*

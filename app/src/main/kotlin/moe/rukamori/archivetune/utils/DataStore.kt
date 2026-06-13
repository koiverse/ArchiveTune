/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import moe.rukamori.archivetune.constants.HISTORY_DURATION_LEGACY_FLOAT_KEY
import moe.rukamori.archivetune.constants.HISTORY_DURATION_MIN
import moe.rukamori.archivetune.constants.HISTORY_DURATION_MAX
import moe.rukamori.archivetune.constants.HistoryDuration
import moe.rukamori.archivetune.extensions.toEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.properties.ReadOnlyProperty

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { _ ->
        listOf(
            object : DataMigration<Preferences> {
                override suspend fun shouldMigrate(currentData: Preferences): Boolean {
                    return try {
                        val hasFloat = currentData[HISTORY_DURATION_LEGACY_FLOAT_KEY] != null
                        val hasInt = try {
                            currentData[HistoryDuration] != null
                        } catch (e: ClassCastException) {
                            false
                        }
                        hasFloat && !hasInt
                    } catch (e: Exception) {
                        false
                    }
                }

                override suspend fun migrate(currentData: Preferences): Preferences =
                    currentData.toMutablePreferences().apply {
                        val oldFloat = try {
                            currentData[HISTORY_DURATION_LEGACY_FLOAT_KEY]
                        } catch (e: Exception) {
                            null
                        }
                        if (oldFloat != null) {
                            this.remove(HISTORY_DURATION_LEGACY_FLOAT_KEY)
                            this[HistoryDuration] = oldFloat
                                .toInt()
                                .coerceIn(HISTORY_DURATION_MIN, HISTORY_DURATION_MAX)
                        }
                    }

                override suspend fun cleanUp() {}
            }
        )
    },
)
object PreferenceStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _prefs = MutableStateFlow<Preferences?>(null)
    @Volatile private var started = false

    fun start(context: Context) {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
            scope.launch {
                context.dataStore.data.collect { preferences ->
                    _prefs.value = preferences
                }
            }
        }
    }

    fun <T> get(key: Preferences.Key<T>): T? = _prefs.value?.get(key)

    fun launchEdit(
        dataStore: DataStore<Preferences>,
        block: MutablePreferences.() -> Unit,
    ) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs.block()
            }
        }
    }
}

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
    PreferenceStore.get(key)
        ?: if (Looper.getMainLooper().thread == Thread.currentThread()) {
            null
        } else {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1500) {
                    data.first()[key]
                }
            }
        }

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T,
): T =
    PreferenceStore.get(key)
        ?: if (Looper.getMainLooper().thread == Thread.currentThread()) {
            defaultValue
        } else {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(1500) {
                    data.first()[key]
                } ?: defaultValue
            }
        }

suspend fun <T> DataStore<Preferences>.getAsync(key: Preferences.Key<T>): T? =
    data.first()[key]

suspend fun <T> DataStore<Preferences>.getAsync(
    key: Preferences.Key<T>,
    defaultValue: T,
): T = data.first()[key] ?: defaultValue

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val initialValue = remember(key) {
        PreferenceStore.get(key) ?: defaultValue
    }
    val state = remember(key) { mutableStateOf(initialValue) }

    LaunchedEffect(key) {
        context.dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
            .collect { value ->
                state.value = value
            }
    }

    return remember(key, state) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(newValue) {
                    state.value = newValue
                    PreferenceStore.launchEdit(context.dataStore) {
                        this[key] = newValue
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val initialValue = remember(key) {
        PreferenceStore.get(key).toEnum(defaultValue)
    }
    val state = remember(key) { mutableStateOf(initialValue) }

    LaunchedEffect(key) {
        context.dataStore.data
            .map { it[key].toEnum(defaultValue) }
            .distinctUntilChanged()
            .collect { value ->
                state.value = value
            }
    }

    return remember(key, state) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(newValue) {
                    state.value = newValue
                    PreferenceStore.launchEdit(context.dataStore) {
                        this[key] = newValue.name
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

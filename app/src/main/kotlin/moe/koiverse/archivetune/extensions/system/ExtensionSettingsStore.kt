package moe.koiverse.archivetune.extensions.system

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File

class ExtensionSettingsStore(
    context: Context,
    private val extensionId: String
) {
    private val fileName = "ext_$extensionId.preferences_pb"
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { preferencesOf() },
        produceFile = { context.preferencesDataStoreFile(fileName) }
    )

    fun booleanFlow(key: String, default: Boolean): Flow<Boolean> {
        val k = booleanPreferencesKey(key)
        return dataStore.data.map { it[k] ?: default }
    }

    fun intFlow(key: String, default: Int): Flow<Int> {
        val k = intPreferencesKey(key)
        return dataStore.data.map { it[k] ?: default }
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        val k = booleanPreferencesKey(key)
        return runBlocking { dataStore.data.map { it[k] ?: default }.first() }
    }

    fun getInt(key: String, default: Int): Int {
        val k = intPreferencesKey(key)
        return runBlocking { dataStore.data.map { it[k] ?: default }.first() }
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        val k = booleanPreferencesKey(key)
        dataStore.edit { it[k] = value }
    }

    suspend fun setInt(key: String, value: Int) {
        val k = intPreferencesKey(key)
        dataStore.edit { it[k] = value }
    }
}


package moe.koiverse.archivetune.extensions.system

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

class ExtensionSettingsStore(
    context: Context,
    private val extensionId: String
) {
    private val dataStore: DataStore<Preferences> = getOrCreateStore(context, extensionId)

    companion object {
        private val stores = mutableMapOf<String, DataStore<Preferences>>()
        private fun getOrCreateStore(context: Context, id: String): DataStore<Preferences> {
            return synchronized(stores) {
                stores.getOrPut(id) {
                    PreferenceDataStoreFactory.create(
                        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                        produceFile = { context.preferencesDataStoreFile("ext_$id") }
                    )
                }
            }
        }
    }

    fun booleanFlow(key: String, default: Boolean): Flow<Boolean> {
        val k = booleanPreferencesKey(key)
        return dataStore.data.map { it[k] ?: default }
    }

    fun intFlow(key: String, default: Int): Flow<Int> {
        val k = intPreferencesKey(key)
        return dataStore.data.map { it[k] ?: default }
    }

    fun stringFlow(key: String, default: String): Flow<String> {
        val k = stringPreferencesKey(key)
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

    fun getString(key: String, default: String): String {
        val k = stringPreferencesKey(key)
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

    suspend fun setString(key: String, value: String) {
        val k = stringPreferencesKey(key)
        dataStore.edit { it[k] = value }
    }
}


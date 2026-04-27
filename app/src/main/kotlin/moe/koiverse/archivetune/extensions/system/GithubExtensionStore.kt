package moe.koiverse.archivetune.extensions.system

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.githubExtDataStore by preferencesDataStore(name = "github_extension_sources")

private val SOURCES_KEY = stringPreferencesKey("sources_json")

/**
 * Persists the list of GitHub-tracked extension sources using DataStore.
 */
class GithubExtensionStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val sourcesFlow: Flow<List<GithubExtensionSource>> =
        context.githubExtDataStore.data.map { prefs ->
            val raw = prefs[SOURCES_KEY] ?: return@map emptyList()
            runCatching {
                json.decodeFromString<List<GithubExtensionSource>>(raw)
            }.getOrDefault(emptyList())
        }

    suspend fun save(sources: List<GithubExtensionSource>) {
        context.githubExtDataStore.edit { prefs ->
            prefs[SOURCES_KEY] = json.encodeToString(sources)
        }
    }

    suspend fun upsert(source: GithubExtensionSource) {
        context.githubExtDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<GithubExtensionSource>>(prefs[SOURCES_KEY] ?: "[]")
            }.getOrDefault(emptyList())
            val updated = current.filterNot { it.extensionId == source.extensionId } + source
            prefs[SOURCES_KEY] = json.encodeToString(updated)
        }
    }

    suspend fun remove(extensionId: String) {
        context.githubExtDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<GithubExtensionSource>>(prefs[SOURCES_KEY] ?: "[]")
            }.getOrDefault(emptyList())
            prefs[SOURCES_KEY] = json.encodeToString(current.filterNot { it.extensionId == extensionId })
        }
    }

    suspend fun getAll(): List<GithubExtensionSource> {
        return runCatching {
            val prefs = context.githubExtDataStore.data.first()
            val raw = prefs[SOURCES_KEY] ?: return@runCatching emptyList()
            json.decodeFromString<List<GithubExtensionSource>>(raw)
        }.getOrDefault(emptyList())
    }
}

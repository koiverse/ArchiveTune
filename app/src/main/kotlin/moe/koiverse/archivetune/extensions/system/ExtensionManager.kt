package moe.koiverse.archivetune.extensions.system

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.utils.dataStore
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import moe.koiverse.archivetune.extensions.system.ui.UIConfig
import kotlinx.coroutines.flow.update

data class InstalledExtension(
    val manifest: ExtensionManifest,
    val dir: File,
    val enabled: Boolean,
    val error: String? = null
)

@Singleton
class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _installed = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val installed: StateFlow<List<InstalledExtension>> = _installed.asStateFlow()

    private val runtimes = mutableMapOf<String, ExtensionRuntime>()
    private val uiConfigs = MutableStateFlow<Map<String, Pair<String, UIConfigAndBase>>>(emptyMap())
    data class UIConfigAndBase(val config: UIConfig, val base: File)

    private fun rootDir(): File {
        val parent = context.filesDir.parentFile ?: context.filesDir
        return File(parent, "extensions").apply { mkdirs() }
    }

    fun discover() {
        val list = mutableListOf<InstalledExtension>()
        rootDir().listFiles()?.forEach { extDir ->
            val manifestFile = extDir.resolve("manifest.json")
            if (!manifestFile.exists()) return@forEach
            runCatching {
                val manifest = ExtensionValidator.loadManifest(manifestFile)
                val res = ExtensionValidator.validateManifest(context, manifest, extDir)
                val enabled = isEnabled(manifest.id)
                val installedExt = InstalledExtension(manifest, extDir, enabled, error = if (res.valid) null else res.errors.joinToString(";"))
                list.add(installedExt)
            }.onFailure { e ->
                list.add(InstalledExtension(ExtensionManifest(extDir.name, extDir.name, "0.0.0", "Unknown", "index.js"), extDir, false, error = e.message))
            }
        }
        _installed.value = list.sortedBy { it.manifest.name.lowercase() }
        list.filter { it.enabled && it.error == null }.forEach { load(it.manifest.id) }
    }

    fun installFromZip(uri: Uri): Result<Unit> {
        return runCatching {
            context.contentResolver.openInputStream(uri).use { inStream ->
                requireNotNull(inStream)
                val zis = ZipInputStream(inStream)
                var manifest: ExtensionManifest? = null
                val tempFiles = mutableListOf<Pair<String, ByteArray>>()
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val name = entry.name
                    if (!entry.isDirectory) {
                        val bytes = zis.readBytes()
                        tempFiles.add(name to bytes)
                        if (name.endsWith("manifest.json")) {
                            runCatching {
                                val text = bytes.decodeToString()
                                manifest = json.decodeFromString<ExtensionManifest>(text)
                            }
                        }
                    }
                    zis.closeEntry()
                }
                val mf = manifest ?: throw IllegalStateException("Missing manifest.json")
                val target = rootDir().resolve(mf.id)
                if (target.exists()) target.deleteRecursively()
                target.mkdirs()
                tempFiles.forEach { (name, bytes) ->
                    val out = File(target, name)
                    val canonicalTarget = target.canonicalPath
                    val canonicalOut = out.canonicalPath
                    if (!canonicalOut.startsWith(canonicalTarget)) {
                        throw SecurityException("Invalid entry path")
                    }
                    out.parentFile?.mkdirs()
                    out.writeBytes(bytes)
                }
                val res = ExtensionValidator.validateManifest(context, mf, target)
                if (!res.valid) throw IllegalStateException(res.errors.joinToString(";"))
                discover()
                enable(mf.id)
            }
        }
    }

    fun load(id: String) {
        val ext = _installed.value.firstOrNull { it.manifest.id == id } ?: return
        if (runtimes.containsKey(id)) return
        if (ext.error != null) return
        val store = ExtensionSettingsStore(context, id)
        val rt = ExtensionRuntime(context, ext.manifest, ext.dir, store)
        val res = rt.load()
        if (res.isSuccess) {
            runtimes[id] = rt
        } else {
            disable(id)
        }
    }

    fun unload(id: String) {
        val rt = runtimes.remove(id) ?: return
        runCatching { rt.unload() }
        clearUiConfigsForExtension(id)
    }

    fun enable(id: String) {
        scope.launch {
            context.dataStore.edit { it[booleanPreferencesKey(flagKey(id))] = true }
        }
        load(id)
        updateEnabledState(id, true)
    }

    fun disable(id: String) {
        scope.launch {
            context.dataStore.edit { it[booleanPreferencesKey(flagKey(id))] = false }
        }
        unload(id)
        updateEnabledState(id, false)
    }

    fun reload(id: String) {
        unload(id)
        load(id)
    }

    fun onTrackPlay(metadata: MediaMetadata) {
        runtimes.values.forEach { runCatching { it.onTrackPlay(metadata) } }
    }

    fun onQueueBuild(title: String?) {
        runtimes.values.forEach { runCatching { it.onQueueBuild(title) } }
    }
    
    fun setUiConfig(extensionId: String, route: String, config: UIConfig, baseDir: File) {
        uiConfigs.update { it.toMutableMap().apply { set(route, extensionId to UIConfigAndBase(config, baseDir)) } }
    }
    
    fun clearUiConfig(route: String) {
        uiConfigs.update { it.toMutableMap().apply { remove(route) } }
    }
    
    fun clearUiConfigsForExtension(extensionId: String) {
        uiConfigs.update {
            it.filterValues { pair -> pair.first != extensionId }
        }
    }
    
    fun uiConfig(route: String): Pair<String, UIConfigAndBase>? {
        return uiConfigs.value[route]
    }
    
    fun delete(id: String): Result<Unit> {
        return runCatching {
            disable(id)
            clearUiConfigsForExtension(id)
            val dir = rootDir().resolve(id)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            _installed.value = _installed.value.filterNot { it.manifest.id == id }
            runtimes.remove(id)
        }
    }

    private fun flagKey(id: String) = "ext_enabled_$id"

    private fun isEnabled(id: String): Boolean {
        val k = booleanPreferencesKey(flagKey(id))
        return runBlocking {
            context.dataStore.data.map { it[k] ?: false }.first()
        }
    }

    private fun updateEnabledState(id: String, enabled: Boolean) {
        _installed.value = _installed.value.map {
            if (it.manifest.id == id) it.copy(enabled = enabled) else it
        }
    }
}

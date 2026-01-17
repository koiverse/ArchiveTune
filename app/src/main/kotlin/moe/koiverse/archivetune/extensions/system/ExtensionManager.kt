package moe.koiverse.archivetune.extensions.system

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.utils.dataStore
import android.net.Uri
import java.util.zip.ZipInputStream
import java.io.File
import java.util.concurrent.Executors
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
    private val runtimeDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + runtimeDispatcher)
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile
    private var discoverJob: Job? = null

    private val _installed = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val installed: StateFlow<List<InstalledExtension>> = _installed.asStateFlow()

    private val runtimes = mutableMapOf<String, ExtensionRuntime>()
    private val uiConfigs = MutableStateFlow<Map<String, List<Pair<String, UIConfigAndBase>>>>(emptyMap())
    data class UIConfigAndBase(val config: UIConfig, val base: File)

    private fun rootDir(): File {
        val parent = context.filesDir.parentFile ?: context.filesDir
        return File(parent, "extensions").apply { mkdirs() }
    }

    fun discover() {
        discoverJob?.cancel()
        discoverJob =
            scope.launch {
                installBuiltInExtensionsIfMissing()
                val list = mutableListOf<InstalledExtension>()
                rootDir().listFiles()?.forEach { extDir ->
                    val manifestFile = extDir.resolve("manifest.json")
                    if (!manifestFile.exists()) return@forEach
                    runCatching {
                        val manifest = ExtensionValidator.loadManifest(manifestFile)
                        val res = ExtensionValidator.validateManifest(context, manifest, extDir)
                        val enabled = isEnabled(manifest.id, manifest.autoEnable)
                        val installedExt =
                            InstalledExtension(
                                manifest,
                                extDir,
                                enabled,
                                error = if (res.valid) null else res.errors.joinToString(";"),
                            )
                        list.add(installedExt)
                    }.onFailure { e ->
                        list.add(
                            InstalledExtension(
                                ExtensionManifest(
                                    extDir.name,
                                    extDir.name,
                                    "0.0.0",
                                    "Unknown",
                                    "index.js",
                                ),
                                extDir,
                                false,
                                error = e.message,
                            ),
                        )
                    }
                }
                _installed.value = list.sortedBy { it.manifest.name.lowercase() }
                list.filter { it.enabled && it.error == null }.forEach { loadInternal(it.manifest.id) }
            }
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
                _installed.value =
                    (_installed.value.filterNot { it.manifest.id == mf.id } +
                            InstalledExtension(mf, target, enabled = true, error = null))
                        .sortedBy { it.manifest.name.lowercase() }
                discover()
                enable(mf.id)
            }
        }
    }

    fun load(id: String) {
        scope.launch { loadInternal(id) }
    }

    fun unload(id: String) {
        scope.launch { unloadInternal(id) }
    }

    fun enable(id: String) {
        updateEnabledState(id, true)
        scope.launch {
            context.dataStore.edit { it[booleanPreferencesKey(flagKey(id))] = true }
            loadInternal(id)
        }
    }

    fun disable(id: String) {
        updateEnabledState(id, false)
        scope.launch {
            context.dataStore.edit { it[booleanPreferencesKey(flagKey(id))] = false }
            unloadInternal(id)
        }
    }

    fun reload(id: String) {
        scope.launch {
            unloadInternal(id)
            loadInternal(id)
        }
    }

    fun onTrackPlay(metadata: MediaMetadata) {
        scope.launch {
            runtimes.values.forEach { runCatching { it.onTrackPlay(metadata) } }
        }
    }

    fun onQueueBuild(title: String?) {
        scope.launch {
            runtimes.values.forEach { runCatching { it.onQueueBuild(title) } }
        }
    }
    
    fun onTrackPause(metadata: MediaMetadata) {
        scope.launch {
            runtimes.values.forEach { runCatching { it.onTrackPause(metadata) } }
        }
    }
    
    fun setUiConfig(extensionId: String, route: String, config: UIConfig, baseDir: File) {
        uiConfigs.update {
            val map = it.toMutableMap()
            val existing = map[route].orEmpty()
            val filtered = existing.filterNot { entry -> entry.first == extensionId }
            val updated = filtered + (extensionId to UIConfigAndBase(config, baseDir))
            map[route] = updated.sortedByDescending { entry -> entry.second.config.priority }
            map
        }
    }
    
    fun clearUiConfig(route: String) {
        uiConfigs.update { current ->
            current.toMutableMap().apply { remove(route) }
        }
    }
    
    fun clearUiConfigsForExtension(extensionId: String) {
        uiConfigs.update { current ->
            current.mapValues { (_, list) ->
                list.filterNot { entry -> entry.first == extensionId }
            }.filterValues { list -> list.isNotEmpty() }
        }
    }
    
    fun uiConfig(route: String): Pair<String, UIConfigAndBase>? {
        return uiConfigs.value[route]?.maxByOrNull { it.second.config.priority }
    }
    
    fun uiConfigs(route: String): List<Pair<String, UIConfigAndBase>> {
        return uiConfigs.value[route].orEmpty()
    }
    
    fun delete(id: String): Result<Unit> {
        return runCatching {
            updateEnabledState(id, false)
            scope.launch {
                context.dataStore.edit { it[booleanPreferencesKey(flagKey(id))] = false }
            }
            unloadInternal(id)
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

    private suspend fun isEnabled(id: String, default: Boolean = false): Boolean {
        val k = booleanPreferencesKey(flagKey(id))
        return context.dataStore.data.first()[k] ?: default
    }

    private fun updateEnabledState(id: String, enabled: Boolean) {
        _installed.value = _installed.value.map {
            if (it.manifest.id == id) it.copy(enabled = enabled) else it
        }
    }

    private fun loadInternal(id: String) {
        val ext = _installed.value.firstOrNull { it.manifest.id == id } ?: return
        if (runtimes.containsKey(id)) return
        if (ext.error != null) return
        val store = ExtensionSettingsStore(context, id)
        val rt = ExtensionRuntime(context, ext.manifest, ext.dir, store)
        val res = rt.load()
        if (res.isSuccess) {
            runtimes[id] = rt
        } else {
            val message = res.exceptionOrNull()?.message ?: res.exceptionOrNull()?.javaClass?.simpleName ?: "Unknown error"
            _installed.value = _installed.value.map { item ->
                if (item.manifest.id == id) item.copy(enabled = false, error = message) else item
            }
            updateEnabledState(id, false)
            scope.launch {
                context.dataStore.edit { it[booleanPreferencesKey(flagKey(id))] = false }
            }
        }
    }

    private fun unloadInternal(id: String) {
        val rt = runtimes.remove(id) ?: return
        runCatching { rt.unload() }
        clearUiConfigsForExtension(id)
    }

    private fun installBuiltInExtensionsIfMissing() {
        val assetManager = context.assets
        val builtinRoot = "builtin"
        val items = runCatching { assetManager.list(builtinRoot) }.getOrNull().orEmpty()
        if (items.isEmpty()) return
        items.forEach { id ->
            if (id.isBlank()) return@forEach
            val targetDir = rootDir().resolve(id)
            val needsRepair =
                if (!targetDir.exists()) {
                    true
                } else {
                    val manifestFile = targetDir.resolve("manifest.json")
                    if (!manifestFile.exists()) {
                        true
                    } else {
                        val entry =
                            runCatching { ExtensionValidator.loadManifest(manifestFile).entry }.getOrNull()
                        if (entry.isNullOrBlank()) {
                            true
                        } else {
                            !targetDir.resolve(entry).exists()
                        }
                    }
                }

            if (!needsRepair) return@forEach
            if (targetDir.exists()) targetDir.deleteRecursively()
            copyAssetPath("$builtinRoot/$id", targetDir)
        }
    }

    private fun copyAssetPath(assetPath: String, out: File) {
        val assetManager = context.assets
        val children = runCatching { assetManager.list(assetPath) }.getOrNull().orEmpty()
        if (children.isEmpty()) {
            out.parentFile?.mkdirs()
            assetManager.open(assetPath).use { input ->
                out.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return
        }
        out.mkdirs()
        children.forEach { child ->
            copyAssetPath("$assetPath/$child", File(out, child))
        }
    }
}

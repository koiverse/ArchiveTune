package moe.koiverse.archivetune.extensions.system

import android.content.Context
import moe.koiverse.archivetune.models.MediaMetadata
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ui.UIConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import dagger.hilt.android.EntryPointAccessors
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.File

class ExtensionRuntime(
    private val appContext: Context,
    private val manifest: ExtensionManifest,
    private val baseDir: File,
    private val settings: ExtensionSettingsStore
) {
    private var cx: RhinoContext? = null
    private var scope: Scriptable? = null
    private var onLoadFn: Function? = null
    private var onUnloadFn: Function? = null
    private var onTrackPlayFn: Function? = null
    private var onQueueBuildFn: Function? = null

    fun load(): Result<Unit> {
        return runCatching {
            val c = RhinoContext.enter()
            cx = c
            scope = c.initSafeStandardObjects()
            val host = HostApi(appContext, manifest, settings, baseDir)
            val wrappedHost = RhinoContext.javaToJS(host, scope)
            ScriptableObject.putProperty(scope, "ArchiveTune", wrappedHost)

            val entryFile = baseDir.resolve(manifest.entry)
            val code = entryFile.readText()
            c.evaluateString(scope, code, manifest.entry, 1, null)

            onLoadFn = lookupFunction("onLoad")
            onUnloadFn = lookupFunction("onUnload")
            onTrackPlayFn = lookupFunction("onTrackPlay")
            onQueueBuildFn = lookupFunction("onQueueBuild")

            onLoadFn?.let { fn ->
                fn.call(c, scope, scope, arrayOf(wrappedHost))
            }
        }
    }

    fun unload() {
        runCatching {
            val c = cx ?: return@runCatching
            val s = scope ?: return@runCatching
            onUnloadFn?.call(c, s, s, emptyArray())
        }
        RhinoContext.exit()
        cx = null
        scope = null
        onLoadFn = null
        onUnloadFn = null
        onTrackPlayFn = null
        onQueueBuildFn = null
    }

    fun onTrackPlay(metadata: MediaMetadata) {
        val c = cx ?: return
        val s = scope ?: return
        val payload = mapOf(
            "id" to metadata.id,
            "title" to metadata.title,
            "artists" to metadata.artists,
            "album" to metadata.album
        )
        val jsPayload = RhinoContext.javaToJS(payload, s)
        runCatching { onTrackPlayFn?.call(c, s, s, arrayOf(jsPayload)) }
    }

    fun onQueueBuild(queueTitle: String?) {
        val c = cx ?: return
        val s = scope ?: return
        val payload = mapOf("title" to (queueTitle ?: ""))
        val jsPayload = RhinoContext.javaToJS(payload, s)
        runCatching { onQueueBuildFn?.call(c, s, s, arrayOf(jsPayload)) }
    }

    private fun lookupFunction(name: String): Function? {
        val s = scope ?: return null
        val prop = ScriptableObject.getProperty(s, name)
        return if (prop is Function) prop else null
    }
}

class HostApi(
    private val context: Context,
    private val manifest: ExtensionManifest,
    private val settings: ExtensionSettingsStore,
    private val baseDir: File
) {
    fun log(message: String) {
        android.util.Log.i("Extension-${manifest.id}", message.take(1000))
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        return settings.getBoolean(key, default)
    }

    fun getInt(key: String, default: Int): Int {
        return settings.getInt(key, default)
    }

    fun setUI(route: String, jsonConfig: String) {
        if (!manifest.permissions.contains(ExtensionPermission.UIOverride.name)) return
        val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
        val manager = entryPoint.extensionManager()
        val cfg = Json { ignoreUnknownKeys = true }.decodeFromString<UIConfig>(jsonConfig)
        manager.setUiConfig(manifest.id, route, cfg, baseDir)
    }

    fun clearUI(route: String) {
        if (!manifest.permissions.contains(ExtensionPermission.UIOverride.name)) return
        val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
        val manager = entryPoint.extensionManager()
        manager.clearUiConfig(route)
    }
}


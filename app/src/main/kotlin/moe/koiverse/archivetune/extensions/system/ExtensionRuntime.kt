package moe.koiverse.archivetune.extensions.system

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import app.cash.quickjs.QuickJs
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ui.UIConfig
import moe.koiverse.archivetune.extensions.system.ui.UiSlots
import moe.koiverse.archivetune.models.MediaMetadata
import java.io.File

class ExtensionRuntime(
    private val appContext: Context,
    private val manifest: ExtensionManifest,
    private val baseDir: File,
    private val settings: ExtensionSettingsStore
) {
    private var engine: QuickJs? = null

    fun load(): Result<Unit> {
        return runCatching {
        val quickJs = QuickJs.create()
        engine = quickJs
        val host = HostApi(appContext, manifest, settings, baseDir)
        quickJs.set("ArchiveTune", HostApiContract::class.java, host)
        quickJs.evaluate("""globalThis.__ArchiveTuneHooks = {}; globalThis.__ArchiveTuneDispatch = function(name, payloadJson) { var hooks = globalThis.__ArchiveTuneHooks[name]; if (!hooks) return; var payload = null; if (payloadJson != null && payloadJson !== '') { try { payload = JSON.parse(payloadJson); } catch (e) { payload = null; } } for (var i = 0; i < hooks.length; i++) { var h = hooks[i]; if (typeof h === 'function') { try { h(payload); } catch (e) {} } } };""")
        val slotsScript = """
            if (typeof ArchiveTune === 'object' && ArchiveTune !== null) {
                ArchiveTune.slots = {
                    playerOverlay: "${UiSlots.PLAYER_OVERLAY}",
                    lyricsOverlay: "${UiSlots.LYRICS_OVERLAY}",
                    queueOverlay: "${UiSlots.QUEUE_OVERLAY}",
                    searchFilter: "${UiSlots.SEARCH_FILTER}",
                    settingsEntry: "${UiSlots.SETTINGS_ENTRY}",
                    slot: function(id) { return "slot_" + id; },
                    topBarActions: function(route) { return "topbar_actions_" + route; },
                    bottomBar: function(route) { return "bottombar_" + route; },
                    fab: function(route) { return "fab_" + route; },
                    contextMenu: function(contextId, itemType) { return "context_" + contextId + "_" + itemType; },
                    navItem: function(position) { return "nav_item_" + position; },
                    homeWidget: function(id) { return "home_widget_" + id; }
                };
            }
        """.trimIndent()
        quickJs.evaluate(slotsScript)
        val entryFile = baseDir.resolve(manifest.entry)
            val code = entryFile.readText()
            quickJs.evaluate(code)
            registerHooks()
            callLifecycle("onLoad")
            dispatch("onLoad", null)
        }
    }

    fun unload() {
        runCatching {
            dispatch("onUnload", null)
            callLifecycle("onUnload")
        }
        engine?.close()
        engine = null
    }

    fun onTrackPlay(metadata: MediaMetadata) {
        val payload = trackPayload(metadata)
        dispatch("onTrackPlay", payload)
    }

    fun onTrackPause(metadata: MediaMetadata) {
        val payload = trackPayload(metadata)
        dispatch("onTrackPause", payload)
    }

    fun onQueueBuild(queueTitle: String?) {
        val payload = queuePayload(queueTitle)
        dispatch("onQueueBuild", payload)
    }

    private fun registerHooks() {
        val quickJs = engine ?: return
        if (manifest.hooks.isEmpty()) return
        manifest.hooks.groupBy { it.event }.forEach { (event, hooksForEvent) ->
            val sorted = hooksForEvent.sortedByDescending { it.priority }
            val registrationScript = buildString {
                append("(function(){ var list = globalThis.__ArchiveTuneHooks['")
                append(event)
                append("']; if (!list) { list = []; globalThis.__ArchiveTuneHooks['")
                append(event)
                append("'] = list; }")
                sorted.forEach { hook ->
                    if (hook.handler.isNotBlank()) {
                        append(" if (typeof ")
                        append(hook.handler)
                        append(" === 'function') { list.push(")
                        append(hook.handler)
                        append("); }")
                    }
                }
                append("})();")
            }
            quickJs.evaluate(registrationScript)
        }
    }

    private fun dispatch(event: String, payloadJson: String?) {
        val quickJs = engine ?: return
        val hasHooks = manifest.hooks.any { it.event == event }
        if (!hasHooks) {
            dispatchLegacy(event, payloadJson)
            return
        }
        val safePayload = payloadJson ?: ""
        val script = buildString {
            append("if (typeof __ArchiveTuneDispatch === 'function') { __ArchiveTuneDispatch('")
            append(event)
            append("', ")
            append(jsonStringLiteral(safePayload))
            append("); }")
        }
        quickJs.evaluate(script)
    }

    private fun dispatchLegacy(event: String, payloadJson: String?) {
        val quickJs = engine ?: return
        val functionName = when (event) {
            "onTrackPlay" -> "onTrackPlay"
            "onTrackPause" -> "onTrackPause"
            "onQueueBuild" -> "onQueueBuild"
            else -> return
        }
        val script = if (payloadJson.isNullOrEmpty()) {
            "if (typeof " + functionName + " === 'function') { try { " + functionName + "(); } catch (e) {} }"
        } else {
            "if (typeof " + functionName + " === 'function') { try { " + functionName + "(" + payloadJson + "); } catch (e) {} }"
        }
        quickJs.evaluate(script)
    }

    private fun trackPayload(metadata: MediaMetadata): String {
        val title = metadata.title ?: ""
        val album = metadata.album?.title ?: ""
        val artists = metadata.artists?.joinToString(",") ?: ""
        val id = metadata.id ?: ""
        return "{" + "\"id\":${jsonStringLiteral(id)}," + "\"title\":${jsonStringLiteral(title)}," + "\"artists\":${jsonStringLiteral(artists)}," + "\"album\":${jsonStringLiteral(album)}" + "}"
    }

    private fun queuePayload(queueTitle: String?): String {
        val title = queueTitle ?: ""
        return "{" + "\"title\":${jsonStringLiteral(title)}" + "}"
    }

    private fun callLifecycle(name: String) {
        val quickJs = engine ?: return
        val script = "if (typeof " + name + " === 'function') { try { " + name + "(ArchiveTune); } catch (e) {} }"
        quickJs.evaluate(script)
    }

    private fun jsonStringLiteral(value: String): String {
        val escaped = buildString {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
        return "\"" + escaped + "\""
    }
}

interface HostApiContract {
    fun log(message: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getString(key: String, default: String): String
    fun setUI(route: String, jsonConfig: String)
    fun clearUI(route: String)
    fun showToast(message: String)
}

class HostApi(
    private val context: Context,
    private val manifest: ExtensionManifest,
    private val settings: ExtensionSettingsStore,
    private val baseDir: File
) : HostApiContract {
    private val json = Json { ignoreUnknownKeys = true }

    override fun log(message: String) {
        android.util.Log.i("Extension-${manifest.id}", message.take(1000))
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return settings.getBoolean(key, default)
    }

    override fun getInt(key: String, default: Int): Int {
        return settings.getInt(key, default)
    }

    override fun getString(key: String, default: String): String {
        return settings.getString(key, default)
    }

    override fun setUI(route: String, jsonConfig: String) {
        if (!manifest.permissions.contains(ExtensionPermission.UIOverride.name)) return
        val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
        val manager = entryPoint.extensionManager()
        val cfg = json.decodeFromString<UIConfig>(jsonConfig)
        manager.setUiConfig(manifest.id, route, cfg, baseDir)
    }

    override fun clearUI(route: String) {
        if (!manifest.permissions.contains(ExtensionPermission.UIOverride.name)) return
        val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
        val manager = entryPoint.extensionManager()
        manager.clearUiConfig(route)
    }

    override fun showToast(message: String) {
        if (!manifest.permissions.contains(ExtensionPermission.NotificationShow.name)) return
        val text = message.take(200)
        if (text.isBlank()) return
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

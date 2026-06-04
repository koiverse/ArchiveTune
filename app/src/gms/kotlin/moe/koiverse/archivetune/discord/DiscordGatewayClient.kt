package moe.koiverse.archivetune.discord

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber

private val json = Json { ignoreUnknownKeys = true }

private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"

object DiscordGatewayClient {
    private const val TAG = "DiscordGatewayClient"

    private val mutex = Mutex()
    private var session: WebSocketSession? = null
    private var heartbeatJob: Job? = null
    private var connectionJob: Job? = null
    private var scope: CoroutineScope? = null
    private var heartbeatIntervalMs: Long = 41_250L
    private var sequenceNumber: Int? = null
    private var currentToken: String? = null
    private var currentActivity: DiscordPresenceActivity? = null

    private val httpClient = HttpClient {
        install(WebSockets)
    }

    val isConnected: Boolean
        get() = session != null && heartbeatJob?.isActive == true

    suspend fun connect(token: String, activity: DiscordPresenceActivity? = null): Result<Unit> =
        mutex.withLock {
            runCatching {
                disconnectInternal()

                currentToken = token
                currentActivity = activity

                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

                connectionJob = scope!!.launch {
                    httpClient.webSocket(GATEWAY_URL) {
                        session = this

                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    handleGatewayMessage(text)
                                }
                                is Frame.Close -> {
                                    Timber.tag(TAG).d("Gateway connection closed: ${frame.readText()}")
                                    break
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

    suspend fun updatePresence(activity: DiscordPresenceActivity): Result<Unit> =
        mutex.withLock {
            runCatching {
                currentActivity = activity
                sendPresenceUpdate(activity)
            }
        }

    suspend fun clearPresence(): Result<Unit> =
        mutex.withLock {
            runCatching {
                currentActivity = null
                val payload = buildJsonObject {
                    put("op", 3)
                    putJsonObject("d") {
                        put("since", null)
                        putJsonArray("activities", buildJsonArray { })
                        put("status", "online")
                        put("afk", false)
                    }
                }
                session?.send(Frame.Text(json.encodeToString(payload)))
            }
        }

    suspend fun disconnect() {
        mutex.withLock {
            disconnectInternal()
        }
    }

    private suspend fun disconnectInternal() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        connectionJob?.cancel()
        connectionJob = null
        scope?.cancel()
        scope = null
        runCatching {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnect"))
        }
        session = null
        currentToken = null
        currentActivity = null
    }

    private suspend fun handleGatewayMessage(text: String) {
        runCatching {
            val msg = json.decodeFromString<GatewayMessage>(text)
            when (msg.op) {
                0 -> {
                    sequenceNumber = msg.s ?: sequenceNumber
                    if (msg.t == "READY") {
                        Timber.tag(TAG).d("Gateway ready")
                        val current = currentActivity
                        if (current != null) {
                            sendPresenceUpdate(current)
                        }
                    }
                }
                1 -> sendHeartbeat()
                7 -> {
                    Timber.tag(TAG).d("Gateway requested reconnect")
                    reconnect()
                }
                9 -> {
                    Timber.tag(TAG).w("Invalid session, re-identifying")
                    val d = msg.d
                    val canResume = d is JsonPrimitive && d.content == "true"
                    if (!canResume) {
                        sequenceNumber = null
                    }
                    identify()
                }
                10 -> {
                    val d = msg.d as? JsonObject
                    val interval = d?.get("heartbeat_interval")?.jsonPrimitive?.content?.toLongOrNull()
                    if (interval != null) {
                        heartbeatIntervalMs = interval
                        startHeartbeat()
                    }
                    identify()
                }
            }
        }.onFailure {
            Timber.tag(TAG).e(it, "Failed to handle gateway message: $text")
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope?.launch {
            while (isActive) {
                delay(heartbeatIntervalMs)
                sendHeartbeat()
            }
        }
    }

    private suspend fun sendHeartbeat() {
        val payload = buildJsonObject {
            put("op", 1)
            put("d", sequenceNumber?.let { JsonPrimitive(it) } ?: JsonPrimitive(null))
        }
        runCatching {
            session?.send(Frame.Text(json.encodeToString(payload)))
        }
    }

    private suspend fun identify() {
        val token = currentToken ?: return

        val payload = buildJsonObject {
            put("op", 2)
            putJsonObject("d") {
                put("token", token)
                putJsonObject("properties") {
                    put("os", "android")
                    put("browser", "ArchiveTune")
                    put("device", "ArchiveTune")
                }
                putJsonObject("presence") {
                    put("since", 0)
                    putJsonArray("activities", buildJsonArray { })
                    put("status", "online")
                    put("afk", false)
                }
            }
        }
        runCatching {
            session?.send(Frame.Text(json.encodeToString(payload)))
        }
    }

    private suspend fun sendPresenceUpdate(activity: DiscordPresenceActivity) {
        val payload = buildPresencePayload(activity)
        runCatching {
            session?.send(Frame.Text(json.encodeToString(payload)))
        }
    }

    private fun buildPresencePayload(activity: DiscordPresenceActivity): JsonObject {
        val activityType = activity.type.nativeValue

        return buildJsonObject {
            put("op", 3)
            putJsonObject("d") {
                put("since", JsonPrimitive(null))
                putJsonArray("activities") {
                    addJsonObject {
                        put("name", activity.name ?: "ArchiveTune")
                        put("type", activityType)
                        activity.details?.let { put("details", it) }
                        activity.state?.let { put("state", it) }
                        activity.detailsUrl?.let { put("details_url", it) }

                        val assets = activity.assets
                        if (assets.largeImage != null || assets.smallImage != null) {
                            putJsonObject("assets") {
                                assets.largeImage?.let { put("large_image", it) }
                                assets.largeText?.let { put("large_text", it) }
                                assets.smallImage?.let { put("small_image", it) }
                                assets.smallText?.let { put("small_text", it) }
                            }
                        }

                        val ts = activity.timestamps
                        if (ts.startEpochSeconds != null || ts.endEpochSeconds != null) {
                            putJsonObject("timestamps") {
                                ts.startEpochSeconds?.let { put("start", it) }
                                ts.endEpochSeconds?.let { put("end", it) }
                            }
                        }

                        if (activity.buttons.isNotEmpty()) {
                            putJsonArray("buttons") {
                                activity.buttons.take(2).forEach { button ->
                                    addJsonObject {
                                        put("label", button.label)
                                        put("url", button.url)
                                    }
                                }
                            }
                        }
                    }
                }
                put("status", activity.onlineStatus.toGatewayStatus())
                put("afk", false)
            }
        }
    }

    private suspend fun reconnect() {
        val token = currentToken
        val activity = currentActivity
        disconnectInternal()

        if (token != null) {
            delay(1000L)
            connect(token, activity)
        }
    }

    @Serializable
    private data class GatewayMessage(
        val op: Int,
        val d: JsonElement? = null,
        val s: Int? = null,
        val t: String? = null,
    )
}

private fun DiscordOnlineStatus.toGatewayStatus(): String = when (this) {
    DiscordOnlineStatus.Online -> "online"
    DiscordOnlineStatus.Idle -> "idle"
    DiscordOnlineStatus.Dnd -> "dnd"
    DiscordOnlineStatus.Invisible -> "invisible"
    DiscordOnlineStatus.Streaming -> "online"
}

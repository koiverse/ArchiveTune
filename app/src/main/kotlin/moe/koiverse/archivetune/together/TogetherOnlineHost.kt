/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.together

import androidx.compose.runtime.Immutable
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

@Immutable
sealed class TogetherOnlineHostState {
    data object Idle : TogetherOnlineHostState()
    data object Connecting : TogetherOnlineHostState()
    data class Connected(
        val wsUrl: String,
        val sessionId: String,
        val hostParticipantId: String,
    ) : TogetherOnlineHostState()
}

class TogetherOnlineHost(
    externalScope: CoroutineScope,
    val sessionId: String,
    private val sessionKey: String,
    private val hostId: String,
    private val hostDisplayName: String,
    initialSettings: TogetherRoomSettings,
) {
    private val client =
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(15, TimeUnit.SECONDS)
                    pingInterval(25, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                }
            }
            install(WebSockets) {
                pingIntervalMillis = 25_000
            }
        }

    private val scope = CoroutineScope(externalScope.coroutineContext + SupervisorJob())
    private val mutex = Mutex()
    private var settings: TogetherRoomSettings = initialSettings

    private var session: WebSocketSession? = null
    private var loopJob: Job? = null
    private var hostParticipantId: String? = null

    private val clientId = UUID.randomUUID().toString()

    private data class Guest(
        val participantId: String,
        val clientId: String,
        val name: String,
        var pending: Boolean,
    )

    private val guests = LinkedHashMap<String, Guest>()

    @Volatile
    private var lastParticipants: List<TogetherParticipant> = emptyList()

    var onEvent: ((TogetherServerEvent) -> Unit)? = null

    suspend fun connect(wsUrl: String) {
        disconnect()
        hostParticipantId = null
        guests.clear()
        lastParticipants = emptyList()

        val trimmed = wsUrl.trim()
        val urls = listOfNotNull(trimmed, alternateWebSocketSchemeOrNull(trimmed)).distinct()

        var lastError: Throwable? = null
        for (candidate in urls) {
            try {
                client.webSocket(urlString = candidate) {
                    session = this
                    val hello =
                        ClientHello(
                            protocolVersion = TogetherProtocolVersion,
                            sessionId = sessionId,
                            sessionKey = sessionKey,
                            clientId = clientId,
                            displayName = hostDisplayName.trim().ifBlank { "Host" },
                        )
                    send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), hello))
                    runLoop(this, candidate)
                }
                return
            } catch (t: Throwable) {
                lastError = t
            }
        }

        onEvent?.invoke(TogetherServerEvent.Error(connectionFailureMessage(lastError), lastError))
    }

    private fun alternateWebSocketSchemeOrNull(url: String): String? {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("ws://") -> "wss://${trimmed.removePrefix("ws://")}"
            trimmed.startsWith("wss://") -> "ws://${trimmed.removePrefix("wss://")}"
            else -> null
        }
    }

    private fun connectionFailureMessage(t: Throwable?): String {
        val root = generateSequence(t) { it.cause }.lastOrNull()
        val raw = root?.message?.trim().orEmpty()
        val reason =
            when (root) {
                is java.net.UnknownHostException -> "Server not found"
                is java.net.ConnectException -> "Connection refused"
                is java.net.SocketTimeoutException -> "Connection timed out"
                is javax.net.ssl.SSLHandshakeException -> "Secure connection failed"
                is IllegalArgumentException ->
                    if (raw.contains("ws", ignoreCase = true) && raw.contains("scheme", ignoreCase = true)) "Invalid server websocket URL" else null
                else -> null
            }

        val detail = reason ?: raw.takeIf { it.isNotBlank() }
        return if (detail == null) "Connection failed" else "Connection failed: $detail"
    }

    suspend fun disconnect() {
        loopJob?.cancel()
        loopJob?.cancelAndJoin()
        loopJob = null
        runCatching { session?.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnect")) }
        session = null
        hostParticipantId = null
        guests.clear()
        lastParticipants = emptyList()
    }

    fun currentParticipants(): List<TogetherParticipant> = lastParticipants

    suspend fun currentSettings(): TogetherRoomSettings = mutex.withLock { settings }

    suspend fun updateSettings(newSettings: TogetherRoomSettings) {
        mutex.withLock {
            settings = newSettings
        }
    }

    suspend fun approveParticipant(participantId: String, approved: Boolean) {
        val guest = guests[participantId] ?: return
        if (!guest.pending) return

        if (!approved) {
            runCatching {
                session?.send(
                    TogetherJson.json.encodeToString(
                        TogetherMessage.serializer(),
                        JoinDecision(sessionId = sessionId, participantId = participantId, approved = false),
                    ),
                )
            }
            guest.pending = false
            return
        }

        guest.pending = false
        runCatching {
            session?.send(
                TogetherJson.json.encodeToString(
                    TogetherMessage.serializer(),
                    JoinDecision(sessionId = sessionId, participantId = participantId, approved = true),
                ),
            )
        }
        onEvent?.invoke(
            TogetherServerEvent.ParticipantJoined(
                TogetherParticipant(
                    id = participantId,
                    name = guest.name,
                    isHost = false,
                    isPending = false,
                    isConnected = true,
                ),
            ),
        )
        rebuildParticipantsSnapshot()
    }

    suspend fun kickParticipant(participantId: String, reason: String?) {
        if (!guests.containsKey(participantId)) return
        runCatching {
            session?.send(
                TogetherJson.json.encodeToString(
                    TogetherMessage.serializer(),
                    KickParticipant(sessionId = sessionId, participantId = participantId, reason = reason),
                ),
            )
        }
    }

    suspend fun broadcastRoomState(state: TogetherRoomState) {
        val snapshotSettings = mutex.withLock { settings }
        rebuildParticipantsSnapshot()
        val roomState =
            state.copy(
                hostId = hostId,
                settings = snapshotSettings,
                participants = lastParticipants,
            )

        runCatching {
            session?.send(
                TogetherJson.json.encodeToString(
                    TogetherMessage.serializer(),
                    RoomStateMessage(roomState),
                ),
            )
        }
    }

    private fun rebuildParticipantsSnapshot() {
        val host =
            TogetherParticipant(
                id = hostId,
                name = hostDisplayName,
                isHost = true,
                isPending = false,
                isConnected = true,
            )

        val guestList =
            guests.values
                .sortedBy { it.name.lowercase() }
                .map {
                    TogetherParticipant(
                        id = it.participantId,
                        name = it.name,
                        isHost = false,
                        isPending = it.pending,
                        isConnected = true,
                    )
                }

        lastParticipants = buildList {
            add(host)
            addAll(guestList)
        }
    }

    private suspend fun runLoop(session: WebSocketSession, wsUrl: String) {
        loopJob =
            scope.launch {
                try {
                    while (true) {
                        val frame =
                            try {
                                session.incoming.receive()
                            } catch (_: ClosedReceiveChannelException) {
                                break
                            }

                        val text = (frame as? Frame.Text)?.readText() ?: continue
                        val message =
                            runCatching { TogetherJson.json.decodeFromString(TogetherMessage.serializer(), text) }
                                .getOrElse {
                                    onEvent?.invoke(TogetherServerEvent.Error("Failed to decode message", it))
                                    continue
                                }

                        when (message) {
                            is ServerWelcome -> {
                                if (message.sessionId == sessionId) {
                                    hostParticipantId = message.participantId
                                    mutex.withLock { settings = message.settings }
                                }
                            }

                            is JoinRequest -> {
                                if (message.sessionId == sessionId) {
                                    val participant = message.participant.copy(isHost = false, isConnected = true, isPending = true)
                                    guests[participant.id] =
                                        Guest(
                                            participantId = participant.id,
                                            clientId = "",
                                            name = participant.name,
                                            pending = true,
                                        )
                                    rebuildParticipantsSnapshot()
                                    onEvent?.invoke(TogetherServerEvent.JoinRequested(participant))
                                }
                            }

                            is ParticipantJoined -> {
                                if (message.sessionId == sessionId) {
                                    val participant = message.participant.copy(isHost = false, isConnected = true, isPending = false)
                                    guests[participant.id] =
                                        Guest(
                                            participantId = participant.id,
                                            clientId = "",
                                            name = participant.name,
                                            pending = false,
                                        )
                                    rebuildParticipantsSnapshot()
                                    onEvent?.invoke(TogetherServerEvent.ParticipantJoined(participant))
                                }
                            }

                            is ParticipantLeft -> {
                                if (message.sessionId == sessionId) {
                                    guests.remove(message.participantId)
                                    rebuildParticipantsSnapshot()
                                    onEvent?.invoke(TogetherServerEvent.ParticipantLeft(message.participantId, message.reason))
                                }
                            }

                            is ControlRequest -> {
                                if (message.sessionId == sessionId) onEvent?.invoke(TogetherServerEvent.ControlRequested(message))
                            }

                            is AddTrackRequest -> {
                                if (message.sessionId == sessionId) onEvent?.invoke(TogetherServerEvent.AddTrackRequested(message))
                            }

                            is ServerError -> {
                                onEvent?.invoke(TogetherServerEvent.Error(message.message, null))
                            }

                            else -> Unit
                        }
                    }
                } catch (t: Throwable) {
                    onEvent?.invoke(TogetherServerEvent.Error("Connection loop failed", t))
                } finally {
                    hostParticipantId = null
                    guests.clear()
                    lastParticipants = emptyList()
                    runCatching { session.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnected")) }
                    onEvent?.invoke(TogetherServerEvent.Error("Disconnected", null))
                }
            }
        loopJob?.join()
    }
}

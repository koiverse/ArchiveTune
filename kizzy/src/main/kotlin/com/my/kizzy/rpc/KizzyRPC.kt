package com.my.kizzy.rpc

import com.my.kizzy.KizzyLogger
import com.my.kizzy.DefaultKizzyLogger
import com.my.kizzy.gateway.DiscordWebSocket
import com.my.kizzy.gateway.entities.presence.Activity
import com.my.kizzy.gateway.entities.presence.Assets
import com.my.kizzy.gateway.entities.presence.Metadata
import com.my.kizzy.gateway.entities.presence.Presence
import com.my.kizzy.gateway.entities.presence.Timestamps
import com.my.kizzy.repository.KizzyRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
import java.util.logging.Logger

/**
 * Modified by Koiverse
 */
open class KizzyRPC(private val token: String, private val injectedLogger: KizzyLogger? = null) {
    private val kizzyRepository = KizzyRepository()
    private val discordWebSocket = DiscordWebSocket(token)
    private var platform: String? = null
    private val logTag = "RPC"
    // Use injected logger if provided, otherwise fall back to DefaultKizzyLogger which uses java.util.logging
    private val logger: KizzyLogger = injectedLogger ?: DefaultKizzyLogger("MainRPC")

    fun closeRPC() = discordWebSocket.close()

    fun isRpcRunning(): Boolean = discordWebSocket.isWebSocketConnected()

    suspend fun stopActivity() {
        if (!isRpcRunning()) discordWebSocket.connect()
        discordWebSocket.sendActivity(Presence(activities = emptyList()))
    }

    private suspend fun flushPreviousPresence() {
        if (!isRpcRunning()) return
        // Send an empty activities list to clear the current activity, wait briefly to allow Discord to process
        discordWebSocket.sendActivity(Presence(activities = emptyList()))
        // small delay is optional; avoid long sleeps here to keep responsiveness
    }

    fun setPlatform(platform: String? = null) = apply { this.platform = platform }

    /**
     * Pre-resolve an RpcImage using the internal repository. Returns the resolved image id or null on failure.
     * This allows callers to ensure images are uploaded/resolved before sending presence.
     */
    suspend fun preloadImage(image: com.my.kizzy.rpc.RpcImage?): String? {
        // Do not swallow exceptions here; let callers observe failures so they can log details.
        return image?.resolveImage(kizzyRepository)
    }

    private fun String.sanitize(): String =
        if (length > 128) substring(0, 128) else this

    private suspend fun makePresence(
        name: String,
        state: String?,
        stateUrl: String? = null,
        details: String?,
        detailsUrl: String? = null,
        largeImage: RpcImage?,
        smallImage: RpcImage?,
        largeText: String? = null,
        smallText: String? = null,
        buttons: List<Pair<String, String>>? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        type: Type = Type.LISTENING,
        statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
        streamUrl: String? = null,
        applicationId: String? = null,
        status: String? = "online",
        since: Long? = null,
    ): Presence {
        // Resolve image ids once so we have consistent values to log and send.
        val resolvedLarge = largeImage?.resolveImage(kizzyRepository)
        val resolvedSmall = smallImage?.resolveImage(kizzyRepository)

        try {
            logger.info("Resolved assets: large=$resolvedLarge small=$resolvedSmall")
        } catch (_: Exception) {}

        // If applicationId is provided, only send it when it's meaningful (non-null and buttons exist).
        val finalApplicationId = applicationId.takeIf { !buttons.isNullOrEmpty() }

        return Presence(
            activities = listOf(
                Activity(
                    name = name,
                    state = state,
                    stateUrl = stateUrl,
                    details = details,
                    detailsUrl = detailsUrl,
                    assets = Assets(
                        largeImage = resolvedLarge,
                        smallImage = resolvedSmall,
                        largeText = largeText,
                        smallText = smallText
                    ),
                    type = type.value,
                    platform = platform?.sanitize(),
                    statusDisplayType = statusDisplayType.value,
                    timestamps = Timestamps(startTime, endTime),
                    buttons = buttons?.map { it.first },
                    metadata = Metadata(buttonUrls = buttons?.map { it.second }),
                    applicationId = finalApplicationId,
                    url = streamUrl
                )
            ),
            afk = true,
            since = since,
            status = status ?: "online"
        )
    }

    suspend fun buildActivity(
        name: String,
        state: String?,
        stateUrl: String? = null,
        details: String?,
        detailsUrl: String? = null,
        largeImage: RpcImage?,
        smallImage: RpcImage?,
        largeText: String? = null,
        smallText: String? = null,
        buttons: List<Pair<String, String>>? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        type: Type = Type.LISTENING,
        statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
        streamUrl: String? = null,
        applicationId: String? = null,
        status: String? = "online",
        since: Long? = null,
    ) {
        if (!isRpcRunning()) {
            val shortToken = try { token.take(8) + "…" } catch (_: Exception) { "(token)" }
            logger.fine("trying to connect WebSocket with token=$shortToken")
            try {
                discordWebSocket.connect()
            } catch (ex: Exception) {
                val msg = ex.message ?: ex.toString()
                logger.severe("failed to connect WebSocket: $msg\n${ex.stackTraceToString()}")
                throw ex
            }
        }
        // Clear any previous presence briefly to avoid Discord reverting to an older presence
        try {
            flushPreviousPresence()
        } catch (_: Exception) {}

        discordWebSocket.sendActivity(
            makePresence(
                name, state, stateUrl, details, detailsUrl,
                largeImage, smallImage, largeText, smallText,
                buttons, startTime, endTime, type, statusDisplayType,
                streamUrl, applicationId, status, since
            )
        )
    }

    suspend fun updateActivity(
        name: String,
        state: String?,
        stateUrl: String? = null,
        details: String?,
        detailsUrl: String? = null,
        largeImage: RpcImage?,
        smallImage: RpcImage?,
        largeText: String? = null,
        smallText: String? = null,
        buttons: List<Pair<String, String>>? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        type: Type = Type.LISTENING,
        statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
        streamUrl: String? = null,
        applicationId: String? = null,
        status: String? = "online",
        since: Long? = null,
    ) {
        if (!discordWebSocket.isWebSocketConnected()) return
        discordWebSocket.sendActivity(
            makePresence(
                name, state, stateUrl, details, detailsUrl,
                largeImage, smallImage, largeText, smallText,
                buttons, startTime, endTime, type, statusDisplayType,
                streamUrl, applicationId, status, since
            )
        )
    }

    suspend fun refreshRPC(
    name: String,
    state: String?,
    stateUrl: String? = null,
    details: String?,
    detailsUrl: String? = null,
    largeImage: RpcImage?,
    smallImage: RpcImage?,
    largeText: String? = null,
    smallText: String? = null,
    buttons: List<Pair<String, String>>? = null,
    startTime: Long? = null,
    endTime: Long? = null,
    type: Type = Type.LISTENING,
    statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
    streamUrl: String? = null,
    applicationId: String? = null,
    status: String? = "online",
    since: Long? = null,
) {
    if (isRpcRunning()) {
        // already connected, just update
        updateActivity(
            name, state, stateUrl, details, detailsUrl,
            largeImage, smallImage, largeText, smallText,
            buttons, startTime, endTime, type, statusDisplayType,
            streamUrl, applicationId, status, since
        )
    } else {
        // not connected yet, build first
        buildActivity(
            name, state, stateUrl, details, detailsUrl,
            largeImage, smallImage, largeText, smallText,
            buttons, startTime, endTime, type, statusDisplayType,
            streamUrl, applicationId, status, since
        )
    }
}


    enum class Type(val value: Int) {
        PLAYING(0),
        STREAMING(1),
        LISTENING(2),
        WATCHING(3),
        COMPETING(5)
    }

    enum class StatusDisplayType(val value: Int) {
        NAME(0),
        STATE(1),
        DETAILS(2)
    }

    companion object {
        suspend fun getUserInfo(token: String): Result<UserInfo> = runCatching {
            val client = HttpClient()
            val response = client.get("https://discord.com/api/v9/users/@me") {
                header("Authorization", token)
            }.bodyAsText()
            val json = JSONObject(response)
            val username = json.getString("username")
            val name = json.optString("global_name", username)
            client.close()
            UserInfo(username, name)
        }
    }
}

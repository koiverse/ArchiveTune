package com.my.kizzy.rpc

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

/**
 * Modified by Koiverse
 */
open class KizzyRPC(token: String) {
    private val kizzyRepository = KizzyRepository()
    private val discordWebSocket = DiscordWebSocket(token)
    private var platform: String? = null

    fun closeRPC() = discordWebSocket.close()

    fun isRpcRunning(): Boolean = discordWebSocket.isWebSocketConnected()

    suspend fun stopActivity() {
        if (!isRpcRunning()) discordWebSocket.connect()
        discordWebSocket.sendActivity(Presence(activities = emptyList()))
    }

    fun setPlatform(platform: String? = null) = apply { this.platform = platform }

    private fun String.sanitize(): String =
        if (length > 128) substring(0, 128) else this

    private fun makePresence(
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
        val large = largeImage?.resolveImage(kizzyRepository)
        val small = smallImage?.resolveImage(kizzyRepository)
        return Presence(
            activities = listOf(
                Activity(
                    name = name,
                    state = state,
                    stateUrl = stateUrl,
                    details = details,
                    detailsUrl = detailsUrl,
                    type = type.value,
                    platform = platform?.sanitize(),
                    statusDisplayType = statusDisplayType.value,
                    timestamps = Timestamps(startTime, endTime),
                    assets = Assets(
                        largeImage = large,
                        smallImage = small,
                        largeText = largeText,
                        smallText = smallText
                    ),
                    buttons = buttons?.map { it.first },
                    metadata = Metadata(buttonUrls = buttons?.map { it.second }),
                    applicationId = applicationId.takeIf { !buttons.isNullOrEmpty() },
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
        if (!isRpcRunning()) discordWebSocket.connect()
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

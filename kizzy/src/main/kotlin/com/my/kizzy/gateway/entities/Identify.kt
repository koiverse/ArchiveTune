/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package com.my.kizzy.gateway.entities

import com.my.kizzy.gateway.entities.presence.Presence
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Identify(
    @SerialName("capabilities")
    val capabilities: Int,
    @SerialName("compress")
    val compress: Boolean,
    @SerialName("large_threshold")
    val largeThreshold: Int,
    @SerialName("properties")
    val properties: Properties,
    @SerialName("presence")
    val presence: Presence? = null,
    @SerialName("client_state")
    val clientState: ClientState? = null,
    @SerialName("token")
    val token: String,
) {
    companion object {
        fun String.toIdentifyPayload() = Identify(
            capabilities = 16381,
            compress = false,
            largeThreshold = 250,
            properties = Properties(
                browser = "disco",
                device = "disco",
                os = "Android"
            ),
            presence = Presence(
                activities = emptyList(),
                afk = false,
                since = 0,
                status = "unknown",
            ),
            clientState = ClientState(
                apiCodeVersion = 0,
                guildVersions = emptyMap(),
            ),
            token = this
        )
    }
}

@Serializable
data class Properties(
    @SerialName("browser")
    val browser: String,
    @SerialName("device")
    val device: String,
    @SerialName("os")
    val os: String,
)

@Serializable
data class ClientState(
    @SerialName("api_code_version")
    val apiCodeVersion: Int = 0,
    @SerialName("guild_versions")
    val guildVersions: Map<String, Int> = emptyMap(),
)

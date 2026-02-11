/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.together

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TogetherOnlineCreateSessionRequest(
    val hostDisplayName: String,
    val settings: TogetherRoomSettings,
)

@Serializable
data class TogetherOnlineCreateSessionResponse(
    val sessionId: String,
    val code: String,
    val hostKey: String,
    val guestKey: String,
    val wsUrl: String,
    val settings: TogetherRoomSettings,
)

@Serializable
data class TogetherOnlineResolveRequest(
    val code: String,
)

@Serializable
data class TogetherOnlineResolveResponse(
    val sessionId: String,
    val guestKey: String,
    val wsUrl: String,
    val settings: TogetherRoomSettings,
)

class TogetherOnlineApi(
    private val baseUrl: String,
) {
    private val v1BaseUrl: String =
        baseUrl
            .trimEnd('/')
            .let { if (it.endsWith("/v1")) it else "$it/v1" }

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

    private val client =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(this@TogetherOnlineApi.json)
            }
        }

    suspend fun createSession(
        hostDisplayName: String,
        settings: TogetherRoomSettings,
    ): TogetherOnlineCreateSessionResponse {
        val resp =
            client.post("$v1BaseUrl/together/sessions") {
                contentType(ContentType.Application.Json)
                setBody(TogetherOnlineCreateSessionRequest(hostDisplayName = hostDisplayName, settings = settings))
            }
        val raw = resp.bodyAsText()
        return json.decodeFromString(TogetherOnlineCreateSessionResponse.serializer(), raw)
    }

    suspend fun resolveCode(
        code: String,
    ): TogetherOnlineResolveResponse {
        val resp =
            client.post("$v1BaseUrl/together/sessions/resolve") {
                contentType(ContentType.Application.Json)
                setBody(TogetherOnlineResolveRequest(code = code.trim()))
            }
        val raw = resp.bodyAsText()
        return json.decodeFromString(TogetherOnlineResolveResponse.serializer(), raw)
    }
}

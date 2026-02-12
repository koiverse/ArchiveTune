/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.together

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
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

class TogetherOnlineApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

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
            engine {
                config {
                    connectTimeout(15, TimeUnit.SECONDS)
                    readTimeout(15, TimeUnit.SECONDS)
                    writeTimeout(15, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                }
            }
        }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 2,
        initialDelayMs: Long = 800,
        block: suspend () -> T,
    ): T {
        var lastException: Throwable? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (t: Throwable) {
                lastException = t
                if (attempt < maxAttempts && isRetryable(t)) {
                    delay(initialDelayMs * attempt)
                } else {
                    throw t
                }
            }
        }
        throw lastException!!
    }

    private fun isRetryable(t: Throwable): Boolean {
        val root = generateSequence(t) { it.cause }.lastOrNull() ?: t
        return root is java.net.SocketTimeoutException ||
            root is java.net.ConnectException ||
            root is java.io.IOException
    }

    private fun requireSuccessfulResponse(resp: HttpResponse) {
        val status = resp.status.value
        if (status in 200..299) return
        val detail =
            when (status) {
                400 -> "Bad request"
                401 -> "Unauthorized"
                403 -> "Forbidden"
                404 -> "Session not found"
                429 -> "Too many requests, please try again later"
                in 500..599 -> "Server error ($status)"
                else -> "Unexpected response ($status)"
            }
        throw TogetherOnlineApiException(detail, statusCode = status)
    }

    suspend fun createSession(
        hostDisplayName: String,
        settings: TogetherRoomSettings,
    ): TogetherOnlineCreateSessionResponse =
        withRetry {
            val payload =
                json.encodeToString(
                    TogetherOnlineCreateSessionRequest.serializer(),
                    TogetherOnlineCreateSessionRequest(hostDisplayName = hostDisplayName, settings = settings),
                )
            val resp =
                client.post("$v1BaseUrl/together/sessions") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            requireSuccessfulResponse(resp)
            val raw = resp.bodyAsText()
            json.decodeFromString(TogetherOnlineCreateSessionResponse.serializer(), raw)
        }

    suspend fun resolveCode(
        code: String,
    ): TogetherOnlineResolveResponse =
        withRetry {
            val payload =
                json.encodeToString(
                    TogetherOnlineResolveRequest.serializer(),
                    TogetherOnlineResolveRequest(code = code.trim()),
                )
            val resp =
                client.post("$v1BaseUrl/together/sessions/resolve") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            requireSuccessfulResponse(resp)
            val raw = resp.bodyAsText()
            json.decodeFromString(TogetherOnlineResolveResponse.serializer(), raw)
        }
}

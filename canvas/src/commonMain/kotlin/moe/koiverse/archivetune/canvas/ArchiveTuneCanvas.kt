/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.canvas

import kotlinx.datetime.Clock

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

object ArchiveTuneCanvas {
    private const val BASE_URL = "https://artwork-archivetune.koiiverse.cloud/"

    @Volatile
    private var bearerToken: String? = null

    fun initialize(bearerToken: String?) {
        this.bearerToken = bearerToken?.trim()?.takeIf { it.isNotEmpty() }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 12_000
                requestTimeoutMillis = 18_000
                socketTimeoutMillis = 18_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            defaultRequest {
                url(BASE_URL)
                bearerToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = mutableMapOf<String, CacheEntry>()
    private val ttlMs = 60_000L

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("sa", song, artist, storefront)
        cache[key]?.let { entry ->
            if (entry.expiresAtMs > Clock.System.now().toEpochMilliseconds()) return entry.value
            cache.remove(key)
        }

        val response =
            runCatching {
                client.get {
                    parameter("s", song)
                    parameter("a", artist)
                    parameter("storefront", storefront)
                }
            }.getOrNull()

        val value =
            when (response?.status) {
                HttpStatusCode.OK -> runCatching { response.body<CanvasArtwork>() }.getOrNull()
                else -> null
            }

        cache[key] =
            CacheEntry(
                value = value,
                expiresAtMs = Clock.System.now().toEpochMilliseconds() + ttlMs,
            )

        return value
    }

    suspend fun getByAlbumId(albumId: String): CanvasArtwork? {
        val key = cacheKey("id", albumId)
        cache[key]?.let { entry ->
            if (entry.expiresAtMs > Clock.System.now().toEpochMilliseconds()) return entry.value
            cache.remove(key)
        }

        val response =
            runCatching {
                client.get {
                    parameter("id", albumId)
                }
            }.getOrNull()

        val value =
            when (response?.status) {
                HttpStatusCode.OK -> runCatching { response.body<CanvasArtwork>() }.getOrNull()
                else -> null
            }

        cache[key] =
            CacheEntry(
                value = value,
                expiresAtMs = Clock.System.now().toEpochMilliseconds() + ttlMs,
            )

        return value
    }

    suspend fun getByAlbumUrl(url: String): CanvasArtwork? {
        val key = cacheKey("url", url)
        cache[key]?.let { entry ->
            if (entry.expiresAtMs > Clock.System.now().toEpochMilliseconds()) return entry.value
            cache.remove(key)
        }

        val response =
            runCatching {
                client.get {
                    parameter("url", url)
                }
            }.getOrNull()

        val value =
            when (response?.status) {
                HttpStatusCode.OK -> runCatching { response.body<CanvasArtwork>() }.getOrNull()
                else -> null
            }

        cache[key] =
            CacheEntry(
                value = value,
                expiresAtMs = Clock.System.now().toEpochMilliseconds() + ttlMs,
            )

        return value
    }

    suspend fun isHealthy(): Boolean {
        val response = runCatching { client.get("health") }.getOrNull() ?: return false
        return response.status == HttpStatusCode.OK
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        val normalized =
            parts
                .map { it.trim().lowercase() }
                .joinToString("|")
        return "$prefix|$normalized"
    }
}

@Serializable
data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val static: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}

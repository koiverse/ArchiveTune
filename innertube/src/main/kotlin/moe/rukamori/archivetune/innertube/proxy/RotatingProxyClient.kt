/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.innertube.proxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class RotatingProxyClient {

    private val selector = RotatingProxySelector()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .proxySelector(selector)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    internal fun selector(): RotatingProxySelector = selector

    fun activeCount(): Int = selector.activeCount()

    fun rotate() = selector.rotate()

    fun loadProxies(configs: List<ProxyConfig>) = selector.loadProxies(configs)

    fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            response.body?.string() ?: error("Empty response body for $url")
        }
    }

    suspend fun fetchAndLoad() {
        val configs = withContext(Dispatchers.IO) { fetchProxyConfigs() }
        selector.loadProxies(validateProxyConfigs(configs))
    }

    private fun fetchProxyConfigs(): List<ProxyConfig> {
        val fetchClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt")
            .build()
        return fetchClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.string()
                ?.lineSequence()
                ?.mapNotNull(::parseProxyLine)
                ?.take(MAX_PROXY_CANDIDATES)
                ?.toList()
                ?: emptyList()
        }
    }

    private suspend fun validateProxyConfigs(configs: List<ProxyConfig>): List<ProxyConfig> = coroutineScope {
        val semaphore = Semaphore(PROXY_VALIDATION_CONCURRENCY)
        configs
            .map { config ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        if (isProxyUsableForYouTubeMusic(config)) config else null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
            .take(MAX_ACTIVE_PROXIES)
    }

    private fun isProxyUsableForYouTubeMusic(config: ProxyConfig): Boolean {
        val validationClient = OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(config.host, config.port)))
            .connectTimeout(PROXY_VALIDATION_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PROXY_VALIDATION_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PROXY_VALIDATION_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val request = Request.Builder()
            .url(YOUTUBE_MUSIC_PROBE_URL)
            .build()

        return runCatching {
            validationClient.newCall(request).execute().use { response ->
                response.code == 204 || response.isSuccessful
            }
        }.getOrDefault(false)
    }

    private fun parseProxyLine(line: String): ProxyConfig? {
        val trimmed = line.trim()
        val colonIdx = trimmed.lastIndexOf(':')
        if (colonIdx < 0) return null
        val host = trimmed.substring(0, colonIdx)
        val port = trimmed.substring(colonIdx + 1).toIntOrNull() ?: return null
        if (port !in 1..65535 || host.isEmpty()) return null
        return ProxyConfig(host, port)
    }

    private companion object {
        private const val MAX_PROXY_CANDIDATES = 60
        private const val MAX_ACTIVE_PROXIES = 20
        private const val PROXY_VALIDATION_CONCURRENCY = 12
        private const val PROXY_VALIDATION_CONNECT_TIMEOUT_SECONDS = 4L
        private const val PROXY_VALIDATION_READ_TIMEOUT_SECONDS = 4L
        private const val PROXY_VALIDATION_CALL_TIMEOUT_SECONDS = 6L
        private const val YOUTUBE_MUSIC_PROBE_URL = "https://music.youtube.com/generate_204"
    }
}

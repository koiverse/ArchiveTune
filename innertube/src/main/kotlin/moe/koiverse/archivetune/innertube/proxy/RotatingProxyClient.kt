/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.innertube.proxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
        selector.loadProxies(configs)
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
                ?.take(100)
                ?.toList()
                ?: emptyList()
        }
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
}

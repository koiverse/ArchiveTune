/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import moe.rukamori.archivetune.innertube.YouTube
import java.net.InetSocketAddress
import java.net.Proxy

object ProxyUtils {
    fun createProxyOrNull(
        type: Proxy.Type,
        host: String?,
        port: Int?,
    ): Proxy? {
        val resolvedHost = host?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val resolvedPort = port?.takeIf { it in 1..65535 } ?: return null
        return Proxy(type, InetSocketAddress.createUnresolved(resolvedHost, resolvedPort))
    }

    fun applyYouTubeProxy(
        enabled: Boolean,
        type: Proxy.Type,
        host: String?,
        port: Int?,
        username: String?,
        password: String?,
    ) {
        val proxy = if (enabled) createProxyOrNull(type, host, port) else null
        YouTube.proxy = proxy
        YouTube.proxyUsername = username?.takeIf { proxy != null && it.isNotBlank() }
        YouTube.proxyPassword = password?.takeIf { proxy != null && it.isNotBlank() }
    }
}

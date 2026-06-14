/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.innertube.proxy

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicReference

internal class RotatingProxySelector : ProxySelector() {

    private class Pool(
        val proxies: Array<Proxy>,
        val deadUntil: AtomicLongArray,
    )

    private val poolRef = AtomicReference(Pool(emptyArray(), AtomicLongArray(0)))
    private val index = AtomicInteger(0)
    private val lastSelectedIndex = AtomicInteger(-1)

    companion object {
        private const val COOLDOWN_MS = 60_000L
    }

    fun loadProxies(configs: List<ProxyConfig>) {
        val newProxies = Array(configs.size) { i ->
            Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(configs[i].host, configs[i].port))
        }
        poolRef.set(Pool(newProxies, AtomicLongArray(newProxies.size)))
        index.set(0)
    }

    fun activeCount(): Int {
        val pool = poolRef.get()
        val now = System.currentTimeMillis()
        return (0 until pool.proxies.size).count { pool.deadUntil.get(it) <= now }
    }

    fun rotate() {
        if (poolRef.get().proxies.isNotEmpty()) {
            index.incrementAndGet()
        }
    }

    fun markLastSelectedFailed() {
        val pool = poolRef.get()
        val selected = lastSelectedIndex.get()
        if (selected in pool.proxies.indices) {
            pool.deadUntil.set(selected, System.currentTimeMillis() + COOLDOWN_MS)
        }
    }

    override fun select(uri: URI?): List<Proxy> {
        val pool = poolRef.get()
        if (pool.proxies.isEmpty()) return listOf(Proxy.NO_PROXY)
        val now = System.currentTimeMillis()
        val start = Math.floorMod(index.getAndIncrement(), pool.proxies.size)
        for (offset in 0 until pool.proxies.size) {
            val i = (start + offset) % pool.proxies.size
            if (pool.deadUntil.get(i) <= now) {
                lastSelectedIndex.set(i)
                return listOf(pool.proxies[i])
            }
        }
        lastSelectedIndex.set(start)
        return listOf(pool.proxies[start])
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        if (sa == null) return
        val pool = poolRef.get()
        for (i in pool.proxies.indices) {
            if (pool.proxies[i].address() == sa) {
                pool.deadUntil.set(i, System.currentTimeMillis() + COOLDOWN_MS)
                return
            }
        }
    }
}

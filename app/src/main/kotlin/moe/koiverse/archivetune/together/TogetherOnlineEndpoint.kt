/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */
 
package moe.koiverse.archivetune.together

import java.security.MessageDigest
import java.net.URI
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import moe.koiverse.archivetune.BuildConfig

object TogetherOnlineEndpoint {
    private const val DefaultBaseUrl = "http://87.106.62.92:15079"

    fun baseUrlOrNull(): String? {
        val secret = BuildConfig.TOGETHER_ONLINE_SECRET
        val ciphertextB64 = BuildConfig.TOGETHER_ONLINE_ENDPOINT_B64
        val plaintextFallback = BuildConfig.TOGETHER_ONLINE_ENDPOINT.trim().ifBlank { null }
        val fallback = plaintextFallback ?: DefaultBaseUrl
        if (secret.isBlank() || ciphertextB64.isBlank()) return fallback

        val combined =
            runCatching { Base64.getDecoder().decode(ciphertextB64) }.getOrNull()
                ?: return fallback
        if (combined.size <= 12) return fallback

        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)

        val keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))

        val plaintext =
            runCatching { cipher.doFinal(ciphertext).toString(Charsets.UTF_8).trim() }.getOrNull()
                ?: return fallback

        return plaintext.ifBlank { fallback }
    }

    fun onlineWebSocketUrlOrNull(
        rawWsUrl: String,
        baseUrl: String,
    ): String? {
        val derived = deriveWebSocketUrlFromBaseUrl(baseUrl) ?: return null
        val normalized = normalizeWebSocketUrl(rawWsUrl, baseUrl) ?: return derived

        val host =
            runCatching { URI(normalized).host }.getOrNull()?.trim()?.lowercase()
                ?: return derived
        if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0") return derived

        val baseHost =
            runCatching { URI(baseUrl.trim()).host }.getOrNull()?.trim()?.lowercase()
        if (baseHost != null && isIpv4Address(baseHost) && !isIpv4Address(host)) return derived

        return normalized
    }

    private fun isIpv4Address(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            val n = part.toIntOrNull() ?: return@all false
            n in 0..255 && part == n.toString()
        }
    }

    private fun deriveWebSocketUrlFromBaseUrl(
        baseUrl: String,
    ): String? {
        val uri = runCatching { URI(baseUrl.trim()) }.getOrNull() ?: return null
        val host = uri.host?.trim()?.ifBlank { null } ?: return null
        val scheme = uri.scheme?.trim()?.lowercase()
        val wsScheme = if (scheme == "https") "wss" else "ws"

        val portPart = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
        val normalizedPath =
            uri.path
                ?.trim()
                ?.trimEnd('/')
                .orEmpty()
                .let { if (it.endsWith("/v1")) it else "$it/v1" }

        return "$wsScheme://$host$portPart$normalizedPath/together/ws"
    }

    private fun normalizeWebSocketUrl(
        raw: String,
        baseUrl: String,
    ): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("ws://") || trimmed.startsWith("wss://")) return trimmed
        if (trimmed.startsWith("http://")) return "ws://${trimmed.removePrefix("http://")}"
        if (trimmed.startsWith("https://")) return "wss://${trimmed.removePrefix("https://")}"
        if (trimmed.startsWith("/")) {
            val baseUri = runCatching { URI(baseUrl.trim()) }.getOrNull() ?: return null
            val host = baseUri.host?.trim()?.ifBlank { null } ?: return null
            val scheme = baseUri.scheme?.trim()?.lowercase()
            val wsScheme = if (scheme == "https") "wss" else "ws"
            val portPart = if (baseUri.port != -1 && baseUri.port != 80 && baseUri.port != 443) ":${baseUri.port}" else ""
            val basePath = baseUri.path?.trim()?.trimEnd('/').orEmpty()
            return "$wsScheme://$host$portPart$basePath$trimmed"
        }

        val baseScheme = runCatching { URI(baseUrl.trim()).scheme?.trim()?.lowercase() }.getOrNull()
        val wsScheme = if (baseScheme == "https") "wss" else "ws"
        return "$wsScheme://$trimmed"
    }
}

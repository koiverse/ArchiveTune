/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */
 
package moe.koiverse.archivetune.together

import java.security.MessageDigest
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
}

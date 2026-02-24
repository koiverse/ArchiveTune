/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Cold-start poToken generator based on SmartTube's PoTokenService.
 * Produces a deterministic proof-of-origin token from an identifier
 * (visitorData or dataSyncId) and a client-state string without
 * requiring a WebView or BotGuard execution.
 */

package moe.koiverse.archivetune.innertube.utils

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
object PoTokenGenerator {

    private const val TOKEN_VERSION: Byte = 0x22
    private const val MAGIC_HEADER: Byte = 0x0A
    private const val INNER_TAG: Byte = 0x38
    private const val TIMESTAMP_TAG: Byte = 0x02

    fun generateColdStartToken(
        identifier: String,
        clientState: String = "",
    ): String {
        val timestamp = System.currentTimeMillis()
        val identifierBytes = identifier.toByteArray(Charsets.UTF_8)
        val stateBytes = clientState.toByteArray(Charsets.UTF_8)

        val keyBytes = ByteArray(16).also { Random.nextBytes(it) }

        val encryptedId = xorEncrypt(identifierBytes, keyBytes)

        val timestampBytes = encodeLong(timestamp)
        val innerPayload = buildByteArray {
            append(INNER_TAG)
            appendVarInt(stateBytes.size)
            append(stateBytes)
            append(TIMESTAMP_TAG)
            appendVarInt(timestampBytes.size)
            append(timestampBytes)
        }

        val tokenPayload = buildByteArray {
            append(MAGIC_HEADER)
            appendVarInt(keyBytes.size)
            append(keyBytes)
            append(TOKEN_VERSION)
            appendVarInt(encryptedId.size)
            append(encryptedId)
            append(innerPayload)
        }

        return Base64.UrlSafe.encode(tokenPayload).trimEnd('=')
    }

    fun generateSessionToken(
        identifier: String,
    ): String = generateColdStartToken(identifier, "session")

    fun generateContentToken(
        identifier: String,
        videoId: String,
    ): String = generateColdStartToken(identifier, videoId)


    private fun xorEncrypt(data: ByteArray, key: ByteArray): ByteArray {
        return ByteArray(data.size) { i ->
            (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
    }

    private fun encodeLong(value: Long): ByteArray {
        val buf = ByteArray(8)
        var v = value
        for (i in 0 until 8) {
            buf[i] = (v and 0xFF).toByte()
            v = v shr 8
        }
        var len = 8
        while (len > 1 && buf[len - 1] == 0.toByte()) len--
        return buf.copyOf(len)
    }

    private inline fun buildByteArray(block: ByteArrayBuilder.() -> Unit): ByteArray {
        return ByteArrayBuilder().apply(block).toByteArray()
    }

    private class ByteArrayBuilder {
        private val list = mutableListOf<Byte>()

        fun append(b: Byte) {
            list.add(b)
        }

        fun append(bytes: ByteArray) {
            for (b in bytes) list.add(b)
        }

        fun appendVarInt(value: Int) {
            var v = value
            while (v >= 0x80) {
                list.add((v or 0x80).toByte())
                v = v shr 7
            }
            list.add(v.toByte())
        }

        fun toByteArray(): ByteArray = list.toByteArray()
    }
}

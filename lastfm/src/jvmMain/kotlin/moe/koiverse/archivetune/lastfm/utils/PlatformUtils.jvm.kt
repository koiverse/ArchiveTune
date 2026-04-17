package moe.koiverse.archivetune.lastfm.utils

import java.security.MessageDigest

actual fun md5(str: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(str.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

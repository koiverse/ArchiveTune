@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package moe.koiverse.archivetune.lastfm.utils

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CC_MD5
import platform.CommonCrypto.CC_MD5_DIGEST_LENGTH

actual fun md5(str: String): String {
    val data = str.encodeToByteArray()
    val hash = UByteArray(CC_MD5_DIGEST_LENGTH)
    data.usePinned { pinned ->
        CC_MD5(pinned.addressOf(0), data.size.toUInt(), hash.refTo(0))
    }
    return hash.joinToString("") { "%02x".format(it.toByte()) }
}

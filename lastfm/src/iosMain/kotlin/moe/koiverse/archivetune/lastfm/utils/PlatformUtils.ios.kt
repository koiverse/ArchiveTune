@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package moe.koiverse.archivetune.lastfm.utils

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH

actual fun md5(str: String): String {
    val data = str.encodeToByteArray()
    val hash = UByteArray(CC_MD5_DIGEST_LENGTH.toInt())
    data.usePinned { pinned ->
        CC_MD5(pinned.addressOf(0), data.size.toUInt(), hash.refTo(0))
    }
    return hash.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package moe.koiverse.archivetune.innertube.utils

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CC_SHA1
import platform.CommonCrypto.CC_SHA1_DIGEST_LENGTH
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.timeIntervalSince1970

actual fun sha1(str: String): String {
    val data = str.encodeToByteArray()
    val hash = UByteArray(CC_SHA1_DIGEST_LENGTH)
    data.usePinned { pinned ->
        CC_SHA1(pinned.addressOf(0), data.size.toUInt(), hash.refTo(0))
    }
    return hash.joinToString("") { "%02x".format(it.toByte()) }
}

actual fun currentTimeSeconds(): Long =
    NSDate().timeIntervalSince1970.toLong()

actual fun defaultCountry(): String =
    NSLocale.currentLocale.countryCode ?: "US"

actual fun defaultLanguageTag(): String =
    NSLocale.currentLocale.languageCode

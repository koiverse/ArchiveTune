@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package moe.koiverse.archivetune.innertube.utils

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA1
import platform.CoreCrypto.CC_SHA1_DIGEST_LENGTH
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.timeIntervalSince1970

actual fun sha1(str: String): String {
    val data = str.encodeToByteArray()
    val hash = UByteArray(CC_SHA1_DIGEST_LENGTH.toInt())
    data.usePinned { pinned ->
        CC_SHA1(pinned.addressOf(0), data.size.toUInt(), hash.refTo(0))
    }
    return hash.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

actual fun currentTimeSeconds(): Long =
    NSDate().timeIntervalSince1970.toLong()

actual fun defaultCountry(): String =
    NSLocale.currentLocale.countryCode ?: "US"

actual fun defaultLanguageTag(): String =
    NSLocale.currentLocale.languageCode

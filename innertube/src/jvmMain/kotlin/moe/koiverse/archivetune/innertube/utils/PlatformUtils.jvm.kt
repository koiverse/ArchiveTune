package moe.koiverse.archivetune.innertube.utils

import java.security.MessageDigest
import java.util.Locale

actual fun sha1(str: String): String =
    MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

actual fun defaultCountry(): String = Locale.getDefault().country

actual fun defaultLanguageTag(): String = Locale.getDefault().toLanguageTag()

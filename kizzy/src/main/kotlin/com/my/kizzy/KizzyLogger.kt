/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.my.kizzy

import java.util.logging.Logger

/**
 * Small logging abstraction so the kizzy module can remain JVM-only while the Android app
 * can inject a Timber-backed implementation.
 */
interface KizzyLogger {
    fun info(message: String)
    fun fine(message: String)
    fun warning(message: String)
    fun severe(message: String)
}

/**
 * Default logger for JVM modules that falls back to java.util.logging.
 */
class DefaultKizzyLogger(private val tag: String = "Kizzy") : KizzyLogger {
    private val jlogger: Logger = Logger.getLogger(tag)

    override fun info(message: String) {
        jlogger.info(message)
    }

    override fun fine(message: String) {
        jlogger.fine(message)
    }

    override fun warning(message: String) {
        jlogger.warning(message)
    }

    override fun severe(message: String) {
        jlogger.severe(message)
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.utils

import com.my.kizzy.KizzyLogger
import timber.log.Timber

/**
 * Timber-backed implementation of KizzyLogger for the Android app.
 */
class AppKizzyLogger(private val tag: String = "Kizzy") : KizzyLogger {
    override fun info(message: String) {
        Timber.tag(tag).i(message)
    }

    override fun fine(message: String) {
        Timber.tag(tag).v(message)
    }

    override fun warning(message: String) {
        Timber.tag(tag).w(message)
    }

    override fun severe(message: String) {
        Timber.tag(tag).e(message)
    }
}

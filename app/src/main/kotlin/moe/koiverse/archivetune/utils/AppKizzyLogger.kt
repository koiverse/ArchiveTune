/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
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

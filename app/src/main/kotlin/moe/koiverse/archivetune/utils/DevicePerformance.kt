/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.utils

import android.app.ActivityManager
import android.content.Context

fun Context.isLowRamDevice(): Boolean {
    val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return activityManager?.isLowRamDevice == true
}
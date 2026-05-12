/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {

    private const val DEFAULT_ALIAS_SUFFIX = "Default"
    private const val PACKAGE = "moe.koiverse.archivetune"

    fun switchIcon(context: Context, targetSuffix: String) {
        val pm = context.packageManager
        val allSuffixes = buildAllSuffixes(context)
        for (suffix in allSuffixes) {
            val componentName = ComponentName(PACKAGE, "$PACKAGE.MainActivityAlias_$suffix")
            val newState = if (suffix == targetSuffix) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP,
                )
            } catch (_: Exception) {
                // Component may not exist in older installs or variant builds
            }
        }
    }

    fun resolveActiveAlias(context: Context): String {
        val pm = context.packageManager
        val allSuffixes = buildAllSuffixes(context)
        for (suffix in allSuffixes) {
            val componentName = ComponentName(PACKAGE, "$PACKAGE.MainActivityAlias_$suffix")
            val state = try {
                pm.getComponentEnabledSetting(componentName)
            } catch (_: Exception) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            }
            val isActive = when (suffix) {
                DEFAULT_ALIAS_SUFFIX ->
                    state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                        state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                else -> state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            if (isActive) return suffix
        }
        return DEFAULT_ALIAS_SUFFIX
    }

    fun buildAllSuffixes(context: Context): List<String> {
        val assetSuffixes = runCatching {
            context.assets.list("AppIcon")
                ?.filter { it.endsWith(".png") }
                ?.map { it.removeSuffix(".png").substringBefore("_") }
                ?: emptyList()
        }.getOrDefault(emptyList())
        return listOf(DEFAULT_ALIAS_SUFFIX) + assetSuffixes.sortedBy { it }
    }
}

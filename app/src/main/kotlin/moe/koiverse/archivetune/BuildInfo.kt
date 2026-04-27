/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune

internal val currentBuildHash: String?
    get() = BuildConfig.NIGHTLY_BUILD_HASH.takeIf { it.isNotBlank() }

internal fun formatVersionName(
    versionName: String = BuildConfig.VERSION_NAME,
    buildHash: String? = currentBuildHash,
): String = listOfNotNull(versionName.takeIf { it.isNotBlank() }, buildHash).joinToString(" ")

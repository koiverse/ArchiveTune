/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.utils

data class AuthScopedCacheValue(
    val url: String,
    val expiresAtMs: Long,
    val authFingerprint: String,
) {
    fun isValidFor(
        authFingerprint: String,
        nowMs: Long = System.currentTimeMillis(),
        minimumRemainingMs: Long = 0L,
    ): Boolean {
        return this.authFingerprint == authFingerprint && expiresAtMs > nowMs + minimumRemainingMs
    }
}

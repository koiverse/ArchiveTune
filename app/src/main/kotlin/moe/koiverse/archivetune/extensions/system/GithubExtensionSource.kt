package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

/**
 * Represents a GitHub-tracked extension source.
 * Stored persistently so the app can check for updates.
 */
@Serializable
data class GithubExtensionSource(
    /** Extension ID (matches manifest.id) */
    val extensionId: String,
    /** GitHub owner (user or org) */
    val owner: String,
    /** GitHub repository name */
    val repo: String,
    /** The asset filename pattern to look for in releases (e.g. "extension.zip").
     *  If null, the first .zip asset is used. */
    val assetPattern: String? = null,
    /** The tag of the currently installed release, e.g. "v1.0.0" */
    val installedTag: String? = null,
    /** Latest known tag from last check */
    val latestTag: String? = null,
    /** Whether an update is available */
    val updateAvailable: Boolean = false,
    /** Epoch millis of last update check */
    val lastCheckedAt: Long = 0L,
) {
    /** Canonical GitHub repo URL */
    val repoUrl: String get() = "https://github.com/$owner/$repo"

    /** GitHub Releases API URL */
    val releasesApiUrl: String get() = "https://api.github.com/repos/$owner/$repo/releases/latest"
}

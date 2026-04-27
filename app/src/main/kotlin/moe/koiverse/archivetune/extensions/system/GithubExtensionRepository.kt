package moe.koiverse.archivetune.extensions.system

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.zip.ZipInputStream

/**
 * Handles all GitHub API interactions for extension install/update.
 */
class GithubExtensionRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    /**
     * Parses a GitHub URL or "owner/repo" string into (owner, repo).
     * Accepts:
     *  - https://github.com/owner/repo
     *  - https://github.com/owner/repo/releases/...
     *  - owner/repo
     */
    fun parseOwnerRepo(input: String): Pair<String, String>? {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("https://github.com/") || trimmed.startsWith("http://github.com/") -> {
                val path = trimmed
                    .removePrefix("https://github.com/")
                    .removePrefix("http://github.com/")
                    .trimStart('/')
                val parts = path.split("/")
                if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    parts[0] to parts[1]
                } else null
            }
            trimmed.contains("/") && !trimmed.contains(" ") -> {
                val parts = trimmed.split("/")
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    parts[0] to parts[1]
                } else null
            }
            else -> null
        }
    }

    /**
     * Fetches the latest release from GitHub API.
     */
    suspend fun fetchLatestRelease(owner: String, repo: String): Result<GithubRelease> {
        return runCatching {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val response = client.get(url) {
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("GitHub API error: ${response.status.value}")
            }
            response.body<GithubRelease>()
        }
    }

    /**
     * Picks the best asset from a release.
     * Priority: exact pattern match > first .zip asset.
     */
    fun pickAsset(release: GithubRelease, pattern: String?): GithubAsset? {
        if (pattern != null) {
            val exact = release.assets.firstOrNull { it.name.equals(pattern, ignoreCase = true) }
            if (exact != null) return exact
            val glob = release.assets.firstOrNull { it.name.contains(pattern, ignoreCase = true) }
            if (glob != null) return glob
        }
        return release.assets.firstOrNull { it.name.endsWith(".zip", ignoreCase = true) }
    }

    /**
     * Downloads a ZIP asset and installs it via ExtensionManager.
     * Returns the installed extension ID on success.
     */
    suspend fun downloadAndInstall(
        asset: GithubAsset,
        manager: ExtensionManager,
    ): Result<String> {
        return runCatching {
            val response = client.get(asset.downloadUrl) {
                header("Accept", "application/octet-stream")
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Download failed: ${response.status.value}")
            }
            val bytes = response.bodyAsBytes()
            manager.installFromBytes(bytes).getOrThrow()
        }
    }

    /**
     * Full flow: fetch latest release, pick asset, download and install.
     * Returns (extensionId, release) on success.
     */
    suspend fun installFromGithub(
        owner: String,
        repo: String,
        assetPattern: String?,
        manager: ExtensionManager,
    ): Result<Pair<String, GithubRelease>> {
        return runCatching {
            val release = fetchLatestRelease(owner, repo).getOrThrow()
            val asset = pickAsset(release, assetPattern)
                ?: throw IllegalStateException("No .zip asset found in release ${release.tagName}")
            val id = downloadAndInstall(asset, manager).getOrThrow()
            id to release
        }
    }

    /**
     * Checks if a newer release is available for a tracked source.
     * Returns updated source with updateAvailable flag set.
     */
    suspend fun checkForUpdate(source: GithubExtensionSource): GithubExtensionSource {
        val release = fetchLatestRelease(source.owner, source.repo).getOrNull()
            ?: return source.copy(lastCheckedAt = System.currentTimeMillis())
        val hasUpdate = source.installedTag != null &&
            release.tagName != source.installedTag &&
            !release.draft
        return source.copy(
            latestTag = release.tagName,
            updateAvailable = hasUpdate,
            lastCheckedAt = System.currentTimeMillis(),
        )
    }

    fun close() {
        client.close()
    }
}

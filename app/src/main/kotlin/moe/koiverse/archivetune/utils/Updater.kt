/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.utils

import moe.koiverse.archivetune.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONArray
import org.json.JSONObject

data class GitCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val url: String
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String,
    val htmlUrl: String
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/koiverse/ArchiveTune/releases/latest")
                    .bodyAsText()
            val json = JSONObject(response)
            val versionName = json.getString("name")
            lastCheckTime = System.currentTimeMillis()
            versionName
        }

    suspend fun getLatestReleaseNotes(): Result<String?> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/koiverse/ArchiveTune/releases/latest")
                    .bodyAsText()
            val json = JSONObject(response)
            lastCheckTime = System.currentTimeMillis()
            if (json.has("body")) json.optString("body") else null
        }

    suspend fun getLatestReleaseInfo(): Result<ReleaseInfo> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/koiverse/ArchiveTune/releases/latest")
                    .bodyAsText()
            val json = JSONObject(response)
            lastCheckTime = System.currentTimeMillis()
            ReleaseInfo(
                tagName = json.optString("tag_name", ""),
                name = json.optString("name", ""),
                body = if (json.has("body")) json.optString("body") else null,
                publishedAt = json.optString("published_at", ""),
                htmlUrl = json.optString("html_url", "")
            )
        }

    suspend fun getCommitHistory(count: Int = 20, branch: String = "dev"): Result<List<GitCommit>> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/koiverse/ArchiveTune/commits?sha=$branch&per_page=$count")
                    .bodyAsText()
            val jsonArray = JSONArray(response)
            val commits = mutableListOf<GitCommit>()
            for (i in 0 until jsonArray.length()) {
                val commitObj = jsonArray.getJSONObject(i)
                val commit = commitObj.getJSONObject("commit")
                val authorObj = commit.optJSONObject("author")
                commits.add(
                    GitCommit(
                        sha = commitObj.optString("sha", "").take(7),
                        message = commit.optString("message", "").lines().firstOrNull() ?: "",
                        author = authorObj?.optString("name", "Unknown") ?: "Unknown",
                        date = authorObj?.optString("date", "") ?: "",
                        url = commitObj.optString("html_url", "")
                    )
                )
            }
            commits
        }

    fun getLatestDownloadUrl(): String {
        val baseUrl = "https://github.com/koiverse/ArchiveTune/releases/latest/download/"
        val architecture = BuildConfig.ARCHITECTURE
        return if (architecture == "universal") {
            baseUrl + "ArchiveTune.apk"
        } else {
            baseUrl + "app-${architecture}-release.apk"
        }
    }
}

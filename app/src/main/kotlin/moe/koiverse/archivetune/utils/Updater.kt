package moe.koiverse.archivetune.utils

import moe.koiverse.archivetune.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

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

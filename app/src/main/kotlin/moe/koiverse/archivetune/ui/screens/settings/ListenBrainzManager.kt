package moe.koiverse.archivetune.ui.screens.settings

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.koiverse.archivetune.db.entities.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

object ListenBrainzManager {
    private val logTag = "ListenBrainzManager"
    private val started = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var lifecycleObserver: Any? = null
    private val httpClient = OkHttpClient()

    private val _lastSubmitTime = MutableStateFlow<Long?>(null)
    val lastSubmitTimeFlow = _lastSubmitTime.asStateFlow()

    suspend fun submitPlayingNow(context: Context, token: String, song: Song?, positionMs: Long): Boolean {
        if (token.isBlank()) return false
        if (song == null) return false
        return withContext(Dispatchers.IO) {
            try {
                val listenedAt = System.currentTimeMillis() / 1000L
                val duration = song.song.duration
                val artistNames = run {
                    song.artists.map { artist ->
                        try {
                            val method = artist.javaClass.getMethod("getName")
                            (method.invoke(artist) as? String) ?: artist.toString()
                        } catch (e: Exception) {
                            artist.toString()
                        }
                    }.joinToString(" & ")
                }
                val releaseName = song.album?.title ?: ""
                val trackMetadata = "{\"track_metadata\":{\"artist_name\":\"${escapeJson(artistNames)}\",\"track_name\":\"${escapeJson(song.title)}\",\"release_name\":\"${escapeJson(releaseName)}\",\"additional_info\":{\"duration_ms\":${duration * 1000},\"position_ms\":$positionMs,\"submission_client\":\"ArchiveTune\"}}}"
                val listensJson = "[$trackMetadata]"
                val bodyJson = "{\"listen_type\":\"playing_now\",\"payload\":$listensJson}"
                val mediaType = "application/json".toMediaType()
                val body = bodyJson.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://api.listenbrainz.org/1/submit-listens")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Token $token")
                    .build()

                httpClient.newCall(request).execute().use { resp ->
                    val success = resp.isSuccessful
                    if (success) {
                        _lastSubmitTime.value = System.currentTimeMillis()
                        Timber.tag(logTag).d("playing_now submitted for %s", song.title)
                    } else {
                        Timber.tag(logTag).w("playing_now submit failed: %s", resp.code)
                    }
                    success
                }
            } catch (ex: Exception) {
                Timber.tag(logTag).e(ex, "submitPlayingNow failed")
                false
            }
        }
    }

    suspend fun submitFinished(context: Context, token: String, song: Song?, startMs: Long, endMs: Long): Boolean {
        if (token.isBlank()) return false
        if (song == null) return false
        return withContext(Dispatchers.IO) {
            try {
                val listenedAt = endMs / 1000L
                val duration = song.song.duration
                val artistNames = run {
                    song.artists.map { artist ->
                        try {
                            val method = artist.javaClass.getMethod("getName")
                            (method.invoke(artist) as? String) ?: artist.toString()
                        } catch (e: Exception) {
                            artist.toString()
                        }
                    }.joinToString(" & ")
                }
                val releaseName = song.album?.title ?: ""
                val listenedAtStart = (startMs / 1000L).coerceAtLeast(0L)
                val trackMetadataSingle = "{\"listened_at\":$listenedAtStart,\"track_metadata\":{\"artist_name\":\"${escapeJson(artistNames)}\",\"track_name\":\"${escapeJson(song.title)}\",\"release_name\":\"${escapeJson(releaseName)}\",\"additional_info\":{\"duration_ms\":${duration * 1000},\"start_ms\":$startMs,\"end_ms\":$endMs,\"submission_client\":\"ArchiveTune\"}}}"
                val listensJson = "[$trackMetadataSingle]"
                val bodyJson = "{\"listen_type\":\"single\",\"payload\":$listensJson}"
                val mediaType = "application/json".toMediaType()
                val body = bodyJson.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://api.listenbrainz.org/1/submit-listens")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Token $token")
                    .build()

                httpClient.newCall(request).execute().use { resp ->
                    val success = resp.isSuccessful
                    if (success) {
                        _lastSubmitTime.value = System.currentTimeMillis()
                        Timber.tag(logTag).d("finished listen submitted for %s", song.title)
                    } else {
                        Timber.tag(logTag).w("finished listen submit failed: %s", resp.code)
                    }
                    success
                }
            } catch (ex: Exception) {
                Timber.tag(logTag).e(ex, "submitFinished failed")
                false
            }
        }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    fun isRunning(): Boolean = started.get()
}

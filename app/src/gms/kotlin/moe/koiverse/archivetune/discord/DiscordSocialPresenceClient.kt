/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.discord

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.BuildConfig
import timber.log.Timber

object DiscordSocialPresenceClient {
    private const val TAG = "DiscordSocialPresenceClient"

    private val mutex = Mutex()
    private var activeApplicationId: Long? = null
    private var activeAccessToken: String? = null

    val isStarted: Boolean
        get() = activeApplicationId != null && activeAccessToken != null

    suspend fun updatePresence(
        context: Context,
        accessToken: String,
        activity: DiscordPresenceActivity,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accessToken.trim()
            if (token.isBlank()) {
                return@withLock Result.failure(IllegalArgumentException("Discord access token is missing"))
            }

            val appId = activity.applicationId
            val startResult = ensureStarted(appId, token)
            if (startResult.isFailure) {
                return@withLock startResult
            }

            DiscordSocialNativeBridge.updatePresence(
                applicationId = appId,
                accessToken = token,
                activity = activity,
            ).onFailure {
                Timber.tag(TAG).w(it, "Discord Social SDK updatePresence failed")
            }
        }
    }

    suspend fun clearPresence(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            DiscordSocialNativeBridge.clearPresence()
        }
    }

    suspend fun close(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            activeApplicationId = null
            activeAccessToken = null
            DiscordSocialNativeBridge.close()
        }
    }

    suspend fun runCallbacks(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            DiscordSocialNativeBridge.runCallbacks()
        }
    }

    private fun ensureStarted(applicationId: Long, accessToken: String): Result<Unit> {
        if (activeApplicationId == applicationId && activeAccessToken == accessToken) {
            return Result.success(Unit)
        }

        return DiscordSocialNativeBridge.start(
            applicationId = applicationId.takeIf { it > 0L } ?: BuildConfig.DISCORD_APPLICATION_ID_LONG,
            accessToken = accessToken,
        ).onSuccess {
            activeApplicationId = applicationId
            activeAccessToken = accessToken
        }.onFailure {
            activeApplicationId = null
            activeAccessToken = null
            Timber.tag(TAG).w(it, "Discord Social SDK start failed")
        }
    }
}

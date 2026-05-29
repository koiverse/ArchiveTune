/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.discord

import timber.log.Timber

object DiscordSocialNativeBridge {
    private const val TAG = "DiscordSocialNativeBridge"
    private const val LIBRARY_NAME = "archivetune_discord_social"

    private val loadFailure: Throwable?
    val isAvailable: Boolean

    init {
        var failure: Throwable? = null
        val available = runCatching {
            System.loadLibrary(LIBRARY_NAME)
            nativeIsSdkEnabled()
        }.onFailure {
            failure = it
            Timber.tag(TAG).w(it, "Discord Social SDK native bridge is unavailable")
        }.getOrDefault(false)

        loadFailure = failure
        isAvailable = available
    }

    fun unavailableMessage(): String =
        loadFailure?.message
            ?: "Discord Social SDK is not enabled. Add app/libs/discord_partner_sdk.aar from the Discord developer portal and rebuild."

    fun start(applicationId: Long, accessToken: String): Result<Unit> =
        callNative {
            nativeStart(
                applicationId = applicationId,
                accessToken = accessToken,
            )
        }

    fun updatePresence(
        applicationId: Long,
        accessToken: String,
        activity: DiscordPresenceActivity,
    ): Result<Unit> {
        val buttons = activity.buttons.take(2)
        return callNative {
            nativeUpdatePresence(
                applicationId = applicationId,
                accessToken = accessToken,
                type = activity.type.nativeValue,
                name = activity.name,
                details = activity.details,
                state = activity.state,
                detailsUrl = activity.detailsUrl,
                stateUrl = activity.stateUrl,
                largeImage = activity.assets.largeImage,
                largeText = activity.assets.largeText,
                largeUrl = activity.assets.largeUrl,
                smallImage = activity.assets.smallImage,
                smallText = activity.assets.smallText,
                smallUrl = activity.assets.smallUrl,
                buttonLabels = buttons.map { it.label }.toTypedArray(),
                buttonUrls = buttons.map { it.url }.toTypedArray(),
                startEpochSeconds = activity.timestamps.startEpochSeconds ?: 0L,
                endEpochSeconds = activity.timestamps.endEpochSeconds ?: 0L,
                statusDisplayType = activity.statusDisplayType.nativeValue,
                supportedPlatforms = activity.supportedPlatforms,
                onlineStatus = activity.onlineStatus.nativeValue,
            )
        }
    }

    fun clearPresence(): Result<Unit> = callNative(::nativeClearPresence)

    fun close(): Result<Unit> = callNative(::nativeClose)

    fun runCallbacks(): Result<Unit> = callNative(::nativeRunCallbacks)

    private inline fun callNative(block: () -> String?): Result<Unit> {
        if (!isAvailable) {
            return Result.failure(IllegalStateException(unavailableMessage()))
        }

        return runCatching {
            val error = block()
            if (error != null) {
                throw IllegalStateException(error)
            }
        }
    }

    external fun nativeIsSdkEnabled(): Boolean

    external fun nativeStart(
        applicationId: Long,
        accessToken: String,
    ): String?

    external fun nativeUpdatePresence(
        applicationId: Long,
        accessToken: String,
        type: Int,
        name: String?,
        details: String?,
        state: String?,
        detailsUrl: String?,
        stateUrl: String?,
        largeImage: String?,
        largeText: String?,
        largeUrl: String?,
        smallImage: String?,
        smallText: String?,
        smallUrl: String?,
        buttonLabels: Array<String>,
        buttonUrls: Array<String>,
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        statusDisplayType: Int,
        supportedPlatforms: Int,
        onlineStatus: Int,
    ): String?

    external fun nativeClearPresence(): String?

    external fun nativeClose(): String?

    external fun nativeRunCallbacks(): String?
}

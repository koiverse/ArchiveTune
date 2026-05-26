/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.discord

import android.app.Activity
import timber.log.Timber

object DiscordSocialSdkAndroidInitializer {
    private const val TAG = "DiscordSocialSdkInit"

    fun setEngineActivity(activity: Activity) {
        runCatching {
            val initClass = Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
            val method = initClass.getMethod("setEngineActivity", Activity::class.java)
            method.invoke(null, activity)
        }.onFailure {
            Timber.tag(TAG).v(it, "Discord Social SDK Android init class is not available")
        }
    }
}

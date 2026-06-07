/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.utils

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

fun clearPlaybackWebAuthSession(context: Context) {
    clearWebAuthStorage(context)
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeSessionCookies(null)
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
}

suspend fun clearWebAuthSession(context: Context) {
    withContext(Dispatchers.Main.immediate) {
        clearWebAuthStorage(context)
        val cookieManager = CookieManager.getInstance()
        suspendCancellableCoroutine<Unit> { continuation ->
            cookieManager.removeSessionCookies {
                cookieManager.removeAllCookies {
                    cookieManager.flush()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }
}

fun resetAuthWebViewSession(
    context: Context,
    webView: WebView,
    clearCookies: Boolean = true,
    onReady: () -> Unit,
) {
    webView.stopLoading()
    webView.clearHistory()
    webView.clearFormData()
    webView.clearCache(true)
    clearWebAuthStorage(context)

    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)
    if (!clearCookies) {
        onReady()
        return
    }

    cookieManager.removeSessionCookies {
        cookieManager.removeAllCookies {
            cookieManager.flush()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            onReady()
        }
    }
}

private fun clearWebAuthStorage(context: Context) {
    val appContext = context.applicationContext
    WebStorage.getInstance().deleteAllData()
    WebViewDatabase.getInstance(appContext).apply {
        clearFormData()
        clearHttpAuthUsernamePassword()
        clearUsernamePassword()
    }
}

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


package moe.koiverse.archivetune.utils

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase

fun clearPlaybackWebAuthSession(context: Context) {
    clearWebAuthStorage(context)
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeSessionCookies(null)
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
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

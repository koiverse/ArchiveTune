/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AccountChannelHandleKey
import moe.koiverse.archivetune.constants.AccountEmailKey
import moe.koiverse.archivetune.constants.AccountNameKey
import moe.koiverse.archivetune.constants.DataSyncIdKey
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.constants.VisitorDataKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.PreferenceStore
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.handleYouTubeWebAuthNavigation
import moe.koiverse.archivetune.utils.putLegacyPoToken
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.utils.reportException
import moe.koiverse.archivetune.utils.resetAuthWebViewSession
import moe.koiverse.archivetune.innertube.YouTube
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

const val LOGIN_ROUTE = "login"
const val LOGIN_URL_ARGUMENT = "url"
const val LOGIN_PRESERVE_SESSION_ARGUMENT = "preserveSession"

private const val DEFAULT_CHANNEL_SWITCH_URL = "https://www.youtube.com/account"

fun buildLoginRoute(
    startUrl: String? = null,
    preserveSession: Boolean = false,
): String {
    val queryParameters = buildList {
        startUrl?.trim()
            ?.takeUnless { it.isBlank() }
            ?.let { add("$LOGIN_URL_ARGUMENT=${Uri.encode(it)}") }
        if (preserveSession) {
            add("$LOGIN_PRESERVE_SESSION_ARGUMENT=true")
        }
    }

    if (queryParameters.isEmpty()) return LOGIN_ROUTE
    return "$LOGIN_ROUTE?${queryParameters.joinToString("&")}"
}

fun buildChannelSwitchRoute(): String = buildLoginRoute(
    startUrl = DEFAULT_CHANNEL_SWITCH_URL,
    preserveSession = true,
)

private const val DEFAULT_LOGIN_URL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

private val YOUTUBE_COOKIE_URLS = listOf(
    "https://music.youtube.com",
    "https://www.youtube.com",
    "https://youtube.com",
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
    startUrl: String? = null,
    preserveSession: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    val initialUrl = startUrl?.takeIf { it.isNotBlank() }
        ?: if (preserveSession) DEFAULT_CHANNEL_SWITCH_URL else DEFAULT_LOGIN_URL
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                val cookieManager = CookieManager.getInstance()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        return view.handleYouTubeWebAuthNavigation(request.url.toString())
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        return view.handleYouTubeWebAuthNavigation(url)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        val isYouTubePage = url?.contains("youtube.com", ignoreCase = true) == true
                        if (isYouTubePage) {
                            loadUrl("javascript:void((function(){try{var c=window.ytcfg;if(c&&c.get){var v=c.get('VISITOR_DATA');if(v){Android.onRetrieveVisitorData(v);return}}var y=window.yt&&window.yt.config_;if(y&&y.VISITOR_DATA){Android.onRetrieveVisitorData(y.VISITOR_DATA);return}var s=document.querySelectorAll('script');for(var i=0;i<s.length;i++){var m=s[i].textContent.match(/\"VISITOR_DATA\":\"([^\"]+)\"/);if(m){Android.onRetrieveVisitorData(m[1]);return}}}catch(e){}})())")
                            loadUrl("javascript:void((function(){try{var c=window.ytcfg;if(c&&c.get){var d=c.get('DATASYNC_ID');if(d){Android.onRetrieveDataSyncId(d);return}}var y=window.yt&&window.yt.config_;if(y&&y.DATASYNC_ID){Android.onRetrieveDataSyncId(y.DATASYNC_ID);return}var s=document.querySelectorAll('script');for(var i=0;i<s.length;i++){var m=s[i].textContent.match(/\"DATASYNC_ID\":\"([^\"]+)\"/);if(m){Android.onRetrieveDataSyncId(m[1]);return}}}catch(e){}})())")
                            loadUrl("javascript:void((function(){try{var c=window.ytcfg;if(c&&c.get){var t=c.get('PO_TOKEN');if(t){Android.onRetrievePoToken(t);return}}var s=document.querySelectorAll('script');for(var i=0;i<s.length;i++){var m=s[i].textContent.match(/\"PO_TOKEN\":\"([^\"]+)\"/);if(m){Android.onRetrievePoToken(m[1]);return}}}catch(e){}})())")
                        }

                        val mergedCookie = mergeYouTubeCookies(cookieManager, url)
                        if (!mergedCookie.isNullOrBlank()) {
                            innerTubeCookie = mergedCookie
                            coroutineScope.launch {
                                YouTube.accountInfo().onSuccess {
                                    accountName = it.name
                                    accountEmail = it.email.orEmpty()
                                    accountChannelHandle = it.channelHandle.orEmpty()
                                }.onFailure {
                                    reportException(it)
                                }
                            }
                        }
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveVisitorData(newVisitorData: String?) {
                        if (!newVisitorData.isNullOrBlank()) {
                            visitorData = newVisitorData
                        }
                    }
                    @JavascriptInterface
                    fun onRetrieveDataSyncId(newDataSyncId: String?) {
                        if (!newDataSyncId.isNullOrBlank()) {
                            dataSyncId = newDataSyncId
                        }
                    }
                    @JavascriptInterface
                    fun onRetrievePoToken(newPoToken: String?) {
                        if (!newPoToken.isNullOrBlank()) {
                            PreferenceStore.launchEdit(context.dataStore) {
                                putLegacyPoToken(newPoToken)
                            }
                        }
                    }
                }, "Android")
                webView = this
                if (preserveSession) {
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    loadUrl(initialUrl)
                } else {
                    resetAuthWebViewSession(context, this) {
                        loadUrl(initialUrl)
                    }
                }
            }
        }
    )

    TopAppBar(
        title = {
            Text(
                stringResource(
                    if (preserveSession) R.string.switch_youtube_channel else R.string.login,
                ),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}

private fun mergeYouTubeCookies(
    cookieManager: CookieManager,
    currentUrl: String? = null,
): String? {
    val cookieParts = linkedMapOf<String, String>()
    val candidateUrls = linkedSetOf<String>()

    currentUrl.toYouTubeCookieOrigin()?.let(candidateUrls::add)
    candidateUrls.addAll(YOUTUBE_COOKIE_URLS)

    cookieManager.flush()

    candidateUrls.forEach { url ->
        cookieManager.getCookie(url)
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.forEach { part ->
                val separatorIndex = part.indexOf('=')
                if (separatorIndex <= 0) return@forEach

                val key = part.substring(0, separatorIndex).trim()
                val value = part.substring(separatorIndex + 1).trim()
                if (key.isNotEmpty()) {
                    cookieParts[key] = value
                }
            }
    }

    return cookieParts.takeIf { it.isNotEmpty() }
        ?.entries
        ?.joinToString(separator = "; ") { (key, value) -> "$key=$value" }
}

private fun String?.toYouTubeCookieOrigin(): String? {
    val parsed = this?.let(Uri::parse) ?: return null
    val host = parsed.host?.lowercase() ?: return null
    if (host != "youtube.com" && !host.endsWith(".youtube.com")) return null

    val scheme = parsed.scheme
        ?.takeIf { it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true) }
        ?: "https"

    return "$scheme://$host"
}

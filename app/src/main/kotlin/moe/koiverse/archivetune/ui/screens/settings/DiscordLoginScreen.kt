/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.DiscordTokenKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var discordToken by rememberPreference(DiscordTokenKey, "")
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isCompleting by remember { mutableStateOf(false) }

    fun completeLogin(token: String) {
        if (isCompleting) return
        val trimmed = token.trim()
        if (trimmed.isEmpty() || trimmed == "null" || trimmed == "error") return
        isCompleting = true
        discordToken = trimmed
        webView?.stopLoading()
        webView?.loadUrl("about:blank")
        navController.navigateUp()
    }

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                WebView.setWebContentsDebuggingEnabled(true)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true

                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }

                WebStorage.getInstance().deleteAllData()

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveToken(token: String) {
                        Log.d("DiscordWebView", "Token: $token")
                        scope.launch(Dispatchers.Main) { completeLogin(token) }
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (isCompleting) return
                        if (!url.contains("discord.com")) return
                        if (url.contains("/login")) return

                        val js = """
                            (function() {
                                function cleanToken(t) {
                                    if (!t) return null;
                                    try {
                                        var s = String(t);
                                        if (s.length >= 2 && s[0] === '"' && s[s.length - 1] === '"') {
                                            s = s.slice(1, -1);
                                        }
                                        return s;
                                    } catch (e) { return null; }
                                }

                                function send(t) {
                                    var s = cleanToken(t);
                                    if (s && s !== "null" && s !== "error") {
                                        Android.onRetrieveToken(s);
                                        return true;
                                    }
                                    return false;
                                }

                                function tryLocalStorage() {
                                    try {
                                        return send(window.localStorage.getItem("token") || window.localStorage.token);
                                    } catch (e) { return false; }
                                }

                                function tryIframe() {
                                    try {
                                        var i = document.createElement('iframe');
                                        i.style.display = 'none';
                                        document.body.appendChild(i);
                                        var alt = i.contentWindow.localStorage.token || i.contentWindow.localStorage.getItem("token");
                                        return send(alt);
                                    } catch (e) { return false; }
                                }

                                function tryWebpack() {
                                    try {
                                        var w = window.webpackChunkdiscord_app;
                                        if (!w || !w.push) return false;
                                        var token = null;
                                        w.push([[Math.random()], {}, function(req) {
                                            try {
                                                for (var k in req.c) {
                                                    var m = req.c[k];
                                                    var exp = m && m.exports && m.exports.default;
                                                    if (exp && typeof exp.getToken === "function") {
                                                        token = exp.getToken();
                                                        break;
                                                    }
                                                }
                                            } catch (e) {}
                                        }]);
                                        return send(token);
                                    } catch (e) { return false; }
                                }

                                function run() {
                                    if (tryLocalStorage()) return;
                                    if (tryWebpack()) return;
                                    tryIframe();
                                }

                                run();
                                setTimeout(run, 1200);
                                setTimeout(run, 3000);
                            })();
                        """.trimIndent()

                        view.evaluateJavascript(js, null)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        if (isCompleting) return true
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(
                        view: WebView,
                        url: String,
                        message: String,
                        result: JsResult
                    ): Boolean {
                        scope.launch(Dispatchers.Main) { completeLogin(message) }
                        result.confirm()
                        return true
                    }
                }

                webView = this
                loadUrl("https://discord.com/login")
            }
        }
    )

    TopAppBar(
        title = { Text(stringResource(R.string.action_login)) },
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

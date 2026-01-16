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
    var webView: WebView? = null

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

                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
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
                        if (token != "null" && token != "error") {
                            discordToken = token
                            scope.launch(Dispatchers.Main) {
                                webView?.loadUrl("about:blank")
                                navController.navigateUp()
                            }
                        }
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (url.contains("/channels/@me") || url.contains("/app")) {
                            view.evaluateJavascript(
                                """
                                (function() {
                                    function getToken() {
                                        try {
                                            var token = localStorage.getItem("token");
                                            if (token) return token.replace(/^"|"$/g, '');
                                        } catch(e) {}
                                        
                                        try {
                                            var webpack = window.webpackChunkdiscord_app;
                                            if (webpack) {
                                                var found = null;
                                                webpack.push([
                                                    [Math.random()],
                                                    {},
                                                    (e) => {
                                                        for (const key in e.c) {
                                                            var mod = e.c[key].exports;
                                                            if (mod && mod.default && mod.default.getToken) {
                                                                found = mod.default.getToken();
                                                                break;
                                                            }
                                                            if (mod && mod.getToken) {
                                                                found = mod.getToken();
                                                                break;
                                                            }
                                                        }
                                                    }
                                                ]);
                                                if (found) return found;
                                            }
                                        } catch(e) {}
                                        return null;
                                    }

                                    var attempts = 0;
                                    var interval = setInterval(function() {
                                        var token = getToken();
                                        if (token) {
                                            clearInterval(interval);
                                            Android.onRetrieveToken(token);
                                        } else {
                                            attempts++;
                                            if (attempts > 10) clearInterval(interval);
                                        }
                                    }, 1000);
                                })();
                                """.trimIndent(), null
                            )
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = false
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(
                        view: WebView,
                        url: String,
                        message: String,
                        result: JsResult
                    ): Boolean {
                        if (message != "null" && message != "error") {
                            discordToken = message
                            scope.launch(Dispatchers.Main) {
                                view.loadUrl("about:blank")
                                navController.navigateUp()
                            }
                        }
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

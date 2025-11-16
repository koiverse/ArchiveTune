package moe.koiverse.archivetune.ui.screens.settings

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.LastFMSessionKey
import moe.koiverse.archivetune.constants.LastFMUsernameKey
import moe.koiverse.archivetune.lastfm.LastFM
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.utils.reportException

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFMLoginScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var lastfmSession by rememberPreference(LastFMSessionKey, "")
    var lastfmUsername by rememberPreference(LastFMUsernameKey, "")
    var webView: WebView? = null
    var authUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Get token from Last.fm API
        LastFM.getToken()
            .onSuccess { tokenResponse ->
                authUrl = LastFM.getAuthUrl(tokenResponse.token)
                isLoading = false
                Log.d("LastFMLogin", "Auth URL: $authUrl")
            }
            .onFailure { error ->
                reportException(error)
                Log.e("LastFMLogin", "Failed to get token: ${error.message}")
                isLoading = false
                navController.navigateUp()
            }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (authUrl != null) {
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

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            Log.d("LastFMLogin", "Page finished: $url")
                            
                            // Check if user clicked "Yes, allow access" - Last.fm redirects back to itself
                            if (url?.contains("last.fm") == true && url.contains("token=")) {
                                val token = url.substringAfter("token=").substringBefore("&")
                                Log.d("LastFMLogin", "Token found in URL: $token")
                                
                                scope.launch(Dispatchers.IO) {
                                    LastFM.getSession(token)
                                        .onSuccess { auth ->
                                            lastfmUsername = auth.session.name
                                            lastfmSession = auth.session.key
                                            Log.d("LastFMLogin", "Session obtained: ${auth.session.name}")
                                            scope.launch(Dispatchers.Main) {
                                                navController.navigateUp()
                                            }
                                        }
                                        .onFailure { error ->
                                            reportException(error)
                                            Log.e("LastFMLogin", "Failed to get session: ${error.message}")
                                        }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            // Allow all Last.fm URLs to load in WebView
                            if (request.url.host?.contains("last.fm") == true) {
                                return false
                            }
                            return false
                        }
                    }

                    webView = this
                    authUrl?.let { loadUrl(it) }
                }
            }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lastfm_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}

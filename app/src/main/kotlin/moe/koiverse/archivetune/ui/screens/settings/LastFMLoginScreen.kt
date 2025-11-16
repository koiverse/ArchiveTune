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
    var webView: WebView? by remember { mutableStateOf<WebView?>(null) }
    var authUrl by remember { mutableStateOf<String?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Get token from Last.fm API
        scope.launch(Dispatchers.IO) {
            LastFM.getToken()
                .onSuccess { tokenResponse ->
                    authToken = tokenResponse.token
                    authUrl = LastFM.getAuthUrl(tokenResponse.token)
                    isLoading = false
                    Log.d("LastFMLogin", "Token: ${tokenResponse.token}")
                    Log.d("LastFMLogin", "Auth URL: $authUrl")
                }
                .onFailure { err ->
                    reportException(err)
                    Log.e("LastFMLogin", "Failed to get token: ${err.message}")
                    error = err.message ?: "Failed to get token"
                    isLoading = false
                }
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (error != null) {
        AlertDialog(
            onDismissRequest = { navController.navigateUp() },
            title = { Text("Error") },
            text = { Text(error!!) },
            confirmButton = {
                TextButton(onClick = { navController.navigateUp() }) {
                    Text("OK")
                }
            }
        )
    } else if (authUrl != null && authToken != null) {
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
                            
                            if (url?.contains("last.fm/api/auth") == true && url.contains("?token=")) {
                                val tokenFromUrl = url.substringAfter("?token=").substringBefore("&")
                                Log.d("LastFMLogin", "Token found in callback URL: $tokenFromUrl")
                                
                                scope.launch(Dispatchers.IO) {
                                    LastFM.getSession(tokenFromUrl)
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
                            } else if (url?.contains("last.fm") == true && !url.contains("api/auth")) {
                                authToken?.let { token ->
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
                                                Log.d("LastFMLogin", "Token not yet authorized: ${error.message}")
                                            }
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

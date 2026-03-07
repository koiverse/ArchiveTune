package moe.koiverse.archivetune.ui.screens.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.innertube.utils.PoTokenGenerator
import moe.koiverse.archivetune.ui.component.IconButton

class PoTokenExtractionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOURCE_URL = "source_url"
        const val EXTRA_GVS_TOKEN = "gvs_token"
        const val EXTRA_PLAYER_TOKEN = "player_token"
        const val EXTRA_VISITOR_DATA = "visitor_data"
        const val EXTRA_ERROR = "error"

        private const val DEFAULT_EXTRACT_URL = "https://youtube.com/account"
    }

    private var activeWebView: WebView? = null
    private var extractedVisitorData: String? = null
    private var extractedGvsToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUrl =
            intent.getStringExtra(EXTRA_SOURCE_URL)
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_EXTRACT_URL

        setContent {
            PoTokenExtractionContent(targetUrl)
        }
    }

    override fun onDestroy() {
        activeWebView?.stopLoading()
        activeWebView?.loadUrl("about:blank")
        activeWebView?.destroy()
        activeWebView = null
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PoTokenExtractionContent(targetUrl: String) {
        val context = LocalContext.current
        var webView by remember { mutableStateOf<WebView?>(null) }
        var currentUrl by remember { mutableStateOf(targetUrl) }
        var isExtracting by remember { mutableStateOf(false) }

        fun normalizeHost(url: String): String {
            return Uri.parse(url).host.orEmpty().removePrefix("www.")
        }

        fun normalizePath(url: String): String {
            val path = Uri.parse(url).path.orEmpty().trimEnd('/')
            return if (path.isBlank()) "/" else path
        }

        fun isAtDestination(current: String, destination: String): Boolean {
            if (current.isBlank() || destination.isBlank()) return false
            val currentHost = normalizeHost(current)
            val destinationHost = normalizeHost(destination)
            if (currentHost != destinationHost) return false

            val currentPath = normalizePath(current)
            val destinationPath = normalizePath(destination)
            return currentPath == destinationPath || currentPath.startsWith("$destinationPath/")
        }

        val canExtract = !isExtracting && isAtDestination(currentUrl, targetUrl)

        fun parseJsResult(raw: String?): String {
            val text = raw?.trim().orEmpty()
            if (text.isBlank() || text == "null") return ""
            val unwrapped =
                if (text.length >= 2 && text.first() == '"' && text.last() == '"') {
                    text.substring(1, text.length - 1)
                } else {
                    text
                }
            return unwrapped
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\u0026", "&")
                .trim()
        }

        fun closeCanceled(error: String? = null) {
            isExtracting = false
            val data = Intent().apply {
                if (!error.isNullOrBlank()) {
                    putExtra(EXTRA_ERROR, error)
                }
            }
            setResult(Activity.RESULT_CANCELED, data)
            finish()
        }

        fun completeIfReady() {
            val visitorData = extractedVisitorData ?: return
            val gvsToken = extractedGvsToken ?: return
            isExtracting = false

            val playerToken = PoTokenGenerator.generateColdStartToken(visitorData, "player")

            setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    putExtra(EXTRA_VISITOR_DATA, visitorData)
                    putExtra(EXTRA_GVS_TOKEN, gvsToken)
                    putExtra(EXTRA_PLAYER_TOKEN, playerToken)
                }
            )
            finish()
        }

        fun triggerExtraction() {
            if (isExtracting) return
            if (!isAtDestination(currentUrl, targetUrl)) {
                Toast.makeText(context, R.string.open_account_before_extract, Toast.LENGTH_SHORT).show()
                return
            }

            isExtracting = true

            extractedVisitorData = null
            extractedGvsToken = null

            webView?.evaluateJavascript(
                "(function(){try{return window.yt?.config_?.VISITOR_DATA || window.ytcfg?.get?.('VISITOR_DATA') || '';}catch(e){return '';}})();"
            ) { result ->
                val visitor = parseJsResult(result)
                if (visitor.isNotBlank()) {
                    extractedVisitorData = visitor
                    completeIfReady()
                }
            }

            webView?.evaluateJavascript(
                "(function(){try{var c=window.ytcfg;if(c&&c.get){var t=c.get('PO_TOKEN');if(t)return t;}var s=document.querySelectorAll('script');for(var i=0;i<s.length;i++){var m=s[i].textContent.match(/\"PO_TOKEN\":\"([^\"]+)\"/);if(m)return m[1];}return '';}catch(e){return '';}})();"
            ) { result ->
                val gvs = parseJsResult(result)
                if (gvs.isNotBlank()) {
                    extractedGvsToken = gvs
                    completeIfReady()
                }
            }

            webView?.postDelayed({
                if (isFinishing) return@postDelayed
                val visitor = extractedVisitorData
                if (!visitor.isNullOrBlank() && extractedGvsToken.isNullOrBlank()) {
                    extractedGvsToken = PoTokenGenerator.generateSessionToken(visitor)
                    completeIfReady()
                    return@postDelayed
                }
                if (extractedVisitorData.isNullOrBlank() || extractedGvsToken.isNullOrBlank()) {
                    isExtracting = false
                    Toast.makeText(context, R.string.token_generation_failed, Toast.LENGTH_SHORT).show()
                }
            }, 4000L)
        }

        BackHandler {
            val wv = webView
            if (wv != null && wv.canGoBack()) {
                wv.goBack()
            } else {
                closeCanceled()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                factory = { ctx ->
                    WebView(ctx).apply {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.removeAllCookies(null)
                        cookieManager.flush()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        clearHistory()
                        clearFormData()
                        clearCache(true)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onRetrieveVisitorData(newVisitorData: String?) {
                                if (!newVisitorData.isNullOrBlank()) {
                                    extractedVisitorData = newVisitorData
                                    runOnUiThread { completeIfReady() }
                                }
                            }

                            @JavascriptInterface
                            fun onRetrievePoToken(newPoToken: String?) {
                                if (!newPoToken.isNullOrBlank()) {
                                    extractedGvsToken = newPoToken
                                    runOnUiThread { completeIfReady() }
                                }
                            }
                        }, "Android")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                currentUrl = url.orEmpty()
                            }
                        }

                        loadUrl(targetUrl)
                        webView = this
                        activeWebView = this
                    }
                },
                update = { view ->
                    webView = view
                    activeWebView = view
                }
            )

            TopAppBar(
                title = { Text(stringResource(R.string.extracting_from_url)) },
                navigationIcon = {
                    IconButton(
                        onClick = { closeCanceled() },
                        onLongClick = { closeCanceled() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                }
            )

            ExtendedFloatingActionButton(
                text = {
                    Text(
                        if (isExtracting) {
                            stringResource(R.string.generating_tokens)
                        } else {
                            stringResource(R.string.regenerate_token)
                        }
                    )
                },
                icon = {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.done),
                            contentDescription = null,
                        )
                    }
                },
                onClick = {
                    if (canExtract) {
                        triggerExtraction()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(16.dp),
                expanded = true,
                containerColor = if (canExtract) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (canExtract) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

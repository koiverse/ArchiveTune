/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.ui.component.*
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Proxy
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InternetSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (dnsOverHttpsEnabled, onDnsOverHttpsEnabledChange) = rememberPreference(key = EnableDnsOverHttpsKey, defaultValue = false)
    val (dnsProvider, onDnsProviderChange) = rememberPreference(key = DnsOverHttpsProviderKey, defaultValue = "Cloudflare")
    val (customDnsUrl, onCustomDnsUrlChange) = rememberPreference(key = stringPreferencesKey("customDnsUrl"), defaultValue = "https://")
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyHost, onProxyHostChange) = rememberPreference(key = ProxyHostKey, defaultValue = "127.0.0.1")
    val (proxyPort, onProxyPortChange) = rememberPreference(key = ProxyPortKey, defaultValue = 8080)
    val (proxyUsername, onProxyUsernameChange) = rememberPreference(key = ProxyUsernameKey, defaultValue = "")
    val (proxyPassword, onProxyPasswordChange) = rememberPreference(key = ProxyPasswordKey, defaultValue = "")
    val (streamBypassProxy, onStreamBypassProxyChange) = rememberPreference(key = StreamBypassProxyKey, defaultValue = false)

    var testingProxy by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    val dnsProviders = listOf("Cloudflare", "Google", "AdGuard", "Quad9", "Custom")

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(title = stringResource(R.string.dns_over_https))
        SwitchPreference(
            title = { Text(stringResource(R.string.dns_over_https)) },
            description = stringResource(R.string.dns_over_https_desc),
            icon = { Icon(painterResource(R.drawable.security), null) },
            checked = dnsOverHttpsEnabled,
            onCheckedChange = onDnsOverHttpsEnabledChange,
        )

        if (dnsOverHttpsEnabled) {
            ListPreference(
                title = { Text(stringResource(R.string.dns_provider)) },
                icon = { Icon(painterResource(R.drawable.website), null) },
                selectedValue = dnsProvider,
                values = dnsProviders,
                valueText = { it },
                onValueSelected = onDnsProviderChange,
            )

            if (dnsProvider == "Custom") {
                EditTextPreference(
                    title = { Text(stringResource(R.string.dns_custom_url)) },
                    value = customDnsUrl,
                    onValueChange = onCustomDnsUrlChange,
                )
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.proxy))
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_proxy)) },
            icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
            checked = proxyEnabled,
            onCheckedChange = {
                onProxyEnabledChange(it)
                if (it) {
                    YouTube.proxy = Proxy(proxyType, java.net.InetSocketAddress.createUnresolved(proxyHost, proxyPort))
                    YouTube.proxyUsername = proxyUsername
                    YouTube.proxyPassword = proxyPassword
                } else {
                    YouTube.proxy = null
                    YouTube.proxyUsername = null
                    YouTube.proxyPassword = null
                }
            },
        )
        if (proxyEnabled) {
            Column {
                ListPreference(
                    title = { Text(stringResource(R.string.proxy_type)) },
                    selectedValue = proxyType,
                    values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                    valueText = { it.name },
                    onValueSelected = {
                        onProxyTypeChange(it)
                        YouTube.proxy = Proxy(it, java.net.InetSocketAddress.createUnresolved(proxyHost, proxyPort))
                    },
                )
                EditTextPreference(
                    title = { Text(stringResource(R.string.proxy_host)) },
                    value = proxyHost,
                    onValueChange = {
                        onProxyHostChange(it)
                        YouTube.proxy = Proxy(proxyType, java.net.InetSocketAddress.createUnresolved(it, proxyPort))
                    },
                )
                NumberPickerPreference(
                    title = { Text(stringResource(R.string.proxy_port)) },
                    value = proxyPort,
                    onValueChange = {
                        onProxyPortChange(it)
                        YouTube.proxy = Proxy(proxyType, java.net.InetSocketAddress.createUnresolved(proxyHost, it))
                    },
                    minValue = 1,
                    maxValue = 65535,
                )

                PreferenceGroupTitle(title = stringResource(R.string.proxy_auth))
                EditTextPreference(
                    title = { Text(stringResource(R.string.proxy_username)) },
                    value = proxyUsername,
                    onValueChange = {
                        onProxyUsernameChange(it)
                        YouTube.proxyUsername = it
                    },
                )
                EditTextPreference(
                    title = { Text(stringResource(R.string.proxy_password)) },
                    value = proxyPassword,
                    onValueChange = {
                        onProxyPasswordChange(it)
                        YouTube.proxyPassword = it
                    },
                )

                SwitchPreference(
                    title = { Text(stringResource(R.string.stream_bypass_proxy)) },
                    description = stringResource(R.string.stream_bypass_proxy_desc),
                    icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                    checked = streamBypassProxy,
                    onCheckedChange = {
                        onStreamBypassProxyChange(it)
                        YouTube.streamBypassProxy = it
                    },
                )

                PreferenceEntry(
                    title = { Text(stringResource(R.string.test_proxy_connection)) },
                    icon = { Icon(painterResource(R.drawable.check), null) },
                    onClick = {
                        if (testingProxy) return@PreferenceEntry
                        scope.launch(Dispatchers.IO) {
                            testingProxy = true
                            try {
                                val proxyAddr = java.net.InetSocketAddress.createUnresolved(proxyHost, proxyPort)
                                val proxy = Proxy(proxyType, proxyAddr)
                                val clientBuilder = OkHttpClient.Builder()
                                    .proxy(proxy)
                                    .connectTimeout(10, TimeUnit.SECONDS)
                                    .readTimeout(10, TimeUnit.SECONDS)
                                
                                if (proxyUsername.isNotBlank() && proxyPassword.isNotBlank()) {
                                    clientBuilder.proxyAuthenticator { _, response ->
                                        val credential = okhttp3.Credentials.basic(proxyUsername, proxyPassword)
                                        response.request.newBuilder()
                                            .header("Proxy-Authorization", credential)
                                            .build()
                                    }
                                }

                                val client = clientBuilder.build()
                                val request = Request.Builder()
                                    .url("https://music.youtube.com/generate_204")
                                    .build()
                                client.newCall(request).execute().use { response ->
                                    testResult = if (response.isSuccessful || response.code == 204) {
                                        context.getString(R.string.proxy_connection_success)
                                    } else {
                                        context.getString(R.string.proxy_connection_failed, "HTTP ${response.code}")
                                    }
                                }
                            } catch (e: Exception) {
                                testResult = context.getString(R.string.proxy_connection_failed, e.message ?: "Unknown error")
                            } finally {
                                testingProxy = false
                            }
                        }
                    }
                )
            }
        }
    }

    if (testingProxy) {
        DefaultDialog(
            onDismiss = { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.testing_proxy_connection))
        }
    }

    if (testResult != null) {
        ActionPromptDialog(
            title = stringResource(R.string.test_proxy_connection),
            onDismiss = { testResult = null },
            onConfirm = { testResult = null },
            content = {
                Text(testResult!!)
            }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.internet)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.*
import moe.koiverse.archivetune.innertube.YouTube
import moe.koiverse.archivetune.ui.component.*
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import okhttp3.Dns
import java.net.Proxy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternetSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dnsOverHttpsEnabled, onDnsOverHttpsEnabledChange) = rememberPreference(key = EnableDnsOverHttpsKey, defaultValue = false)
    val (dnsProvider, onDnsProviderChange) = rememberPreference(key = DnsOverHttpsProviderKey, defaultValue = "Cloudflare")
    val (customDnsUrl, onCustomDnsUrlChange) = rememberPreference(key = stringPreferencesKey("customDnsUrl"), defaultValue = "https://")
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")
    val (streamBypassProxy, onStreamBypassProxyChange) = rememberPreference(key = StreamBypassProxyKey, defaultValue = false)

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
            onCheckedChange = onProxyEnabledChange,
        )
        if (proxyEnabled) {
            Column {
                ListPreference(
                    title = { Text(stringResource(R.string.proxy_type)) },
                    selectedValue = proxyType,
                    values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                    valueText = { it.name },
                    onValueSelected = onProxyTypeChange,
                )
                EditTextPreference(
                    title = { Text(stringResource(R.string.proxy_url)) },
                    value = proxyUrl,
                    onValueChange = onProxyUrlChange,
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
            }
        }
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

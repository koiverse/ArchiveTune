package moe.koiverse.archivetune.innertube.utils

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun platformEngine(): HttpClientEngineFactory<HttpClientEngineConfig> =
    Darwin as HttpClientEngineFactory<HttpClientEngineConfig>

actual fun HttpClientConfig<*>.configurePlatformEngine(proxyConfig: Any?) {
    // No proxy support on iOS Darwin engine by default
}

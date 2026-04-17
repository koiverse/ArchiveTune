package moe.koiverse.archivetune.innertube.utils

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

expect fun platformEngine(): HttpClientEngineFactory<HttpClientEngineConfig>

expect fun HttpClientConfig<*>.configurePlatformEngine(proxyConfig: Any?)

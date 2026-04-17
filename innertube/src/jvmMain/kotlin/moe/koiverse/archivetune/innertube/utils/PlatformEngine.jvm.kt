package moe.koiverse.archivetune.innertube.utils

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import java.net.Proxy

actual fun platformEngine(): HttpClientEngineFactory<HttpClientEngineConfig> =
    OkHttp as HttpClientEngineFactory<HttpClientEngineConfig>

actual fun HttpClientConfig<*>.configurePlatformEngine(proxyConfig: Any?) {
    engine {
        if (proxyConfig is Proxy) {
            (this as? io.ktor.client.engine.okhttp.OkHttpConfig)?.config {
                proxy(proxyConfig)
            }
        }
    }
}

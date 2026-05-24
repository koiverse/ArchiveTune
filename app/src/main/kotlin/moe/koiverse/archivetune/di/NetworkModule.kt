/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */




package moe.koiverse.archivetune.di

import android.content.Context
import com.google.net.cronet.okhttptransport.CronetInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import moe.koiverse.archivetune.utils.NetworkConnectivityObserver
import okhttp3.OkHttpClient
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(
        @ApplicationContext context: Context,
    ): NetworkConnectivityObserver = NetworkConnectivityObserver(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
        buildCronetEngine(context)?.let { engine ->
            builder.addInterceptor(CronetInterceptor.newBuilder(engine).build())
        }
        return builder.build()
    }

    private fun buildCronetEngine(context: Context): CronetEngine? = runCatching {
        CronetProvider.getAllProviders(context)
            .firstOrNull { it.isEnabled && it.name != CronetProvider.PROVIDER_NAME_FALLBACK }
            ?.createBuilder()
            ?.enableHttp2(true)
            ?.enableQuic(true)
            ?.enableBrotli(true)
            ?.build()
    }.getOrNull()
}

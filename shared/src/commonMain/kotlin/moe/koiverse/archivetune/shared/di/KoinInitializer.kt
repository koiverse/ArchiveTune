package moe.koiverse.archivetune.shared.di

import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(commonModule, platformModule)
    }
}

fun initKoinIos() = initKoin()

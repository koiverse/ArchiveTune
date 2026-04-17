package moe.koiverse.archivetune.shared.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val commonModule = module {
}

expect val platformModule: Module

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.ksp)
}

kotlin {
    android {
        namespace = "moe.koiverse.archivetune.shared"
        compileSdk = 36
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
            implementation(libs.compose.multiplatform.resources)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kermit)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            implementation(libs.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(libs.room.runtime)
            implementation(libs.room.ktx)
            implementation(libs.datastore)

            implementation(libs.viewmodel.kmp)
            implementation(libs.viewmodel.compose.kmp)
            implementation(libs.lifecycle.runtime.compose.kmp)
            implementation(libs.navigation.kmp)

            implementation(libs.coil)

            implementation(project(":innertube"))
            implementation(project(":kugou"))
            implementation(project(":lrclib"))
            implementation(project(":lastfm"))
            implementation(project(":betterlyrics"))
            implementation(project(":simpmusic"))
            implementation(project(":canvas"))
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.network.okhttp)

            implementation(project(":kizzy"))
            implementation(project(":shazamkit"))
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    listOf(
        "kspAndroid",
        "kspIosArm64",
        "kspIosSimulatorArm64",
    ).forEach { target ->
        add(target, libs.room.compiler)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

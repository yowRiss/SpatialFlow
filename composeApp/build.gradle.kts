import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "com.codetrio.spatialflow.kmp"
        compileSdk = 37
        minSdk = 25
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.resources)

            // JetBrains' multiplatform Lifecycle and Navigation artifacts.
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.navigation.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jaudiotagger)
                implementation(libs.vlcj)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("SpatialFlowDatabase") {
            packageName.set("com.codetrio.spatialflow.shared.database")
        }
    }
}

compose.resources {
    packageOfResClass = "com.codetrio.spatialflow.shared.resources"
}

compose.desktop {
    application {
        mainClass = "com.codetrio.spatialflow.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Rpm, TargetFormat.Msi)
            packageName = "spatialflow"
            packageVersion = "1.8.0"
            description = "SpatialFlow music player"
            vendor = "SpatialFlow"
            linux {
                menuGroup = "AudioVideo"
            }
        }
    }
}

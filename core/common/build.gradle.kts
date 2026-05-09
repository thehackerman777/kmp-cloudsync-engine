plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    jvm("desktop")
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.datetime)
                implementation(libs.napier)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotest.runner)
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.property)
                implementation(libs.turbine)
            }
        }

        val androidMain by getting
        val desktopMain by getting
        val jsMain by getting
    }
}

android {
    namespace = "io.cloudsync.core"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

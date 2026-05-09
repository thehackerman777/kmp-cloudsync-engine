plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("CloudSyncDatabase") {
            packageName.set("io.cloudsync.data.local.db")
            srcDirs("src/commonMain/sqldelight")
        }
    }
}

kotlin {
    androidTarget {
        compilations.all { kotlinOptions { jvmTarget = "17" } }
    }
    jvm("desktop")
    js(IR) { browser(); nodejs() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:common"))
                implementation(project(":domain"))
                implementation(project(":network"))
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.json)
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.datetime)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.napier)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotest.assertions)
                implementation(libs.turbine)
            }
        }
        val androidMain by getting {
            dependencies { implementation(libs.sqldelight.android) }
        }
        val desktopMain by getting {
            dependencies { implementation(libs.sqldelight.sqlite) }
        }
        val jsMain by getting
    }
}

android {
    namespace = "io.cloudsync.data"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

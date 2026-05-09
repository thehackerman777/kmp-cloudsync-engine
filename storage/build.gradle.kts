plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all { kotlinOptions { jvmTarget = "17" } }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "CloudSyncStorage"; isStatic = true }
    }
    jvm("desktop")
    js(IR) { browser(); nodejs() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:common"))
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.sqldelight.runtime)
                implementation(libs.napier)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotest.runner)
                implementation(libs.kotest.assertions)
            }
        }
        val androidMain by getting {
            dependencies { implementation("app.cash.sqldelight:jdbc-driver:2.0.2")
            implementation("org.xerial:sqlite-jdbc:3.45.3.0")
            implementation(libs.sqldelight.runtime) }
        }
        val desktopMain by getting {
            dependencies { implementation("app.cash.sqldelight:jdbc-driver:2.0.2")
            implementation("org.xerial:sqlite-jdbc:3.45.3.0")
            implementation(libs.sqldelight.runtime) }
        }
    }
}

android {
    namespace = "io.cloudsync.storage"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

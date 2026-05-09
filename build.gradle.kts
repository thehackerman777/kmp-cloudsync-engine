plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktorfit) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.atomicfu) apply false
    kotlin("android") version "2.1.0" apply false
}

subprojects {
    group = "io.cloudsync"
    version = rootProject.version
}

val dokka by tasks.registering(org.jetbrains.dokka.gradle.DokkaTask::class) {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    moduleName.set("KMP CloudSync Engine")
    moduleVersion.set(project.version.toString())
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
}

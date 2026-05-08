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
    alias(libs.plugins.binary.compatibility) apply false
    alias(libs.plugins.kotlin.atomicfu) apply false
}

allprojects {
    // API validation snapshot configuration
    apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")

    apiValidation {
        nonPublicMarkers.add("io.cloudsync.core.InternalCloudSyncApi")
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        config = files("${rootDir}/config/detekt/detekt.yml")
        buildUponDefaultConfig = true
        allRules = false
        parallel = true
    }
}

val dokka by tasks.registering(org.jetbrains.dokka.gradle.DokkaTask::class) {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    moduleName.set("KMP CloudSync Engine")
    moduleVersion.set(project.version.toString())
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

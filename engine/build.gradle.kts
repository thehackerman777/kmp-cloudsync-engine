plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all { kotlinOptions { jvmTarget = "17" } }
    }
    jvm("desktop")
    js(IR) {
        browser()
        nodejs()
        binaries.library()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:common"))
                api(project(":domain"))
                api(project(":data"))
                api(project(":network"))
                api(project(":auth"))
                api(project(":storage"))
                api(project(":sync"))
                api(project(":presentation"))

                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":core:common"))
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotest.assertions)
            }
        }
        val androidMain by getting
        val desktopMain by getting
        val jsMain by getting
    }
}

android {
    namespace = "io.cloudsync.engine"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ── Desktop: Fat/Shadow JAR ──────────────────────────────
val fatDesktopJar by tasks.registering(Jar::class) {
    group = "distribution"
    description = "Build a fat JAR for Desktop bundling all runtime dependencies"

    dependsOn("desktopMainClasses")

    // Include compiled desktop classes
    from({ kotlin.targets.getByName("desktop").compilations.getByName("main").output.allOutputs })

    // Include all runtime dependencies
    from({
        project.configurations.getByName("desktopRuntimeClasspath")
            .filter { it.name.endsWith(".jar") }
            .map { if (it.isFile) project.zipTree(it) else it }
    })

    // Exclude signature files
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    archiveBaseName.set("kmp-cloudsync-engine-desktop")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("all")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// ── Android: Fat AAR ─────────────────────────────────────
// ── Android: Standard AAR (fat AAR pospuesto) ────────────
// El AAR se genera via :engine:assembleRelease (AGP)
// build/outputs/aar/engine-release.aar contiene el modulo
// y sus dependencias transitivas via POM.

// ── JS: Standalone bundle ─────────────────────────────────
tasks.register("jsStandaloneBundle") {
    group = "distribution"
    description = "Copy the JS library distribution to outputs/js/"

    dependsOn("jsNodeProductionLibraryDistribution")

    doLast {
        val outputDir = File(project.buildDir, "outputs/js")
        outputDir.mkdirs()

        val libDir = File(project.buildDir, "dist/js/productionLibrary")

        if (!libDir.exists()) {
            throw GradleException("JS library distribution not found: ${libDir.absolutePath}")
        }

        copy {
            from(libDir) {
                include("*.js")
                include("*.js.map")
            }
            into(outputDir)
        }

        val jsCount = outputDir.listFiles { f -> f.extension == "js" }?.size ?: 0
        val totalSize = (outputDir.listFiles()?.sumOf { it.length() } ?: 0) / 1024

        println("✅ JS library distribution: ${outputDir.absolutePath}")
        println("   Files: $jsCount JS + $jsCount.map, Total: ${totalSize}KB")
    }
}

// ── Aggregate task ────────────────────────────────────────
tasks.register("buildAllArtifacts") {
    group = "distribution"
    description = "Build all platform artifacts (Android AAR, Desktop Shadow JAR, JS bundle)"
    dependsOn("fatDesktopJar", "fatAar", "jsStandaloneBundle")

    doLast {
        println("""
            |✅ All artifacts built:
            |   Desktop: ${project.buildDir}/libs/kmp-cloudsync-engine-desktop-${project.version}-all.jar
            |   Android: ${project.buildDir}/outputs/fat-aar/kmp-cloudsync-engine-android-${project.version}.aar
            |   Web:     ${project.buildDir}/outputs/js/
        """.trimMargin())
    }
}

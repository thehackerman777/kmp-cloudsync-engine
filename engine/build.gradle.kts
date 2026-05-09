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
        browser {
            webpackTask {
                output.libraryTarget = "umd"
                output.library = "CloudSyncEngine"
            }
        }
        nodejs()
        binaries.executable()
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

// Desktop: Fat/Shadow JAR
val fatDesktopJar by tasks.registering(Jar::class) {
    group = "distribution"
    description = "Build a fat JAR for Desktop bundling all runtime dependencies"

    dependsOn("desktopMainClasses")

    from({ kotlin.targets.getByName("desktop").compilations.getByName("main").output.allOutputs })

    from({
        project.configurations.getByName("desktopRuntimeClasspath")
            .filter { it.name.endsWith(".jar") }
            .map { if (it.isFile) project.zipTree(it) else it }
    })

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    archiveBaseName.set("kmp-cloudsync-engine-desktop")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("all")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// JS: Webpack production bundle (single UMD file)
tasks.register("jsWebBundle") {
    group = "distribution"
    description = "Build a single-file UMD JS bundle via webpack"

    dependsOn("jsBrowserProductionWebpack")

    doLast {
        val outputDir = File(project.buildDir, "outputs/js")
        outputDir.mkdirs()

        val webpackDir = File(project.buildDir, "kotlin-webpack/js/productionExecutable")
        if (!webpackDir.exists()) {
            throw GradleException("Webpack output not found: " + webpackDir.absolutePath)
        }

        val bundleFiles = webpackDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".js") && !it.name.endsWith(".js.map") }
            ?.filter { !it.name.contains("worker") }
            ?.sortedBy { it.name }

        if (bundleFiles.isNullOrEmpty()) {
            throw GradleException("No JS bundle found in " + webpackDir.absolutePath)
        }

        bundleFiles.forEach { f ->
            f.copyTo(File(outputDir, f.name), overwrite = true)
        }

        webpackDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".js.map") }
            ?.forEach { f ->
                f.copyTo(File(outputDir, f.name), overwrite = true)
            }

        val totalSize = (outputDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1024)
        println("JS webpack bundle copied to: " + outputDir.absolutePath)
        println("   Total: " + totalSize + "KB")
        outputDir.listFiles()?.sortedBy { it.name }?.forEach { f ->
            println("   " + f.name + " (" + (f.length() / 1024) + "KB)")
        }
    }
}

// Aggregate task
tasks.register("buildAllArtifacts") {
    group = "distribution"
    description = "Build all platform artifacts (Android AAR, Desktop Shadow JAR, JS bundle)"
    dependsOn("fatDesktopJar", "jsWebBundle")

    doLast {
        println("All artifacts built:")
        println("   Desktop: " + project.buildDir + "/libs/kmp-cloudsync-engine-desktop-0.2.0-all.jar")
        println("   Web:     " + project.buildDir + "/outputs/js/kmp-cloudsync-engine.js")
        println("   Android: assembleRelease - build/outputs/aar/")
    }
}

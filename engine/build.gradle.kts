plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
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

// ---- Publishing ----
publishing {
    repositories {
        mavenLocal()
    }

    publications {
        withType<MavenPublication> {
            pom {
                name.set("KMP CloudSync Engine")
                description.set("KMP CloudSync Engine - Multiplatform sync library")
                url.set("https://github.com/cloudsync/kmp-cloudsync-engine")
            }
        }
    }
}

// ---- Desktop: Fat/Shadow JAR ----
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

// ---- Fat AAR (all dependencies bundled into AAR structure) ----
val fatAAR by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Build a fat AAR bundling all runtime dependencies (classes.jar + deps)"

    dependsOn("assembleRelease")

    val aarInput = file("build/outputs/aar/engine-release.aar")
    val outputAar = file("build/outputs/aar/engine-fat-release.aar")

    from({
        if (aarInput.exists()) {
            zipTree(aarInput)
        } else {
            fileTree("/dev/null")
        }
    })

    from({
        project.configurations.getByName("releaseRuntimeClasspath")
            .filter { it.name.endsWith(".jar") }
            .map { if (it.isFile) project.zipTree(it) else it }
    })

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("META-INF/MANIFEST.MF")

    archiveFileName.set("engine-fat-release.aar")
    destinationDirectory.set(file("build/outputs/aar"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    doLast {
        println("Fat AAR built: " + outputAar.absolutePath + " (" + (outputAar.length() / 1024) + "KB)")
    }
}

// ---- JS: Webpack production bundle ----
tasks.register("jsWebBundle") {
    group = "distribution"
    description = "Build a single-file UMD JS bundle via webpack"

    dependsOn("jsBrowserProductionWebpack")

    doLast {
        val outputDir = File(project.buildDir, "outputs/js")
        outputDir.mkdirs()

        val webpackDir = File(project.buildDir, "kotlin-webpack/js/productionExecutable")
        if (!webpackDir.exists()) {
            throw GradleException("No webpack output: " + webpackDir.absolutePath)
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

// ---- Aggregate task ----
tasks.register("buildAllArtifacts") {
    group = "distribution"
    description = "Build all platform artifacts (Android AAR, Desktop Shadow JAR, JS bundle)"
    dependsOn("fatDesktopJar", "jsWebBundle", "publishToMavenLocal")

    doLast {
        println("All artifacts built:")
        println("   Desktop: " + project.buildDir + "/libs/kmp-cloudsync-engine-desktop-0.2.0-all.jar")
        println("   Web:     " + project.buildDir + "/outputs/js/")
        println("   Android: build/outputs/aar/engine-fat-release.aar")
    }
}

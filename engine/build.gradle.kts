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
                outputFileName = "kmp-cloudsync-engine.js"
            }
        }
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
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.kotest.runner)
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
    compileSdk = 35
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
tasks.register("fatAar") {
    group = "distribution"
    description = "Build a fat AAR for Android bundling all module dependencies"

    dependsOn(
        ":core:common:assembleRelease",
        ":domain:assembleRelease",
        ":data:assembleRelease",
        ":network:assembleRelease",
        ":auth:assembleRelease",
        ":storage:assembleRelease",
        ":sync:assembleRelease",
        ":presentation:assembleRelease",
        ":engine:assembleRelease"
    )

    doLast {
        val rootDir = project.rootDir
        val engineAar = File(rootDir, "engine/build/outputs/aar/engine-release.aar")

        if (!engineAar.exists()) {
            throw GradleException("Engine AAR not found: ${engineAar.absolutePath}")
        }

        val depAars = listOf(
            "core/common", "domain", "data", "network",
            "auth", "storage", "sync", "presentation"
        ).map { module ->
            val path = module.replace("/", "-")
            File(rootDir, "$module/build/outputs/aar/${path}-release.aar")
        }.filter { it.exists() }

        val outputFile = File(project.buildDir, "outputs/fat-aar/kmp-cloudsync-engine-android-${project.version}.aar")
        outputFile.parentFile.mkdirs()

        val mergeDir = File(project.buildDir, "fat-aar-merge")
        mergeDir.deleteRecursively()
        mergeDir.mkdirs()

        // Extract all AARs
        val allAars = listOf(engineAar) + depAars
        allAars.forEach { aar ->
            val moduleName = aar.parentFile.parentFile.name
            copy {
                from(project.zipTree(aar))
                into(File(mergeDir, moduleName))
            }
        }

        // Merge classes.jar files
        val mergedClasses = File(mergeDir, "merged-classes")
        mergedClasses.mkdirs()
        allAars.forEach { aar ->
            val aarDir = File(mergeDir, aar.parentFile.parentFile.name)
            val classesJar = File(aarDir, "classes.jar")
            if (classesJar.exists()) {
                copy {
                    from(project.zipTree(classesJar))
                    into(mergedClasses)
                    exclude("META-INF/MANIFEST.MF")
                    exclude("META-INF/*.SF")
                    exclude("META-INF/*.DSA")
                    exclude("META-INF/*.RSA")
                }
            }
        }

        // Re-jar as classes.jar
        val finalClassesJar = File(mergeDir, "classes.jar")
        ant.withGroovyBuilder {
            "jar"("destfile" to finalClassesJar.absolutePath, "basedir" to mergedClasses.absolutePath)
        }

        // Build final AAR from engine AAR structure + merged classes.jar
        val aarBase = File(mergeDir, "aar-base")
        copy {
            from(project.zipTree(engineAar))
            into(aarBase)
            exclude("classes.jar")
        }
        copy {
            from(finalClassesJar)
            into(aarBase)
        }

        ant.withGroovyBuilder {
            "zip"("destfile" to outputFile.absolutePath) {
                "fileset"("dir" to aarBase.absolutePath)
            }
        }

        println("✅ Fat AAR created: ${outputFile.absolutePath} (${outputFile.length() / 1024} KB)")
    }
}

// ── JS: Standalone bundle ─────────────────────────────────
tasks.register("jsStandaloneBundle") {
    group = "distribution"
    description = "Pack JS distribution files into a single standalone bundle"

    dependsOn(":engine:jsBrowserProductionWebpack")

    doLast {
        val outputDir = File(project.buildDir, "outputs/js")
        outputDir.mkdirs()

        // Kotlin/JS IR with library mode outputs to productionLibrary
        val libDir = File(project.buildDir, "dist/js/productionLibrary")
        val execDir = File(project.buildDir, "dist/js/productionExecutable")

        val sourceDir = when {
            libDir.exists() -> libDir
            execDir.exists() -> execDir
            else -> throw GradleException("JS output directory not found")
        }

        copy {
            from(sourceDir) {
                include("*.js")
                include("*.map")
            }
            into(outputDir)
        }

        val jsFiles = outputDir.listFiles { f -> f.name.endsWith(".js") } ?: emptyArray()
        if (jsFiles.isEmpty()) {
            throw GradleException("No JS files found after build")
        }

        println("✅ JS bundle ready at: ${outputDir.absolutePath}")
        jsFiles.forEach { println("   ${it.name} (${it.length() / 1024} KB)") }
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

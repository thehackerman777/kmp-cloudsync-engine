plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
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
                // Re-export all internal modules via api for transitive consumption
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

// ── Desktop: Shadow JAR ──────────────────────────────────
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowDesktopJar") {
    group = "distribution"
    description = "Build a fat/shadow JAR for Desktop bundling all dependencies"

    dependsOn(":engine:desktopMainClasses")

    from(project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class)
        .targets.getByName("desktop")
        .compilations.getByName("main")
        .output.allOutputs)

    configurations.add(project.configurations.getByName("desktopRuntimeClasspath"))

    archiveBaseName.set("kmp-cloudsync-engine-desktop")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("all")

    mergeServiceFiles()
    minimize()
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
        val buildDir = layout.buildDirectory.get().asFile
        val outputDir = File(buildDir, "fat-aar")
        val classesDir = File(outputDir, "classes")
        val resDir = File(outputDir, "res")
        val jniDir = File(outputDir, "jni")

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        // Collect all dependency AARs
        val engineAar = File(
            buildDir.parentFile.parentFile,
            "engine/build/outputs/aar/engine-release.aar"
        )

        val dependencyAars = listOf(
            "core/common", "domain", "data", "network",
            "auth", "storage", "sync", "presentation"
        ).map { module ->
            File(
                buildDir.parentFile.parentFile,
                "$module/build/outputs/aar/${module.replace("/", "-")}-release.aar"
            )
        }.filter { it.exists() }

        val allAars = listOf(engineAar) + dependencyAars

        // Extract all AARs into the merged directory
        allAars.forEach { aar ->
            if (aar.exists()) {
                copy {
                    from(zipTree(aar))
                    into(classesDir)
                    include("classes.jar")
                    rename("classes.jar", "classes-${aar.parentFile.parentFile.name}.jar")
                }
                copy {
                    from(zipTree(aar))
                    into(classesDir)
                    include("libs/*.jar")
                }
                copy {
                    from(zipTree(aar))
                    into(resDir)
                    include("res/**")
                }
                copy {
                    from(zipTree(aar))
                    into(jniDir)
                    include("jni/**")
                }
            }
        }

        // Merge all class jars into a single classes.jar
        val mergedJar = File(classesDir, "classes.jar")
        val jars = classesDir.listFiles()?.filter {
            it.name.endsWith(".jar") && it.name != "classes.jar"
        } ?: emptyList()
        if (jars.isNotEmpty()) {
            val classFiles = mutableListOf<File>()
            jars.forEach { jar ->
                copy {
                    from(zipTree(jar))
                    into(File(classesDir, "merged"))
                    exclude("META-INF/MANIFEST.MF")
                    exclude("META-INF/*.SF")
                    exclude("META-INF/*.DSA")
                    exclude("META-INF/*.RSA")
                }
                classFiles.addAll(File(classesDir, "merged").walkTopDown().filter { it.extension == "class" }.toList())
            }

            // Re-jar as classes.jar
            ant.withGroovyBuilder {
                "jar"("destfile" to mergedJar.absolutePath, "basedir" to "${classesDir.absolutePath}/merged")
            }

            // Cleanup
            jars.forEach { it.delete() }
            File(classesDir, "merged").deleteRecursively()
        }

        // Build the fat AAR
        val fatAarDir = File(buildDir, "outputs/fat-aar")
        fatAarDir.mkdirs()
        val fatAarFile = File(fatAarDir, "kmp-cloudsync-engine-android-${project.version}.aar")

        // We need the base AAR structure from engine module
        val baseAarDir = File(buildDir, "fat-aar-base")
        baseAarDir.deleteRecursively()
        baseAarDir.mkdirs()

        // Copy the engine AAR as base, replace its classes.jar
        copy {
            from(zipTree(engineAar))
            into(baseAarDir)
            exclude("classes.jar")
            exclude("META-INF/MANIFEST.MF")
        }
        if (mergedJar.exists()) {
            copy {
                from(mergedJar)
                into(baseAarDir)
            }
        }

        // Create the fat AAR
        ant.withGroovyBuilder {
            "zip"("destfile" to fatAarFile.absolutePath) {
                "fileset"("dir" to baseAarDir.absolutePath)
            }
        }

        println("📦 Fat AAR created: ${fatAarFile.absolutePath}")
    }
}

// ── JS: Standalone bundle ─────────────────────────────────
tasks.register("jsStandaloneBundle") {
    group = "distribution"
    description = "Pack JS distribution files into a single standalone bundle"

    dependsOn(":engine:jsBrowserProductionWebpack")

    doLast {
        val distDir = layout.buildDirectory.dir("dist/js").get().asFile
        val outputDir = layout.buildDirectory.dir("outputs/js").get().asFile
        outputDir.mkdirs()

        val sourceDir = layout.buildDirectory.dir("dist/js/productionLibrary").get().asFile
        if (sourceDir.exists()) {
            copy {
                from(sourceDir)
                into(outputDir)
                include("*.js")
            }
            println("📦 JS bundle ready at: ${outputDir.absolutePath}")
        } else {
            // Fallback: use webpack output
            val webpackDir = layout.buildDirectory.dir("dist/js/productionExecutable").get().asFile
            if (webpackDir.exists()) {
                copy {
                    from(webpackDir)
                    into(outputDir)
                    include("*.js")
                }
                println("📦 JS webpack bundle ready at: ${outputDir.absolutePath}")
            }
        }
    }
}

// ── Aggregate task ────────────────────────────────────────
tasks.register("buildAllArtifacts") {
    group = "distribution"
    description = "Build all platform artifacts (Android AAR, Desktop Shadow JAR, JS bundle)"
    dependsOn(":engine:shadowDesktopJar", ":engine:fatAar", ":engine:jsStandaloneBundle")

    doLast {
        println("✅ All artifacts built:")
        println("   Desktop: build/libs/kmp-cloudsync-engine-desktop-${project.version}-all.jar")
        println("   Android: build/outputs/fat-aar/kmp-cloudsync-engine-android-${project.version}.aar")
        println("   Web:     build/outputs/js/")
    }
}

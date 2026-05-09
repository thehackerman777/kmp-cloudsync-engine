plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":engine"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

application {
    mainClass.set("io.cloudsync.sample.desktop.MainKt")
}

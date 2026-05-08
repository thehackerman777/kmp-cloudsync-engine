pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "kmp-cloudsync-engine"

include(
    ":core:common",
    ":core:testing",
    ":domain",
    ":data",
    ":network",
    ":auth",
    ":sync",
    ":storage",
    ":presentation"
)

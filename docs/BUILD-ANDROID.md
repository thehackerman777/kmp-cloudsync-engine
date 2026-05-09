# Building with Android (AAR)

## Option 1: MavenLocal (recommended for development)

**Step 1: Publish engine to MavenLocal**

```bash
cd kmp-cloudsync-engine
./gradlew :engine:publishToMavenLocal --no-daemon
```

**Step 2: Add dependency to your Android project**

```kotlin
// build.gradle.kts (app module)
dependencies {
    implementation("io.cloudsync:engine:0.2.0")
}
```

```kotlin
// settings.gradle.kts or build.gradle.kts repositories
repositories {
    mavenLocal()  // Must be present
    google()
    mavenCentral()
}
```

## Option 2: Fat AAR (standalone, includes all deps)

The fat AAR bundles all runtime dependencies into a single AAR file.

```bash
# Build fat AAR
./gradlew :engine:fatAAR --no-daemon
```

Location: `engine/build/outputs/aar/engine-fat-release.aar`

**Using Fat AAR:**
1. Copy `engine-fat-release.aar` to your project's `libs/` directory
2. Add to your build.gradle.kts:

```kotlin
dependencies {
    implementation(files("libs/engine-fat-release.aar"))
}
```

## Usage in Compose

```kotlin
import io.cloudsync.engine.CloudSync
import kotlinx.coroutines.launch

// Configure
val engine = CloudSync.configure("""{"configName":"my-app","serverUrl":"https://api.example.com"}""")

// Start
scope.launch {
    engine.start()
}

// Sync
scope.launch {
    engine.syncNow()
}
```

## Requirements

- Android SDK 26+
- Kotlin 2.1+
- JVM target 17

# Building with Desktop (Fat JAR)

## Building the Fat JAR

```bash
cd kmp-cloudsync-engine
./gradlew :engine:fatDesktopJar --no-daemon
```

Output: `engine/build/libs/kmp-cloudsync-engine-desktop-0.2.0-all.jar`

## Using in a Kotlin/JVM Project

```kotlin
// build.gradle.kts
dependencies {
    implementation(files("path/to/kmp-cloudsync-engine-desktop-0.2.0-all.jar"))
}
```

## Running directly

```bash
java -jar engine/build/libs/kmp-cloudsync-engine-desktop-0.2.0-all.jar
```

## Usage in Kotlin

```kotlin
import io.cloudsync.engine.CloudSync
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val engine = CloudSync.configure("""{"configName":"desktop-app","serverUrl":"https://api.example.com"}""")
    engine.start()
    engine.syncNow()
    println("State: ${engine.syncState.value}")
    engine.stop()
}
```

## Requirements

- JVM 17+
- Kotlin 2.1+

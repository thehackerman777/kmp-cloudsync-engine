# VPS Build Workflow

## Overview

All builds run on the designated build server.
- **OS:** Ubuntu 24.04 LTS
- **Java:** OpenJDK 21
- **Node.js:** v22.22.2
- **Android SDK:** API 34 + 35, build-tools 34.0.0

## Quick Reference

### Build everything

```bash
cd ~/projects/kmp-cloudsync-engine
./scripts/build-all.sh
```

### Publish to MavenLocal

```bash
./scripts/publish-local.sh
```

### Test samples

```bash
# Desktop sample (compile + run)
./scripts/test-desktop.sh

# Web sample (validate JS bundle)
./scripts/test-web.sh

# Android sample (compile check)
./scripts/test-android.sh

# Validate all artifacts
./scripts/validate-artifacts.sh
```

### Individual tasks

```bash
# Build engine only
./gradlew :engine:build --no-daemon

# Desktop fat JAR
./gradlew :engine:fatDesktopJar --no-daemon

# JS web bundle
./gradlew :engine:jsWebBundle --no-daemon

# Android AAR
./gradlew :engine:assembleRelease --no-daemon

# Fat AAR
./gradlew :engine:fatAAR --no-daemon

# Publish to MavenLocal
./gradlew :engine:publishToMavenLocal --no-daemon
```

## Troubleshooting

### Gradle daemon issues
```bash
./gradlew --stop
./gradlew --no-daemon :engine:build
```

### Out of memory
```properties
# In gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

### JS bundle not found
```bash
# Ensure binaries.executable() is set in build.gradle.kts for JS target
./gradlew :engine:jsBrowserProductionWebpack --no-daemon
./gradlew :engine:jsWebBundle --no-daemon
```

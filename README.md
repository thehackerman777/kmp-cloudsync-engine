<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/logo-dark.svg">
    <img alt="KMP CloudSync Engine" src="docs/assets/logo-light.svg" width="400">
  </picture>
</p>

<h1 align="center">KMP CloudSync Engine</h1>

<p align="center">
  <strong>Offline-First Private Data Synchronization Framework for Kotlin Multiplatform</strong>
</p>

<p align="center">
  <a href="https://github.com/thehackerman777/kmp-cloudsync-engine/actions/workflows/ci.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/thehackerman777/kmp-cloudsync-engine/ci.yml?branch=main&label=CI&logo=github" alt="CI">
  </a>
  <a href="https://github.com/thehackerman777/kmp-cloudsync-engine/releases">
    <img src="https://img.shields.io/github/v/release/thehackerman777/kmp-cloudsync-engine?include_prereleases&logo=semver" alt="Version">
  </a>
  <a href="https://github.com/thehackerman777/kmp-cloudsync-engine/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License">
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-2.1.0-purple?logo=kotlin" alt="Kotlin">
  </a>
  <img src="https://img.shields.io/badge/platform-Android%20%7C%20Desktop%20%7C%20Web-blue" alt="Platforms">
  <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build">
  <a href="https://github.com/thehackerman777/kmp-cloudsync-engine/actions/workflows/ci.yml">
    <img src="https://img.shields.io/badge/tests-passing-brightgreen" alt="Tests">
  </a>
</p>

---

## 📋 Overview

**KMP CloudSync Engine** is a production-grade SDK for private, secure, and offline-first data synchronization across Android, Desktop, and Web platforms powered by Kotlin Multiplatform.

It uses **Google Drive's invisible `appDataFolder`** as the cloud backend, ensuring configurations remain hidden from users and fully under application control — no visible folders, no user quota impact, no unwanted access.

> Built for: configuration sync, private data replication, multi-device settings, enterprise app state restoration, and secure credential backup.

### ✨ Key Features

- **🔄 Bidirectional Sync** — Automatic upload/download with version comparison
- **📴 Offline-First** — Local-read priority, background sync, seamless reconnect
- **⚔️ Conflict Resolution** — Last Write Wins (LWW), local/remote priority, manual merge
- **🔒 Private Cloud** — Google Drive `appDataFolder` — invisible, secure, zero-footprint
- **📦 Multiplatform** — Android, Desktop (JVM), Web (JS), iOS (coming)
- **🧩 Modular Architecture** — Clean Architecture with fully decoupled modules
- **📊 Observability** — Sync states, events, diagnostics, and telemetry out of the box
- **🔐 OAuth2 Security** — PKCE, token refresh, secure storage, auto-revocation
- **⚡ Background Sync** — Configurable intervals, adaptive scheduling, network-aware
- **📝 Version Tracking** — Incremental versioning, checksum validation, audit trails

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Presentation                         │
│            (Compose / Platform UI Models)                 │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                       Domain                              │
│   ┌──────────┐  ┌─────────────┐  ┌───────────────────┐   │
│   │  Models   │  │ Repositories │  │    Use Cases       │   │
│   └──────────┘  └─────────────┘  └───────────────────┘   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                        Data                               │
│   ┌────────────────┐  ┌──────────────────────────────┐   │
│   │  LocalDataSource│  │       RemoteDataSource        │   │
│   │   (SQLDelight)   │  │    (Google Drive API)        │   │
│   └────────────────┘  └──────────────────────────────┘   │
│   ┌──────────────────────────────────────────────────┐   │
│   │           ConfigurationRepository                 │   │
│   └──────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│          ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│          │  Network  │  │   Auth   │  │  Storage  │       │
│          │  (Ktor)   │  │ (OAuth2) │  │(SQLDelight)│      │
│          └──────────┘  └──────────┘  └──────────┘       │
│          ┌──────────────────────────────────────────┐    │
│          │              Sync Engine                   │    │
│          └──────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### Module Dependency Graph

```
┌────────────┐    ┌──────────┐    ┌────────────┐    ┌──────────┐
│ Presentation│───▶│  Domain  │◀───│    Data    │◀───│  Storage  │
└────────────┘    └──────────┘    └─────┬──────┘    └──────────┘
                                        │
                              ┌─────────▼─────────┐
                              │    Network (Ktor)  │
                              └─────────┬─────────┘
                                        │
                              ┌─────────▼─────────┐
                              │  Auth (OAuth2)     │
                              └─────────┬─────────┘
                                        │
                              ┌─────────▼─────────┐
                              │ Sync Engine        │
                              └───────────────────┘
```

---

## 🧩 Modules

| Module | Description | Platforms |
|--------|-------------|-----------|
| `:core:common` | Shared types, dispatchers, DI container, extensions | All |
| `:core:testing` | Test utilities, matchers, fakes | All |
| `:domain` | Business entities, repository contracts, use cases | All |
| `:data` | Local (SQLDelight) + Remote (Drive API) data sources | All |
| `:network` | Ktor client, interceptors, retry logic, DTOs | All |
| `:auth` | OAuth2 PKCE flow, token management, secure storage | All |
| `:sync` | Sync engine, conflict resolver, version manager, scheduler | All |
| `:storage` | Database driver factory, serialization | All |
| `:presentation` | Platform-agnostic UI state models | All |

---


---

## 🏗️ Build Pipeline

### Platform Artifacts

| Artifact | Command | Output |
|----------|---------|--------|
| **Android AAR** | `./gradlew :engine:assembleRelease` | `engine/build/outputs/aar/engine-release.aar` |
| **Desktop Fat JAR** | `./gradlew :engine:fatDesktopJar` | `engine/build/libs/*-desktop-*-all.jar` (~21MB) |
| **JS Bundle** | `./gradlew :engine:jsWebBundle` | `engine/build/outputs/js/engine.js` (~101KB) |
| **All artifacts** | `./gradlew :engine:buildAllArtifacts` | — |
| **MavenLocal** | `./gradlew :engine:publishToMavenLocal` | `io.cloudsync:engine:0.2.0` |

### Quick Reference

```bash
# Full build + validate
./scripts/build-all.sh

# Publish for local consumption
./scripts/publish-local.sh

# Validate generated artifacts
./scripts/validate-artifacts.sh
```

---

## 📦 Sample Projects

Consume the engine in real projects:

### Desktop (JVM Console)

```bash
./gradlew :samples:desktop-app:run
```

### Android (Compose)

```bash
./gradlew :samples:android-app:assembleDebug
```

### Web (JS)

```bash
node -e "const m = require(./engine/build/outputs/js/engine.js); const e = m.io.cloudsync.engine.CloudSyncEngine.Companion.create({}); e.start(); console.log(Engine state:, e.getState());"
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Building for Android](docs/BUILD-ANDROID.md) | Consume AAR via MavenLocal or Fat AAR |
| [Building for Desktop](docs/BUILD-DESKTOP.md) | Consume the Fat JAR |
| [Building for Web](docs/BUILD-WEB.md) | Consume the JS UMD bundle |
| [Release Checklist](docs/RELEASE-CHECKLIST.md) | Steps for publishing a release |
| [VPS Workflow](docs/VPS-WORKFLOW.md) | Build server setup & scripts |

---


## 🚀 Quick Start

### 1. Add Dependency

**Gradle (Kotlin DSL):**

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.cloudsync:kmp-cloudsync-engine:0.1.0")
}
```

### 2. Configure and Initialize

```kotlin
val engine = CloudSyncEngine.configure(
    SyncConfiguration(
        clientId = System.getenv("GOOGLE_CLIENT_ID"),
        clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
        scope = "https://www.googleapis.com/auth/drive.appdata",
        applicationName = "MyApp",
        syncPolicy = SyncPolicy(
            maxRetries = 5,
            enableBackgroundSync = true,
            backgroundSyncIntervalMs = 300_000L
        )
    )
)

// Start the engine (triggers initial sync immediately)
scope.launch {
    val result = engine.start()
    result.onSuccess { println("✅ Sync started successfully") }
}
```

### 3. Read/Write Configurations

```kotlin
// Save a configuration
val config = Configuration(
    id = "user-preferences",
    namespace = "myapp",
    payload = """{"theme":"dark","language":"en"}"""
)
engine.save(config)

// Observe configurations reactively
engine.observeAll().collect { configs ->
    println("Current configs: ${configs.size}")
}

// Force a sync cycle
engine.syncNow()
```

### 4. Observe Sync State

```kotlin
engine.syncState.collect { state ->
    when (state) {
        SyncState.IDLE -> println("⏸️ Engine idle")
        SyncState.SYNCING -> println("🔄 Syncing...")
        SyncState.ERROR -> println("❌ Sync error")
        SyncState.AUTHENTICATING -> println("🔐 Authenticating...")
        else -> println("State: $state")
    }
}
```

---

## 🔧 Configuration

### OAuth2 Setup

1. **Create a Google Cloud Project** → [console.cloud.google.com](https://console.cloud.google.com)
2. **Enable Google Drive API**
3. **Configure OAuth consent screen** (External testing for dev)
4. **Create Desktop OAuth2 credentials**
5. **Add the redirect URI** (`http://localhost:8090`)

```kotlin
val engine = CloudSyncEngine.configure {
    authProvider = GoogleDriveAuthProvider(
        clientId = "YOUR_CLIENT_ID",
        clientSecret = "YOUR_CLIENT_SECRET"
    )
}
```

### Sync Policy

```kotlin
val policy = SyncPolicy(
    maxRetries = 5,
    baseBackoffMs = 1_000,
    maxBackoffMs = 60_000,
    enableBackoffJitter = true,
    defaultConflictStrategy = ConflictStrategy.LAST_WRITE_WINS,
    enableBackgroundSync = true,
    backgroundSyncIntervalMs = 300_000,
    enableCompression = true,
    enableEncryption = true
)
```

---

## 📊 CI/CD Pipeline

| Stage | Workflow | Trigger | Description |
|-------|----------|---------|-------------|
| 🔍 Lint | `ci.yml` | PR, push | Detekt, ktlint, API check |
| 🧪 Test | `ci.yml` | PR, push | Cross-platform unit + integration tests |
| 📦 Build | `ci.yml` | PR, push | Android/Desktop/JS compilation |
| 🔒 Security | `security.yml` | Push, weekly | CodeQL, secret scanning, SAST |
| 🌙 Nightly | `nightly.yml` | Daily | Full build, dependency scan, stale check |
| 🚀 Release | `cd.yml` | Tag `v*.*.*` | Build, publish, GitHub Release |

### Publishing

```bash
git tag v1.0.0
git push origin v1.0.0
# Triggers: Build → Publish to Maven Central + GitHub Packages → Release
```

---

## 🧪 Testing

```bash
# Run all tests across all modules
./gradlew allTests

# Run specific module tests
./gradlew :sync:allTests

# Generate coverage report
./gradlew jacocoTestReport
```

---

## 🛣️ Roadmap

| Phase | Feature | Status |
|-------|---------|--------|
| P0 | Core sync engine (bidirectional, LWW) | ✅ Complete |
| P0 | Google Drive appDataFolder integration | ✅ Complete |
| P0 | OAuth2 PKCE flow | ✅ Complete |
| P0 | SQLDelight local persistence | ✅ Complete |
| P1 | iOS target support | 🚧 In Progress |
| P1 | Web target (JS/WASM) | 🚧 In Progress |
| P1 | Conflict resolution UI | 📋 Planned |
| P1 | Compression (gzip/snappy) | 📋 Planned |
| P2 | E2E encryption (AES-256-GCM) | 📋 Planned |
| P2 | Multi-cloud (Firebase, AWS S3) | 📋 Planned |
| P2 | Telemetry & metrics | 📋 Planned |
| P3 | AI-driven conflict prediction | 🔮 Future |
| P3 | Automated data recovery | 🔮 Future |

---

## 🔒 Security

- **OAuth2 PKCE** — Protection against authorization code interception
- **Token encryption** — AES-256 at rest via platform keychains
- **Drive appDataFolder** — Files invisible to users, no accidental exposure
- **Checksum validation** — SHA-256 integrity checks on every operation
- **Token auto-refresh** — Seamless rotation without user interruption
- **Revocation** — Complete token invalidation on logout

➡️ See [SECURITY.md](docs/SECURITY.md) for full security documentation.

---

## 📚 Documentation

- [Architecture Guide](docs/ARCHITECTURE.md) — Detailed module design and patterns
- [Security Model](docs/SECURITY.md) — Threat model and security measures
- [Contributing Guide](docs/CONTRIBUTING.md) — How to contribute
- [Roadmap](docs/ROADMAP.md) — Future plans and milestones
- [API Reference](https://docs.thehackerman777.github.io) — Full Dokka-generated API docs
- [Migration Guide](docs/MIGRATION.md) — Version migration guides

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](docs/CONTRIBUTING.md) for guidelines.

**Quick start:**
```bash
git clone git@github.com:cloudsync/kmp-cloudsync-engine.git
cd kmp-cloudsync-engine
./gradlew build
```

---

## 📄 License

```
Copyright 2024 CloudSync Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<p align="center">
  <sub>Built with ❤️ by the CloudSync Team</sub><br>
  <sub>Powered by <a href="https://kotlinlang.org/">Kotlin</a> · <a href="https://ktor.io/">Ktor</a> · <a href="https://cashapp.github.io/sqldelight/">SQLDelight</a></sub>
</p>

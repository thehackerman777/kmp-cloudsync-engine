# KMP CloudSync Engine — Architecture Guide

## Overview

KMP CloudSync Engine follows **Clean Architecture** principles with a **modular monolith** structure. Each module has clear responsibilities, explicit dependencies, and testable boundaries.

## Architectural Principles

### 1. Dependency Rule

Dependencies point inward: **Presentation** → **Domain** ← **Data** ← **Infrastructure**

```
Presentation  →  Domain  ←  Data  ←  Network / Auth / Sync / Storage
```

- **Domain** has zero external dependencies (pure Kotlin)
- **Data** depends on Domain interfaces
- **Infrastructure** depends on Data contracts
- **Presentation** depends only on Domain models

### 2. Offline-First Strategy

```
┌─────────────┐       ┌──────────────┐
│  Read Data   │──────▶│  Local First  │
└─────────────┘       └──────┬───────┘
                             │
                    ┌────────▼────────┐
                    │  Is Data Cached? │
                    └──┬──────────┬───┘
                       │          │
                   Yes ▼          ▼ No
              ┌──────────┐  ┌──────────┐
              │ Return    │  │ Fetch     │
              │ Local     │  │ Remote    │
              └──────────┘  └────┬─────┘
                                 │
                          ┌──────▼──────┐
                          │ Cache Local  │
                          └─────────────┘
```

### 3. State Machine

The sync engine follows a deterministic state machine:

```
    ┌────────────────────────────────────────────┐
    │                                            │
    ▼                                            │
  IDLE ──▶ INITIALIZING ──▶ AUTHENTICATING ──▶ SYNCING ──▶ IDLE
   │         │                  │                │            │
   │         ▼                  ▼                ▼            │
   └──── ERROR ◀─────────── RETRYING ◀────────────────────────┘
```

## Module Architecture

### Core (`:core:common`)

```
core/common/
├── CloudSyncEngine.kt        # Public SDK entry point
├── SyncState.kt              # State machine enum
├── SyncDiagnostics.kt        # Diagnostic data class
├── InternalCloudSyncApi.kt   # @RequiresOptIn annotation
├── di/
│   └── CloudSyncDI.kt        # Service locator / Koin module
├── dispatcher/
│   └── CoroutineDispatchers.kt  # Platform dispatcher abstraction
├── result/
│   └── SyncResult.kt         # Typed Result monad + error hierarchy
└── extension/
    ├── InstantExtensions.kt   # Time utilities
    └── ByteArrayExtensions.kt # Checksum helpers
```

### Domain (`:domain`)

```
domain/
├── model/
│   ├── Configuration.kt      # Core domain entity
│   ├── ConflictResolution.kt # Conflict result
│   └── SyncMetadata.kt       # Sync audit trail
├── repository/
│   └── IConfigurationRepository.kt  # Repository contract
├── usecase/
│   ├── SyncConfigurationsUseCase.kt # Sync orchestration logic
│   ├── GetConfigurationUseCase.kt   # Read operations
│   └── SaveConfigurationUseCase.kt  # Write operations
└── config/
    └── SyncPolicy.kt          # Domain sync configuration
```

### Data (`:data`)

```
data/
├── local/
│   ├── LocalDataSource.kt    # SQLDelight-based persistence
│   ├── dao/                  # Data access objects (generated)
│   └── entity/               # DB entity mappers
├── remote/
│   ├── RemoteDataSource.kt  # Google Drive API data source
│   ├── dto/                  # Drive API DTOs
│   └── source/               # API source implementations
└── repository/
    └── ConfigurationRepository.kt  # Repository implementation
```

### Network (`:network`)

```
network/
├── client/
│   └── NetworkClientProvider.kt  # Ktor HttpClient factory
├── interceptor/
│   ├── AuthInterceptor.kt    # OAuth2 Bearer injection + refresh
│   ├── RetryInterceptor.kt   # Exponential backoff + jitter
│   └── LoggingInterceptor.kt # Sanitized request logging
├── dto/
│   └── DriveApiModels.kt     # Drive API response DTOs
└── exception/
    └── NetworkExceptions.kt  # Typed exception hierarchy
```

### Auth (`:auth`)

```
auth/
├── AuthManager.kt            # Central auth lifecycle manager
├── oauth2/
│   └── OAuth2Client.kt       # PKCE authorization code flow
├── token/
│   └── TokenProvider.kt      # In-memory token management
├── secure/
│   └── SecureStorage.kt      # Platform encrypted storage (expect)
└── provider/
    └── AuthProviderRegistry.kt  # Pluggable auth providers
```

### Sync (`:sync`)

```
sync/
├── SyncOrchestrator.kt       # High-level state management
├── engine/
│   └── SyncEngine.kt         # Core sync cycle logic
├── conflict/
│   ├── ConflictResolver.kt   # LWW + strategy resolution
│   └── ConflictDetector.kt   # Conflict detection
├── version/
│   └── VersionManager.kt     # Incremental version tracking
├── scheduler/
│   ├── SyncScheduler.kt      # Background timing
│   └── SyncTrigger.kt        # Event-based triggers
├── metadata/
│   └── SyncMetadataTracker.kt # Sync audit trail
└── policy/
    └── RetryPolicy.kt        # Exponential backoff + circuit breaker
```

## Data Flow

### Write Operation (Offline-First)

```
User App
   │
   ▼
Repository.save(config)        # 1. Repository receives save command
   │
   ├──▶ LocalDataSource.save()  # 2. Persist locally immediately
   │      └── SQLDelight INSERT
   │
   └──▶ Mark as pending sync   # 3. Flag for background sync
   │
   ▼
SyncEngine.uploadChanges()     # 4. Async: push to cloud
   │
   └──▶ RemoteDataSource.upload()
          └── Drive API PUT
```

### Read Operation (Offline-First)

```
User App
   │
   ▼
Repository.getById(id)         # 1. Repository receives read
   │
   ├──▶ LocalDataSource.getById()  # 2. Try local first
   │      ├── Found: return cached  # 3. Offline-fast path ✅
   │      └── Not found: fall through
   │
   └──▶ RemoteDataSource.download() # 4. Fallback to remote
          └── Cache locally + return
```

### Sync Cycle

```
1. Check connectivity
2. Upload pending local changes
3. Download remote changes
4. Compare versions
5. Detect conflicts
6. Auto-resolve (LWW)
7. Apply resolutions
8. Update metadata
9. Emit sync events
10. Schedule next cycle
```

## Testing Strategy

### Unit Tests
- Domain use cases with mocked repositories
- Sync engine with mocked data sources
- Conflict resolver with sample configurations
- Version manager with version sequences

### Integration Tests
- LocalDataSource with in-memory SQLDelight
- RemoteDataSource with mocked Ktor engine
- Full sync cycle with in-memory driver

### End-to-End Tests
- Auth flow simulation
- Multi-device conflict scenarios
- Offline → Online transition

## Future Extensions

The architecture supports these extensions with minimal changes:

1. **E2E Encryption** — Add `EncryptionInterceptor` to sync pipeline
2. **Multi-Cloud** — Register new `AuthProvider` + `RemoteDataSource` implementations
3. **Compression** — Layer in `PayloadCompressor` at serialization boundary
4. **Metrics** — Subscribe to `syncEvents` flow for observability
5. **AI Automation** — Plug into `ConflictResolver` with ML-based resolution suggestions

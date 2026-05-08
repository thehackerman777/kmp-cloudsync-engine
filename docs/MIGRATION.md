# Migration Guide

## v0.1.0 → v1.0.0 (Future)

### Breaking Changes

- `SyncConfiguration` renamed to `SyncPolicy`
- `ConfigurationRepository` now requires explicit `IConfigurationRepository` interface
- OAuth2 initialization requires `AuthProvider` object (not raw config)

### Migration Steps

```kotlin
// v0.1.0 (old)
val engine = CloudSyncEngine.configure(config) { ... }

// v1.0.0 (new)
val engine = CloudSyncEngine.configure {
    authProvider = GoogleDriveAuthProvider(clientId = "...")
    syncPolicy = SyncPolicy(maxRetries = 5)
}
```

## What's Not Stable Yet

- `presentation` module APIs are subject to change
- `auth.SecureStorage` platform implementations are experimental

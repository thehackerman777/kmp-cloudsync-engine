package io.cloudsync.engine

import io.cloudsync.core.CloudSyncEngine as CoreEngine
import io.cloudsync.core.SyncDiagnostics
import io.cloudsync.core.SyncState
import io.cloudsync.core.result.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * KMP CloudSync Engine Entry Point.
 *
 * Single entry point for all platforms (Android, Desktop, Web).
 * Wraps [CoreEngine] to provide a unified front.
 *
 * ```kotlin
 * val engine = CloudSync.configure(config)
 * engine.start()
 * ```
 */
public class CloudSync private constructor(
    internal val delegate: CoreEngine
) {
    public val syncState: StateFlow<SyncState> get() = delegate.syncState
    public val syncResults: Flow<SyncResult<Unit>> get() = delegate.syncResults
    public val configurationName: String get() = delegate.configurationName

    public fun initialize(config: String): CloudSync {
        delegate.initialize(config)
        return this
    }

    public suspend fun start(): SyncResult<Unit> = delegate.start()
    public suspend fun stop() { delegate.stop() }
    public suspend fun syncNow(): SyncResult<Unit> = delegate.syncNow()
    public suspend fun reset() { delegate.reset() }
    public fun diagnostics(): SyncDiagnostics = delegate.diagnostics()

    public companion object {
        public fun configure(config: String): CloudSync =
            CloudSync(CoreEngine.configure(config))
    }
}

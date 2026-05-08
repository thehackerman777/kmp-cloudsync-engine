package io.cloudsync.core

import io.cloudsync.core.result.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry point for the KMP CloudSync Engine SDK.
 *
 * Thread-safe singleton orchestrator that manages the full lifecycle of
 * configuration synchronization across Android, Desktop, and Web platforms.
 *
 * Usage:
 * ```kotlin
 * val engine = CloudSyncEngine.configure {
 *     clientId = System.getenv("GOOGLE_CLIENT_ID")
 *     applicationName = "MyApp"
 *     scope = "offline_access"
 * }
 * engine.start()
 * ```
 *
 * @see SyncEngine
 * @see SyncConfiguration
 */
public expect class CloudSyncEngine private constructor() {

    /**
     * Current state of the sync engine.
     * Emits values on every state transition.
     */
    public val syncState: StateFlow<SyncState>

    /**
     * Stream of sync results for observability and telemetry.
     */
    public val syncResults: Flow<SyncResult<Unit>>

    /**
     * Current sync configuration name in use.
     */
    public val configurationName: String

    /**
     * Initializes the engine with the given configuration.
     * Must be called before [start].
     */
    public fun initialize(config: String): CloudSyncEngine

    /**
     * Starts the sync engine. Triggers an initial sync cycle
     * and schedules background synchronization.
     */
    public suspend fun start(): SyncResult<Unit>

    /**
     * Stops all sync operations gracefully.
     * Cancels pending operations and flushes local buffers.
     */
    public suspend fun stop()

    /**
     * Triggers an immediate manual sync cycle.
     * Respects rate limiting and conflict resolution policies.
     */
    public suspend fun syncNow(): SyncResult<Unit>

    /**
     * Resets local state and re-authenticates.
     * Useful after token revocation or auth errors.
     */
    public suspend fun reset()

    /**
     * Returns diagnostic information for debugging.
     */
    public fun diagnostics(): SyncDiagnostics

    public companion object {
        /**
         * Creates a new configured instance.
         * Reuse across the application lifecycle.
         */
        public fun configure(config: String): CloudSyncEngine
    }
}

package io.cloudsync.domain.repository

import io.cloudsync.core.result.SyncResult
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.ConflictResolution
import io.cloudsync.domain.model.SyncMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for configuration persistence and synchronization.
 *
 * Acts as the single source of truth for configuration data, abstracting
 * local storage and remote synchronization behind a clean interface.
 *
 * This is the core boundary of the Clean Architecture:
 * - Domain defines the contract
 * - Data layer provides the implementation
 * - Sync layer orchestrates the data flow
 */
public interface IConfigurationRepository {

    /**
     * Retrieves a configuration by its unique ID.
     * Returns cached value from local storage first (offline-first).
     */
    public suspend fun getById(id: String): SyncResult<Configuration>

    /**
     * Retrieves all configurations in a namespace.
     */
    public suspend fun getByNamespace(namespace: String): SyncResult<List<Configuration>>

    /**
     * Saves a configuration locally and marks it for sync.
     */
    public suspend fun save(configuration: Configuration): SyncResult<Configuration>

    /**
     * Deletes a configuration locally and remotely.
     */
    public suspend fun delete(id: String): SyncResult<Unit>

    /**
     * Returns a reactive stream of all configurations.
     */
    public fun observeAll(): Flow<List<Configuration>>

    /**
     * Returns a reactive stream of configurations in a namespace.
     */
    public fun observeByNamespace(namespace: String): Flow<List<Configuration>>

    /**
     * Returns configurations with pending local changes.
     */
    public suspend fun getPendingSync(): SyncResult<List<Configuration>>

    /**
     * Returns the version history for a configuration.
     */
    public suspend fun getVersionHistory(id: String, limit: Int = 10): SyncResult<List<SyncMetadata>>

    /**
     * Resolves a sync conflict using the specified strategy.
     */
    public suspend fun resolveConflict(resolution: ConflictResolution): SyncResult<Configuration>

    /**
     * Returns the total count of stored configurations.
     */
    public suspend fun count(): Long

    /**
     * Clears all local data (use with caution).
     */
    public suspend fun clearLocal()
}

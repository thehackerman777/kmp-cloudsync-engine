package io.cloudsync.sync.engine

import io.cloudsync.core.result.SyncResult
import io.cloudsync.data.local.LocalDataSource
import io.cloudsync.data.remote.RemoteDataSource
import io.cloudsync.data.repository.ConfigurationRepository
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.ConflictResolution
import io.cloudsync.domain.model.ConflictStrategy
import io.cloudsync.sync.conflict.ConflictResolver
import io.cloudsync.sync.version.VersionManager
import io.cloudsync.sync.metadata.SyncMetadataTracker
import io.cloudsync.sync.policy.RetryPolicy
import kotlinx.coroutines.flow.Flow

/**
 * Core sync engine responsible for bidirectional synchronization.
 *
 * Implements the offline-first sync strategy:
 * 1. Local-first reads (always serve from local DB)
 * 2. Background sync with cloud (Google Drive appDataFolder)
 * 3. Version comparison and conflict detection
 * 4. Automated conflict resolution (Last Write Wins by default)
 * 5. Change tracking for incremental sync
 *
 * The engine is stateful — it tracks sync progress, pending operations,
 * and maintains a change log for incremental synchronization.
 */
public class SyncEngine(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val conflictResolver: ConflictResolver,
    private val versionManager: VersionManager,
    private val metadataTracker: SyncMetadataTracker,
    private val retryPolicy: RetryPolicy = RetryPolicy()
) {
    private var initialized = false

    /**
     * Initializes the sync engine.
     * Validates connections and prepares internal state.
     */
    public fun initialize() {
        versionManager.initialize()
        metadataTracker.initialize()
        initialized = true
    }

    /**
     * Shuts down the sync engine and releases resources.
     */
    public fun shutdown() {
        initialized = false
    }

    /**
     * Resets the engine to initial state.
     */
    public fun reset() {
        versionManager.reset()
        metadataTracker.reset()
        initialized = false
    }

    /**
     * Uploads local changes to the remote storage.
     *
     * Process:
     * 1. Query local pending changes
     * 2. Check version compatibility
     * 3. Upload with retry logic
     * 4. Update sync metadata
     */
    public suspend fun uploadChanges(): SyncResult<Unit> {
        check(initialized) { "SyncEngine not initialized" }

        return retryPolicy.withRetry("upload") {
            val pending = localDataSource.getPendingChanges()
            if (pending.isEmpty()) return@withRetry SyncResult.success(Unit)

            for (config in pending) {
                val remoteVersion = remoteDataSource.getVersion(config.id)
                if (remoteVersion != null && remoteVersion > config.version) {
                    // Conflict: remote is ahead — will be resolved in resolveConflicts()
                    continue
                }

                val uploadResult = remoteDataSource.upload(config)
                if (uploadResult is SyncResult.Error) {
                    return@withRetry uploadResult
                }

                localDataSource.markSynced(config.id)
                metadataTracker.recordUpload(config.id, config.version)
            }

            SyncResult.success(Unit)
        }
    }

    /**
     * Downloads remote changes to local storage.
     *
     * Process:
     * 1. Fetch remote file list
     * 2. Compare versions with local
     * 3. Download newer files
     * 4. Apply to local storage
     */
    public suspend fun downloadChanges(): SyncResult<Unit> {
        check(initialized) { "SyncEngine not initialized" }

        return retryPolicy.withRetry("download") {
            val remoteFiles = remoteDataSource.listFiles()
            val result = when (remoteFiles) {
                is SyncResult.Success -> remoteFiles.data
                is SyncResult.Error -> return@withRetry remoteFiles
                else -> return@withRetry SyncResult.success(Unit)
            }

            for (file in result) {
                val localVersion = localDataSource.getVersion(file.id)
                val remoteVersion = file.version

                if (remoteVersion > localVersion) {
                    val downloadResult = remoteDataSource.download(file.id)
                    if (downloadResult is SyncResult.Success) {
                        localDataSource.saveIfNewer(downloadResult.data)
                        metadataTracker.recordDownload(file.id, remoteVersion)
                    }
                }
            }

            SyncResult.success(Unit)
        }
    }

    /**
     * Detects and resolves synchronization conflicts.
     *
     * Uses the configured [ConflictStrategy] (default: Last Write Wins).
     */
    public suspend fun resolveConflicts(): SyncResult<Unit> {
        check(initialized) { "SyncEngine not initialized" }

        val conflicts = localDataSource.detectConflicts()

        for ((local, remote) in conflicts) {
            val resolution = conflictResolver.resolve(local, remote)
            localDataSource.applyResolution(resolution)
            metadataTracker.recordConflict(resolution)
        }

        return SyncResult.success(Unit)
    }

    /**
     * Checks connectivity to the remote provider.
     */
    public suspend fun checkConnectivity(): Boolean {
        return remoteDataSource.checkConnectivity()
    }
}

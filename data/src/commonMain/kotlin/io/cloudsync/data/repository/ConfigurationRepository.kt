package io.cloudsync.data.repository

import io.cloudsync.core.result.SyncResult
import io.cloudsync.data.local.LocalDataSource
import io.cloudsync.data.remote.RemoteDataSource
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.ConflictResolution
import io.cloudsync.domain.model.SyncMetadata
import io.cloudsync.domain.repository.IConfigurationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repository implementation following Clean Architecture.
 *
 * Orchestrates between local and remote data sources:
 * - Reads: Local-first (offline-first)
 * - Writes: Local + remote (write-through)
 * - Sync: Background reconciliation
 *
 * This is the bridge between domain and data layers.
 */
public class ConfigurationRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : IConfigurationRepository {

    override suspend fun getById(id: String): SyncResult<Configuration> {
        return try {
            val local = localDataSource.getById(id)
            if (local != null) {
                SyncResult.success(local)
            } else {
                val remote = remoteDataSource.download(id)
                remote.onSuccess { localDataSource.save(it) }
                remote
            }
        } catch (e: Exception) {
            SyncResult.error(
                io.cloudsync.core.result.SyncErrorCode.STORAGE_IO_ERROR,
                "Failed to get configuration: $id",
                e
            )
        }
    }

    override suspend fun getByNamespace(namespace: String): SyncResult<List<Configuration>> {
        return try {
            SyncResult.success(localDataSource.getByNamespace(namespace))
        } catch (e: Exception) {
            SyncResult.error(
                io.cloudsync.core.result.SyncErrorCode.STORAGE_IO_ERROR,
                "Failed to get namespace: $namespace",
                e
            )
        }
    }

    override suspend fun save(configuration: Configuration): SyncResult<Configuration> {
        return try {
            val saved = localDataSource.save(configuration)
            SyncResult.success(saved)
        } catch (e: Exception) {
            SyncResult.error(
                io.cloudsync.core.result.SyncErrorCode.STORAGE_IO_ERROR,
                "Failed to save configuration",
                e
            )
        }
    }

    override suspend fun delete(id: String): SyncResult<Unit> {
        return try {
            localDataSource.delete(id)
            remoteDataSource.delete(id)
            SyncResult.success(Unit)
        } catch (e: Exception) {
            SyncResult.error(
                io.cloudsync.core.result.SyncErrorCode.STORAGE_IO_ERROR,
                "Failed to delete configuration: $id",
                e
            )
        }
    }

    override fun observeAll(): Flow<List<Configuration>> = localDataSource.observeAll()

    override fun observeByNamespace(namespace: String): Flow<List<Configuration>> =
        localDataSource.observeByNamespace(namespace)

    override suspend fun getPendingSync(): SyncResult<List<Configuration>> {
        return try {
            SyncResult.success(localDataSource.getPendingChanges())
        } catch (e: Exception) {
            SyncResult.error(
                io.cloudsync.core.result.SyncErrorCode.STORAGE_IO_ERROR,
                "Failed to get pending sync",
                e
            )
        }
    }

    override suspend fun getVersionHistory(id: String, limit: Int): SyncResult<List<SyncMetadata>> {
        // Stub — full implementation with version history querying
        return SyncResult.success(emptyList())
    }

    override suspend fun resolveConflict(resolution: ConflictResolution): SyncResult<Configuration> {
        return try {
            localDataSource.applyResolution(resolution)
            val saved = localDataSource.getById(resolution.resolved.id)
                ?: return SyncResult.error(
                    io.cloudsync.core.result.SyncErrorCode.STORAGE_CORRUPTION,
                    "Resolved config not found"
                )
            SyncResult.success(saved)
        } catch (e: Exception) {
            SyncResult.error(
                io.cloudsync.core.result.SyncErrorCode.SYNC_CONFLICT_DETECTED,
                "Failed to resolve conflict",
                e
            )
        }
    }

    override suspend fun count(): Long {
        return 0L // Placeholder
    }

    override suspend fun clearLocal() {
        // Implementation would clear all local tables
    }
}

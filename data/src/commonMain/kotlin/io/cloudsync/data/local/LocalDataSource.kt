package io.cloudsync.data.local

import io.cloudsync.core.extension.sha256Hex
import io.cloudsync.core.result.SyncResult
import io.cloudsync.data.local.db.CloudSyncDatabase
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.ConflictResolution
import io.cloudsync.domain.model.SyncMetadata
import io.cloudsync.domain.model.SyncSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Local data source using SQLDelight for offline-first persistence.
 *
 * Provides:
 * - CRUD operations on configurations
 * - Pending change tracking
 * - Version history persistence
 * - Sync metadata management
 * - Conflict detection
 */
public class LocalDataSource(private val database: CloudSyncDatabase) {

    private val queries = database.cloudSyncDbQueries
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Retrieves a configuration by ID.
     */
    public suspend fun getById(id: String): Configuration? {
        return queries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    /**
     * Retrieves all configurations.
     */
    public suspend fun getAll(): List<Configuration> {
        return queries.selectAll().executeAsList().map { it.toDomain() }
    }

    /**
     * Retrieves configurations by namespace.
     */
    public suspend fun getByNamespace(namespace: String): List<Configuration> {
        return queries.selectByNamespace(namespace).executeAsList().map { it.toDomain() }
    }

    /**
     * Observes all configurations reactively (for LiveData/StateFlow integration).
     */
    public fun observeAll(): Flow<List<Configuration>> {
        return queries.selectAll().asFlow().mapToList(io.cloudsync.data.local.db.CloudSyncDatabase::class) { it.toDomain() }
    }

    /**
     * Observes configurations by namespace reactively.
     */
    public fun observeByNamespace(namespace: String): Flow<List<Configuration>> {
        return queries.selectByNamespace(namespace).asFlow().mapToList(io.cloudsync.data.local.db.CloudSyncDatabase::class) { it.toDomain() }
    }

    /**
     * Saves a configuration (insert or update).
     */
    public suspend fun save(configuration: Configuration): Configuration {
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = getById(configuration.id)
        val version = (existing?.version ?: 0) + 1
        val checksum = configuration.payload.encodeToByteArray().sha256Hex()
        val createdAt = existing?.createdAt ?: now

        val updated = configuration.copy(
            version = version,
            checksum = checksum,
            updatedAt = now,
            createdAt = createdAt,
            synced = false,
            sizeBytes = configuration.payload.encodeToByteArray().size.toLong()
        )

        queries.insertOrReplace(
            id = updated.id,
            namespace = updated.namespace,
            payload = updated.payload,
            version = updated.version,
            checksum = updated.checksum,
            updated_at = updated.updatedAt,
            created_at = updated.createdAt,
            synced = if (updated.synced) 1L else 0L,
            deleted = 0L,
            tags = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()), updated.tags),
            size_bytes = updated.sizeBytes,
            encrypted = if (updated.encrypted) 1L else 0L
        )

        // Record version history
        queries.insertVersionHistory(
            config_id = updated.id,
            version = updated.version,
            checksum = updated.checksum,
            timestamp = now,
            source = SyncSource.LOCAL.name
        )

        return updated
    }

    /**
     * Deletes a configuration (soft delete).
     */
    public suspend fun delete(id: String) {
        queries.markDeleted(id)
    }

    /**
     * Returns configurations with pending local changes.
     */
    public suspend fun getPendingChanges(): List<Configuration> {
        return queries.selectPendingSync().executeAsList().map { it.toDomain() }
    }

    /**
     * Gets the version of a configuration.
     */
    public suspend fun getVersion(id: String): Long {
        return queries.getVersion(id).executeAsOneOrNull()?.version ?: 0L
    }

    /**
     * Marks a configuration as synced.
     */
    public suspend fun markSynced(id: String) {
        queries.markSynced(id)
    }

    /**
     * Saves a downloaded remote configuration if it's newer than local.
     */
    public suspend fun saveIfNewer(remote: Configuration) {
        val local = getById(remote.id)
        if (local == null || remote.version > local.version) {
            save(remote.copy(synced = true))
        }
    }

    /**
     * Detects conflicts between local and what would be applied.
     * Returns pairs of (local, remote) where both have pending changes.
     */
    public suspend fun detectConflicts(): List<Pair<Configuration, Configuration>> {
        // Simplified: in production, compare with remote checksums
        return emptyList()
    }

    /**
     * Applies a conflict resolution to local storage.
     */
    public suspend fun applyResolution(resolution: ConflictResolution) {
        save(resolution.resolved)
    }
}

/**
 * SQLDelight entity → Domain model mapper.
 */
private fun io.cloudsync.data.local.db.Configuration.toDomain(): Configuration = Configuration(
    id = id,
    namespace = namespace,
    payload = payload,
    version = version,
    checksum = checksum,
    updatedAt = updated_at,
    createdAt = created_at,
    synced = synced == 1L,
    tags = emptyList(), // Parse from JSON if needed
    sizeBytes = size_bytes,
    encrypted = encrypted == 1L
)



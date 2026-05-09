package io.cloudsync.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.cloudsync.core.extension.sha256Hex
import io.cloudsync.core.result.SyncResult
import io.cloudsync.data.local.db.CloudSyncDatabase
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.ConflictResolution
import io.cloudsync.domain.model.SyncMetadata
import io.cloudsync.domain.model.SyncSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Local data source using SQLDelight for offline-first persistence.
 */
public class LocalDataSource(private val database: CloudSyncDatabase) {

    private val queries = database.cloudSyncDbQueries
    private val json = Json { ignoreUnknownKeys = true }

    public suspend fun getById(id: String): Configuration? {
        return queries.selectById(id).executeAsOneOrNull()?.let { it.toDomain() }
    }

    public suspend fun getAll(): List<Configuration> {
        return queries.selectAll().executeAsList().map { it.toDomain() }
    }

    public suspend fun getByNamespace(namespace: String): List<Configuration> {
        return queries.selectByNamespace(namespace).executeAsList().map { it.toDomain() }
    }

    public fun observeAll(): Flow<List<Configuration>> {
        return queries.selectAll().asFlow().mapToList(Dispatchers.Default).map { list ->
            list.map { it.toDomain() }
        }
    }

    public fun observeByNamespace(namespace: String): Flow<List<Configuration>> {
        return queries.selectByNamespace(namespace).asFlow().mapToList(Dispatchers.Default).map { list ->
            list.map { it.toDomain() }
        }
    }

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
            tags = "[]",
            size_bytes = updated.sizeBytes,
            encrypted = if (updated.encrypted) 1L else 0L
        )

        queries.insertVersionHistory(
            config_id = updated.id,
            version = updated.version,
            checksum = updated.checksum,
            timestamp = now,
            source = SyncSource.LOCAL.name
        )

        return updated
    }

    public suspend fun delete(id: String) {
        queries.markDeleted(id)
    }

    public suspend fun getPendingChanges(): List<Configuration> {
        return queries.selectPendingSync().executeAsList().map { it.toDomain() }
    }

    public suspend fun getVersion(id: String): Long {return queries.getVersion(id).executeAsOneOrNull() ?: 0L
    }

    public suspend fun markSynced(id: String) {
        queries.markSynced(id)
    }

    public suspend fun saveIfNewer(remote: Configuration) {
        val local = getById(remote.id)
        if (local == null || remote.version > local.version) {
            save(remote.copy(synced = true))
        }
    }

    public suspend fun detectConflicts(): List<Pair<Configuration, Configuration>> {
        return emptyList()
    }

    public suspend fun applyResolution(resolution: ConflictResolution) {
        save(resolution.resolved)
    }
}



private fun io.cloudsync.data.local.db.Configurations.toDomain(): io.cloudsync.domain.model.Configuration {
    return io.cloudsync.domain.model.Configuration(
        id = id,
        namespace = namespace,
        payload = payload,
        version = version,
        checksum = checksum,
        updatedAt = updated_at,
        createdAt = created_at,
        synced = synced == 1L,
        tags = emptyList(),
        sizeBytes = size_bytes,
        encrypted = encrypted == 1L
    )
}

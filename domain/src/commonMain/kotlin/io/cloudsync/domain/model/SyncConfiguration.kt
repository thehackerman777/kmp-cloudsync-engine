package io.cloudsync.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a single persisted configuration entry.
 *
 * Each configuration has a unique key, a versioned payload, and metadata
 * for conflict resolution and audit trails.
 */
@Serializable
public data class Configuration(
    val id: String,
    val namespace: String,
    val payload: String,
    val version: Long = 0,
    val checksum: String = "",
    val updatedAt: Long = 0,
    val createdAt: Long = 0,
    val synced: Boolean = true,
    val tags: List<String> = emptyList(),
    val sizeBytes: Long = 0,
    val encrypted: Boolean = false,
    /** Optional user ID for identity-scoped configurations. */
    val userId: String? = null,
    /** Optional device ID for device-scoped configurations. */
    val deviceId: String? = null
) {
    public val hasPendingChanges: Boolean get() = !synced

    public fun isNewerThan(other: Configuration): Boolean = version > other.version

    public fun contentEquals(other: Configuration): Boolean = checksum == other.checksum
}

@Serializable
public data class ConflictResolution(
    public val localVersion: Configuration,
    public val remoteVersion: Configuration,
    public val resolved: Configuration,
    public val strategy: ConflictStrategy,
    public val timestamp: Long
)

@Serializable
public enum class ConflictStrategy {
    LAST_WRITE_WINS,
    LOCAL_PRIORITY,
    REMOTE_PRIORITY,
    MANUAL_REQUIRED
}

/**
 * Sync metadata attached to each configuration revision.
 */
@Serializable
public data class SyncMetadata(
    val configurationId: String,
    val version: Long,
    val checksum: String,
    val timestamp: Long,
    val deviceId: String,
    val source: SyncSource
)

@Serializable
public enum class SyncSource {
    LOCAL,
    REMOTE,
    MERGED
}

package io.cloudsync.sync.operation

import kotlinx.serialization.Serializable

/**
 * Represents a single operation in the sync history log.
 *
 * Each [SyncOperationLog] entry records one atomic sync action
 * (upload, download, merge, conflict resolution, etc.) with metadata
 * for debugging, audit trails, and UI display.
 *
 * @property id Unique identifier for this operation log entry.
 * @property timestamp Epoch millis when the operation occurred.
 * @property direction The direction or type of the operation.
 * @property configId The ID of the configuration affected.
 * @property configNamespace The namespace of the configuration affected.
 * @property version The version of the configuration after the operation.
 * @property checksum Checksum of the configuration payload.
 * @property sizeBytes Size of the configuration payload in bytes.
 * @property durationMs How long the operation took in milliseconds.
 * @property result The outcome of the operation.
 * @property details Human-readable description or error details.
 * @property sourceDeviceId Identifier of the device that originated the change.
 */
@Serializable
public data class SyncOperationLog(
    val id: String,
    val timestamp: Long,
    val direction: SyncOperationDirection,
    val configId: String,
    val configNamespace: String = "",
    val version: Long = 0L,
    val checksum: String = "",
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val result: SyncOperationResult = SyncOperationResult.SUCCESS,
    val details: String = "",
    val sourceDeviceId: String = ""
)

/**
 * Sync operation direction/type.
 *
 * Describes the kind of sync action that was performed.
 * Used by [SyncOperationLog] to classify operations.
 */
@Serializable
public enum class SyncOperationDirection {
    /** Local configuration uploaded to remote storage. */
    UPLOAD,

    /** Remote configuration downloaded to local storage. */
    DOWNLOAD,

    /** Two-way merge of local and remote state. */
    MERGE,

    /** Conflict detected and resolved automatically. */
    CONFLICT_RESOLUTION,

    /** Operation resulted in an error. */
    ERROR,

    /** Manual user intervention was required. */
    MANUAL_INTERVENTION
}

/**
 * Result status for a sync operation.
 *
 * @property SUCCESS Operation completed without errors.
 * @property FAILED Operation failed with an error.
 * @property PARTIAL Operation partially succeeded.
 * @property SKIPPED Operation was skipped (e.g., no changes).
 */
@Serializable
public enum class SyncOperationResult {
    SUCCESS,
    FAILED,
    PARTIAL,
    SKIPPED
}

package io.cloudsync.sync.metadata

import io.cloudsync.domain.model.ConflictResolution
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Tracks sync metadata for observability and debugging.
 *
 * Records:
 * - Upload/download events with timestamps
 * - Conflict events with resolution strategies
 * - Sync cycle statistics
 * - Error tracking
 */
public class SyncMetadataTracker {
    private val uploadLog = mutableListOf<TransferEvent>()
    private val downloadLog = mutableListOf<TransferEvent>()
    private val conflictLog = mutableListOf<ConflictEvent>()
    private val errorLog = mutableListOf<ErrorEvent>()
    private var cycleCount = 0L

    private val maxLogSize = 1000

    /** Initialize tracker. */
    public fun initialize() { cycleCount = 0 }

    /** Reset all tracking data. */
    public fun reset() {
        uploadLog.clear()
        downloadLog.clear()
        conflictLog.clear()
        errorLog.clear()
        cycleCount = 0
    }

    /** Record an upload event. */
    public fun recordUpload(configId: String, version: Long) {
        trimLog(uploadLog)
        uploadLog.add(TransferEvent(
            configId = configId,
            version = version,
            direction = TransferDirection.UPLOAD,
            timestamp = Clock.System.now().toEpochMilliseconds()
        ))
    }

    /** Record a download event. */
    public fun recordDownload(configId: String, version: Long) {
        trimLog(downloadLog)
        downloadLog.add(TransferEvent(
            configId = configId,
            version = version,
            direction = TransferDirection.DOWNLOAD,
            timestamp = Clock.System.now().toEpochMilliseconds()
        ))
    }

    /** Record a conflict resolution event. */
    public fun recordConflict(resolution: ConflictResolution) {
        trimLog(conflictLog)
        conflictLog.add(ConflictEvent(
            configId = resolution.resolved.id,
            localVersion = resolution.localVersion.version,
            remoteVersion = resolution.remoteVersion.version,
            strategy = resolution.strategy.toString(),
            timestamp = resolution.timestamp
        ))
    }

    /** Record a sync error. */
    public fun recordError(configId: String, error: String) {
        trimLog(errorLog)
        errorLog.add(ErrorEvent(
            configId = configId,
            error = error,
            timestamp = Clock.System.now().toEpochMilliseconds()
        ))
    }

    /** Increment sync cycle counter. */
    public fun incrementCycle() { cycleCount++ }

    /** Get current sync statistics. */
    public fun getStats(): SyncStats = SyncStats(
        totalCycles = cycleCount,
        totalUploads = uploadLog.size.toLong(),
        totalDownloads = downloadLog.size.toLong(),
        totalConflicts = conflictLog.size.toLong(),
        totalErrors = errorLog.size.toLong(),
        lastUploadTimestamp = uploadLog.lastOrNull()?.timestamp,
        lastDownloadTimestamp = downloadLog.lastOrNull()?.timestamp,
        lastErrorTimestamp = errorLog.lastOrNull()?.timestamp
    )

    private fun <T> trimLog(log: MutableList<T>) {
        if (log.size >= maxLogSize) {
            log.removeAt(0)
        }
    }
}

@Serializable
public data class TransferEvent(
    val configId: String,
    val version: Long,
    val direction: TransferDirection,
    val timestamp: Long
)

public enum class TransferDirection { UPLOAD, DOWNLOAD }

@Serializable
public data class ConflictEvent(
    val configId: String,
    val localVersion: Long,
    val remoteVersion: Long,
    val strategy: String,
    val timestamp: Long
)

@Serializable
public data class ErrorEvent(
    val configId: String,
    val error: String,
    val timestamp: Long
)

@Serializable
public data class SyncStats(
    val totalCycles: Long,
    val totalUploads: Long,
    val totalDownloads: Long,
    val totalConflicts: Long,
    val totalErrors: Long,
    val lastUploadTimestamp: Long?,
    val lastDownloadTimestamp: Long?,
    val lastErrorTimestamp: Long?
)

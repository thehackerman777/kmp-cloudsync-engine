package io.cloudsync.sync.operation

import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for recording and querying sync operation history.
 *
 * Provides granular logging of every sync action (upload, download, conflict
 * resolution, errors) for audit trails, debugging, and the enhanced UI state.
 *
 * Operations are persisted in-memory with optional JSON export.
 * In a full production build, these would be backed by the SQLDelight DB.
 *
 * @property maxEntries Maximum number of operation log entries to retain.
 * @property json JSON instance for serialization.
 */
public class SyncOperationRepository(
    private val maxEntries: Int = 1000,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val _operations = MutableStateFlow<List<SyncOperationLog>>(emptyList())
    private var idCounter = 0L

    /** Observable stream of all recorded operations (newest first). */
    public val operations: Flow<List<SyncOperationLog>> = _operations.asStateFlow()

    /** Snapshot of current operations. */
    public val currentOperations: List<SyncOperationLog>
        get() = _operations.value

    /**
     * Records a new sync operation in the log.
     *
     * @param direction The type of operation performed.
     * @param configId The configuration that was operated on.
     * @param configNamespace Namespace of the configuration.
     * @param version Version after the operation.
     * @param checksum Checksum of the payload.
     * @param sizeBytes Payload size.
     * @param durationMs How long the operation took.
     * @param result The outcome.
     * @param details Optional human-readable details or error message.
     * @param sourceDeviceId Device that originated the change.
     * @return The recorded [SyncOperationLog] entry.
     */
    public fun record(
        direction: SyncOperationDirection,
        configId: String,
        configNamespace: String = "",
        version: Long = 0L,
        checksum: String = "",
        sizeBytes: Long = 0L,
        durationMs: Long = 0L,
        result: SyncOperationResult = SyncOperationResult.SUCCESS,
        details: String = "",
        sourceDeviceId: String = ""
    ): SyncOperationLog {
        val entry = SyncOperationLog(
            id = generateId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            direction = direction,
            configId = configId,
            configNamespace = configNamespace,
            version = version,
            checksum = checksum,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            result = result,
            details = details,
            sourceDeviceId = sourceDeviceId
        )
        appendEntry(entry)
        return entry
    }

    /**
     * Records a pre-built [SyncOperationLog] entry.
     */
    public fun record(entry: SyncOperationLog): SyncOperationLog {
        val stamped = if (entry.id.isEmpty()) {
            entry.copy(id = generateId())
        } else {
            entry
        }
        appendEntry(stamped)
        return stamped
    }

    /**
     * Returns the most recent entries.
     *
     * @param limit Maximum number of entries to return.
     * @param offset Number of entries to skip.
     * @param filter Optional filter by direction.
     */
    public fun getHistory(
        limit: Int = 50,
        offset: Int = 0,
        filter: SyncOperationDirection? = null
    ): List<SyncOperationLog> {
        var entries = _operations.value
        if (filter != null) {
            entries = entries.filter { it.direction == filter }
        }
        return entries.drop(offset).take(limit)
    }

    /**
     * Returns all operations for a specific configuration.
     */
    public fun getByConfigId(configId: String): List<SyncOperationLog> {
        return _operations.value.filter { it.configId == configId }
    }

    /**
     * Returns the most recent N operations.
     */
    public fun getRecent(count: Int = 10): List<SyncOperationLog> {
        return _operations.value.take(count)
    }

    /**
     * Computes aggregate statistics for operations within a time range.
     *
     * @param rangeStart Epoch millis start of range (defaults to 0 = all time).
     * @param rangeEnd Epoch millis end of range (defaults to now).
     */
    public fun getStats(
        rangeStart: Long = 0L,
        rangeEnd: Long = Clock.System.now().toEpochMilliseconds()
    ): OperationStats {
        val inRange = _operations.value.filter {
            it.timestamp in rangeStart..rangeEnd
        }
        val succeeded = inRange.count { it.result == SyncOperationResult.SUCCESS }
        val failed = inRange.count { it.result == SyncOperationResult.FAILED }
        val totalDuration = inRange.sumOf { it.durationMs }

        return OperationStats(
            totalOperations = inRange.size.toLong(),
            successfulOperations = succeeded.toLong(),
            failedOperations = failed.toLong(),
            partialOperations = inRange.count { it.result == SyncOperationResult.PARTIAL }.toLong(),
            skippedOperations = inRange.count { it.result == SyncOperationResult.SKIPPED }.toLong(),
            totalDurationMs = totalDuration,
            averageDurationMs = if (inRange.isNotEmpty()) totalDuration / inRange.size else 0L,
            uploadCount = inRange.count { it.direction == SyncOperationDirection.UPLOAD }.toLong(),
            downloadCount = inRange.count { it.direction == SyncOperationDirection.DOWNLOAD }.toLong(),
            conflictCount = inRange.count { it.direction == SyncOperationDirection.CONFLICT_RESOLUTION }.toLong(),
            errorCount = inRange.count { it.direction == SyncOperationDirection.ERROR }.toLong(),
            successRate = if (inRange.isNotEmpty()) {
                (succeeded.toFloat() / inRange.size * 100f)
            } else 100f
        )
    }

    /**
     * Clears all operation history.
     */
    public fun clear() {
        _operations.value = emptyList()
    }

    private fun appendEntry(entry: SyncOperationLog) {
        val current = _operations.value.toMutableList()
        current.add(0, entry) // newest first
        // Enforce max entries
        if (current.size > maxEntries) {
            _operations.value = current.take(maxEntries)
        } else {
            _operations.value = current
        }
    }

    private fun generateId(): String {
        val now = Clock.System.now().toEpochMilliseconds()
        idCounter++
        return "op_${now}_$idCounter"
    }
}

/**
 * Aggregate statistics for sync operations.
 *
 * @property totalOperations Total number of operations in the range.
 * @property successfulOperations Number of SUCCESS results.
 * @property failedOperations Number of FAILED results.
 * @property partialOperations Number of PARTIAL results.
 * @property skippedOperations Number of SKIPPED results.
 * @property totalDurationMs Sum of all operation durations.
 * @property averageDurationMs Average duration across all operations.
 * @property uploadCount Number of UPLOAD operations.
 * @property downloadCount Number of DOWNLOAD operations.
 * @property conflictCount Number of CONFLICT_RESOLUTION operations.
 * @property errorCount Number of ERROR operations.
 * @property successRate Percentage of operations that succeeded (0-100).
 */
@Serializable
public data class OperationStats(
    val totalOperations: Long = 0L,
    val successfulOperations: Long = 0L,
    val failedOperations: Long = 0L,
    val partialOperations: Long = 0L,
    val skippedOperations: Long = 0L,
    val totalDurationMs: Long = 0L,
    val averageDurationMs: Long = 0L,
    val uploadCount: Long = 0L,
    val downloadCount: Long = 0L,
    val conflictCount: Long = 0L,
    val errorCount: Long = 0L,
    val successRate: Float = 100f
)

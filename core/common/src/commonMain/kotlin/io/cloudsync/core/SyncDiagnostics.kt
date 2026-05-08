package io.cloudsync.core

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Comprehensive diagnostic snapshot of the sync engine.
 * Used for debugging, telemetry, and health checks.
 */
@Serializable
public data class SyncDiagnostics(
    /** Current engine state */
    val currentState: SyncState,
    /** Engine uptime in milliseconds */
    val uptimeMs: Long,
    /** Number of sync cycles completed since startup */
    val totalSyncCycles: Long,
    /** Number of successful sync cycles */
    val successfulSyncs: Long,
    /** Number of failed sync cycles */
    val failedSyncs: Long,
    /** Number of conflicts resolved */
    val conflictsResolved: Long,
    /** Timestamp of last successful sync */
    val lastSyncTimestamp: Instant?,
    /** Current network connectivity status */
    val isOnline: Boolean,
    /** OAuth token expiry timestamp */
    val tokenExpiryAt: Instant?,
    /** Local storage usage in bytes */
    val localStorageBytes: Long,
    /** Number of pending changes to upload */
    val pendingUploads: Int,
    /** Number of pending changes to download */
    val pendingDownloads: Int,
    /** Active retry count */
    val retryCount: Int,
    /** Current backoff delay in milliseconds */
    val backoffMs: Long,
    /** Sync engine version */
    val engineVersion: String = BuildConfig.VERSION
)

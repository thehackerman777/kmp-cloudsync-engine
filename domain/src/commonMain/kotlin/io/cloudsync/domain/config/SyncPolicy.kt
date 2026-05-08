package io.cloudsync.domain.config

import kotlinx.serialization.Serializable

/**
 * Global sync policy configuration for the engine.
 * Controls retry, conflict resolution, and scheduling behavior.
 */
@Serializable
public data class SyncPolicy(
    /** Maximum number of retry attempts for failed sync operations. */
    val maxRetries: Int = 5,

    /** Base delay in milliseconds for exponential backoff. */
    val baseBackoffMs: Long = 1_000,

    /** Maximum backoff delay in milliseconds. */
    val maxBackoffMs: Long = 60_000,

    /** Backoff multiplier (exponential: 2^n). */
    val backoffMultiplier: Double = 2.0,

    /** Whether to apply jitter to backoff delays. */
    val enableBackoffJitter: Boolean = true,

    /** Default conflict resolution strategy. */
    val defaultConflictStrategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS,

    /** Interval in milliseconds between background sync cycles. */
    val backgroundSyncIntervalMs: Long = 300_000, // 5 minutes

    /** Whether to enable automatic background sync. */
    val enableBackgroundSync: Boolean = true,

    /** Whether to sync on network reconnection. */
    val syncOnReconnect: Boolean = true,

    /** Connection timeout in milliseconds. */
    val connectionTimeoutMs: Long = 30_000,

    /** Read timeout in milliseconds. */
    val readTimeoutMs: Long = 60_000,

    /** Maximum payload size in bytes (default: 10 MB). */
    val maxPayloadSizeBytes: Long = 10 * 1024 * 1024,

    /** Whether to compress payloads before upload. */
    val enableCompression: Boolean = true,

    /** Whether to encrypt payloads at rest. */
    val enableEncryption: Boolean = true
)

@Serializable
public enum class ConflictStrategy {
    LAST_WRITE_WINS,
    LOCAL_PRIORITY,
    REMOTE_PRIORITY,
    MANUAL_REQUIRED
}

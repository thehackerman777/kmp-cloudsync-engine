package io.cloudsync.sync.version

import io.cloudsync.core.extension.sha256Hex
import kotlinx.datetime.Clock

/**
 * Manages version tracking for configurations.
 *
 * Implements incremental versioning with:
 * - Monotonically increasing version counters
 * - Checksum-based change detection
 * - Version history tracking
 * - Staleness detection
 */
public class VersionManager {
    private val versions = mutableMapOf<String, VersionInfo>()
    private val history = mutableMapOf<String, MutableList<VersionEntry>>()
    private val maxHistoryPerConfig = 50

    /** Initialize version state. */
    public fun initialize() { /* Load persisted state */ }

    /** Reset all version tracking. */
    public fun reset() {
        versions.clear()
        history.clear()
    }

    /**
     * Computes the next version for a configuration.
     * Monotonically increasing within each namespace.
     */
    public fun nextVersion(configId: String): Long {
        val current = versions[configId]?.version ?: 0L
        val next = current + 1
        versions[configId] = VersionInfo(
            configId = configId,
            version = next,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        return next
    }

    /**
     * Gets the current version for a configuration.
     */
    public fun getVersion(configId: String): Long {
        return versions[configId]?.version ?: 0L
    }

    /**
     * Records a new version entry in history.
     */
    public fun recordVersion(configId: String, version: Long, checksum: String) {
        val entry = VersionEntry(
            configId = configId,
            version = version,
            checksum = checksum,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        history.getOrPut(configId) { mutableListOf() }.add(entry)

        // Trim history to max size
        val list = history[configId]
        if (list != null && list.size > maxHistoryPerConfig) {
            history[configId] = list.drop(list.size - maxHistoryPerConfig).toMutableList()
        }
    }

    /**
     * Gets the version history for a configuration.
     */
    public fun getHistory(configId: String, limit: Int = 10): List<VersionEntry> {
        return history[configId]?.takeLast(limit) ?: emptyList()
    }

    /**
     * Detects if a configuration has changed based on checksum.
     */
    public fun hasChanged(configId: String, newChecksum: String): Boolean {
        return versions[configId]?.checksum != newChecksum
    }

    /**
     * Determines if remote version is newer than local.
     */
    public fun isRemoteNewer(configId: String, remoteVersion: Long): Boolean {
        return remoteVersion > getVersion(configId)
    }
}

/**
 * Version information for a single configuration.
 */
public data class VersionInfo(
    val configId: String,
    val version: Long,
    val checksum: String = "",
    val timestamp: Long
)

/**
 * Historical entry for version tracking.
 */
public data class VersionEntry(
    val configId: String,
    val version: Long,
    val checksum: String,
    val timestamp: Long
)

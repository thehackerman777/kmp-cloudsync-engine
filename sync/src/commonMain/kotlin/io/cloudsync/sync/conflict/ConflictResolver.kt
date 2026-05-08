package io.cloudsync.sync.conflict

import io.cloudsync.core.extension.sha256Hex
import io.cloudsync.domain.model.ConflictResolution
import io.cloudsync.domain.model.ConflictStrategy
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.SyncMetadata
import io.cloudsync.domain.model.SyncSource
import kotlinx.datetime.Clock

/**
 * Resolves synchronization conflicts between local and remote configurations.
 *
 * Strategies:
 * - LAST_WRITE_WINS: The configuration with the latest timestamp wins.
 * - LOCAL_PRIORITY: Always prefer the local version.
 * - REMOTE_PRIORITY: Always prefer the remote version.
 * - MANUAL_REQUIRED: Flag for user intervention (future: UI component).
 */
public class ConflictResolver(
    private val defaultStrategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS
) {
    /**
     * Resolves a conflict between local and remote configurations.
     *
     * @param local The local version of the configuration.
     * @param remote The remote version of the configuration.
     * @param strategy The strategy to use (defaults to class default).
     * @return The resolved [ConflictResolution].
     */
    public fun resolve(
        local: Configuration,
        remote: Configuration,
        strategy: ConflictStrategy = defaultStrategy
    ): ConflictResolution {
        val resolved = when (strategy) {
            ConflictStrategy.LAST_WRITE_WINS -> resolveLWW(local, remote)
            ConflictStrategy.LOCAL_PRIORITY -> local
            ConflictStrategy.REMOTE_PRIORITY -> remote
            ConflictStrategy.MANUAL_REQUIRED -> local // Flag, no auto-resolve
        }

        return ConflictResolution(
            localVersion = local,
            remoteVersion = remote,
            resolved = resolved,
            strategy = strategy,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    /**
     * Last Write Wins resolution: compare timestamps.
     * If they have the same checksum, no conflict.
     */
    private fun resolveLWW(local: Configuration, remote: Configuration): Configuration {
        // Same content → no conflict, just return local
        if (local.contentEquals(remote)) return local

        return if (local.updatedAt >= remote.updatedAt) {
            local.copy(version = maxOf(local.version, remote.version) + 1)
        } else {
            remote.copy(version = maxOf(local.version, remote.version) + 1)
        }
    }
}

/**
 * Detects whether two configurations are in conflict.
 */
public class ConflictDetector {
    /**
     * Returns true if the local and remote configurations conflict.
     * Conflict = different checksums AND both modified after last sync.
     */
    public fun isConflict(local: Configuration, remote: Configuration, lastSyncVersion: Long): Boolean {
        if (local.contentEquals(remote)) return false
        return local.version > lastSyncVersion && remote.version > lastSyncVersion
    }
}

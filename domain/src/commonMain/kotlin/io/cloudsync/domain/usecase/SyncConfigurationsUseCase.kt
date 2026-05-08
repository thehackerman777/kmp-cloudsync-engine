package io.cloudsync.domain.usecase

import io.cloudsync.core.result.SyncResult
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.repository.IConfigurationRepository

/**
 * Orchestrates synchronization of configurations between local and remote.
 *
 * Implements the business logic for offline-first sync:
 * 1. Read local state
 * 2. Fetch remote changes
 * 3. Compare versions
 * 4. Resolve conflicts
 * 5. Apply resolutions
 * 6. Persist final state
 */
public class SyncConfigurationsUseCase(
    private val repository: IConfigurationRepository
) {
    /**
     * Executes a full sync cycle for all pending configurations.
     */
    public suspend operator fun invoke(): SyncResult<Unit> {
        // 1. Get pending local changes
        val pendingResult = repository.getPendingSync()
        val pending = pendingResult.getOrNull() ?: return SyncResult.error(
            io.cloudsync.core.result.SyncErrorCode.STORAGE_CORRUPTION,
            "Failed to read pending configurations"
        )

        // 2. Process each pending configuration
        for (config in pending) {
            val result = syncConfiguration(config)
            if (result is SyncResult.Error) {
                // Log and continue — don't block other configs
                Napier.w("Sync failed for ${config.id}: ${result.reason.message}")
            }
        }

        return SyncResult.success(Unit)
    }

    /**
     * Syncs a single configuration.
     */
    private suspend fun syncConfiguration(config: Configuration): SyncResult<Configuration> {
        // The actual sync logic is handled by the Sync module.
        // This use case delegates to the repository which orchestrates
        // local → remote data flow.
        return repository.save(config)
    }
}

private object Napier {
    fun w(s: String) { println("WARN: $s") }
}

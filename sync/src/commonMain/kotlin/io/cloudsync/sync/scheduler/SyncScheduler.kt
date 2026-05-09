package io.cloudsync.sync.scheduler

import io.cloudsync.sync.SyncOrchestrator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Background sync scheduler with configurable intervals and
 * network-aware triggering.
 *
 * Scheduling strategies:
 * - Periodic: Fixed interval between sync cycles
 * - Adaptive: Adjusts interval based on change frequency
 * - On-demand: Triggered by external events (network change, data mutation)
 */
public class SyncScheduler(
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val adaptiveEnabled: Boolean = true
) {
    private var job: Job? = null
    private var currentIntervalMs = intervalMs
    private var consecutiveEmptySyncs = 0

    /**
     * Starts the background sync loop.
     */
    public fun start(scope: CoroutineScope) {
        job = scope.launch {
            while (isActive) {
                delay(currentIntervalMs)
                // The actual sync is triggered by the orchestrator
                // The scheduler just manages timing
            }
        }
    }

    /**
     * Stops the scheduler.
     */
    public fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * Called after each sync cycle to adapt the interval.
     */
    public fun onSyncCompleted(hasChanges: Boolean) {
        if (!adaptiveEnabled) return

        if (!hasChanges) {
            consecutiveEmptySyncs++
            // Gradually increase interval when idle (max 30 min)
            currentIntervalMs = minOf(
                currentIntervalMs * 2,
                MAX_INTERVAL_MS
            )
        } else {
            consecutiveEmptySyncs = 0
            currentIntervalMs = intervalMs // Reset to base interval
        }
    }

    public companion object {
        private const val DEFAULT_INTERVAL_MS = 300_000L // 5 minutes
        private const val MAX_INTERVAL_MS = 1_800_000L // 30 minutes
    }
}

/**
 * Injectable trigger for on-demand sync.
 */
public class SyncTrigger {
    private val _triggers = MutableSharedFlow<SyncTriggerEvent>(extraBufferCapacity = 16)

    /** Observable trigger events. */
    public val events: Flow<SyncTriggerEvent> = _triggers.asSharedFlow()

    /** Triggers a sync on network reconnection. */
    public suspend fun onNetworkAvailable() {
        _triggers.emit(SyncTriggerEvent.NetworkAvailable)
    }

    /** Triggers a sync on local data mutation. */
    public suspend fun onLocalDataChanged() {
        _triggers.emit(SyncTriggerEvent.LocalDataChanged)
    }

    /** Triggers a periodic sync. */
    public suspend fun onTimerElapsed() {
        _triggers.emit(SyncTriggerEvent.TimerElapsed)
    }
}

public sealed class SyncTriggerEvent {
    public data object NetworkAvailable : SyncTriggerEvent()
    public data object LocalDataChanged : SyncTriggerEvent()
    public data object TimerElapsed : SyncTriggerEvent()
}

package io.cloudsync.sync

import io.cloudsync.core.SyncState
import io.cloudsync.core.result.SyncError
import io.cloudsync.core.result.SyncErrorCode
import io.cloudsync.core.result.SyncResult
import io.cloudsync.sync.SyncConfiguration
import io.cloudsync.sync.engine.SyncEngine
import io.cloudsync.sync.scheduler.SyncScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * High-level sync orchestrator that manages the full sync lifecycle.
 *
 * Responsibilities:
 * - Initialize all sync components
 * - Manage sync state machine transitions
 * - Coordinate background sync scheduling
 * - Handle error recovery and retry logic
 * - Provide observable sync events
 *
 * This is the main entry point for sync operations used by [CloudSyncEngine].
 */
public class SyncOrchestrator(
    private val syncEngine: SyncEngine,
    private val syncScheduler: SyncScheduler,
    private val scope: CoroutineScope,
    private val config: SyncConfig = SyncConfig()
) {
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private val _syncResults = MutableSharedFlow<SyncResult<Unit>>(replay = 0, extraBufferCapacity = 64)
    private val _syncEvents = MutableSharedFlow<SyncEvent>(replay = 10, extraBufferCapacity = 128)

    private var syncJob: Job? = null
    private var schedulerJob: Job? = null

    /** Observable sync state. */
    public val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Observable sync results stream. */
    public val syncResults: Flow<SyncResult<Unit>> = _syncResults.asSharedFlow()

    /** Observable sync events for detailed telemetry. */
    public val syncEvents: Flow<SyncEvent> = _syncEvents.asSharedFlow()

    /**
     * Starts the sync orchestrator.
     * Triggers initial sync and begins background scheduling.
     */
    public suspend fun start(): SyncResult {
        _syncState.value = SyncState.INITIALIZING

        syncEngine.initialize()

        // Initial sync cycle
        _syncState.value = SyncState.SYNCING
        val result = runSyncCycle()

        if (result is SyncResult.Success) {
            _syncState.value = SyncState.IDLE
            // Start background scheduler
            if (config.enableBackgroundSync) {
                startBackgroundSync()
            }
            _syncEvents.emit(SyncEvent.Started)
        } else {
            _syncState.value = SyncState.ERROR
            _syncEvents.emit(SyncEvent.Error((result as SyncResult.Error).reason))
        }

        return result
    }

    /**
     * Stops the sync orchestrator gracefully.
     */
    public suspend fun stop() {
        _syncState.value = SyncState.SHUTTING_DOWN
        schedulerJob?.cancel()
        syncJob?.cancel()
        syncEngine.shutdown()
        _syncState.value = SyncState.IDLE
        _syncEvents.emit(SyncEvent.Stopped)
    }

    /**
     * Triggers an immediate manual sync cycle.
     */
    public suspend fun syncNow(): SyncResult {
        if (_syncState.value == SyncState.SYNCING) {
            return SyncResult.error(SyncErrorCode.SYNC_CYCLE_IN_PROGRESS, "Sync already in progress")
        }

        _syncState.value = SyncState.SYNCING
        val result = runSyncCycle()
        _syncState.value = if (result is SyncResult.Success) SyncState.IDLE else SyncState.ERROR
        return result
    }

    /**
     * Resets engine state.
     */
    public suspend fun reset() {
        syncJob?.cancel()
        schedulerJob?.cancel()
        syncEngine.reset()
        _syncState.value = SyncState.IDLE
    }

    private suspend fun runSyncCycle(): SyncResult {
        return try {
            _syncEvents.emit(SyncEvent.CycleStarted)

            // 1. Upload pending local changes
            val uploadResult = syncEngine.uploadChanges()
            if (uploadResult is SyncResult.Error && !uploadResult.reason.retryable) {
                _syncEvents.emit(SyncEvent.Error(uploadResult.reason))
                return uploadResult
            }

            // 2. Download remote changes
            val downloadResult = syncEngine.downloadChanges()
            if (downloadResult is SyncResult.Error && !downloadResult.reason.retryable) {
                _syncEvents.emit(SyncEvent.Error(downloadResult.reason))
                return downloadResult
            }

            // 3. Resolve any conflicts
            val conflictResult = syncEngine.resolveConflicts()
            if (conflictResult is SyncResult.Error) {
                _syncEvents.emit(SyncEvent.Error(conflictResult.reason))
            }

            _syncEvents.emit(SyncEvent.CycleCompleted)
            _syncResults.emit(conflictResult)
            conflictResult
        } catch (e: CancellationException) {
            _syncEvents.emit(SyncEvent.Cancelled)
            SyncResult.Cancelled
        } catch (e: Exception) {
            val error = SyncError(SyncErrorCode.INTERNAL_ERROR, "Sync cycle failed", e)
            _syncEvents.emit(SyncEvent.Error(error))
            _syncResults.emit(SyncResult.Error(error))
            SyncResult.Error(error)
        }
    }

    private fun startBackgroundSync() {
        schedulerJob = scope.launch {
            syncScheduler.start(this)
        }
    }
}

/**
 * Sync event types for observability.
 */
public sealed class SyncEvent {
    public data object Started : SyncEvent()
    public data object Stopped : SyncEvent()
    public data object CycleStarted : SyncEvent()
    public data object CycleCompleted : SyncEvent()
    public data object Cancelled : SyncEvent()
    public data class Error(val reason: SyncError) : SyncEvent()
    public data class Progress(val percentage: Float, val description: String) : SyncEvent()
    public data class Conflict(val configId: String, val localVersion: Long, val remoteVersion: Long) : SyncEvent()
}

/**
 * Sync configuration for the orchestrator.
 */
public data class SyncConfiguration(
    val enableBackgroundSync: Boolean = true,
    val backgroundIntervalMs: Long = 300_000L,
    val enableRealtimeSync: Boolean = false,
    val maxConcurrentOperations: Int = 3,
    val autoResolveConflicts: Boolean = true,
    val enableChangeTracking: Boolean = true,
    val syncOnStartup: Boolean = true
)

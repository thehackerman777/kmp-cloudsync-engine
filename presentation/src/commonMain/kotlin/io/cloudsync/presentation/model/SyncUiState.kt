package io.cloudsync.presentation.model

import io.cloudsync.core.SyncState
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.SyncMetadata

/**
 * UI models for presentation layer.
 *
 * These are platform-agnostic and can be consumed by
 * Android Compose, Desktop Compose, or Web UI.
 *
 * Extended with account profiles, operation history, and enhanced
 * diagnostics for better user-facing sync status display.
 */
public data class SyncUiState(
    val engineState: SyncState = SyncState.IDLE,
    val configurations: List<ConfigurationUiModel> = emptyList(),
    val syncProgress: Float = 0f,
    val lastSyncTime: String = "",
    val pendingChanges: Int = 0,
    val isOnline: Boolean = false,
    val error: String? = null,
    val diagnostics: SyncDiagnosticsUi? = null,

    // === Extended UI State (CloudSync UX improvements) ===

    /** List of provider account profiles with visual indicators. */
    val accounts: List<UiAccountProfile> = emptyList(),

    /** List of recent sync operations for history display. */
    val operations: List<UiSyncOperation> = emptyList(),

    /** The currently running sync operation, if any. */
    val currentOperation: UiSyncOperation? = null,

    /** Human-readable explanation of the current sync state. */
    val syncExplanation: String = "",

    /** Formatted result of the last full sync (e.g., "✅ 2 configuraciones sincronizadas, 1 conflicto resuelto"). */
    val lastFullSyncResult: String? = null
)

/**
 * UI model for a single configuration entry in a list/row.
 */
public data class ConfigurationUiModel(
    val id: String,
    val namespace: String,
    val version: Long,
    val isSynced: Boolean,
    val lastModified: String,
    val size: String
)

/**
 * Enhanced diagnostics UI model.
 *
 * Extends the basic diagnostics with operation-level metrics
 * for richer status display and debugging.
 */
public data class SyncDiagnosticsUi(
    val totalSyncs: Long,
    val totalConflicts: Long,
    val totalErrors: Long,
    val localStorageUsage: String,

    // === Extended Diagnostics ===

    /** Number of failed operations since startup. */
    val failedOperations: Long = 0L,

    /** Average duration of sync operations in milliseconds. */
    val avgDurationMs: Long = 0L,

    /** Success rate as a percentage (0.0 - 100.0). */
    val successRate: Float = 100f,

    /** Number of completed upload operations. */
    val totalUploads: Long = 0L,

    /** Number of completed download operations. */
    val totalDownloads: Long = 0L,

    /** Number of active account profiles. */
    val activeAccountCount: Int = 0,

    /** Connection status for each provider. */
    val providerStatuses: Map<String, String> = emptyMap()
)

package io.cloudsync.presentation.model

import io.cloudsync.core.SyncState
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.SyncMetadata

/**
 * UI models for presentation layer.
 *
 * These are platform-agnostic and can be consumed by
 * Android Compose, Desktop Compose, or Web UI.
 */
public data class SyncUiState(
    val engineState: SyncState = SyncState.IDLE,
    val configurations: List<ConfigurationUiModel> = emptyList(),
    val syncProgress: Float = 0f,
    val lastSyncTime: String = "",
    val pendingChanges: Int = 0,
    val isOnline: Boolean = false,
    val error: String? = null,
    val diagnostics: SyncDiagnosticsUi? = null
)

public data class ConfigurationUiModel(
    val id: String,
    val namespace: String,
    val version: Long,
    val isSynced: Boolean,
    val lastModified: String,
    val size: String
)

public data class SyncDiagnosticsUi(
    val totalSyncs: Long,
    val totalConflicts: Long,
    val totalErrors: Long,
    val localStorageUsage: String
)

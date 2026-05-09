package io.cloudsync.presentation.model

import io.cloudsync.sync.operation.SyncOperationDirection
import io.cloudsync.sync.operation.SyncOperationLog
import io.cloudsync.sync.operation.SyncOperationResult
import io.cloudsync.sync.explanation.SyncExplanationGenerator

/**
 * UI-ready representation of a sync operation log entry.
 *
 * Transforms a domain [SyncOperationLog] into display-friendly properties
 * for rendering in lists, timelines, or notification banners.
 *
 * @property summary Human-readable description of the operation (e.g., "Subido configuración 'app-settings' v3").
 * @property directionIcon Platform-agnostic icon token representing the operation direction.
 * @property timestampFormatted Human-readable time representation.
 * @property resultBadge Visual badge for the operation result.
 */
public data class UiSyncOperation(
    val id: String,
    val summary: String,
    val directionIcon: String,
    val timestampFormatted: String,
    val resultBadge: UiResultBadge
) {
    public companion object {
        /**
         * Maps a [SyncOperationDirection] to an icon token string.
         */
        private fun directionToIcon(direction: SyncOperationDirection): String = when (direction) {
            SyncOperationDirection.UPLOAD -> "arrow_upward"
            SyncOperationDirection.DOWNLOAD -> "arrow_downward"
            SyncOperationDirection.MERGE -> "sync"
            SyncOperationDirection.CONFLICT_RESOLUTION -> "handshake"
            SyncOperationDirection.ERROR -> "error"
            SyncOperationDirection.MANUAL_INTERVENTION -> "person"
        }

        /**
         * Creates a [UiSyncOperation] from a domain [SyncOperationLog].
         */
        public fun fromDomain(operation: SyncOperationLog): UiSyncOperation {
            return UiSyncOperation(
                id = operation.id,
                summary = SyncExplanationGenerator.explainOperation(operation),
                directionIcon = directionToIcon(operation.direction),
                timestampFormatted = formatTimestamp(operation.timestamp),
                resultBadge = UiResultBadge.fromResult(operation.result)
            )
        }

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "Ahora"
                diff < 3_600_000 -> "Hace ${diff / 60_000} min"
                diff < 86_400_000 -> "Hace ${diff / 3_600_000} h"
                diff < 604_800_000 -> "Hace ${diff / 86_400_000} d\u00edas"
                else -> {
                    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
                    instant.toString().substringBefore("T")
                }
            }
        }
    }
}

/**
 * Visual badge for displaying sync operation results in the UI.
 *
 * @property label Human-readable result text.
 * @property colorRes Resource token for the badge color.
 * @property isError Whether this result indicates a failure.
 */
public data class UiResultBadge(
    val label: String,
    val colorRes: String,
    val isError: Boolean = false
) {
    public companion object {
        /**
         * Maps a [SyncOperationResult] to a [UiResultBadge].
         */
        public fun fromResult(result: SyncOperationResult): UiResultBadge = when (result) {
            SyncOperationResult.SUCCESS -> UiResultBadge(
                label = "\u2713",
                colorRes = "badge_green",
                isError = false
            )
            SyncOperationResult.FAILED -> UiResultBadge(
                label = "\u2717",
                colorRes = "badge_red",
                isError = true
            )
            SyncOperationResult.PARTIAL -> UiResultBadge(
                label = "\u26A0",
                colorRes = "badge_yellow",
                isError = false
            )
            SyncOperationResult.SKIPPED -> UiResultBadge(
                label = "\u2014",
                colorRes = "badge_gray",
                isError = false
            )
        }
    }
}

package io.cloudsync.sync.explanation

import io.cloudsync.core.result.SyncResult
import io.cloudsync.sync.operation.SyncOperationDirection
import io.cloudsync.sync.operation.SyncOperationLog
import io.cloudsync.sync.operation.SyncOperationResult
import io.cloudsync.sync.operation.OperationStats

/**
 * Utility for generating human-readable explanations of sync results.
 *
 * Transforms technical [SyncResult] and [SyncOperationLog] data into
 * natural-language summaries suitable for display in UI status messages,
 * notifications, or diagnostic logs.
 *
 * ## Usage
 * ```kotlin
 * val explanation = SyncExplanationGenerator.explain(result, operations)
 * // "✅ Sincronización completada. Se subieron 3 cambios locales..."
 * ```
 */
public object SyncExplanationGenerator {

    /**
     * Generates a human-readable explanation of a full sync result.
     *
     * @param result The overall sync result.
     * @param operations The list of operation logs from this sync cycle.
     * @param configCount Total number of configurations involved (optional).
     * @param totalDurationMs Total sync cycle duration.
     * @return A formatted, human-readable string.
     */
    public fun explain(
        result: SyncResult<Unit>,
        operations: List<SyncOperationLog>,
        configCount: Int? = null,
        totalDurationMs: Long = 0L
    ): String {
        return when (result) {
            is SyncResult.Success -> formatSuccess(operations, configCount, totalDurationMs)
            is SyncResult.Error -> formatFailure(operations, result.reason.message, totalDurationMs)
            is SyncResult.Cancelled -> "\u26A0\uFE0F Sincronizaci\u00f3n cancelada."
            is SyncResult.Loading -> "\uD83D\uDD04 Sincronizando..."
        }
    }

    /**
     * Generates a concise one-line summary of the last sync.
     */
    public fun quickSummary(
        operations: List<SyncOperationLog>,
        totalDurationMs: Long = 0L
    ): String {
        val uploads = operations.count { it.direction == SyncOperationDirection.UPLOAD }
        val downloads = operations.count { it.direction == SyncOperationDirection.DOWNLOAD }
        val conflicts = operations.count { it.direction == SyncOperationDirection.CONFLICT_RESOLUTION }
        val errors = operations.count { it.result == SyncOperationResult.FAILED }

        val parts = mutableListOf<String>()
        if (uploads > 0) parts.add("${uploads} subida${if (uploads != 1) "s" else ""}")
        if (downloads > 0) parts.add("${downloads} descarga${if (downloads != 1) "s" else ""}")
        if (conflicts > 0) parts.add("${conflicts} conflicto${if (conflicts != 1) "s" else ""} resuelto${if (conflicts != 1) "s" else ""}")

        val durationStr = if (totalDurationMs > 0) formatDuration(totalDurationMs) else ""

        val summary = parts.joinToString(", ").ifEmpty { "Sin cambios" }
        val durationPart = if (durationStr.isNotEmpty()) " ($durationStr)" else ""
        val errorPart = if (errors > 0) " \u26A0\uFE0F $errors error${if (errors != 1) "es" else ""}" else ""

        return if (errors > 0) {
            "\u26A0\uFE0F $summary$errorPart$durationPart"
        } else {
            "\u2705 $summary$durationPart"
        }
    }

    /**
     * Generates an explanation for a single operation.
     *
     * @param operation The operation log entry.
     * @return Human-readable description like "Subido configuración 'app-settings' v3"
     */
    public fun explainOperation(operation: SyncOperationLog): String {
        val action = when (operation.direction) {
            SyncOperationDirection.UPLOAD -> "Subido"
            SyncOperationDirection.DOWNLOAD -> "Descargado"
            SyncOperationDirection.MERGE -> "Sincronizado"
            SyncOperationDirection.CONFLICT_RESOLUTION -> "Conflicto resuelto en"
            SyncOperationDirection.ERROR -> "Error en"
            SyncOperationDirection.MANUAL_INTERVENTION -> "Intervenci\u00f3n manual en"
        }

        val name = if (operation.configNamespace.isNotEmpty()) {
            "'${operation.configNamespace}/${operation.configId}'"
        } else {
            "'${operation.configId}'"
        }

        val version = if (operation.version > 0) " v${operation.version}" else ""
        val resultStr = when (operation.result) {
            SyncOperationResult.FAILED -> " \u2014 fall\u00f3"
            SyncOperationResult.PARTIAL -> " \u2014 parcial"
            SyncOperationResult.SKIPPED -> " \u2014 omitido"
            else -> ""
        }
        val durationStr = if (operation.durationMs > 0) {
            " (${operation.durationMs}ms)"
        } else ""

        return "$action $name$version$resultStr$durationStr"
    }

    /**
     * Generates a detailed breakdown of sync operations by phase.
     *
     * @param stats Aggregate statistics for the sync cycle.
     * @return A formatted multi-line explanation.
     */
    public fun explainStats(stats: OperationStats): String {
        val lines = mutableListOf<String>()

        if (stats.uploadCount > 0) {
            lines.add("Se subieron ${stats.uploadCount} cambio${if (stats.uploadCount != 1L) "s" else ""} locales")
        }
        if (stats.downloadCount > 0) {
            lines.add("Se descargaron ${stats.downloadCount} cambio${if (stats.downloadCount != 1L) "s" else ""} remotos")
        }
        if (stats.conflictCount > 0) {
            lines.add("Se resolvieron ${stats.conflictCount} conflicto${if (stats.conflictCount != 1L) "s" else ""}")
        }

        if (stats.failedOperations > 0 || stats.errorCount > 0) {
            lines.add("${stats.failedOperations} operacione${if (stats.failedOperations != 1L) "s" else ""} fallida${if (stats.failedOperations != 1L) "s" else ""}")
        }

        val durationStr = if (stats.totalDurationMs > 0) formatDuration(stats.totalDurationMs) else ""

        val prefix = when {
            stats.failedOperations > 0 -> "\u26A0\uFE0F "
            stats.successRate == 100f -> "\u2705 "
            else -> "\u2705 "
        }

        val base = if (lines.isEmpty()) "Sin cambios"
        else lines.joinToString(". ")

        return if (durationStr.isNotEmpty()) {
            "$prefix$base. Duraci\u00f3n total: $durationStr."
        } else {
            "$prefix$base."
        }
    }

    private fun formatSuccess(
        operations: List<SyncOperationLog>,
        configCount: Int?,
        totalDurationMs: Long
    ): String {
        val uploads = operations.filter { it.direction == SyncOperationDirection.UPLOAD }
        val downloads = operations.filter { it.direction == SyncOperationDirection.DOWNLOAD }
        val conflicts = operations.filter { it.direction == SyncOperationDirection.CONFLICT_RESOLUTION }
        val errors = operations.filter { it.result == SyncOperationResult.FAILED }

        val parts = mutableListOf<String>()

        if (uploads.isNotEmpty()) {
            val names = uploads.take(3).joinToString(", ") {
                buildConfigPath(it.configNamespace, it.configId)
            }
            val suffix = if (uploads.size > 3) " y ${uploads.size - 3} m\u00e1s" else ""
            parts.add("Se subieron ${uploads.size} cambio${if (uploads.size != 1) "s" else ""} locales ($names$suffix)")
        }

        if (downloads.isNotEmpty()) {
            val names = downloads.take(3).joinToString(", ") {
                buildConfigPath(it.configNamespace, it.configId)
            }
            val suffix = if (downloads.size > 3) " y ${downloads.size - 3} m\u00e1s" else ""
            parts.add("Se descargaron ${downloads.size} cambio${if (downloads.size != 1) "s" else ""} remotos ($names$suffix)")
        }

        if (conflicts.isNotEmpty()) {
            val conflictDetails = conflicts.take(2).joinToString(", ") {
                val strategyInfo = if (it.details.contains("strategy:")) {
                    val strategy = it.details.substringAfter("strategy:").substringBefore(",").trim()
                    " (estrategia: $strategy)"
                } else ""
                "${buildConfigPath(it.configNamespace, it.configId)}$strategyInfo"
            }
            val suffix = if (conflicts.size > 2) " y ${conflicts.size - 2} m\u00e1s" else ""
            parts.add("Se resolvi\u00f3${if (conflicts.size != 1) "n" else ""} ${conflicts.size} conflicto${if (conflicts.size != 1) "s" else ""} en $conflictDetails$suffix")
        }

        if (errors.isNotEmpty()) {
            parts.add("${errors.size} operacione${if (errors.size != 1) "s" else ""} con error${if (errors.size != 1) "es" else ""}")
        }

        if (parts.isEmpty()) {
            return "\u2705 Sincronizaci\u00f3n completada. Sin cambios."
        }

        val durationStr = if (totalDurationMs > 0) formatDuration(totalDurationMs) else ""

        val body = parts.joinToString(". ")
        val durationPart = if (durationStr.isNotEmpty()) " Duraci\u00f3n total: $durationStr." else ""
        return "\u2705 Sincronizaci\u00f3n completada. $body$durationPart"
    }

    private fun formatFailure(
        operations: List<SyncOperationLog>,
        errorMessage: String,
        totalDurationMs: Long
    ): String {
        val succeeded = operations.count { it.result == SyncOperationResult.SUCCESS }
        val failed = operations.count { it.result == SyncOperationResult.FAILED }

        val partialProgress = if (succeeded > 0) {
            " Se completaron $succeeded operacione${if (succeeded != 1) "s" else ""} antes del error."
        } else ""

        val durationStr = if (totalDurationMs > 0) formatDuration(totalDurationMs) else ""

        val durationPart = if (durationStr.isNotEmpty()) " ($durationStr)" else ""
        return "\u274C Error de sincronizaci\u00f3n: $errorMessage.$partialProgress$durationPart"
    }

    private fun buildConfigPath(namespace: String, configId: String): String {
        return if (namespace.isNotEmpty()) "'$namespace/$configId'"
        else "'$configId'"
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     */
    private fun formatDuration(ms: Long): String {
        return when {
            ms < 1_000 -> "${ms}ms"
            ms < 10_000 -> {
                val secs = ms / 1000
                val millis = (ms % 1000) / 100
                "${secs}.${millis}s"
            }
            else -> {
                val secs = ms / 1000
                "${secs}s"
            }
        }
    }
}

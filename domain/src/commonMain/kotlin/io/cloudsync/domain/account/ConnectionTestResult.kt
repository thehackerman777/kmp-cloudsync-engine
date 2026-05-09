package io.cloudsync.domain.account

import io.cloudsync.core.result.SyncError
import kotlinx.serialization.Serializable

/**
 * Result of a provider connection test.
 *
 * Used by [AccountManager.testConnection] and [io.cloudsync.sync.engine.SyncEngine.checkConnectivity]
 * to report detailed diagnostics about network reachability and authentication validity
 * for a specific provider.
 *
 * @see ProviderConnectionHealth for the ongoing health status.
 */
@Serializable
public sealed class ConnectionTestResult {

    /**
     * The connection test succeeded.
     *
     * @property latencyMs Round-trip latency in milliseconds for the test request.
     */
    public data class Success(val latencyMs: Long) : ConnectionTestResult()

    /**
     * The connection test failed.
     *
     * @property error Structured error with code and message.
     * @property recoverable Whether the engine can attempt automatic recovery
     *                       (e.g., token refresh vs. manual re-authentication).
     */
    public data class Failure(
        val error: SyncError,
        val recoverable: Boolean = false
    ) : ConnectionTestResult()

    /**
     * Convenience property to check if the test passed.
     */
    public val isSuccess: Boolean get() = this is Success
}

package io.cloudsync.domain.account

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Describes the health status of a connection to a cloud provider.
 *
 * This sealed hierarchy allows callers to pattern-match on the
 * connection state and take appropriate action (e.g., show a warning
 * if the token is expiring soon, or guide the user to re-authenticate
 * if the connection is revoked).
 */
@Serializable
public sealed class ProviderConnectionHealth {

    /**
     * The provider connection is healthy and operational.
     */
    public data object Connected : ProviderConnectionHealth()

    /**
     * The access token will expire at [expiresAt].
     * Background refresh should be attempted soon.
     */
    public data class ExpiringSoon(
        val expiresAt: Instant
    ) : ProviderConnectionHealth()

    /**
     * The provider connection is no longer valid.
     *
     * @param reason Human-readable explanation of the disconnection cause.
     * @param isRetryable Whether the engine can attempt automatic reconnection.
     */
    public data class Disconnected(
        val reason: String,
        val isRetryable: Boolean = false
    ) : ProviderConnectionHealth()

    /**
     * No provider has been configured yet.
     */
    public data object Unconfigured : ProviderConnectionHealth()
}

package io.cloudsync.auth.token

import io.cloudsync.auth.oauth2.TokenResponse
import io.cloudsync.core.extension.isExpired
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Thread-safe token manager providing access to OAuth2 tokens.
 *
 * Stores tokens in-memory for fast access during the session lifecycle.
 * Persistence is delegated to [io.cloudsync.auth.secure.SecureStorage].
 */
public class TokenProvider {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAt: Long = 0L
    private var scope: String = ""

    /**
     * Returns the current access token value.
     */
    public fun getAccessToken(): AccessToken? {
        return accessToken?.let { AccessToken(it, expiresAt) }
    }

    /**
     * Returns the current refresh token value.
     */
    public fun getRefreshToken(): RefreshToken? {
        return refreshToken?.let { RefreshToken(it) }
    }

    /**
     * Returns true if the current access token is expired.
     */
    public fun isTokenExpired(): Boolean {
        return accessToken == null || Clock.System.now().toEpochMilliseconds().isExpired(expiresAt)
    }

    /**
     * Stores tokens from an OAuth2 token response.
     */
    public fun storeTokens(response: TokenResponse) {
        accessToken = response.accessToken
        refreshToken = response.refreshToken.ifBlank { refreshToken }
        expiresAt = Clock.System.now().toEpochMilliseconds() + (response.expiresIn * 1000)
        scope = response.scope
    }

    /**
     * Clears all stored tokens.
     */
    public fun clearTokens() {
        accessToken = null
        refreshToken = null
        expiresAt = 0L
        scope = ""
    }

    /**
     * Returns token diagnostics (values masked for security).
     */
    public fun diagnostics(): TokenDiagnostics = TokenDiagnostics(
        hasAccessToken = accessToken != null,
        hasRefreshToken = refreshToken != null,
        expiresAt = expiresAt,
        isExpired = isTokenExpired(),
        scope = scope
    )
}

@Serializable
public data class AccessToken(
    val value: String,
    val expiresAt: Long
)

@Serializable
public data class RefreshToken(
    val value: String
)

@Serializable
public data class TokenDiagnostics(
    val hasAccessToken: Boolean,
    val hasRefreshToken: Boolean,
    val expiresAt: Long,
    val isExpired: Boolean,
    val scope: String
)

private fun Long.isExpired(expiresAt: Long): Boolean = this >= expiresAt

package io.cloudsync.auth

import io.cloudsync.auth.oauth2.OAuth2Client
import io.cloudsync.auth.oauth2.OAuth2Config
import io.cloudsync.auth.oauth2.TokenResponse
import io.cloudsync.auth.token.AccessToken
import io.cloudsync.auth.token.RefreshToken
import io.cloudsync.auth.token.TokenProvider
import io.cloudsync.auth.secure.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Central authentication manager for the CloudSync engine.
 *
 * Manages the complete OAuth2 lifecycle:
 * - Initial authorization (browser/webview flow)
 * - Token exchange (authorization code → access + refresh)
 * - Token storage (secure encrypted storage)
 * - Token refresh (automatic on expiry)
 * - Token revocation
 *
 * Supports multiple auth providers, defaulting to Google Drive.
 */
public class AuthManager(
    private val oAuth2Client: OAuth2Client,
    private val tokenProvider: TokenProvider,
    private val secureStorage: SecureStorage,
    private val config: AuthConfig = AuthConfig()
) {
    private val _authState = MutableStateFlow(AuthState.UNAUTHENTICATED)

    /** Observable authentication state. */
    public val authState: StateFlow<AuthState> = _authState

    /** Current auth configuration. */
    public val configuration: OAuth2Config get() = oAuth2Client.configuration

    /**
     * Initiates the OAuth2 authorization flow.
     * Platform-specific: opens browser/webview for user consent.
     *
     * @return The authorization URL to present to the user.
     */
    public suspend fun authorize(): Result<String> = runCatching {
        _authState.value = AuthState.AUTHORIZING
        val authUrl = oAuth2Client.buildAuthorizationUrl()
        _authState.value = AuthState.AWAITING_CALLBACK
        authUrl
    }

    /**
     * Exchanges an authorization code for tokens.
     *
     * @param code The authorization code from the OAuth2 callback.
     */
    public suspend fun handleCallback(code: String): Result<Unit> = runCatching {
        _authState.value = AuthState.AUTHENTICATING
        val tokenResponse = oAuth2Client.exchangeCode(code)
        tokenProvider.storeTokens(tokenResponse)
        secureStorage.saveCredentials(tokenResponse)
        _authState.value = AuthState.AUTHENTICATED
    }

    /**
     * Attempts to restore authentication from stored tokens.
     * Call this on application startup.
     */
    public suspend fun restoreSession(): Boolean {
        return try {
            val stored = secureStorage.loadCredentials()
            if (stored != null) {
                tokenProvider.storeTokens(stored)
                if (tokenProvider.isTokenExpired()) {
                    val refreshed = oAuth2Client.refreshAccessToken(stored.refreshToken)
                    tokenProvider.storeTokens(refreshed)
                    secureStorage.saveCredentials(refreshed)
                }
                _authState.value = AuthState.AUTHENTICATED
                true
            } else {
                _authState.value = AuthState.UNAUTHENTICATED
                false
            }
        } catch (e: Exception) {
            Napier.e("Session restore failed", e)
            _authState.value = AuthState.ERROR
            false
        }
    }

    /**
     * Refreshes the access token explicitly.
     */
    public suspend fun refreshToken(): Result<Unit> = runCatching {
        val refreshToken = tokenProvider.getRefreshToken()
            ?: throw IllegalStateException("No refresh token available")
        val tokenResponse = oAuth2Client.refreshAccessToken(refreshToken)
        tokenProvider.storeTokens(tokenResponse)
        secureStorage.saveCredentials(tokenResponse)
    }

    /**
     * Logs out, revokes tokens, and clears stored credentials.
     */
    public suspend fun logout() {
        try {
            val accessToken = tokenProvider.getAccessToken()
            if (accessToken != null) {
                oAuth2Client.revokeToken(accessToken.value)
            }
        } catch (e: Exception) {
            Napier.w("Token revocation failed (may already be invalid)", e)
        } finally {
            tokenProvider.clearTokens()
            secureStorage.clearCredentials()
            _authState.value = AuthState.UNAUTHENTICATED
        }
    }

    /**
     * Returns diagnostic info about the current auth state.
     */
    public fun diagnostics(): AuthDiagnostics = AuthDiagnostics(
        state = _authState.value,
        hasToken = tokenProvider.getAccessToken() != null,
        tokenExpired = tokenProvider.isTokenExpired(),
        hasRefreshToken = tokenProvider.getRefreshToken() != null,
        provider = configuration.provider
    )
}

@kotlinx.serialization.Serializable
public enum class AuthState {
    UNAUTHENTICATED,
    AUTHORIZING,
    AWAITING_CALLBACK,
    AUTHENTICATING,
    AUTHENTICATED,
    ERROR
}

public data class AuthConfig(
    val autoRefreshThresholdMs: Long = 300_000, // Refresh if expiry < 5 min
    val retryOnFailure: Boolean = true,
    val maxRetries: Int = 3
)

public data class AuthDiagnostics(
    val state: AuthState,
    val hasToken: Boolean,
    val tokenExpired: Boolean?,
    val hasRefreshToken: Boolean,
    val provider: String
)

private object Napier {
    fun e(msg: String, e: Throwable) { println("ERROR: $msg - ${e.message}") }
    fun w(msg: String, e: Throwable) { println("WARN: $msg - ${e.message}") }
}

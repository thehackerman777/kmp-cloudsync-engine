package io.cloudsync.auth.provider

import io.cloudsync.auth.AuthState
import io.cloudsync.auth.oauth2.OAuth2Config
import io.cloudsync.auth.oauth2.TokenResponse
import io.cloudsync.auth.secure.SecureStorage
import io.cloudsync.auth.token.TokenProvider
import io.cloudsync.domain.identity.IdentityProvider
import io.cloudsync.domain.identity.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Mock authentication provider for development and testing.
 *
 * Allows the samples to function without real Google Cloud Console credentials.
 * Simulates the full OAuth2 lifecycle:
 * - Authorization URL generation (returns a mock URL)
 * - Authorization code exchange (returns fake tokens)
 * - Token refresh
 * - Token revocation
 * - User info retrieval (returns a fake user profile)
 *
 * Usage in development:
 * ```kotlin
 * val provider = MockAuthProvider()
 * val mockAuthManager = provider.createMockAuthManager(secureStorage)
 * mockAuthManager.authorize()       // Returns mock auth URL
 * mockAuthManager.handleCallback()  // Auto-exchanges mock code
 * val profile = mockAuthManager.getMockProfile()  // Returns fake UserProfile
 * ```
 *
 * In production, replace with the real provider (e.g., GoogleDriveAuthProvider).
 */
@OptIn(ExperimentalUuidApi::class)
public class MockAuthProvider(
    private val mockUserId: String = "mock-user-${Uuid.random().toString().take(8)}",
    private val mockEmail: String = "dev@cloudsync.example.com",
    private val mockDisplayName: String = "Dev User"
) : AuthProvider {

    override val id: String = "mock"
    override val displayName: String = "Mock Provider (Development)"
    override val priority: Int = Int.MAX_VALUE // Lowest priority

    override fun oAuth2Config(): OAuth2Config = OAuth2Config(
        clientId = "mock-client-id",
        clientSecret = "mock-client-secret",
        redirectUri = "http://localhost:8090",
        scope = "mock:dev:testing",
        provider = "mock",
        usePKCE = false
    )

    /**
     * Creates a fully functional [MockAuthManager] backed by this mock provider.
     *
     * @param secureStorage Platform-specific secure storage for persisting tokens.
     */
    public fun createMockAuthManager(secureStorage: SecureStorage): MockAuthManager {
        val mockOAuthClient = MockOAuth2Client(configuration = oAuth2Config())
        return MockAuthManager(
            mockOAuthClient = mockOAuthClient,
            mockProvider = this,
            tokenProvider = TokenProvider(),
            secureStorage = secureStorage
        )
    }

    /**
     * Returns a mock [UserProfile] for development.
     */
    public fun getMockProfile(): UserProfile = UserProfile(
        providerUserId = mockUserId,
        localUserId = mockUserId,
        email = mockEmail,
        displayName = mockDisplayName,
        avatarUrl = null,
        provider = IdentityProvider.GOOGLE,
        createdAt = Clock.System.now().toEpochMilliseconds(),
        lastAuthenticatedAt = Clock.System.now().toEpochMilliseconds()
    )

    /**
     * Validates environment for mock usage (cross-platform safe).
     * Always returns null in dev mode.
     */
    public fun validateEnvironment(): String? {
        return null
    }
}

/**
 * Mock OAuth2 client that simulates the OAuth2 flow without network calls.
 *
 * This is a standalone class (not extending [io.cloudsync.auth.oauth2.OAuth2Client])
 * that provides the same method surface for mock/dev scenarios.
 * Uses composition/delegation instead of inheritance to avoid ktor internals.
 */
@OptIn(ExperimentalUuidApi::class)
public class MockOAuth2Client(
    override val configuration: OAuth2Config
) : MockOAuth2ClientInterface {

    override fun buildAuthorizationUrl(): String {
        return "http://localhost:8090/mock-oauth/authorize?" +
            "client_id=${configuration.clientId}&" +
            "redirect_uri=${configuration.redirectUri}&" +
            "response_type=code&" +
            "scope=${configuration.scope}&" +
            "state=mock_state_${Uuid.random().toString().take(8)}"
    }

    override suspend fun exchangeCode(code: String): TokenResponse {
        // Simulate network delay
        delay(100)
        return TokenResponse(
            accessToken = "mock_access_token_${Uuid.random().toString().take(16)}",
            expiresIn = 3600L,
            refreshToken = "mock_refresh_token_${Uuid.random().toString().take(16)}",
            scope = configuration.scope,
            tokenType = "Bearer"
        )
    }

    override suspend fun refreshAccessToken(refreshToken: String): TokenResponse {
        delay(50)
        return TokenResponse(
            accessToken = "mock_access_token_refreshed_${Uuid.random().toString().take(16)}",
            expiresIn = 3600L,
            refreshToken = refreshToken,
            scope = configuration.scope,
            tokenType = "Bearer"
        )
    }

    override suspend fun revokeToken(token: String) {
        // No-op in mock mode
    }
}

/**
 * Internal interface matching the OAuth2 client surface used by tests/mocks.
 * Avoids inheritance from [io.cloudsync.auth.oauth2.OAuth2Client] which depends on ktor.
 */
public interface MockOAuth2ClientInterface {
    public val configuration: OAuth2Config
    public fun buildAuthorizationUrl(): String
    public suspend fun exchangeCode(code: String): TokenResponse
    public suspend fun refreshAccessToken(refreshToken: String): TokenResponse
    public suspend fun revokeToken(token: String)
}

/**
 * Mock AuthManager that wraps [MockOAuth2Client] and tracks auth state.
 *
 * Mirrors the API of [io.cloudsync.auth.AuthManager] for use in dev samples.
 * Unlike the real AuthManager, this does not require platform-specific OAuth
 * browser/webview flows.
 */
public class MockAuthManager(
    private val mockOAuthClient: MockOAuth2Client,
    private val mockProvider: MockAuthProvider,
    private val tokenProvider: TokenProvider,
    private val secureStorage: SecureStorage
) {
    private val _authState = MutableStateFlow(AuthState.UNAUTHENTICATED)

    /** Observable authentication state. */
    public val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Initiates the mock authorization flow and returns a mock authorization URL. */
    public suspend fun authorize(): Result<String> = runCatching {
        _authState.value = AuthState.AUTHORIZING
        val url = mockOAuthClient.buildAuthorizationUrl()
        _authState.value = AuthState.AWAITING_CALLBACK
        url
    }

    /**
     * Handles the OAuth callback with the given code.
     * In mock mode, a default code is used if none provided.
     */
    public suspend fun handleCallback(code: String = "mock_code_auto"): Result<Unit> = runCatching {
        _authState.value = AuthState.AUTHENTICATING
        val tokenResponse = mockOAuthClient.exchangeCode(code)
        tokenProvider.storeTokens(tokenResponse)
        secureStorage.saveCredentials(tokenResponse)
        _authState.value = AuthState.AUTHENTICATED
    }

    /** Attempts to restore authentication from stored tokens. */
    public suspend fun restoreSession(): Boolean {
        return try {
            val stored = secureStorage.loadCredentials()
            if (stored != null) {
                tokenProvider.storeTokens(stored)
                _authState.value = AuthState.AUTHENTICATED
                true
            } else {
                _authState.value = AuthState.UNAUTHENTICATED
                false
            }
        } catch (e: Exception) {
            _authState.value = AuthState.ERROR
            false
        }
    }

    /** Logs out and clears stored credentials. */
    public suspend fun logout() {
        tokenProvider.clearTokens()
        secureStorage.clearCredentials()
        _authState.value = AuthState.UNAUTHENTICATED
    }

    /** Returns a mock [UserProfile] for the current development session. */
    public fun getMockProfile(): UserProfile = mockProvider.getMockProfile()
}

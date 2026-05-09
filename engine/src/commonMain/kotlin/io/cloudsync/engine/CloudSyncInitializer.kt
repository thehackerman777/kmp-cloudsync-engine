package io.cloudsync.engine

import io.cloudsync.auth.provider.MockAuthManager
import io.cloudsync.auth.provider.MockAuthProvider
import io.cloudsync.auth.secure.SecureStorage
import io.cloudsync.core.result.SyncErrorCode
import io.cloudsync.core.result.SyncResult
import io.cloudsync.domain.identity.DevicePlatform
import io.cloudsync.domain.identity.IdentityManager
import io.cloudsync.domain.identity.IdentityResult
import io.cloudsync.domain.repository.IConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Engine initializer that handles the full bootstrap:
 * 1. Identity (device + user)
 * 2. Authentication (OAuth or mock)
 * 3. Engine configuration
 *
 * Provides a unified entry point so that samples don't need to
 * manually coordinate identity + auth + engine setup.
 *
 * @param identityManager Manages device and user identity.
 * @param configurationRepository Persistence layer for configuration data.
 * @param secureStorage Platform-specific secure storage for OAuth tokens.
 */
public class CloudSyncInitializer(
    private val identityManager: IdentityManager,
    private val configurationRepository: IConfigurationRepository,
    private val secureStorage: SecureStorage
) {
    private val _initState = MutableStateFlow(InitState.IDLE)

    /** Observable initialization progress. */
    public val initState: StateFlow<InitState> = _initState.asStateFlow()

    private var mockAuthManager: MockAuthManager? = null
    private var selectedMode: SyncMode = SyncMode.MOCK

    /**
     * Initializes the engine in the given mode.
     *
     * @param platform Target platform (Android, Desktop, Web, iOS).
     * @param mode MOCK (development), OAUTH_PRODUCTION (production), ANONYMOUS (offline-only).
     * @param appVersion Application version string for identity tracking.
     */
    public suspend fun initialize(
        platform: DevicePlatform,
        mode: SyncMode = SyncMode.MOCK,
        appVersion: String = "0.1.0"
    ): SyncResult<Unit> {
        return try {
            _initState.value = InitState.INITIALIZING_IDENTITY
            selectedMode = mode

            // 1. Initialize identity (device + user)
            val identityResult = identityManager.initialize(platform, appVersion)
            if (identityResult is IdentityResult.Error) {
                _initState.value = InitState.ERROR
                return SyncResult.error(
                    SyncErrorCode.INTERNAL_ERROR,
                    "Identity init failed: ${identityResult.message}"
                )
            }

            // 2. Handle auth based on mode
            _initState.value = InitState.AUTHENTICATING
            when (mode) {
                SyncMode.MOCK -> {
                    val mockProvider = MockAuthProvider()
                    mockAuthManager = mockProvider.createMockAuthManager(secureStorage)
                    val authResult = mockAuthManager!!.handleCallback()
                    if (authResult.isFailure) {
                        _initState.value = InitState.ERROR
                        return SyncResult.error(
                            SyncErrorCode.AUTH_REQUIRED,
                            "Mock auth failed: ${authResult.exceptionOrNull()?.message}"
                        )
                    }

                    // Link identity
                    val profile = mockProvider.getMockProfile()
                    identityManager.linkAuthenticatedIdentity(
                        providerUserId = profile.providerUserId ?: profile.localUserId,
                        email = profile.email,
                        displayName = profile.displayName,
                        avatarUrl = profile.avatarUrl,
                        provider = profile.provider
                    )
                }
                SyncMode.OAUTH_PRODUCTION -> {
                    return SyncResult.error(
                        SyncErrorCode.AUTH_REQUIRED,
                        "Production OAuth not yet configured. Use SyncMode.MOCK for development."
                    )
                }
                SyncMode.ANONYMOUS -> {
                    // Identity is already anonymous from initialization
                    // No additional auth needed
                }
            }

            _initState.value = InitState.INITIALIZED
            SyncResult.success(Unit)
        } catch (e: Exception) {
            _initState.value = InitState.ERROR
            SyncResult.error(SyncErrorCode.INTERNAL_ERROR, "Initialization failed", e)
        }
    }

    /**
     * Returns the [MockAuthManager] if the engine was initialized in MOCK mode.
     * Null if in ANONYMOUS or OAUTH_PRODUCTION mode, or before initialization.
     */
    public fun getMockAuthManager(): MockAuthManager? = mockAuthManager
}

/** States of the [CloudSyncInitializer] initialization process. */
public enum class InitState {
    /** Initialization has not started. */
    IDLE,
    /** Setting up device and user identity. */
    INITIALIZING_IDENTITY,
    /** Authenticating with the configured provider. */
    AUTHENTICATING,
    /** Initialization complete and ready to use. */
    INITIALIZED,
    /** An error occurred during initialization. */
    ERROR
}

/**
 * Synchronization mode for the engine.
 */
public enum class SyncMode {
    /** Mock provider for development (no real Google credentials needed). */
    MOCK,

    /** Real OAuth with Google Drive. Requires Google Cloud Console credentials. */
    OAUTH_PRODUCTION,

    /** No authentication, offline-only mode. Data stays local. */
    ANONYMOUS
}

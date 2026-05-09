package io.cloudsync.domain.identity

import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.repository.IConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

/**
 * Central identity manager for the sync engine.
 *
 * Responsibilities:
 * - Generate and persist anonymous device identity
 * - Bridge with AuthManager after successful OAuth
 * - Provide canonical userId for data namespacing
 * - Track authentication state for the UI layer
 *
 * This is the "who am I?" service for the engine.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
public class IdentityManager(
    private val configurationRepository: IConfigurationRepository
) {
    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)

    /** Observable current user profile. */
    public val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    /** Observable current device info. */
    public val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    /** True if the user has an authenticated (non-anonymous) profile. */
    public val isAuthenticated: Boolean get() = _currentProfile.value?.isAuthenticated == true

    /**
     * Initializes identity. Either restores existing identity or creates anonymous one.
     */
    public suspend fun initialize(platform: DevicePlatform, appVersion: String = ""): IdentityResult {
        // Try to restore existing identity from storage
        val stored = restoreFromStorage()
        if (stored != null) {
            _currentProfile.value = stored
            return IdentityResult.Restored(stored)
        }

        // Create anonymous identity
        val now = Clock.System.now().toEpochMilliseconds()
        val localUserId = Uuid.random().toString()
        val deviceId = Uuid.random().toString()
        val osVersion = platform.name.lowercase()

        val deviceInfo = DeviceInfo(
            deviceId = deviceId,
            deviceName = "$platform Device",
            platform = platform,
            createdAt = now,
            lastSeenAt = now,
            osVersion = osVersion,
            appVersion = appVersion
        )

        val profile = UserProfile(
            localUserId = localUserId,
            provider = IdentityProvider.ANONYMOUS,
            createdAt = now
        )

        _currentProfile.value = profile
        _deviceInfo.value = deviceInfo
        persistIdentity(profile, deviceInfo)

        return IdentityResult.Created(profile, deviceInfo)
    }

    /**
     * Links an authenticated (OAuth) identity to this session.
     * Called by the AuthManager after successful OAuth flow.
     */
    public suspend fun linkAuthenticatedIdentity(
        providerUserId: String,
        email: String?,
        displayName: String?,
        avatarUrl: String?,
        provider: IdentityProvider
    ): UserProfile {
        val current = _currentProfile.value ?: error("IdentityManager not initialized")
        val now = Clock.System.now().toEpochMilliseconds()

        val updated = current.copy(
            providerUserId = providerUserId,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            provider = provider,
            lastAuthenticatedAt = now
        )

        _currentProfile.value = updated
        persistIdentity(updated, _deviceInfo.value ?: error("No device info"))

        return updated
    }

    /**
     * Clears identity (logout).
     */
    public suspend fun clearIdentity() {
        // Generate new anonymous identity
        initialize(_deviceInfo.value?.platform ?: DevicePlatform.ANDROID)
    }

    private suspend fun restoreFromStorage(): UserProfile? {
        val config = configurationRepository.getById("cloudsync:identity:user-profile")
        return config.getOrNull()?.let {
            Json.decodeFromString<UserProfile>(it.payload)
        }
    }

    private suspend fun persistIdentity(profile: UserProfile, device: DeviceInfo) {
        val json = Json { encodeDefaults = true }
        configurationRepository.save(
            Configuration(
                id = "cloudsync:identity:user-profile",
                namespace = "cloudsync:identity",
                payload = json.encodeToString(UserProfile.serializer(), profile),
                encrypted = true
            )
        )
        configurationRepository.save(
            Configuration(
                id = "cloudsync:identity:device-info",
                namespace = "cloudsync:identity",
                payload = json.encodeToString(DeviceInfo.serializer(), device),
                encrypted = true
            )
        )
    }
}

/**
 * Result of identity initialization.
 */
public sealed class IdentityResult {
    public data class Created(
        val profile: UserProfile,
        val deviceInfo: DeviceInfo
    ) : IdentityResult()

    public data class Restored(
        val profile: UserProfile
    ) : IdentityResult()

    public data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : IdentityResult()
}

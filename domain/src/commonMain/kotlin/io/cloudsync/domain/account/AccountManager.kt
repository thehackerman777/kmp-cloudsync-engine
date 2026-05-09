package io.cloudsync.domain.account

import io.cloudsync.core.result.SyncError
import io.cloudsync.core.result.SyncErrorCode
import io.cloudsync.core.result.SyncResult
import io.cloudsync.domain.repository.IConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages multiple cloud provider account profiles.
 *
 * Provides CRUD operations, persistence via [IConfigurationRepository],
 * and connection health tracking. This is the primary entry point for
 * account management in the CloudSync engine.
 *
 * ## Usage
 * ```kotlin
 * val accountManager = AccountManager(configurationRepository)
 * accountManager.addProfile(profile)
 * val health = accountManager.checkConnection(profile.id)
 * ```
 *
 * Profiles are serialized as JSON and stored under a reserved namespace
 * ("__account_profiles__") in the configuration repository.
 *
 * @property repository The configuration repository used for profile persistence.
 * @property json JSON instance for serialization/deserialization.
 */
public class AccountManager(
    private val repository: IConfigurationRepository,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val _profiles = MutableStateFlow<List<AccountProfile>>(emptyList())
    private val _connectionHealth = MutableStateFlow<Map<String, ProviderConnectionHealth>>(emptyMap())

    /** Observable list of all registered account profiles. */
    public val profiles: StateFlow<List<AccountProfile>> = _profiles.asStateFlow()

    /** Observable map of profile ID to connection health status. */
    public val connectionHealth: StateFlow<Map<String, ProviderConnectionHealth>> =
        _connectionHealth.asStateFlow()

    /** Internal namespace used for storing profile data. */
    private companion object {
        const val PROFILES_NAMESPACE = "__account_profiles__"
        const val PROFILES_KEY = "all_profiles"
    }

    /**
     * Loads all saved profiles from persistent storage.
     * Call this on application startup after the configuration repository is ready.
     */
    public suspend fun loadProfiles(): SyncResult<List<AccountProfile>> {
        val result = repository.getByNamespace(PROFILES_NAMESPACE)
        return when (result) {
            is SyncResult.Success -> {
                val configs = result.data
                if (configs.isNotEmpty()) {
                    val payload = configs.first().payload
                    val profileList = try {
                        json.decodeFromString<SerializableProfileList>(payload)
                    } catch (e: Exception) {
                        return SyncResult.error(
                            SyncErrorCode.STORAGE_SERIALIZATION_ERROR,
                            "Failed to deserialize account profiles",
                            e
                        )
                    }
                    _profiles.value = profileList.profiles
                    SyncResult.success(profileList.profiles)
                } else {
                    _profiles.value = emptyList()
                    SyncResult.success(emptyList())
                }
            }
            is SyncResult.Error -> result.map { emptyList() }
            else -> SyncResult.success(emptyList())
        }
    }

    /**
     * Adds a new account profile.
     *
     * Persists the profile immediately. If a profile with the same [id]
     * already exists, it will be replaced.
     */
    public suspend fun addProfile(profile: AccountProfile): SyncResult<AccountProfile> {
        val current = _profiles.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == profile.id }
        val stamped = if (profile.connectedAt == 0L) {
            profile.copy(connectedAt = Clock.System.now().toEpochMilliseconds())
        } else {
            profile
        }
        if (existingIndex >= 0) {
            current[existingIndex] = stamped
        } else {
            current.add(stamped)
        }
        val saveResult = persistProfiles(current)
        return when (saveResult) {
            is SyncResult.Success -> {
                _profiles.value = current
                SyncResult.success(stamped)
            }
            is SyncResult.Error -> saveResult
            is SyncResult.Cancelled -> saveResult
            is SyncResult.Loading -> saveResult
            else -> SyncResult.success(stamped)
        }
    }

    /**
     * Updates an existing account profile.
     *
     * @return The updated profile, or an error if the profile was not found.
     */
    public suspend fun updateProfile(profile: AccountProfile): SyncResult<AccountProfile> {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index < 0) {
            return SyncResult.error(
                SyncErrorCode.NOT_FOUND,
                "Account profile not found: ${profile.id}"
            )
        }
        current[index] = profile
        val saveResult = persistProfiles(current)
        return when (saveResult) {
            is SyncResult.Success -> {
                _profiles.value = current
                SyncResult.success(profile)
            }
            is SyncResult.Error -> saveResult
            is SyncResult.Cancelled -> saveResult
            is SyncResult.Loading -> saveResult
            else -> SyncResult.success(profile)
        }
    }

    /**
     * Removes an account profile by its ID.
     *
     * @return Success if the profile was found and removed.
     */
    public suspend fun removeProfile(profileId: String): SyncResult<Unit> {
        val current = _profiles.value.toMutableList()
        val removed = current.removeAll { it.id == profileId }
        if (!removed) {
            return SyncResult.error(
                SyncErrorCode.NOT_FOUND,
                "Account profile not found: $profileId"
            )
        }
        val saveResult = persistProfiles(current)
        return when (saveResult) {
            is SyncResult.Success -> {
                _profiles.value = current
                _connectionHealth.value = _connectionHealth.value - profileId
                SyncResult.success(Unit)
            }
            is SyncResult.Error -> saveResult
            is SyncResult.Cancelled -> saveResult
            is SyncResult.Loading -> saveResult
        }
    }

    /**
     * Retrieves a profile by ID.
     */
    public fun getProfile(profileId: String): AccountProfile? {
        return _profiles.value.find { it.id == profileId }
    }

    /**
     * Returns all profiles for a given provider type.
     */
    public fun getProfilesByProvider(provider: CloudProvider): List<AccountProfile> {
        return _profiles.value.filter { it.provider == provider }
    }

    /**
     * Updates the [AuthStatus] for a profile and persists the change.
     */
    public suspend fun updateAuthStatus(profileId: String, status: AuthStatus): SyncResult<AccountProfile> {
        val profile = getProfile(profileId)
            ?: return SyncResult.error(
                SyncErrorCode.NOT_FOUND,
                "Account profile not found: $profileId"
            )
        return updateProfile(profile.copy(authStatus = status))
    }

    /**
     * Updates the [lastSyncAt] timestamp for a profile.
     */
    public suspend fun recordSync(profileId: String): SyncResult<AccountProfile> {
        val profile = getProfile(profileId)
            ?: return SyncResult.error(
                SyncErrorCode.NOT_FOUND,
                "Account profile not found: $profileId"
            )
        val now = Clock.System.now().toEpochMilliseconds()
        return updateProfile(profile.copy(lastSyncAt = now))
    }

    /**
     * Updates the connection health for a profile.
     * This is a runtime-only indicator and is not persisted.
     */
    public fun updateConnectionHealth(profileId: String, health: ProviderConnectionHealth) {
        _connectionHealth.value = _connectionHealth.value + (profileId to health)
    }

    /**
     * Tests the connection for a given profile.
     *
     * Base implementation returns Unconfigured for profiles not found.
     * Subclasses or provider-specific implementations should override
     * this to perform actual network tests.
     */
    public suspend fun testConnection(profileId: String): ConnectionTestResult {
        val health = _connectionHealth.value[profileId]
        return when (health) {
            is ProviderConnectionHealth.Connected -> ConnectionTestResult.Success(latencyMs = 0L)
            is ProviderConnectionHealth.ExpiringSoon -> ConnectionTestResult.Success(latencyMs = 0L)
            is ProviderConnectionHealth.Disconnected -> ConnectionTestResult.Failure(
                error = SyncError(
                    SyncErrorCode.NETWORK_UNAVAILABLE,
                    health.reason
                ),
                recoverable = health.isRetryable
            )
            is ProviderConnectionHealth.Unconfigured, null -> ConnectionTestResult.Failure(
                error = SyncError(
                    SyncErrorCode.NOT_INITIALIZED,
                    "Provider not configured: $profileId"
                ),
                recoverable = true
            )
        }
    }

    /**
     * Returns the number of operational (CONNECTED) profiles.
     */
    public val operationalProfileCount: Int
        get() = _profiles.value.count { it.isOperational }

    /**
     * Clears all profiles and resets health state.
     */
    public suspend fun clearAll(): SyncResult<Unit> {
        _profiles.value = emptyList()
        _connectionHealth.value = emptyMap()
        return persistProfiles(emptyList())
    }

    /**
     * Persists the full profile list to the configuration repository.
     */
    private suspend fun persistProfiles(profiles: List<AccountProfile>): SyncResult<Unit> {
        return try {
            val payload = json.encodeToString(SerializableProfileList(profiles))
            val config = io.cloudsync.domain.model.Configuration(
                id = PROFILES_KEY,
                namespace = PROFILES_NAMESPACE,
                payload = payload,
                version = 1L,
                checksum = "",
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                createdAt = 0L,
                synced = false,
                sizeBytes = payload.encodeToByteArray().size.toLong()
            )
            val result = repository.save(config)
            result.map { }
        } catch (e: Exception) {
            SyncResult.error(
                SyncErrorCode.STORAGE_SERIALIZATION_ERROR,
                "Failed to persist account profiles",
                e
            )
        }
    }

    /**
     * Wrapper for serializing a list of profiles.
     */
    @Serializable
    private data class SerializableProfileList(
        val profiles: List<AccountProfile>
    )
}

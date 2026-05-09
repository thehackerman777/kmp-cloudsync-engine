package io.cloudsync.domain.account

import kotlinx.serialization.Serializable

/**
 * Represents a cloud provider account profile in the CloudSync engine.
 *
 * Each [AccountProfile] corresponds to one authenticated provider session
 * (Google Drive, Dropbox, OneDrive, etc.) and carries metadata about the
 * connection status, display info, and sync timestamps.
 *
 * Profiles are managed by [AccountManager] and persisted via
 * [io.cloudsync.domain.repository.IConfigurationRepository].
 *
 * @property id Unique identifier for this profile (typically provider + user hash).
 * @property provider The cloud storage provider type.
 * @property displayName Human-readable name for the account.
 * @property email Email address associated with the provider account.
 * @property authStatus Current authentication status of this profile.
 * @property connectedAt Timestamp (epoch millis) when the account was first connected.
 * @property lastSyncAt Timestamp (epoch millis) of the last successful sync.
 * @property avatarUrl Optional URL for the provider avatar/icon.
 */
@Serializable
public data class AccountProfile(
    val id: String,
    val provider: CloudProvider,
    val displayName: String,
    val email: String,
    val authStatus: AuthStatus = AuthStatus.PENDING,
    val connectedAt: Long = 0L,
    val lastSyncAt: Long? = null,
    val avatarUrl: String? = null
) {
    /**
     * Returns true if this profile is in a state that allows sync operations.
     */
    public val isOperational: Boolean get() = authStatus == AuthStatus.CONNECTED
}

/**
 * Supported cloud storage providers.
 *
 * Extend this enum when adding new backend integrations.
 * Each entry maps to a specific [io.cloudsync.auth.provider.AuthProvider]
 * in the auth module.
 */
@Serializable
public enum class CloudProvider {
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE;

    /**
     * Human-readable display name for this provider.
     */
    public val displayName: String get() = when (this) {
        GOOGLE_DRIVE -> "Google Drive"
        DROPBOX -> "Dropbox"
        ONEDRIVE -> "Microsoft OneDrive"
    }
}

/**
 * Authentication status for a provider profile.
 *
 * Each status determines whether sync operations are allowed
 * or what user action is required.
 */
@Serializable
public enum class AuthStatus {
    /** Fully authenticated and ready for sync operations. */
    CONNECTED,

    /** Access token has expired; a refresh is required. */
    EXPIRED,

    /** Provider has revoked access; re-authentication required. */
    REVOKED,

    /** Awaiting initial authorization flow completion. */
    PENDING
}

package io.cloudsync.domain.identity

import kotlinx.serialization.Serializable

/**
 * Represents a resolved user identity linked to an authenticated cloud account.
 *
 * After successful OAuth, the engine calls the provider's userinfo endpoint
 * to populate this profile. Anonymous users have only a localId (UUID v4).
 */
@Serializable
public data class UserProfile(
    /** Provider-specific user ID (Google: 'sub' claim). Null for anonymous. */
    val providerUserId: String? = null,
    /** Anonymous local identifier (UUID v4, generated on first launch). */
    val localUserId: String,
    /** Email from the identity provider. */
    val email: String? = null,
    /** Display name. */
    val displayName: String? = null,
    /** Avatar URL. */
    val avatarUrl: String? = null,
    /** Which provider was used (google, anonymous, etc). */
    val provider: IdentityProvider = IdentityProvider.ANONYMOUS,
    /** When this identity was first created. */
    val createdAt: Long,
    /** When this identity was last authenticated. */
    val lastAuthenticatedAt: Long? = null
) {
    /** True if this user has a real (non-anonymous) identity. */
    val isAuthenticated: Boolean get() = providerUserId != null

    /** Canonical user ID used to namespace all synced data. */
    val canonicalUserId: String get() = providerUserId ?: localUserId
}

@Serializable
public enum class IdentityProvider {
    ANONYMOUS,
    GOOGLE,
    MICROSOFT,
    DROPBOX,
    CUSTOM
}

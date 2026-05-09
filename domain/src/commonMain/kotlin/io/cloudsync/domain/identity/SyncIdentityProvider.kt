package io.cloudsync.domain.identity

/**
 * Provider interface for identity resolution during sync operations.
 * Every sync operation (upload, download, conflict resolution) should
 * tag data with this identity for multi-device awareness.
 */
public interface SyncIdentityProvider {
    /** Current canonical user ID. */
    public val currentUserId: String

    /** Current device ID. */
    public val currentDeviceId: String

    /** True if the current identity is authenticated. */
    public val isAuthenticated: Boolean

    /** Creates a namespaced key for a configuration (userId:namespace:key). */
    public fun namespaceKey(namespace: String, key: String): String

    /** Creates a namespaced key for scoping to device (userId:deviceId:namespace:key). */
    public fun deviceScopedKey(namespace: String, key: String): String
}

public class DefaultSyncIdentityProvider(
    private val identityManager: IdentityManager
) : SyncIdentityProvider {
    override val currentUserId: String
        get() = identityManager.currentProfile.value?.canonicalUserId ?: "unknown"

    override val currentDeviceId: String
        get() = identityManager.deviceInfo.value?.deviceId ?: "unknown"

    override val isAuthenticated: Boolean
        get() = identityManager.isAuthenticated

    override fun namespaceKey(namespace: String, key: String): String =
        "$currentUserId:$namespace:$key"

    override fun deviceScopedKey(namespace: String, key: String): String =
        "$currentUserId:$currentDeviceId:$namespace:$key"
}

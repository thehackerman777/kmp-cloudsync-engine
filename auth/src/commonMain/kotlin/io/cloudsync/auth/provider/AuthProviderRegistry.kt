package io.cloudsync.auth.provider

import io.cloudsync.auth.AuthManager
import io.cloudsync.auth.oauth2.OAuth2Client
import io.cloudsync.auth.oauth2.OAuth2Config
import io.cloudsync.auth.oauth2.TokenResponse
import io.cloudsync.auth.token.TokenProvider
import io.cloudsync.auth.secure.SecureStorage
import io.cloudsync.network.client.NetworkClientProvider

/**
 * Registry for cloud authentication providers.
 *
 * Extensible registry that allows adding new providers
 * (Firebase, AWS, OneDrive, Dropbox) without modifying core auth logic.
 */
public class AuthProviderRegistry(
    private val secureStorage: SecureStorage,
    private val networkClientProvider: NetworkClientProvider
) {
    private val providers = mutableMapOf<String, AuthProvider>()

    /** Registers a new auth provider. */
    public fun register(provider: AuthProvider) {
        providers[provider.id] = provider
    }

    /** Gets a registered provider by ID. */
    public fun get(providerId: String): AuthProvider? = providers[providerId]

    /** Lists all registered providers. */
    public fun listProviders(): List<AuthProvider> = providers.values.toList()

    /** Creates an AuthManager for a specific provider. */
    public fun createAuthManager(providerId: String): AuthManager? {
        val provider = providers[providerId] ?: return null
        val oAuth2Client = OAuth2Client(
            configuration = provider.oAuth2Config(),
            httpClient = networkClientProvider.createAuthClient()
        )
        return AuthManager(
            oAuth2Client = oAuth2Client,
            tokenProvider = TokenProvider(),
            secureStorage = secureStorage
        )
    }
}

/**
 * Provider contract for pluggable authentication backends.
 */
public interface AuthProvider {
    /** Unique identifier (e.g., "google", "onedrive", "dropbox"). */
    public val id: String

    /** Human-readable display name. */
    public val displayName: String

    /** Returns the OAuth2 configuration for this provider. */
    public fun oAuth2Config(): OAuth2Config

    /** Priority for multi-cloud sync (lower = higher priority). */
    public val priority: Int get() = 10
}

/** Google Drive authentication provider. */
public class GoogleDriveAuthProvider(
    private val clientId: String,
    private val clientSecret: String = ""
) : AuthProvider {
    override val id: String = "google"
    override val displayName: String = "Google Drive"

    override fun oAuth2Config(): OAuth2Config = OAuth2Config(
        clientId = clientId,
        clientSecret = clientSecret,
        scope = "https://www.googleapis.com/auth/drive.appdata",
        provider = "google"
    )
}

/** Future: OneDrive authentication provider. */
public class OneDriveAuthProvider(
    private val clientId: String,
    private val clientSecret: String = ""
) : AuthProvider {
    override val id: String = "onedrive"
    override val displayName: String = "Microsoft OneDrive"
    override val priority: Int = 20

    override fun oAuth2Config(): OAuth2Config = OAuth2Config(
        clientId = clientId,
        clientSecret = clientSecret,
        authEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
        tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
        scope = "offline_access Files.ReadWrite.AppFolder",
        provider = "onedrive"
    )
}

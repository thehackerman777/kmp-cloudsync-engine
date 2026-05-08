package io.cloudsync.auth.secure

import io.cloudsync.auth.oauth2.TokenResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Abstract secure storage for OAuth2 credentials.
 *
 * Platform-specific implementations use:
 * - Android: EncryptedSharedPreferences (AndroidX Security)
 * - Desktop: OS keychain (Secret Service DBus / Keychain)
 * - iOS: Keychain Services
 * - Web: localStorage (with optional WASM encryption)
 */
public expect class SecureStorage {

    /**
     * Saves OAuth2 credentials to secure platform storage.
     */
    public suspend fun saveCredentials(response: TokenResponse)

    /**
     * Loads OAuth2 credentials from secure platform storage.
     * Returns null if no stored credentials exist.
     */
    public suspend fun loadCredentials(): TokenResponse?

    /**
     * Clears all stored credentials.
     */
    public suspend fun clearCredentials()

    /**
     * Stores an arbitrary encrypted value.
     */
    public suspend fun store(key: String, value: String)

    /**
     * Retrieves an encrypted value by key.
     */
    public suspend fun retrieve(key: String): String?

    /**
     * Removes a specific key from secure storage.
     */
    public suspend fun remove(key: String)
}

package io.cloudsync.auth.secure

import io.cloudsync.auth.oauth2.TokenResponse
import java.util.prefs.Preferences

/**
 * Desktop implementation of SecureStorage using Java Preferences API.
 * For production, use OS-level keychain (Secret Service / Keychain).
 */
public actual class SecureStorage {
    private val prefs = Preferences.userNodeForPackage(SecureStorage::class.java)

    public actual suspend fun saveCredentials(response: TokenResponse) {
        prefs.put(ACCESS_TOKEN_KEY, response.accessToken)
        prefs.putLong(EXPIRES_IN_KEY, response.expiresIn)
        if (response.refreshToken.isNotBlank()) {
            prefs.put(REFRESH_TOKEN_KEY, response.refreshToken)
        }
    }

    public actual suspend fun loadCredentials(): TokenResponse? {
        val accessToken = prefs.get(ACCESS_TOKEN_KEY, null) ?: return null
        val expiresIn = prefs.getLong(EXPIRES_IN_KEY, 3600)
        val refreshToken = prefs.get(REFRESH_TOKEN_KEY, "") ?: ""
        return TokenResponse(
            accessToken = accessToken,
            expiresIn = expiresIn,
            refreshToken = refreshToken
        )
    }

    public actual suspend fun clearCredentials() {
        prefs.remove(ACCESS_TOKEN_KEY)
        prefs.remove(REFRESH_TOKEN_KEY)
        prefs.remove(EXPIRES_IN_KEY)
    }

    public actual suspend fun store(key: String, value: String) {
        prefs.put(key, value)
    }

    public actual suspend fun retrieve(key: String): String? {
        return prefs.get(key, null)
    }

    public actual suspend fun remove(key: String) {
        prefs.remove(key)
    }

    private companion object {
        private const val ACCESS_TOKEN_KEY = "cloudsync_access_token"
        private const val REFRESH_TOKEN_KEY = "cloudsync_refresh_token"
        private const val EXPIRES_IN_KEY = "cloudsync_expires_in"
    }
}

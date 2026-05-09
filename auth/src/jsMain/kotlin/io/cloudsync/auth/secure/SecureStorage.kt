package io.cloudsync.auth.secure

import io.cloudsync.auth.oauth2.TokenResponse
import kotlinx.browser.localStorage

/**
 * JS implementation of SecureStorage using localStorage.
 * For production, consider IndexedDB with encryption.
 */
public actual class SecureStorage {

    public actual suspend fun saveCredentials(response: TokenResponse) {
        localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
        localStorage.setItem(EXPIRES_IN_KEY, response.expiresIn.toString())
        localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
    }

    public actual suspend fun loadCredentials(): TokenResponse? {
        val accessToken = localStorage.getItem(ACCESS_TOKEN_KEY) ?: return null
        val expiresIn = localStorage.getItem(EXPIRES_IN_KEY)?.toLongOrNull() ?: 3600
        val refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY) ?: ""
        return TokenResponse(
            accessToken = accessToken,
            expiresIn = expiresIn,
            refreshToken = refreshToken
        )
    }

    public actual suspend fun clearCredentials() {
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        localStorage.removeItem(EXPIRES_IN_KEY)
    }

    public actual suspend fun store(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    public actual suspend fun retrieve(key: String): String? {
        return localStorage.getItem(key)
    }

    public actual suspend fun remove(key: String) {
        localStorage.removeItem(key)
    }

    private companion object {
        private const val ACCESS_TOKEN_KEY = "cloudsync_access_token"
        private const val REFRESH_TOKEN_KEY = "cloudsync_refresh_token"
        private const val EXPIRES_IN_KEY = "cloudsync_expires_in"
    }
}

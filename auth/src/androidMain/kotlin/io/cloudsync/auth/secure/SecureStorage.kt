package io.cloudsync.auth.secure

import io.cloudsync.auth.oauth2.TokenResponse
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android implementation of SecureStorage using EncryptedSharedPreferences.
 */
public actual class SecureStorage {
    private var prefs: SharedPreferences? = null

    /**
     * Initialize with Android context. Must be called before any other operation.
     */
    public fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    public actual suspend fun saveCredentials(response: TokenResponse) {
        val p = prefs ?: return
        p.edit()
            .putString(ACCESS_TOKEN_KEY, response.accessToken)
            .putLong(EXPIRES_IN_KEY, response.expiresIn)
            .putString(REFRESH_TOKEN_KEY, response.refreshToken.ifBlank { "" })
            .apply()
    }

    public actual suspend fun loadCredentials(): TokenResponse? {
        val p = prefs ?: return null
        val accessToken = p.getString(ACCESS_TOKEN_KEY, null) ?: return null
        val expiresIn = p.getLong(EXPIRES_IN_KEY, 3600)
        val refreshToken = p.getString(REFRESH_TOKEN_KEY, "") ?: ""
        return TokenResponse(
            accessToken = accessToken,
            expiresIn = expiresIn,
            refreshToken = refreshToken
        )
    }

    public actual suspend fun clearCredentials() {
        prefs?.edit()?.apply {
            remove(ACCESS_TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
            remove(EXPIRES_IN_KEY)
        }?.apply()
    }

    public actual suspend fun store(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    public actual suspend fun retrieve(key: String): String? {
        return prefs?.getString(key, null)
    }

    public actual suspend fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }

    private companion object {
        private const val PREFS_NAME = "cloudsync_secure_prefs"
        private const val ACCESS_TOKEN_KEY = "cloudsync_access_token"
        private const val REFRESH_TOKEN_KEY = "cloudsync_refresh_token"
        private const val EXPIRES_IN_KEY = "cloudsync_expires_in"
    }
}

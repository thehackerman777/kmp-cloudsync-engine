package io.cloudsync.auth.oauth2

import io.cloudsync.auth.token.AccessToken
import io.cloudsync.auth.token.RefreshToken
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth2 client for the authorization code flow with PKCE support.
 *
 * Implements Google's OAuth2 for installed applications:
 * https://developers.google.com/identity/protocols/oauth2/native-app
 *
 * Flow:
 * 1. Build authorization URL → user consent → redirect with code
 * 2. Exchange authorization code for access + refresh tokens
 * 3. Refresh access token when expired
 * 4. Revoke tokens on logout
 */
public class OAuth2Client(
    public val configuration: OAuth2Config,
    private val httpClient: HttpClient
) {
    /**
     * Builds the Google OAuth2 authorization URL.
     */
    public fun buildAuthorizationUrl(): String {
        return buildString {
            append("${configuration.authEndpoint}?")
            append("client_id=${configuration.clientId}")
            append("&redirect_uri=${configuration.redirectUri}")
            append("&response_type=code")
            append("&scope=${configuration.scope.encodeURLParam()}")
            append("&access_type=offline")
            append("&prompt=consent")
            if (configuration.usePKCE) {
                append("&code_challenge_method=S256")
                append("&code_challenge=${configuration.codeChallenge}")
            }
        }
    }

    /**
     * Exchanges an authorization code for access and refresh tokens.
     */
    public suspend fun exchangeCode(code: String): TokenResponse {
        val response = httpClient.post("${configuration.tokenEndpoint}") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(buildParameters {
                append("code", code)
                append("client_id", configuration.clientId)
                append("client_secret", configuration.clientSecret)
                append("redirect_uri", configuration.redirectUri)
                append("grant_type", "authorization_code")
                if (configuration.usePKCE) {
                    append("code_verifier", configuration.codeVerifier)
                }
            })
        }
        return response.body<TokenResponse>()
    }

    /**
     * Refreshes an access token using a refresh token.
     */
    public suspend fun refreshAccessToken(refreshToken: String): TokenResponse {
        val response = httpClient.post("${configuration.tokenEndpoint}") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(buildParameters {
                append("refresh_token", refreshToken)
                append("client_id", configuration.clientId)
                append("client_secret", configuration.clientSecret)
                append("grant_type", "refresh_token")
            })
        }
        return response.body<TokenResponse>()
    }

    /**
     * Revokes a token.
     */
    public suspend fun revokeToken(token: String) {
        httpClient.post("${configuration.revocationEndpoint}") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(buildParameters {
                append("token", token)
            })
        }
    }
}

public data class OAuth2Config(
    val clientId: String,
    val clientSecret: String = "",
    val redirectUri: String = "http://localhost:8090",
    val scope: String = "https://www.googleapis.com/auth/drive.appdata",
    val authEndpoint: String = "https://accounts.google.com/o/oauth2/v2/auth",
    val tokenEndpoint: String = "https://oauth2.googleapis.com/token",
    val revocationEndpoint: String = "https://oauth2.googleapis.com/revoke",
    val provider: String = "google",
    val usePKCE: Boolean = true,
    val codeChallenge: String = "",
    val codeVerifier: String = ""
)

@Serializable
public data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("scope") val scope: String = "",
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("id_token") val idToken: String? = null
)

private fun String.encodeURLParam(): String = buildString {
    this@encodeURLParam.forEach { c ->
        when (c) {
            in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '~' -> append(c)
            ' ' -> append("%20")
            else -> append("%${c.code.toString(16).uppercase()}")
        }
    }
}

private fun buildParameters(block: ParametersBuilder.() -> Unit): String {
    val builder = ParametersBuilderImpl()
    builder.block()
    return builder.build()
}

private class ParametersBuilderImpl : ParametersBuilder {
    private val params = mutableListOf<Pair<String, String>>()

    override fun append(key: String, value: String) {
        params.add(key to value)
    }

    fun build(): String = params.joinToString("&") { (k, v) ->
        "${k.encodeURLParam()}=${v.encodeURLParam()}"
    }
}

// Minimal ParametersBuilder interface to avoid dependency on ktor
private interface ParametersBuilder {
    fun append(key: String, value: String)
}

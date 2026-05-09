package io.cloudsync.auth.network

import io.cloudsync.auth.token.TokenProvider
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Injects OAuth2 Bearer tokens into outgoing requests via interceptor.
 */
public class AuthInterceptor(
    private val tokenProvider: TokenProvider
) {
    public fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.install(HttpRequestAuth) {
            bearer {
                loadTokens {
                    tokenProvider.getAccessToken()?.let { BearerTokens(it.value, "") }
                }
                sendWithoutRequest { true }
            }
        }
    }
}

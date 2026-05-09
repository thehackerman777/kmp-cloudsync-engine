package io.cloudsync.auth.network

import io.cloudsync.auth.token.TokenProvider
import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

/**
 * Injects OAuth2 Bearer tokens into outgoing requests.
 */
public class AuthInterceptor(
    private val tokenProvider: TokenProvider
) {
    public fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenProvider.getAccessToken()
                    if (token != null) BearerTokens(token.value, "") else null
                }
                sendWithoutRequest { true }
            }
        }
    }
}

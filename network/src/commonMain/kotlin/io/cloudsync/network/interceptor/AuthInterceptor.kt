package io.cloudsync.network.interceptor

import io.cloudsync.auth.token.AccessToken
import io.cloudsync.auth.token.TokenProvider
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Ktor plugin that injects OAuth2 Bearer tokens into outgoing requests.
 *
 * Handles automatic token refresh on 401 responses:
 * 1. Attempt request with current token
 * 2. If 401 received, refresh token via [TokenProvider]
 * 3. Retry the original request with the new token
 * 4. If refresh fails, propagate auth error
 */
public class AuthInterceptor(
    private val tokenProvider: TokenProvider
) {
    public fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.plugin(HttpSend).intercept { request ->
            val token = tokenProvider.getAccessToken()
            if (token != null) {
                request.headers.append(HttpHeaders.Authorization, "Bearer ${token.value}")
            }

            val call = execute(request)

            if (call.response.status == HttpStatusCode.Unauthorized) {
                call.response.close()
                val refreshed = tokenProvider.refreshToken()
                if (refreshed != null) {
                    request.headers.remove(HttpHeaders.Authorization)
                    request.headers.append(HttpHeaders.Authorization, "Bearer ${refreshed.value}")
                    execute(request)
                } else {
                    call
                }
            } else {
                call
            }
        }
    }
}

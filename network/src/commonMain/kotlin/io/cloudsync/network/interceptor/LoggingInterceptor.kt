package io.cloudsync.network.interceptor

import io.ktor.client.*
import io.ktor.client.plugins.logging.*

/**
 * Sanitized HTTP logging interceptor.
 */
public class LoggingInterceptor(
    private val level: LogLevel = LogLevel.HEADERS
) {
    public fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.install(Logging) {
            this.level = this@LoggingInterceptor.level
        }
    }
}

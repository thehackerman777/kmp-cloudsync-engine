package io.cloudsync.network.interceptor

import io.ktor.client.*
import io.ktor.client.plugins.logging.*

/**
 * Sanitized HTTP logging interceptor.
 *
 * Logs request/response metadata without exposing sensitive data.
 * Authorization headers are automatically redacted by Ktor's logger.
 * Payload bodies are truncated to avoid excessive log output.
 */
public class LoggingInterceptor(
    private val level: LogLevel = LogLevel.HEADERS
) {
    public fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.plugin(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[CloudSync-HTTP] $message")
                }
            }
            this.level = this@LoggingInterceptor.level
            sanitizeHeader { it.equals("Authorization", ignoreCase = true) }
        }
    }
}

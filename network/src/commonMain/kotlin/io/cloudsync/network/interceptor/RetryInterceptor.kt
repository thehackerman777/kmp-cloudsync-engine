package io.cloudsync.network.interceptor

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Configurable retry interceptor with exponential backoff.
 * Applies to Ktor 3.x via the HttpRequestRetry plugin.
 */
public class RetryInterceptor(
    private val config: RetryConfig = RetryConfig()
) {
    public fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.install(HttpRequestRetry) {
            retryOnServerErrors(config.maxRetries)
            exponentialDelay()
            modifyRequest {  
                // Optional: log retry
            }
        }
    }
}

public data class RetryConfig(
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 1_000,
    val maxDelayMs: Long = 30_000,
    val multiplier: Double = 2.0,
    val enableJitter: Boolean = true
)

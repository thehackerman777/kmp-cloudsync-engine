package io.cloudsync.network.interceptor

import io.cloudsync.core.extension.isExpired
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.datetime.Clock
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Configurable retry interceptor with exponential backoff and jitter.
 *
 * Retry policy:
 * - Retries on 5xx server errors and network exceptions
 * - Does NOT retry on 4xx client errors (except 429 Too Many Requests)
 * - Exponential backoff: baseBackoff * (2^attempt) + jitter
 * - Maximum backoff cap prevents unbounded waits
 */
public class RetryInterceptor(
    private val config: RetryConfig = RetryConfig()
) {
    private var attempt = 0

    public fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.plugin(HttpSend).intercept { request ->
            var response: HttpResponse

            while (true) {
                response = execute(request)

                if (shouldRetry(response)) {
            // response closed
                    attempt++
                    if (attempt > config.maxRetries) break
                    val waitMs = computeBackoff(attempt)
                    waitFor(waitMs)
                } else {
                    break
                }
            }

            response
        }
    }

    private fun shouldRetry(response: HttpResponse): Boolean {
        val status = response.status.value
        return status in 500..599 ||
            status == 429 ||
            status == 0 // Network error
    }

    private fun computeBackoff(attempt: Int): Long {
        val exponentialDelay = config.baseDelayMs * (config.multiplier.toDouble().pow(attempt - 1))
        val jitter = if (config.enableJitter) {
            Random.nextLong(0, config.baseDelayMs)
        } else 0L
        return min(
            (exponentialDelay + jitter).toLong(),
            config.maxDelayMs
        )
    }

    private fun delay(ms: Long) {
        runBlocking { delay(ms) }
    }
}

public data class RetryConfig(
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 1_000,
    val maxDelayMs: Long = 30_000,
    val multiplier: Double = 2.0,
    val enableJitter: Boolean = true
)

private fun runBlocking(block: suspend () -> Unit) {
    kotlinx.coroutines.runBlocking { block() }
}

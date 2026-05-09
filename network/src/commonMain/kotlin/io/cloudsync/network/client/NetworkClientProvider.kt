package io.cloudsync.network.client

import io.cloudsync.network.interceptor.RetryInterceptor
import io.cloudsync.network.interceptor.LoggingInterceptor
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Provides configured Ktor HttpClient instances for the CloudSync engine.
 */
public class NetworkClientProvider(private val config: NetworkConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
        coerceInputValues = true
        explicitNulls = false
    }

    public fun createDriveClient(): HttpClient = HttpClient {
        install(ContentNegotiation) { json(this@NetworkClientProvider.json) }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMs
            connectTimeoutMillis = config.connectTimeoutMs
            socketTimeoutMillis = config.socketTimeoutMs
        }

        defaultRequest {
            url(config.baseUrl)
            headers.append("User-Agent", config.userAgent)
            headers.append("Accept", "application/json")
        }
    }

    public fun createAuthClient(): HttpClient = HttpClient {
        install(ContentNegotiation) { json(this@NetworkClientProvider.json) }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }

        defaultRequest { url("https://oauth2.googleapis.com") }
    }
}

public class DriveApiException(
    public val statusCode: Int,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message)

public data class NetworkConfig(
    val baseUrl: String = "https://www.googleapis.com",
    val connectTimeoutMs: Long = 30_000,
    val requestTimeoutMs: Long = 60_000,
    val socketTimeoutMs: Long = 60_000,
    val maxRetries: Int = 3,
    val userAgent: String = "KMP-CloudSync-Engine/0.1.0"
)

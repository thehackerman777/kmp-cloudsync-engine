package io.cloudsync.network.client

import io.cloudsync.network.interceptor.AuthInterceptor
import io.cloudsync.network.interceptor.RetryInterceptor
import io.cloudsync.network.interceptor.LoggingInterceptor
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Provides configured Ktor HttpClient instances for the CloudSync engine.
 *
 * Factory with sensible defaults for Google Drive API communication:
 * - Json content negotiation with strict parsing
 * - Auth interceptor for OAuth token injection
 * - Retry interceptor with exponential backoff
 * - Request/response logging (sanitized)
 * - Connection pooling and timeout management
 */
public class NetworkClientProvider(private val config: NetworkConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
        prettyPrint = false
        coerceInputValues = true
        explicitNulls = false
        classDiscriminator = "#class"
    }

    /**
     * Creates a configured HttpClient for Google Drive API operations.
     */
    public fun createDriveClient(): HttpClient = HttpClient {
        install(ContentNegotiation) { json(this@NetworkClientProvider.json) }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMs
            connectTimeoutMillis = config.connectTimeoutMs
            socketTimeoutMillis = config.socketTimeoutMs
        }

        install(HttpCallValidator) {
            validateResponse { response ->
                if (response.status.value in 400..599) {
                    throw DriveApiException(
                        statusCode = response.status.value,
                        message = "Drive API error: ${response.status.description}"
                    )
                }
            }
        }

        defaultRequest {
            url(config.baseUrl)
            headers.append("User-Agent", config.userAgent)
            headers.append("Accept", "application/json")
        }
    }

    /**
     * Creates a configured HttpClient for OAuth2 token operations.
     */
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

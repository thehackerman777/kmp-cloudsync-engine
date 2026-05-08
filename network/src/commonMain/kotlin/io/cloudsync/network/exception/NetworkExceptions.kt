package io.cloudsync.network.exception

/**
 * Hierarchy of network exceptions for granular error handling.
 */

public open class CloudSyncNetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

public class AuthenticationException(
    message: String = "Authentication failed. Token may be expired or revoked.",
    cause: Throwable? = null
) : CloudSyncNetworkException(message, cause)

public class QuotaExceededException(
    message: String = "API quota exceeded. Please try again later.",
    cause: Throwable? = null
) : CloudSyncNetworkException(message, cause)

public class RateLimitException(
    val retryAfterMs: Long = 60_000,
    message: String = "Rate limited by API provider.",
    cause: Throwable? = null
) : CloudSyncNetworkException(message, cause)

public class NetworkUnavailableException(
    message: String = "Network is unavailable. Check internet connection.",
    cause: Throwable? = null
) : CloudSyncNetworkException(message, cause)

public class ServerErrorException(
    val statusCode: Int,
    message: String = "Server error: HTTP $statusCode",
    cause: Throwable? = null
) : CloudSyncNetworkException(message, cause)

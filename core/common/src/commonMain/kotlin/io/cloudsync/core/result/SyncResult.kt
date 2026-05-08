package io.cloudsync.core.result


import kotlinx.serialization.Serializable

/**
 * Typed result monad for sync operations.
 * Provides structured error handling without exceptions.
 *
 * @param T The type of the success value.
 */
@Serializable
public sealed class SyncResult<out T> {

    /** Successful operation with a value. */
    public data class Success<T>(val data: T) : SyncResult<T>()

    /** Failed operation with diagnostic information. */
    public data class Error(val reason: SyncError) : SyncResult<Nothing>()

    /** Operation cancelled (e.g., engine stopped). */
    public data object Cancelled : SyncResult<Nothing>()

    /** Operation is loading/in-progress. */
    public data object Loading : SyncResult<Nothing>()

    /** Transform the success value. */
    public inline fun <R> map(transform: (T) -> R): SyncResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Cancelled -> this
        is Loading -> this
    }

    /** Get the value or null on failure. */
    public fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /** Get the error or null on success. */
    public fun errorOrNull(): SyncError? = when (this) {
        is Error -> reason
        else -> null
    }

    /** Execute block on success. */
    public inline fun onSuccess(action: (T) -> Unit): SyncResult<T> {
        if (this is Success) action(data)
        return this
    }

    /** Execute block on error. */
    public inline fun onError(action: (SyncError) -> Unit): SyncResult<T> {
        if (this is Error) action(reason)
        return this
    }

    public companion object {
        /** Convenience factory for success. */
        public fun <T> success(value: T): SyncResult<T> = Success(value)

        /** Convenience factory for error. */
        public fun error(code: SyncErrorCode, message: String, cause: Throwable? = null): SyncResult<Nothing> =
            Error(SyncError(code, message, cause))
    }
}

/**
 * Structured error with domain classification.
 */
@Serializable
public data class SyncError(
    val code: SyncErrorCode,
    val message: String,
    val throwableMessage: String? = null,
    val retryable: Boolean = false
) {
    public constructor(code: SyncErrorCode, message: String, cause: Throwable?) : this(
        code = code,
        message = message,
        throwableMessage = cause?.message,
        retryable = false || code.isRetryable
    )
}

/**
 * Domain error classification codes.
 */
@Serializable
public enum class SyncErrorCode(
    public val isRetryable: Boolean = false
) {
    // Auth errors
    AUTH_TOKEN_EXPIRED(true),
    AUTH_INVALID_CREDENTIALS,
    AUTH_REQUIRED,
    AUTH_ACCESS_DENIED,
    AUTH_REFRESH_FAILED(true),

    // Network errors
    NETWORK_UNAVAILABLE(true),
    NETWORK_TIMEOUT(true),
    NETWORK_IO_ERROR(true),
    NETWORK_SSL_ERROR,

    // Sync errors
    SYNC_CONFLICT_DETECTED,
    SYNC_VERSION_MISMATCH(true),
    SYNC_CYCLE_IN_PROGRESS,
    SYNC_QUOTA_EXCEEDED,
    SYNC_FILE_TOO_LARGE,
    SYNC_METADATA_CORRUPT,

    // Storage errors
    STORAGE_FULL,
    STORAGE_CORRUPTION,
    STORAGE_IO_ERROR(true),
    STORAGE_SERIALIZATION_ERROR,

    // Internal errors
    INTERNAL_ERROR(true),
    NOT_INITIALIZED,
    ALREADY_STARTED,
    ENGINE_SHUTDOWN
}

// IOException not available in common code - handled by platform

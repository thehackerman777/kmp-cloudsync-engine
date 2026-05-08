package io.cloudsync.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstraction over coroutine dispatchers for testability.
 *
 * In production, delegates to platform-specific dispatchers.
 * In tests, allows injection of [kotlinx.coroutines.test.StandardTestDispatcher].
 */
public expect class CloudSyncDispatchers {

    /** For CPU-intensive operations (checksums, serialization). */
    public val default: CoroutineDispatcher

    /** For I/O operations (network, database). */
    public val io: CoroutineDispatcher

    /** For UI/main thread operations. */
    public val main: CoroutineDispatcher

    /** For background sync operations. */
    public val sync: CoroutineDispatcher

    /** For long-running scheduled tasks. */
    public val scheduler: CoroutineDispatcher

    public companion object {
        /**
         * Creates a production dispatcher set.
         */
        public fun create(): CloudSyncDispatchers

        /**
         * Creates a test dispatcher set with a single [testDispatcher].
         */
        public fun test(testDispatcher: CoroutineDispatcher): CloudSyncDispatchers
    }
}

/**
 * Default platform-specific dispatcher for sync operations.
 * Uses a single-threaded context to avoid concurrent sync cycles.
 */
internal expect fun syncDispatcher(): CoroutineDispatcher

/**
 * Default platform-specific dispatcher for scheduled tasks.
 */
internal expect fun schedulerDispatcher(): CoroutineDispatcher

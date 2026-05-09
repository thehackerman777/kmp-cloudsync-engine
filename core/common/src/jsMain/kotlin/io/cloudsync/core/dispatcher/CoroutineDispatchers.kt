package io.cloudsync.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public actual class CloudSyncDispatchers {
    public actual val default: CoroutineDispatcher = Dispatchers.Default
    public actual val io: CoroutineDispatcher = Dispatchers.Default
    public actual val main: CoroutineDispatcher = Dispatchers.Default
    public actual val sync: CoroutineDispatcher = Dispatchers.Default
    public actual val scheduler: CoroutineDispatcher = Dispatchers.Default

    public actual companion object {
        public actual fun create(): CloudSyncDispatchers = CloudSyncDispatchers()
        public actual fun test(testDispatcher: CoroutineDispatcher): CloudSyncDispatchers {
            return CloudSyncDispatchers()
        }
    }
}

internal actual fun syncDispatcher(): CoroutineDispatcher = Dispatchers.Default
internal actual fun schedulerDispatcher(): CoroutineDispatcher = Dispatchers.Default

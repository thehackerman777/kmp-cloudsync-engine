package io.cloudsync.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext

public actual class CloudSyncDispatchers {
    public actual val default: CoroutineDispatcher = Dispatchers.Default
    public actual val io: CoroutineDispatcher = Dispatchers.IO
    public actual val main: CoroutineDispatcher = Dispatchers.Main
    public actual val sync: CoroutineDispatcher = newSingleThreadContext("cloudsync-sync")
    public actual val scheduler: CoroutineDispatcher = newSingleThreadContext("cloudsync-scheduler")

    public actual companion object {
        public actual fun create(): CloudSyncDispatchers = CloudSyncDispatchers()
        public actual fun test(testDispatcher: CoroutineDispatcher): CloudSyncDispatchers {
            return CloudSyncDispatchers()
        }
    }
}

internal actual fun syncDispatcher(): CoroutineDispatcher = newSingleThreadContext("cloudsync-sync")
internal actual fun schedulerDispatcher(): CoroutineDispatcher = newSingleThreadContext("cloudsync-scheduler")

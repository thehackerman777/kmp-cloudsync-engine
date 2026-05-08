package io.cloudsync.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext

public actual class CloudSyncDispatchers actual constructor(
    public actual val default: CoroutineDispatcher,
    public actual val io: CoroutineDispatcher,
    public actual val main: CoroutineDispatcher,
    public actual val sync: CoroutineDispatcher,
    public actual val scheduler: CoroutineDispatcher
) {
    public actual companion object {
        public actual fun create(): CloudSyncDispatchers = CloudSyncDispatchers(
            default = Dispatchers.Default,
            io = Dispatchers.IO,
            main = Dispatchers.Default,
            sync = newSingleThreadContext("cloudsync-sync"),
            scheduler = newSingleThreadContext("cloudsync-scheduler")
        )

        public actual fun test(testDispatcher: CoroutineDispatcher): CloudSyncDispatchers = CloudSyncDispatchers(
            default = testDispatcher,
            io = testDispatcher,
            main = testDispatcher,
            sync = testDispatcher,
            scheduler = testDispatcher
        )
    }
}

internal actual fun syncDispatcher(): CoroutineDispatcher = newSingleThreadContext("cloudsync-sync")
internal actual fun schedulerDispatcher(): CoroutineDispatcher = newSingleThreadContext("cloudsync-scheduler")

package io.cloudsync.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * JavaScript entry point for CloudSync Engine.
 *
 * Usage:
 *   const engine = CloudSyncEngine.create(config)
 *   engine.start()
 *   engine.getState()
 *   engine.syncNow()
 *   engine.stop()
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("CloudSyncEngine")
public class CloudSyncJsEntry(private val config: String) {

    private var engine: CloudSync? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    public fun start(): CloudSyncJsEntry {
        if (engine == null) {
            engine = CloudSync.configure(config)
        }
        scope.launch { engine?.start() }
        return this
    }

    public fun stop(): CloudSyncJsEntry {
        scope.launch { engine?.stop(); engine = null }
        return this
    }

    public fun syncNow(): CloudSyncJsEntry {
        scope.launch { engine?.syncNow() }
        return this
    }

    public fun reset(): CloudSyncJsEntry {
        scope.launch { engine?.reset() }
        return this
    }

    public fun getState(): String {
        return engine?.syncState?.value?.name ?: "IDLE"
    }

    public companion object {
        @JsName("create")
        public fun create(config: String): CloudSyncJsEntry {
            return CloudSyncJsEntry(config)
        }
    }
}

package io.cloudsync.sample.desktop

import io.cloudsync.engine.CloudSync
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("=== CloudSync Engine Desktop Sample ===")
    println()

    val config = """{"configName":"desktop-sample","serverUrl":"https://api.example.com"}"""

    println("Configuring engine...")
    val engine = CloudSync.configure(config)

    println("Starting engine...")
    val startResult = engine.start()
    println("Start result: $startResult")
    println("Engine state: ${engine.syncState.value}")

    println()
    println("Triggering sync...")
    val syncResult = engine.syncNow()
    println("Sync result: $syncResult")
    println("Engine state after sync: ${engine.syncState.value}")

    println()
    println("Stopping engine...")
    engine.stop()
    println("Engine stopped successfully!")

    println()
    println("=== Desktop Sample Completed Successfully ===")
}

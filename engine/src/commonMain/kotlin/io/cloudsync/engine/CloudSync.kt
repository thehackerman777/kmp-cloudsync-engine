/**
 * KMP CloudSync Engine — Entry Point
 *
 * This module re-exports the full CloudSync Engine public API.
 * Consumers only need to depend on `:engine` and import from `io.cloudsync.engine`.
 *
 * ```kotlin
 * // Android / Desktop / Web
 * dependencies {
 *     implementation("io.cloudsync:kmp-cloudsync-engine:0.2.0")
 * }
 *
 * // Usage
 * import io.cloudsync.engine.CloudSync
 *
 * val engine = CloudSync.configure(config)
 * engine.start()
 * ```
 *
 * @see io.cloudsync.core.CloudSyncEngine
 */
public typealias CloudSync = io.cloudsync.core.CloudSyncEngine

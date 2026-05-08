package io.cloudsync.core.di









import io.cloudsync.storage.database.DatabaseDriverFactory
import io.cloudsync.sync.engine.SyncEngine
import io.cloudsync.sync.SyncOrchestrator
import kotlinx.coroutines.CoroutineScope

/**
 * Service Locator for the CloudSync Engine.
 *
 * Manages the dependency graph and lifecycle of all components.
 * Designed for integration with Koin, but framework-agnostic at this layer.
 */
public object CloudSyncContainer {

    private val dependencies = mutableMapOf<String, Any>()
    private var initialized = false

    /**
     * Registers a dependency in the container.
     */
    public inline fun <reified T : Any> register(instance: T, name: String? = null) {
        val key = name ?: T::class.qualifiedName ?: error("Unnamed dependency")
        dependencies[key] = instance
    }

    /**
     * Resolves a dependency from the container.
     */
    @Suppress("UNCHECKED_CAST")
    public inline fun <reified T : Any> resolve(name: String? = null): T {
        val key = name ?: T::class.qualifiedName ?: error("Unnamed dependency")
        return dependencies[key] as? T
            ?: throw IllegalStateException("Dependency not found: $key. Ensure it's registered.")
    }

    /**
     * Koin module factory for easy integration.
     */
    public fun koinModule() =  {
        single { CloudSyncContainer }
    }

    internal fun reset() {
        dependencies.clear()
        initialized = false
    }
}

/**
 * CloudSync initialization configuration marker.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class CloudSyncDsl

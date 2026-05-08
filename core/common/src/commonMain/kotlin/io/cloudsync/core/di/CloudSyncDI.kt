package io.cloudsync.core.di

import io.cloudsync.core.InternalCloudSyncApi

/**
 * Service Locator for the CloudSync Engine.
 *
 * Manages the dependency graph and lifecycle of all components.
 * Designed for integration with Koin or other DI frameworks.
 *
 * Note: Module-specific registrations (auth, data, sync, network)
 * are available in their respective modules' DI configurations.
 */
@InternalCloudSyncApi
public object CloudSyncContainer {

    private val dependencies = mutableMapOf<String, Any>()
    private var initialized = false

    /**
     * Registers a dependency in the container.
     */
    public fun register(instance: Any, key: String) {
        val key = name ?: T::class.qualifiedName ?: return
        dependencies[key] = instance
    }

    /**
     * Resolves a dependency from the container.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> resolve(key: String): T? {
        return dependencies[key] as? T
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

package io.cloudsync.domain.usecase

import io.cloudsync.core.result.SyncResult
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.repository.IConfigurationRepository

/**
 * Retrieves a specific configuration by ID.
 * Prioritizes local cache; falls back to remote fetch if unavailable.
 */
public class GetConfigurationUseCase(
    private val repository: IConfigurationRepository
) {
    public suspend operator fun invoke(id: String): SyncResult<Configuration> {
        require(id.isNotBlank()) { "Configuration ID must not be blank" }
        return repository.getById(id)
    }
}

/**
 * Retrieves all configurations in a namespace.
 */
public class GetConfigurationsByNamespaceUseCase(
    private val repository: IConfigurationRepository
) {
    public suspend operator fun invoke(namespace: String): SyncResult<List<Configuration>> {
        require(namespace.isNotBlank()) { "Namespace must not be blank" }
        return repository.getByNamespace(namespace)
    }
}

/**
 * Saves a configuration (create or update).
 */
public class SaveConfigurationUseCase(
    private val repository: IConfigurationRepository
) {
    public suspend operator fun invoke(configuration: Configuration): SyncResult<Configuration> {
        require(configuration.id.isNotBlank()) { "Configuration ID must not be blank" }
        require(configuration.payload.isNotBlank()) { "Configuration payload must not be blank" }
        return repository.save(configuration)
    }
}

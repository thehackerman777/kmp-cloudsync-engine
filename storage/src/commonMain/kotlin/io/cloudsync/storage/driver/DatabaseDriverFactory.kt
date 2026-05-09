package io.cloudsync.storage.driver

import app.cash.sqldelight.db.SqlDriver
import io.cloudsync.core.InternalCloudSyncApi

/**
 * Provides a SQLDelight database driver for the current platform.
 *
 * Default implementation returns null - platform must provide the driver.
 */
@InternalCloudSyncApi
public open class DatabaseDriverFactory {
    public open fun createDriver(): SqlDriver? = null
    public open fun databaseName(): String = "cloudsync.db"
}

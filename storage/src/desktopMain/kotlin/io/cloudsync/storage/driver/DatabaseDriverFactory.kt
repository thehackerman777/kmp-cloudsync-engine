package io.cloudsync.storage.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.cloudsync.core.InternalCloudSyncApi

@InternalCloudSyncApi
public actual class DatabaseDriverFactory {
    public actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(null, null, databaseName())
    }

    public actual fun databaseName(): String = "cloudsync.db"
}

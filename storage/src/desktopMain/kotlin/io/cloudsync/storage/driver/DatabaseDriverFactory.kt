package io.cloudsync.storage.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcSqliteDriver
import io.cloudsync.core.InternalCloudSyncApi

@InternalCloudSyncApi
public actual class DatabaseDriverFactory {
    public actual fun createDriver(): SqlDriver {
        return JdbcSqliteDriver("jdbc:sqlite:cloudsync.db")
    }

    public actual fun databaseName(): String = "cloudsync.db"
}

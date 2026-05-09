package io.cloudsync.storage.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.cloudsync.core.InternalCloudSyncApi

@InternalCloudSyncApi
public actual class DatabaseDriverFactory {
    public actual fun createDriver(): SqlDriver {
        val dbUrl = "jdbc:sqlite:${databaseName()}"
        return JdbcSqliteDriver(dbUrl)
    }

    public actual fun databaseName(): String = "cloudsync.db"
}

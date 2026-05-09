package io.cloudsync.storage.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import java.sql.DriverManager
import io.cloudsync.core.InternalCloudSyncApi

@InternalCloudSyncApi
public actual class DatabaseDriverFactory {
    public actual fun createDriver(): SqlDriver {
        val connection = DriverManager.getConnection("jdbc:sqlite:cloudsync.db")
        return JdbcDriver(connection)
    }

    public actual fun databaseName(): String = "cloudsync.db"
}

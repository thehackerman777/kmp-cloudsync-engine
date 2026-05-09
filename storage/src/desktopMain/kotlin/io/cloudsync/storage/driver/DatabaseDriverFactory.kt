package io.cloudsync.storage.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import io.cloudsync.core.InternalCloudSyncApi
import java.sql.DriverManager

@InternalCloudSyncApi
public actual class DatabaseDriverFactory {
    public actual fun createDriver(): SqlDriver {
        val url = "jdbc:sqlite:${databaseName()}"
        Class.forName("org.sqlite.JDBC")
        val connection = DriverManager.getConnection(url)
        val driver = object : JdbcDriver() {
            override fun connectionAndClose(): java.sql.Connection = connection
        }
        return driver
    }

    public actual fun databaseName(): String = "cloudsync.db"
}

package io.cloudsync.storage.driver

import app.cash.sqldelight.db.SqlDriver
import io.cloudsync.core.InternalCloudSyncApi

/**
 * Platform-specific SQLDelight driver factory.
 *
 * Implementations:
 * - Android: AndroidSqliteDriver (with WAL mode)
 * - Desktop: JdbcSqliteDriver (file-based SQLite)
 * - iOS: NativeSqliteDriver
 * - Web: WebSqlDriver (via SQL.js WASM)
 */
@InternalCloudSyncApi
public expect class DatabaseDriverFactory {

    /**
     * Creates and configures the SQLDelight driver for the current platform.
     */
    public fun createDriver(): SqlDriver

    /**
     * Returns the database name.
     */
    public fun databaseName(): String
}

/**
 * Database configuration options.
 */
public data class DatabaseConfig(
    val name: String = "cloudsync.db",
    val inMemory: Boolean = false,
    val maxSizeBytes: Long = 50 * 1024 * 1024, // 50 MB
    val walModeEnabled: Boolean = true,
    val foreignKeysEnabled: Boolean = true
)

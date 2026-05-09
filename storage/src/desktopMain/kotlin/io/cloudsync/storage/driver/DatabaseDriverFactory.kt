package io.cloudsync.storage.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.Transacter
import io.cloudsync.core.InternalCloudSyncApi
import java.sql.Connection
import java.sql.DriverManager

@InternalCloudSyncApi
public actual class DatabaseDriverFactory {
    public actual fun createDriver(): SqlDriver {
        val url = "jdbc:sqlite:${databaseName()}"
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection(url)
        return DesktopSqlDriver(conn)
    }

    public actual fun databaseName(): String = "cloudsync.db"
}

private class DesktopSqlDriver(
    private val connection: Connection
) : SqlDriver {
    private var transaction: Transacter.Transaction? = null

    override fun execute(
        handle: Int?,
        sql: String,
        nativeArguments: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<Long> {
        val statement = connection.prepareStatement(sql)
        binders?.invoke(DesktopPreparedStatement(statement))
        return QueryResult.Value(statement.executeUpdate().toLong())
    }

    override fun <R> executeQuery(
        handle: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        nativeArguments: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<R> {
        val statement = connection.prepareStatement(sql)
        binders?.invoke(DesktopPreparedStatement(statement))
        val resultSet = statement.executeQuery()
        return mapper(DesktopCursor(resultSet))
    }

    override fun newTransaction(): QueryResult<Transacter.Transaction> {
        connection.autoCommit = false
        val tx = object : Transacter.Transaction {
            override fun rollback() {
                connection.rollback()
                connection.autoCommit = true
            }
            override fun setRollbackOnly() {}
        }
        transaction = tx
        return QueryResult.Value(tx)
    }

    override fun currentTransaction(): Transacter.Transaction? = transaction

    override fun close() {
        connection.close()
    }
}

private class DesktopPreparedStatement(
    private val statement: java.sql.PreparedStatement
) : SqlPreparedStatement {
    override fun bindBytes(index: Int, value: ByteArray) { statement.setBytes(index + 1, value) }
    override fun bindLong(index: Int, value: Long) { statement.setLong(index + 1, value) }
    override fun bindDouble(index: Int, value: Double) { statement.setDouble(index + 1, value) }
    override fun bindString(index: Int, value: String) { statement.setString(index + 1, value) }
    override fun bindNull(index: Int) { statement.setNull(index + 1, java.sql.Types.NULL) }
    override fun execute() { statement.execute() }
}

private class DesktopCursor(
    private val resultSet: java.sql.ResultSet
) : SqlCursor {
    override fun getString(index: Int): String? = resultSet.getString(index + 1)
    override fun getLong(index: Int): Long? = resultSet.getLong(index + 1)
    override fun getDouble(index: Int): Double? = resultSet.getDouble(index + 1)
    override fun getBytes(index: Int): ByteArray? = resultSet.getBytes(index + 1)
    override fun next(): Boolean = resultSet.next()
}

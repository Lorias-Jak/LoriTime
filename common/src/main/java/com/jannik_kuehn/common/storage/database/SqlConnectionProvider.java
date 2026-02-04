package com.jannik_kuehn.common.storage.database;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstraction for SQL connection providers used by the storage layer.
 */
interface SqlConnectionProvider extends Closeable {

    /**
     * Opens the underlying connection pool.
     */
    void open();

    /**
     * Obtains a database connection.
     *
     * @return an open connection
     * @throws SQLException if a connection cannot be obtained
     */
    Connection getConnection() throws SQLException;

    /**
     * Returns whether the provider is closed.
     *
     * @return {@code true} if closed
     */
    boolean isClosed();

    /**
     * Returns the configured table prefix.
     *
     * @return the table prefix
     */
    String getTablePrefix();

    /**
     * Returns the SQL dialect used by this provider.
     *
     * @return the dialect implementation
     */
    SqlDialect getDialect();
}

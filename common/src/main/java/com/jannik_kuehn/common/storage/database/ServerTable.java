package com.jannik_kuehn.common.storage.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Table helper for server entries.
 */
final class ServerTable {

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The {@link SqlDialect} instance.
     */
    private final SqlDialect dialect;

    /**
     * Constructs a ServerTable instance with the specified table name and SQL dialect.
     *
     * @param tableName the name of the database table
     * @param dialect   the SQL dialect to use for database operations
     */
    /* default */
    ServerTable(final String tableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.dialect = dialect;
    }

    /**
     * Generates the SQL statement to create the table for server entries.
     *
     * @return the SQL CREATE TABLE statement for the server table
     */
    /* default */
    String createTableSql() {
        return dialect.createServerTable(tableName);
    }

    /**
     * Retrieves the name of the database table.
     *
     * @return the name of the table
     */
    /* default */
    String getTableName() {
        return tableName;
    }

    /**
     * Ensures a server entry exists in the database and returns its ID.
     * If the server entry does not already exist, it is created.
     *
     * @param connection the database connection to use for the operation
     * @param server     the name of the server to ensure exists in the database
     * @return the ID of the server entry
     * @throws SQLException if a database access error occurs or the server entry cannot be created
     */
    /* default */
    long ensureServer(final Connection connection, final String server) throws SQLException {
        final Optional<Long> existing = findId(connection, server);
        if (existing.isPresent()) {
            return existing.get();
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`server`) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, server);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create server entry");
    }

    private Optional<Long> findId(final Connection connection, final String server) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + tableName + "` WHERE `server` = ?")) {
            select.setString(1, server);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }
}

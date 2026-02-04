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

    private final String tableName;
    private final SqlDialect dialect;

    ServerTable(final String tableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.dialect = dialect;
    }

    String createTableSql() {
        return dialect.createServerTable(tableName);
    }

    String getTableName() {
        return tableName;
    }

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

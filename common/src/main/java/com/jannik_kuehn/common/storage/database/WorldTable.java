package com.jannik_kuehn.common.storage.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Table helper for world entries.
 */
final class WorldTable {

    private final String tableName;

    private final ServerTable serverTable;

    private final SqlDialect dialect;

    WorldTable(final String tableName, final ServerTable serverTable, final SqlDialect dialect) {
        this.tableName = tableName;
        this.serverTable = serverTable;
        this.dialect = dialect;
    }

    String createTableSql() {
        return dialect.createWorldTable(tableName, serverTableName());
    }

    long ensureWorld(final Connection connection, final String server, final String world) throws SQLException {
        final long serverId = serverTable.ensureServer(connection, server);
        final Optional<Long> existing = findId(connection, serverId, world);
        if (existing.isPresent()) {
            return existing.get();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`server_id`, `world`) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setLong(1, serverId);
            insert.setString(2, world);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create world entry");
    }

    private Optional<Long> findId(final Connection connection, final long serverId, final String world)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + tableName + "` WHERE `server_id` = ? AND `world` = ?")) {
            select.setLong(1, serverId);
            select.setString(2, world);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }

    String tableName() {
        return tableName;
    }

    String serverTableName() {
        return serverTable.getTableName();
    }
}

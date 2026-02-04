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

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The {@link ServerTable} instance.
     */
    private final ServerTable serverTable;

    /**
     * The {@link SqlDialect} instance.
     */
    private final SqlDialect dialect;

    /**
     * Constructor
     *
     * @param tableName the table name
     * @param serverTable the {@link ServerTable}
     * @param dialect the {@link SqlDialect}
     */
    /* default */
    WorldTable(final String tableName, final ServerTable serverTable, final SqlDialect dialect) {
        this.tableName = tableName;
        this.serverTable = serverTable;
        this.dialect = dialect;
    }

    /**
     * Creation string of the sql table for the chosen dialect
     *
     * @return the DDL statement
     */
    /* default */
    String createTableSql() {
        return dialect.createWorldTable(tableName, serverTableName());
    }

    /**
     * Ensures that a world entry exists in the database for the given server and world.
     * If the world does not already exist, it creates a new entry.
     *
     * @param connection the database connection to use
     * @param server the name of the server
     * @param world the name of the world
     * @return the unique identifier (ID) of the world entry
     * @throws SQLException if a database access error occurs or the world entry cannot be created
     */
    /* default */
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

    /**
     * Retrieves the name of the database table associated with this instance.
     *
     * @return the name of the table as a {@link String}
     */
    /* default */
    String getTableName() {
        return tableName;
    }

    /**
     * Retrieves the name of the database table associated with the server.
     *
     * @return the name of the server-specific table as a {@link String}
     */
    /* default */
    String serverTableName() {
        return serverTable.getTableName();
    }
}

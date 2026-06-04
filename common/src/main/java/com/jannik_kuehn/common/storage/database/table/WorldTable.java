package com.jannik_kuehn.common.storage.database.table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Table helper for world entries.
 */
public final class WorldTable {

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The {@link ServerTable} instance.
     */
    private final ServerTable serverTable;

    /**
     * Constructor
     *
     * @param tableName   the table name
     * @param serverTable the {@link ServerTable}
     */
    public WorldTable(final String tableName, final ServerTable serverTable) {
        this.tableName = tableName;
        this.serverTable = serverTable;
    }

    /**
     * Ensures that a world entry exists in the database for the given server and world.
     * If the world does not already exist, it creates a new entry.
     *
     * @param connection the database connection to use
     * @param server     the name of the server
     * @param world      the name of the world
     * @return the unique identifier (ID) of the world entry
     * @throws SQLException if a database access error occurs, or the world entry cannot be created
     */
    public long ensureWorld(final Connection connection, final String server, final String world) throws SQLException {
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

    /**
     * Finds an existing world id.
     *
     * @param connection database connection
     * @param server server name
     * @param world world name
     * @return world id if present
     * @throws SQLException if lookup fails
     */
    public Optional<Long> findId(final Connection connection, final String server, final String world)
            throws SQLException {
        final Optional<Long> serverId = serverTable.findId(connection, server);
        return serverId.isEmpty() ? Optional.empty() : findId(connection, serverId.get(), world);
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
     * Reads all known world names.
     *
     * @param connection database connection
     * @return known world names
     * @throws SQLException if lookup fails
     */
    public Set<String> getAllWorlds(final Connection connection) throws SQLException {
        final Set<String> worlds = new LinkedHashSet<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT DISTINCT `world` FROM `" + tableName + "` ORDER BY `world`");
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                worlds.add(result.getString("world"));
            }
        }
        return worlds;
    }

    @Override
    public String toString() {
        return tableName;
    }
}

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
 * Table helper for server entries.
 */
public class ServerTable {

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * Constructs a ServerTable instance with the specified table name and SQL dialect.
     *
     * @param tableName the name of the database table
     */
    /* default */
    public ServerTable(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Ensures a server entry exists in the database and returns its ID.
     * If the server entry does not already exist, it is created.
     *
     * @param connection the database connection to use for the operation
     * @param server     the name of the server to ensure exists in the database
     * @return the ID of the server entry
     * @throws SQLException if a database access error occurs, or the server entry cannot be created
     */
    public long ensureServer(final Connection connection, final String server) throws SQLException {
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

    /**
     * Finds an existing server id.
     *
     * @param connection database connection
     * @param server server name
     * @return server id if present
     * @throws SQLException if lookup fails
     */
    public Optional<Long> findId(final Connection connection, final String server) throws SQLException {
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

    /**
     * Reads all known server names.
     *
     * @param connection database connection
     * @return known server names
     * @throws SQLException if lookup fails
     */
    public Set<String> getAllServers(final Connection connection) throws SQLException {
        final Set<String> servers = new LinkedHashSet<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `server` FROM `" + tableName + "` ORDER BY `server`");
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                servers.add(result.getString("server"));
            }
        }
        return servers;
    }
}

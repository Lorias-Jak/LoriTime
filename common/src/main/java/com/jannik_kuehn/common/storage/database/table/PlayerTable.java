package com.jannik_kuehn.common.storage.database.table;

import com.jannik_kuehn.common.storage.database.SqlDialect;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Table helper for player entries.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class PlayerTable {

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The {@link SqlDialect} instance.
     */
    private final SqlDialect dialect;

    /**
     * Constructs a new instance of PlayerTable with the specified table name and SQL dialect.
     *
     * @param tableName the name of the table to be managed
     * @param dialect   the SQL dialect to use for generating SQL statements
     */
    /* default */
    public PlayerTable(final String tableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.dialect = dialect;
    }

    /**
     * Generates the SQL statement to create the table for player entries.
     *
     * @return the SQL CREATE TABLE statement for the player table
     */
    /* default */
    public String createTableSql() {
        return dialect.createPlayerTable(tableName);
    }

    /**
     * Retrieves the name of the table managed by this instance.
     *
     * @return the name of the table as a String
     */
    /* default */
    public String getTableName() {
        return tableName;
    }

    /**
     * Checks if the table managed by this instance contains any data.
     *
     * @param connection the database connection to use for executing the query
     * @return true if the table contains at least one row, false otherwise
     * @throws SQLException if a database access error occurs
     */
    /* default */
    public boolean hasAnyData(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM `" + tableName + "` LIMIT 1")) {
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /**
     * Finds the ID associated with the given UUID in the database.
     *
     * @param connection the database connection to be used for the query
     * @param uuid       the UUID for which the associated ID should be retrieved
     * @return an Optional containing the ID if found, or an empty Optional if no matching entry exists
     * @throws SQLException if an error occurs while accessing the database
     */
    /* default */
    Optional<Long> findIdByUuid(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + tableName + "` WHERE `uuid` = ?")) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves the ID associated with the given name from the database.
     *
     * @param connection the database connection to use for executing the query
     * @param name       the name for which the associated ID should be retrieved
     * @return an Optional containing the
     */
    /* default */
    Optional<Long> findIdByName(final Connection connection, final String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + tableName + "` WHERE `name` = ?")) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves the UUID associated with the given name from the database.
     *
     * @param connection the database connection to use for executing the query
     * @param name       the name for which the associated UUID should be retrieved
     * @return an Optional containing the UUID if found, or an empty Optional if no matching entry exists
     * @throws SQLException if a database access error occurs
     */
    /* default */
    public Optional<UUID> findUuidByName(final Connection connection, final String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `uuid` FROM `" + tableName + "` WHERE `name` = ?")) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(UuidUtil.fromBytes(result.getBytes("uuid")));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the name associated with the given UUID in the database.
     *
     * @param connection the database connection to be used for the query
     * @param uuid       the UUID for which the associated name should be retrieved
     * @return an Optional containing the name if found, or
     */
    /* default */
    public Optional<String> findNameByUuid(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `name` FROM `" + tableName + "` WHERE `uuid` = ?")) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.ofNullable(result.getString("name"));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all non-null names from the database table managed by this instance.
     *
     * @param connection the database connection to use for executing the query
     * @return a Set containing all unique names retrieved from the table
     * @throws SQLException if a database access error
     */
    /* default */
    public Set<String> getAllNames(final Connection connection) throws SQLException {
        final Set<String> names = new HashSet<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `name` FROM `" + tableName + "` WHERE `name` IS NOT NULL")) {
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    names.add(result.getString("name"));
                }
            }
        }
        return names;
    }

    /**
     * Ensures that a player with the specified UUID exists in the database. If the player exists,
     * their name and last seen timestamp are updated as needed. If the player does not exist, they
     * are inserted into the database with the provided UUID and, if present, the given name.
     *
     * @param connection the database connection to use for the operation
     * @param uuid       the unique identifier of the player
     * @param name       an Optional containing the name of the player, if available
     * @return the unique ID of the player in the database
     * @throws SQLException if a database access error occurs during the operation
     */
    /* default */
    public long ensurePlayer(final Connection connection, final UUID uuid, final Optional<String> name) throws SQLException {
        final Optional<Long> existingId = findIdByUuid(connection, uuid);
        if (existingId.isPresent()) {
            if (name.isPresent()) {
                releaseTakenName(connection, name.get(), uuid);
                updatePlayerName(connection, existingId.get(), name.get());
            }
            updateLastSeen(connection, existingId.get());
            return existingId.get();
        }

        if (name.isPresent()) {
            releaseTakenName(connection, name.get(), uuid);
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`uuid`, `name`, `last_seen`) VALUES (?, ?, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setBytes(1, UuidUtil.toBytes(uuid));
            if (name.isPresent()) {
                insert.setString(2, name.get());
            } else {
                insert.setNull(2, Types.VARCHAR);
            }
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to insert player");
    }

    /* default */
    public void deleteByUuid(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM `" + tableName + "` WHERE `uuid` = ?")) {
            delete.setBytes(1, UuidUtil.toBytes(uuid));
            delete.executeUpdate();
        }
    }

    private void releaseTakenName(final Connection connection, final String name, final UUID owner) throws SQLException {
        final Optional<Long> oldHolder = findIdByName(connection, name);
        if (oldHolder.isEmpty()) {
            return;
        }

        final Optional<Long> ownerId = findIdByUuid(connection, owner);
        if (ownerId.isPresent() && ownerId.get().equals(oldHolder.get())) {
            return;
        }

        try (PreparedStatement unsetName = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `name` = NULL WHERE `id` = ?")) {
            unsetName.setLong(1, oldHolder.get());
            unsetName.executeUpdate();
        }
    }

    private void updatePlayerName(final Connection connection, final long playerId, final String name) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `name` = ?, `last_seen` = CURRENT_TIMESTAMP WHERE `id` = ?")) {
            update.setString(1, name);
            update.setLong(2, playerId);
            update.executeUpdate();
        }
    }

    private void updateLastSeen(final Connection connection, final long playerId) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `last_seen` = CURRENT_TIMESTAMP WHERE `id` = ?")) {
            update.setLong(1, playerId);
            update.executeUpdate();
        }
    }
}

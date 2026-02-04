package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Table helper for time entries.
 */
final class TimeTable {

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The player table name.
     */
    private final String playerTableName;

    /**
     * The world table name.
     */
    private final String worldTableName;

    /**
     * The {@link SqlDialect} instance.
     */
    private final SqlDialect dialect;

    /**
     * Default constructor.
     *
     * @param tableName the table name
     * @param playerTableName the player table name
     * @param worldTableName the world table name
     * @param dialect the {@link SqlDialect} instance
     */
    /* default */
    TimeTable(final String tableName, final String playerTableName, final String worldTableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.playerTableName = playerTableName;
        this.worldTableName = worldTableName;
        this.dialect = dialect;
    }

    /**
     * Generates the SQL statement for creating the time table.
     * This method utilizes the associated SQL dialect to construct
     * the CREATE TABLE statement.
     *
     * @return the SQL statement for creating the time table
     */
    /* default */
    String createTableSql() {
        return dialect.createTimeTable(tableName, playerTableName, worldTableName);
    }

    /**
     * Inserts a player's session duration into the database.
     *
     * This method calculates the player's join and leave timestamps based on the provided duration
     * in seconds and inserts these values, along with the player ID and world ID, into the appropriate table.
     *
     * @param connection the database connection to use for the operation
     * @param playerId the unique identifier of the player
     * @param worldId the unique identifier of the world
     * @param durationSeconds the duration of the player's session in seconds
     * @throws SQLException if an SQL error occurs while attempting to insert the data
     */
    /* default */
    void insertDuration(final Connection connection, final long playerId, final long worldId, final long durationSeconds)
            throws SQLException {
        final Instant leave = Instant.now();
        final Instant join = leave.minusSeconds(durationSeconds);
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`player_id`, `world_id`, `join_time`, `leave_time`) "
                        + "VALUES (?, ?, ?, ?)")) {
            insert.setLong(1, playerId);
            insert.setLong(2, worldId);
            insert.setTimestamp(3, Timestamp.from(join));
            insert.setTimestamp(4, Timestamp.from(leave));
            insert.executeUpdate();
        }
    }

    /**
     * Calculates the total session duration for a player by summing up the recorded
     * duration in the database for the specified player UUID.
     *
     * @param connection the database connection to use for the query
     * @param uuid the unique identifier of the player whose total duration is to be calculated
     * @return an {@code OptionalLong} containing the total session duration in seconds if present,
     *         or an empty {@code OptionalLong} if no data exists for the specified player
     * @throws SQLException if an SQL error occurs during the query execution
     */
    /* default */
    OptionalLong sumForPlayer(final Connection connection, final UUID uuid) throws SQLException {
        final String durationExpression = dialect.durationSecondsExpression("t.join_time", "t.leave_time");
        final String sql = "SELECT SUM(" + durationExpression + ") AS total "
                + "FROM `" + tableName + "` t "
                + "JOIN `" + playerTableName + "` p ON p.id = t.player_id "
                + "WHERE p.uuid = ?";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    final long value = result.getLong("total");
                    if (!result.wasNull()) {
                        return OptionalLong.of(value);
                    }
                }
            }
            return OptionalLong.empty();
        }
    }

    /**
     * Retrieves the total session duration for all players from the database.
     * The session duration is calculated by summing up the differences between
     * join and leave timestamps for each player.
     *
     * @param connection the database connection to use for executing the query
     * @return a map where the keys are player UUIDs (as strings), and the values are
     *         the total session durations in seconds
     * @throws SQLException if an SQL error occurs during the query execution
     */
    /* default */
    Map<String, Long> getAllTotals(final Connection connection) throws SQLException {
        final Map<String, Long> totals = new HashMap<>();
        final String durationExpression = dialect.durationSecondsExpression("t.join_time", "t.leave_time");
        final String sql = "SELECT p.uuid AS uuid, SUM(" + durationExpression + ") AS total "
                + "FROM `" + tableName + "` t "
                + "JOIN `" + playerTableName + "` p ON p.id = t.player_id "
                + "GROUP BY p.uuid";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    totals.put(UuidUtil.fromBytes(result.getBytes("uuid")).toString(), result.getLong("total"));
                }
            }
        }
        return totals;
    }
}

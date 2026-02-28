package com.jannik_kuehn.common.storage.database.table;

import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.storage.database.SqlDialect;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Table helper for time entries.
 */
public final class TimeTable {
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
     * Time source used for duration inserts.
     */
    private final Clock clock;

    /**
     * Default constructor.
     *
     * @param tableName       the table name
     * @param playerTableName the player table name
     * @param worldTableName  the world table name
     * @param dialect         the {@link SqlDialect} instance
     */
    /* default */
    public TimeTable(final String tableName, final String playerTableName, final String worldTableName, final SqlDialect dialect) {
        this(tableName, playerTableName, worldTableName, dialect, Clock.systemUTC());
    }

    /**
     * Default constructor with a custom time source.
     *
     * @param tableName       the table name
     * @param playerTableName the player table name
     * @param worldTableName  the world table name
     * @param dialect         the {@link SqlDialect} instance
     * @param clock           time source used to create join/leave timestamps
     */
    /* default */
    public TimeTable(final String tableName,
                     final String playerTableName,
                     final String worldTableName,
                     final SqlDialect dialect,
                     final Clock clock) {
        this.tableName = tableName;
        this.playerTableName = playerTableName;
        this.worldTableName = worldTableName;
        this.dialect = dialect;
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Generates the SQL statement for creating the timetable.
     * This method uses the associated SQL dialect to construct
     * the CREATE TABLE statement.
     *
     * @return the SQL statement for creating the timetable
     */
    /* default */
    public String createTableSql() {
        return dialect.createTimeTable(tableName, playerTableName, worldTableName);
    }

    /**
     * Inserts or updates a player's session duration with the given reason.
     *
     * <p>Reasons {@link TimeEntryReason#PLAYER_LEAVE}, {@link TimeEntryReason#AUTO_FLUSH} and
     * {@link TimeEntryReason#SHUTDOWN_FLUSH} update the latest entry by setting the leave timestamp
     * to the current moment and replacing the reason. Other reasons create a fresh entry.</p>
     *
     * @param connection      the database connection
     * @param playerId        player id
     * @param worldId         world id
     * @param durationSeconds duration in seconds
     * @param reason          reason for writing the entry
     * @throws SQLException if persistence fails
     */
    /* default */
    public void insertDuration(final Connection connection,
                               final long playerId,
                               final long worldId,
                               final long durationSeconds,
                               final TimeEntryReason reason) throws SQLException {
        Objects.requireNonNull(reason);
        if (shouldUpdateLatest(reason) && updateLatestDuration(connection, playerId, worldId, reason)) {
            return;
        }
        insertNewDuration(connection, playerId, worldId, durationSeconds, reason);
    }

    private boolean shouldUpdateLatest(final TimeEntryReason reason) {
        return EnumSet.of(TimeEntryReason.PLAYER_LEAVE, TimeEntryReason.AUTO_FLUSH, TimeEntryReason.SHUTDOWN_FLUSH).contains(reason);
    }

    private void insertNewDuration(final Connection connection,
                                   final long playerId,
                                   final long worldId,
                                   final long durationSeconds,
                                   final TimeEntryReason reason) throws SQLException {
        final Instant leave = Instant.now(clock);
        final Instant join = leave.minusSeconds(durationSeconds);
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`player_id`, `world_id`, `join_time`, `leave_time`, `reason`) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            insert.setLong(1, playerId);
            insert.setLong(2, worldId);
            insert.setTimestamp(3, Timestamp.from(join));
            insert.setTimestamp(4, Timestamp.from(leave));
            insert.setString(5, reason.name());
            insert.executeUpdate();
        }
    }

    private boolean updateLatestDuration(final Connection connection,
                                         final long playerId,
                                         final long worldId,
                                         final TimeEntryReason reason) throws SQLException {
        final Long entryId = findLatestEntryId(connection, playerId, worldId);
        if (entryId == null) {
            return false;
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `leave_time` = ?, `reason` = ? WHERE `id` = ?")) {
            update.setTimestamp(1, Timestamp.from(Instant.now(clock)));
            update.setString(2, reason.name());
            update.setLong(3, entryId);
            return update.executeUpdate() > 0;
        }
    }

    private Long findLatestEntryId(final Connection connection, final long playerId, final long worldId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + tableName + "` WHERE `player_id` = ? AND `world_id` = ? ORDER BY `leave_time` DESC, `id` DESC LIMIT 1")) {
            select.setLong(1, playerId);
            select.setLong(2, worldId);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return result.getLong("id");
                }
            }
        }
        return null;
    }

    /**
     * Calculates the total session duration for a player by summing up the recorded
     * duration in the database for the specified player UUID.
     *
     * @param connection the database connection to use for the query
     * @param uuid       the unique identifier of the player whose total duration is to be calculated
     * @return an {@code OptionalLong} containing the total session duration in seconds if present,
     * or an empty {@code OptionalLong} if no data exists for the specified player
     * @throws SQLException if an SQL error occurs during the query execution
     */
    /* default */
    public OptionalLong sumForPlayer(final Connection connection, final UUID uuid) throws SQLException {
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
     * the total session durations in seconds
     * @throws SQLException if an SQL error occurs during the query execution
     */
    /* default */
    public Map<String, Long> getAllTotals(final Connection connection) throws SQLException {
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

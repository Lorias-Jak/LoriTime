package com.jannik_kuehn.common.storage.database.table;

import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.storage.database.SqlDialect;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
@SuppressWarnings("PMD.TooManyMethods")
public final class TimeTable {
    /**
     * Suffix used by the normalized session table.
     */
    private static final String TIME_SUFFIX = "_time";

    /**
     * Suffix used by the normalized world table.
     */
    private static final String WORLD_SUFFIX = "_world";

    /**
     * Suffix used by the normalized server table.
     */
    private static final String SERVER_SUFFIX = "_server";

    /**
     * SQL alias for the session join timestamp.
     */
    private static final String JOIN_TIME = "t.join_time";

    /**
     * SQL alias for the session leave timestamp.
     */
    private static final String LEAVE_TIME = "t.leave_time";

    /**
     * Shared SQL join for world-scoped time queries.
     */
    private static final String WORLD_TIME_JOIN = "` w ON w.id = t.world_id ";

    /**
     * Shared SQL join for server-scoped time queries.
     */
    private static final String SERVER_TIME_JOIN = "` s ON s.id = w.server_id ";

    /**
     * Minimum overlap required for a ranged row to count as data.
     */
    private static final long MINIMUM_OVERLAP_SECONDS = 0L;

    /**
     * Shared SQL total alias fragment.
     */
    private static final String TOTAL_ALIAS = ") AS total ";

    /**
     * Shared SQL FROM fragment for the session table.
     */
    private static final String FROM_TIME = "FROM `";

    /**
     * Shared SQL session table alias fragment.
     */
    private static final String TIME_TABLE_ALIAS = "` t ";

    /**
     * Shared SQL JOIN fragment.
     */
    private static final String JOIN_TABLE = "JOIN `";

    /**
     * Shared SQL player join fragment.
     */
    private static final String PLAYER_JOIN = "` p ON p.id = t.player_id ";

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The player table name.
     */
    private final PlayerTable playerTable;

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
     * @param tableName   the table name
     * @param playerTable the player table name
     * @param dialect     the {@link SqlDialect} instance
     */
    public TimeTable(final String tableName, final PlayerTable playerTable, final SqlDialect dialect) {
        this(tableName, playerTable, dialect, Clock.systemUTC());
    }

    /**
     * Default constructor with a custom time source.
     *
     * @param tableName   the table name
     * @param playerTable the player table name
     * @param dialect     the {@link SqlDialect} instance
     * @param clock       time source used to create join/leave timestamps
     */
    /* default */
    public TimeTable(final String tableName,
                     final PlayerTable playerTable,
                     final SqlDialect dialect,
                     final Clock clock) {
        this.tableName = tableName;
        this.playerTable = playerTable;
        this.dialect = dialect;
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Inserts or updates a player's session duration with the given reason.
     *
     * <p>Reasons {@link TimeEntryReason#PLAYER_LEAVE}, {@link TimeEntryReason#PLAYER_AFK},
     * {@link TimeEntryReason#PLAYER_AFK_KICK}, {@link TimeEntryReason#AUTO_FLUSH} and
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

    /**
     * Inserts a session with explicit join and leave timestamps.
     *
     * @param connection the database connection
     * @param playerId   player id
     * @param worldId    world id
     * @param join       join timestamp
     * @param leave      leave timestamp
     * @param reason     persistence reason
     * @throws SQLException if persistence fails
     */
    public long insertSession(final Connection connection,
                              final long playerId,
                              final long worldId,
                              final Instant join,
                              final Instant leave,
                              final TimeEntryReason reason) throws SQLException {
        Objects.requireNonNull(join);
        Objects.requireNonNull(leave);
        Objects.requireNonNull(reason);
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`player_id`, `world_id`, `join_time`, `leave_time`, `reason`) "
                        + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setLong(1, playerId);
            insert.setLong(2, worldId);
            insert.setTimestamp(3, Timestamp.from(join));
            insert.setTimestamp(4, Timestamp.from(leave));
            insert.setString(5, reason.name());
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to insert session");
    }

    /**
     * Updates the leave timestamp and reason for an existing session row.
     *
     * @param connection database connection
     * @param sessionId  session id
     * @param leave      leave timestamp
     * @param reason     persistence reason
     * @throws SQLException if the update fails
     */
    public void updateSession(final Connection connection,
                              final long sessionId,
                              final Instant leave,
                              final TimeEntryReason reason) throws SQLException {
        Objects.requireNonNull(leave);
        Objects.requireNonNull(reason);
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `leave_time` = ?, `reason` = ? WHERE `id` = ?")) {
            update.setTimestamp(1, Timestamp.from(leave));
            update.setString(2, reason.name());
            update.setLong(3, sessionId);
            update.executeUpdate();
        }
    }

    /**
     * Updates the world context for an existing session row.
     *
     * @param connection database connection
     * @param sessionId  session id
     * @param worldId    world id
     * @throws SQLException if the update fails
     */
    public void updateSessionWorld(final Connection connection,
                                   final long sessionId,
                                   final long worldId) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `world_id` = ? WHERE `id` = ?")) {
            update.setLong(1, worldId);
            update.setLong(2, sessionId);
            update.executeUpdate();
        }
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
    public OptionalLong sumForPlayer(final Connection connection, final UUID uuid) throws SQLException {
        final String durationExpression = dialect.durationSecondsExpression(JOIN_TIME, LEAVE_TIME);
        final String sql = "SELECT SUM(" + durationExpression + TOTAL_ALIAS
                + FROM_TIME + tableName + TIME_TABLE_ALIAS
                + JOIN_TABLE + playerTable + PLAYER_JOIN
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
     * Calculates the ranged total session duration for a player.
     *
     * @param connection database connection
     * @param uuid player UUID
     * @param range time range
     * @return optional duration sum
     * @throws SQLException if lookup fails
     */
    public OptionalLong sumForPlayer(final Connection connection, final UUID uuid, final TimeRange range)
            throws SQLException {
        final String sql = "SELECT t.`join_time`, t.`leave_time` "
                + FROM_TIME + tableName + TIME_TABLE_ALIAS
                + JOIN_TABLE + playerTable + PLAYER_JOIN
                + "WHERE p.uuid = ?";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            return optionalRangedTotal(select, range);
        }
    }

    /**
     * Calculates the total session duration for a player on one server.
     *
     * @param connection database connection
     * @param uuid player UUID
     * @param server server name
     * @return optional duration sum
     * @throws SQLException if lookup fails
     */
    public OptionalLong sumForPlayerAndServer(final Connection connection, final UUID uuid, final String server)
            throws SQLException {
        final String durationExpression = dialect.durationSecondsExpression(JOIN_TIME, LEAVE_TIME);
        final String sql = "SELECT SUM(" + durationExpression + TOTAL_ALIAS
                + FROM_TIME + tableName + TIME_TABLE_ALIAS
                + JOIN_TABLE + playerTable + PLAYER_JOIN
                + JOIN_TABLE + worldTableName() + WORLD_TIME_JOIN
                + JOIN_TABLE + serverTableName() + SERVER_TIME_JOIN
                + "WHERE p.uuid = ? AND s.server = ?";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            select.setString(2, server);
            return optionalTotal(select);
        }
    }

    /**
     * Calculates the ranged total session duration for a player on one server.
     *
     * @param connection database connection
     * @param uuid player UUID
     * @param server server name
     * @param range time range
     * @return optional duration sum
     * @throws SQLException if lookup fails
     */
    public OptionalLong sumForPlayerAndServer(final Connection connection, final UUID uuid,
                                              final String server, final TimeRange range) throws SQLException {
        final String sql = "SELECT t.`join_time`, t.`leave_time` "
                + FROM_TIME + tableName + TIME_TABLE_ALIAS
                + JOIN_TABLE + playerTable + PLAYER_JOIN
                + JOIN_TABLE + worldTableName() + WORLD_TIME_JOIN
                + JOIN_TABLE + serverTableName() + SERVER_TIME_JOIN
                + "WHERE p.uuid = ? AND s.server = ?";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            select.setString(2, server);
            return optionalRangedTotal(select, range);
        }
    }

    /**
     * Calculates the total session duration for a player in one world.
     *
     * @param connection database connection
     * @param uuid player UUID
     * @param server server name
     * @param world world name
     * @return optional duration sum
     * @throws SQLException if lookup fails
     */
    public OptionalLong sumForPlayerAndWorld(final Connection connection, final UUID uuid,
                                             final String server, final String world) throws SQLException {
        final String durationExpression = dialect.durationSecondsExpression(JOIN_TIME, LEAVE_TIME);
        final String sql = "SELECT SUM(" + durationExpression + TOTAL_ALIAS
                + FROM_TIME + tableName + TIME_TABLE_ALIAS
                + JOIN_TABLE + playerTable + PLAYER_JOIN
                + JOIN_TABLE + worldTableName() + WORLD_TIME_JOIN
                + JOIN_TABLE + serverTableName() + SERVER_TIME_JOIN
                + "WHERE p.uuid = ? AND s.server = ? AND w.world = ?";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            select.setString(2, server);
            select.setString(3, world);
            return optionalTotal(select);
        }
    }

    /**
     * Calculates the ranged total session duration for a player in one world.
     *
     * @param connection database connection
     * @param uuid player UUID
     * @param server server name
     * @param world world name
     * @param range time range
     * @return optional duration sum
     * @throws SQLException if lookup fails
     */
    public OptionalLong sumForPlayerAndWorld(final Connection connection, final UUID uuid,
                                             final String server, final String world,
                                             final TimeRange range) throws SQLException {
        final String sql = "SELECT t.`join_time`, t.`leave_time` "
                + FROM_TIME + tableName + TIME_TABLE_ALIAS
                + JOIN_TABLE + playerTable + PLAYER_JOIN
                + JOIN_TABLE + worldTableName() + WORLD_TIME_JOIN
                + JOIN_TABLE + serverTableName() + SERVER_TIME_JOIN
                + "WHERE p.uuid = ? AND s.server = ? AND w.world = ?";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            select.setString(2, server);
            select.setString(3, world);
            return optionalRangedTotal(select, range);
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
    public Map<String, Long> getAllTotals(final Connection connection) throws SQLException {
        final Map<String, Long> totals = new HashMap<>();
        final String durationExpression = dialect.durationSecondsExpression(JOIN_TIME, LEAVE_TIME);
        final String sql = "SELECT p.uuid AS uuid, SUM(" + durationExpression + TOTAL_ALIAS
                + FROM_TIME + tableName + TIME_TABLE_ALIAS
                + JOIN_TABLE + playerTable + PLAYER_JOIN
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

    /**
     * Deletes time history for players inactive before the cutoff expression.
     *
     * @param connection database connection
     * @param cutoffSql  SQL timestamp cutoff expression
     * @return deleted rows
     * @throws SQLException if delete fails
     */
    public int deleteInactiveHistory(final Connection connection, final String cutoffSql) throws SQLException {
        return playerTable.deleteInactiveHistory(connection, tableName, cutoffSql);
    }

    /**
     * Deletes all session rows for one player.
     *
     * @param connection database connection
     * @param playerId   player id
     * @return deleted rows
     * @throws SQLException if delete fails
     */
    public int deleteForPlayer(final Connection connection, final long playerId) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM `" + tableName + "` WHERE `player_id` = ?")) {
            delete.setLong(1, playerId);
            return delete.executeUpdate();
        }
    }

    private boolean shouldUpdateLatest(final TimeEntryReason reason) {
        return EnumSet.of(TimeEntryReason.PLAYER_LEAVE, TimeEntryReason.PLAYER_AFK,
                TimeEntryReason.PLAYER_AFK_KICK, TimeEntryReason.AUTO_FLUSH, TimeEntryReason.SHUTDOWN_FLUSH)
                .contains(reason);
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

    private OptionalLong optionalTotal(final PreparedStatement select) throws SQLException {
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

    private OptionalLong optionalRangedTotal(final PreparedStatement select, final TimeRange range) throws SQLException {
        long total = 0L;
        boolean matched = false;
        try (ResultSet result = select.executeQuery()) {
            while (result.next()) {
                final long overlap = range.overlapSeconds(
                        DatabaseInstantReader.readInstant(result, "join_time").toEpochMilli(),
                        DatabaseInstantReader.readInstant(result, "leave_time").toEpochMilli());
                if (overlap > MINIMUM_OVERLAP_SECONDS) {
                    total += overlap;
                    matched = true;
                }
            }
        }
        return matched ? OptionalLong.of(total) : OptionalLong.empty();
    }

    private String worldTableName() {
        return tableName.replace(TIME_SUFFIX, WORLD_SUFFIX);
    }

    private String serverTableName() {
        return tableName.replace(TIME_SUFFIX, SERVER_SUFFIX);
    }

    @Override
    public String toString() {
        return tableName;
    }
}

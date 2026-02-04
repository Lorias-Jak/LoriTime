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

    private final String tableName;

    private final String playerTableName;

    private final String worldTableName;

    private final SqlDialect dialect;

    TimeTable(final String tableName, final String playerTableName, final String worldTableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.playerTableName = playerTableName;
        this.worldTableName = worldTableName;
        this.dialect = dialect;
    }

    String createTableSql() {
        return dialect.createTimeTable(tableName, playerTableName, worldTableName);
    }

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

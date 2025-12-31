package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

final class TimeTable {

    private final String tableName;
    private final String playerTableName;
    private final String worldTableName;

    TimeTable(final String tableName, final String playerTableName, final String worldTableName) {
        this.tableName = tableName;
        this.playerTableName = playerTableName;
        this.worldTableName = worldTableName;
    }

    String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`player_id` BIGINT NOT NULL,"
                + "`world_id` BIGINT NOT NULL,"
                + "`join_time` TIMESTAMP NOT NULL,"
                + "`leave_time` TIMESTAMP NOT NULL,"
                + "INDEX `idx_time_player` (`player_id`),"
                + "INDEX `idx_time_world` (`world_id`),"
                + "CONSTRAINT `fk_time_player` FOREIGN KEY (`player_id`) REFERENCES `"
                + playerTableName + "`(`id`) ON DELETE CASCADE,"
                + "CONSTRAINT `fk_time_world` FOREIGN KEY (`world_id`) REFERENCES `"
                + worldTableName + "`(`id`) ON DELETE CASCADE"
                + ") ENGINE InnoDB";
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
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT SUM(TIMESTAMPDIFF(SECOND, t.join_time, t.leave_time)) AS total "
                        + "FROM `" + tableName + "` t "
                        + "JOIN `" + playerTableName + "` p ON p.id = t.player_id "
                        + "WHERE p.uuid = ?")) {
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
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT p.uuid AS uuid, SUM(TIMESTAMPDIFF(SECOND, t.join_time, t.leave_time)) AS total "
                        + "FROM `" + tableName + "` t "
                        + "JOIN `" + playerTableName + "` p ON p.id = t.player_id "
                        + "GROUP BY p.uuid")) {
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    totals.put(UuidUtil.fromBytes(result.getBytes("uuid")).toString(), result.getLong("total"));
                }
            }
        }
        return totals;
    }
}

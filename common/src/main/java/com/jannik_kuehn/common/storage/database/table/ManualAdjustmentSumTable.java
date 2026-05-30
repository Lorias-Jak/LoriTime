package com.jannik_kuehn.common.storage.database.table;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;

/**
 * Aggregate query helper for manual time adjustments.
 */
public final class ManualAdjustmentSumTable {

    /**
     * Adjustment table name.
     */
    private final String tableName;

    /**
     * Creates a manual adjustment sum helper.
     *
     * @param tableName adjustment table name
     */
    public ManualAdjustmentSumTable(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Sums adjustment seconds for one player.
     *
     * @param connection database connection
     * @param playerId   player id
     * @return optional sum
     * @throws SQLException if the query fails
     */
    public OptionalLong sumForPlayer(final Connection connection, final long playerId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT SUM(`amount_seconds`) AS total FROM `" + tableName + "` WHERE `player_id` = ?")) {
            select.setLong(1, playerId);
            return optionalTotal(select);
        }
    }

    /**
     * Sums adjustment seconds for one player and server scope.
     *
     * @param connection database connection
     * @param playerId player id
     * @param serverId server id
     * @return optional sum
     * @throws SQLException if the query fails
     */
    public OptionalLong sumForPlayerAndServer(final Connection connection, final long playerId, final long serverId)
            throws SQLException {
        final String sql = "SELECT SUM(a.`amount_seconds`) AS total "
                + "FROM `" + tableName + "` a "
                + "LEFT JOIN `" + tableName.replace("_time_adjustment", "_world") + "` w ON w.`id` = a.`world_id` "
                + "WHERE a.`player_id` = ? "
                + "AND ((a.`scope_type` = ? AND a.`server_id` = ?) "
                + "OR (a.`scope_type` = ? AND w.`server_id` = ?))";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setLong(1, playerId);
            select.setString(2, TimeScope.Type.SERVER.name());
            select.setLong(3, serverId);
            select.setString(4, TimeScope.Type.WORLD.name());
            select.setLong(5, serverId);
            return optionalTotal(select);
        }
    }

    /**
     * Sums adjustment seconds for one player and server scope in a time range.
     *
     * @param connection database connection
     * @param playerId player id
     * @param serverId server id
     * @param range time range
     * @return optional sum
     * @throws SQLException if the query fails
     */
    public OptionalLong sumForPlayerAndServer(final Connection connection, final long playerId,
                                              final long serverId, final TimeRange range) throws SQLException {
        final String sql = "SELECT a.`amount_seconds`, a.`created_at` "
                + "FROM `" + tableName + "` a "
                + "LEFT JOIN `" + tableName.replace("_time_adjustment", "_world") + "` w ON w.`id` = a.`world_id` "
                + "WHERE a.`player_id` = ? "
                + "AND ((a.`scope_type` = ? AND a.`server_id` = ?) "
                + "OR (a.`scope_type` = ? AND w.`server_id` = ?))";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setLong(1, playerId);
            select.setString(2, TimeScope.Type.SERVER.name());
            select.setLong(3, serverId);
            select.setString(4, TimeScope.Type.WORLD.name());
            select.setLong(5, serverId);
            return optionalRangedTotal(select, range);
        }
    }

    /**
     * Sums adjustment seconds for one player and world scope.
     *
     * @param connection database connection
     * @param playerId player id
     * @param worldId world id
     * @return optional sum
     * @throws SQLException if the query fails
     */
    public OptionalLong sumForPlayerAndWorld(final Connection connection, final long playerId, final long worldId)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT SUM(`amount_seconds`) AS total FROM `" + tableName + "` "
                        + "WHERE `player_id` = ? AND `scope_type` = ? AND `world_id` = ?")) {
            select.setLong(1, playerId);
            select.setString(2, TimeScope.Type.WORLD.name());
            select.setLong(3, worldId);
            return optionalTotal(select);
        }
    }

    /**
     * Sums adjustment seconds for one player and world scope in a time range.
     *
     * @param connection database connection
     * @param playerId player id
     * @param worldId world id
     * @param range time range
     * @return optional sum
     * @throws SQLException if the query fails
     */
    public OptionalLong sumForPlayerAndWorld(final Connection connection, final long playerId,
                                             final long worldId, final TimeRange range) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `amount_seconds`, `created_at` FROM `" + tableName + "` "
                        + "WHERE `player_id` = ? AND `scope_type` = ? AND `world_id` = ?")) {
            select.setLong(1, playerId);
            select.setString(2, TimeScope.Type.WORLD.name());
            select.setLong(3, worldId);
            return optionalRangedTotal(select, range);
        }
    }

    /**
     * Sums adjustment seconds for one player in a time range.
     *
     * @param connection database connection
     * @param playerId   player id
     * @param range      time range
     * @return optional sum
     * @throws SQLException if the query fails
     */
    public OptionalLong sumForPlayer(final Connection connection, final long playerId, final TimeRange range)
            throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `amount_seconds`, `created_at` FROM `" + tableName + "` "
                        + "WHERE `player_id` = ?")) {
            select.setLong(1, playerId);
            return optionalRangedTotal(select, range);
        }
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

    private OptionalLong optionalRangedTotal(final PreparedStatement select, final TimeRange range)
            throws SQLException {
        long total = 0L;
        boolean matched = false;
        try (ResultSet result = select.executeQuery()) {
            while (result.next()) {
                if (range.contains(DatabaseInstantReader.readInstant(result, "created_at"))) {
                    total += result.getLong("amount_seconds");
                    matched = true;
                }
            }
        }
        if (matched) {
            return OptionalLong.of(total);
        }
        return OptionalLong.empty();
    }
}

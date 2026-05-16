package com.jannik_kuehn.common.storage.database.table;

import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Table helper for manual time adjustments.
 */
public final class ManualAdjustmentTable {

    /**
     * Adjustment table name.
     */
    private final String tableName;

    /**
     * Player table helper.
     */
    private final PlayerTable playerTable;

    /**
     * Creates a manual adjustment table helper.
     *
     * @param tableName   adjustment table name
     * @param playerTable player table helper
     */
    public ManualAdjustmentTable(final String tableName, final PlayerTable playerTable) {
        this.tableName = tableName;
        this.playerTable = playerTable;
    }

    /**
     * Inserts an adjustment row.
     *
     * @param connection database connection
     * @param playerId   adjusted player id
     * @param adjustment adjustment data
     * @throws SQLException if persistence fails
     */
    public void insert(final Connection connection, final long playerId, final ManualTimeAdjustment adjustment) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`player_id`, `amount_seconds`, `reason`, `actor_uuid`, `actor_name`) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            insert.setLong(1, playerId);
            insert.setLong(2, adjustment.amountSeconds());
            insert.setString(3, adjustment.reason().name());
            if (adjustment.actorUuid().isPresent()) {
                insert.setBytes(4, UuidUtil.toBytes(adjustment.actorUuid().get()));
            } else {
                insert.setNull(4, Types.BINARY);
            }
            insert.setString(5, adjustment.actorName());
            insert.executeUpdate();
        }
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
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    final long value = result.getLong("total");
                    if (!result.wasNull()) {
                        return OptionalLong.of(value);
                    }
                }
            }
        }
        return OptionalLong.empty();
    }

    /**
     * Returns all adjustment totals by player UUID string.
     *
     * @param connection database connection
     * @return totals map
     * @throws SQLException if the query fails
     */
    public Map<String, Long> getAllTotals(final Connection connection) throws SQLException {
        final Map<String, Long> totals = new HashMap<>();
        final String sql = "SELECT p.uuid AS uuid, SUM(a.`amount_seconds`) AS total "
                + "FROM `" + tableName + "` a "
                + "JOIN `" + playerTable + "` p ON p.id = a.player_id "
                + "GROUP BY p.uuid";
        try (PreparedStatement select = connection.prepareStatement(sql);
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                totals.put(UuidUtil.fromBytes(result.getBytes("uuid")).toString(), result.getLong("total"));
            }
        }
        return totals;
    }

    /**
     * Deletes adjustment history for players inactive before the cutoff expression.
     *
     * @param connection database connection
     * @param cutoffSql  SQL timestamp cutoff expression
     * @return deleted rows
     * @throws SQLException if delete fails
     */
    public int deleteInactiveHistory(final Connection connection, final String cutoffSql) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM `" + tableName + "` WHERE `player_id` IN ("
                        + "SELECT `id` FROM `" + playerTable + "` WHERE `last_seen` IS NOT NULL AND `last_seen` < " + cutoffSql + ")")) {
            return delete.executeUpdate();
        }
    }

    /**
     * Deletes all adjustment rows for one player.
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

    @Override
    public String toString() {
        return tableName;
    }
}

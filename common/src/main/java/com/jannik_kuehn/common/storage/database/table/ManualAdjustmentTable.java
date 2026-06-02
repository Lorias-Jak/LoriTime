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
     * Aggregate query helper for manual adjustments.
     */
    private final ManualAdjustmentSumTable sumTable;

    /**
     * Creates a manual adjustment table helper.
     *
     * @param tableName   adjustment table name
     * @param playerTable player table helper
     */
    public ManualAdjustmentTable(final String tableName, final PlayerTable playerTable) {
        this.tableName = tableName;
        this.playerTable = playerTable;
        this.sumTable = new ManualAdjustmentSumTable(tableName);
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
        insert(connection, playerId, OptionalLong.empty(), OptionalLong.empty(), adjustment);
    }

    /**
     * Inserts an adjustment row with resolved scope references.
     *
     * @param connection database connection
     * @param playerId adjusted player id
     * @param serverId scoped server id
     * @param worldId scoped world id
     * @param adjustment adjustment data
     * @throws SQLException if persistence fails
     */
    public void insert(final Connection connection, final long playerId,
                       final OptionalLong serverId,
                       final OptionalLong worldId,
                       final ManualTimeAdjustment adjustment) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + tableName + "` (`player_id`, `scope_type`, `server_id`, `world_id`, "
                        + "`amount_seconds`, `reason`, `actor_uuid`, `actor_name`) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setLong(1, playerId);
            insert.setString(2, adjustment.scope().type().name());
            if (serverId.isPresent()) {
                insert.setLong(3, serverId.getAsLong());
            } else {
                insert.setNull(3, Types.BIGINT);
            }
            if (worldId.isPresent()) {
                insert.setLong(4, worldId.getAsLong());
            } else {
                insert.setNull(4, Types.BIGINT);
            }
            insert.setLong(5, adjustment.amountSeconds());
            insert.setString(6, adjustment.reason().name());
            if (adjustment.actorUuid().isPresent()) {
                insert.setBytes(7, UuidUtil.toBytes(adjustment.actorUuid().get()));
            } else {
                insert.setNull(7, Types.BINARY);
            }
            insert.setString(8, adjustment.actorName());
            insert.executeUpdate();
        }
    }

    /**
     * Returns aggregate query helper for adjustment totals.
     *
     * @return adjustment sum helper
     */
    public ManualAdjustmentSumTable sums() {
        return sumTable;
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

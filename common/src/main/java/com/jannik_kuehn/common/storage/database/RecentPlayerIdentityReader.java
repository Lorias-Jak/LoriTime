package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.api.storage.RecentPlayerIdentity;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.provider.LoriTimeConnectionProvider;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Reads recently seen player identities from the database.
 */
public class RecentPlayerIdentityReader {

    /**
     * Disabled recent-day window.
     */
    private static final long DISABLED_RECENT_DAY_WINDOW = 0L;

    /**
     * Database connection provider.
     */
    private final LoriTimeConnectionProvider databaseProvider;

    /**
     * Player table helper.
     */
    private final PlayerTable playerIdentityTable;

    /**
     * SQL dialect.
     */
    private final DatabaseDialect sqlDialect;

    /**
     * Creates a recent player identity reader.
     *
     * @param provider database connection provider
     * @param playerTable player table helper
     * @param dialect SQL dialect
     */
    public RecentPlayerIdentityReader(final LoriTimeConnectionProvider provider,
                                      final PlayerTable playerTable,
                                      final DatabaseDialect dialect) {
        this.databaseProvider = provider;
        this.playerIdentityTable = playerTable;
        this.sqlDialect = dialect;
    }

    /**
     * Reads identities seen within the configured day window.
     *
     * @param recentDays recent-day window
     * @return recent player identities
     * @throws StorageException when the query fails
     */
    public List<RecentPlayerIdentity> read(final long recentDays) throws StorageException {
        if (recentDays <= DISABLED_RECENT_DAY_WINDOW) {
            return List.of();
        }
        try (Connection connection = databaseProvider.getConnection()) {
            return playerIdentityTable.getRecentIdentities(connection, recentConditionSql(recentDays));
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        }
    }

    private String recentConditionSql(final long recentDays) {
        return switch (sqlDialect) {
            case MYSQL, MARIADB -> "`last_seen` >= DATE_SUB(NOW(3), INTERVAL " + recentDays + " DAY)";
            case SQLITE -> "(DATETIME(`last_seen` / 1000, 'unixepoch') >= DATETIME('now', '-" + recentDays + " days') "
                    + "OR DATETIME(`last_seen`) >= DATETIME('now', '-" + recentDays + " days'))";
        };
    }
}

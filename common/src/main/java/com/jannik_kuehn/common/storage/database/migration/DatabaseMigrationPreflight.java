package com.jannik_kuehn.common.storage.database.migration;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.DatabaseDialect;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Determines how the database updater must be invoked for the current database state.
 */
public class DatabaseMigrationPreflight {

    /**
     * The database storage to inspect and update.
     */
    private final DatabaseStorage databaseStorage;

    /**
     * The logger.
     */
    private final WrappedLogger log;

    /**
     * Creates a new database migration preflight.
     *
     * @param databaseStorage the database storage
     * @param log             the logger
     */
    public DatabaseMigrationPreflight(final DatabaseStorage databaseStorage, final WrappedLogger log) {
        this.databaseStorage = databaseStorage;
        this.log = log;
    }

    /**
     * Applies the correct database startup route for fresh, versioned, or legacy SQL databases.
     *
     * @throws StorageException if detection or migration fails
     */
    public void migrateIfNecessary() throws StorageException {
        databaseStorage.openProvider();
        final String tablePrefix = databaseStorage.getTablePrefix();
        final String versionTable = tablePrefix + "_version";
        try (Connection connection = databaseStorage.getProvider().getConnection()) {
            if (connection == null) {
                throw new StorageException("Could not open database connection for migration preflight");
            }
            if (tableExists(connection, versionTable)) {
                log.info("Detected versioned LoriTime database. Running normal database updates.");
                databaseStorage.applyUpdates();
                return;
            }
            if (isLegacyAggregateTable(connection, tablePrefix)) {
                log.info("Detected LoriTime 1.x database table. Seeding database version 1 before migration.");
                seedVersionOne(connection, versionTable);
                databaseStorage.applyUpdates();
                return;
            }
            log.info("Detected fresh database. Running first startup database initialization.");
            databaseStorage.applyFirstStartup();
        } catch (final SQLException ex) {
            throw new StorageException("Database migration preflight failed", ex);
        }
    }

    private boolean tableExists(final Connection connection, final String tableName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM `" + tableName + "` WHERE 1 = 0");
             ResultSet resultSet = statement.executeQuery()) {
            return !resultSet.next();
        } catch (final SQLException ignored) {
            return false;
        }
    }

    private boolean isLegacyAggregateTable(final Connection connection, final String tableName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT `uuid`, `name`, `time` FROM `" + tableName + "` WHERE 1 = 0");
             ResultSet resultSet = statement.executeQuery()) {
            return !resultSet.next();
        } catch (final SQLException ignored) {
            return false;
        }
    }

    private void seedVersionOne(final Connection connection, final String versionTable) throws SQLException {
        final boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            createVersionTable(connection, versionTable);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO `" + versionTable + "` (`version_no`) VALUES (?)")) {
                insert.setInt(1, 1);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (final SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void createVersionTable(final Connection connection, final String versionTable) throws SQLException {
        final String sql = switch (databaseStorage.getDialect()) {
            case MYSQL, MARIADB -> "CREATE TABLE IF NOT EXISTS `" + versionTable + "` ("
                    + "`version_no` INT NOT NULL,"
                    + "`applied_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "INDEX `idx_version_no` (`version_no`)"
                    + ") ENGINE=InnoDB";
            case SQLITE -> "CREATE TABLE IF NOT EXISTS `" + versionTable + "` ("
                    + "`version_no` INTEGER NOT NULL,"
                    + "`applied_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ")";
        };
        try (PreparedStatement create = connection.prepareStatement(sql)) {
            create.executeUpdate();
        }

        if (databaseStorage.getDialect() == DatabaseDialect.SQLITE) {
            try (PreparedStatement createIndex = connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS `idx_" + versionTable + "_version_no` "
                            + "ON `" + versionTable + "` (`version_no`)")) {
                createIndex.executeUpdate();
            }
        }
    }
}

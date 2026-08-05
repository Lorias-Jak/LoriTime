package com.jannik_kuehn.common.storage.database.migration;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.common.utils.UuidUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseMigrationPreflightTest {

    private static final String TABLE_PREFIX = "loritime";

    private static final UUID PLAYER_UUID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @TempDir
    private File dataFolder;

    @Test
    void migratesVersionedDatabaseThroughUpdatePath() throws Exception {
        try (Connection connection = openSqlite()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE `" + TABLE_PREFIX + "_version` (`version_no` INTEGER NOT NULL)");
                statement.executeUpdate("INSERT INTO `" + TABLE_PREFIX + "_version` (`version_no`) VALUES (1)");
            }
        }

        migrate();

        try (Connection connection = openSqlite()) {
            final DatabaseSnapshot expected = new DatabaseSnapshot(3, false, true, true, true, true, true,
                    0, 0, 0, null, null, "global");
            assertEquals(expected, snapshot(connection), "versioned databases should use the update path");
        }
    }

    @Test
    void initializesFreshDatabaseWithFirstStartupSchema() throws Exception {
        migrate();

        try (Connection connection = openSqlite()) {
            final DatabaseSnapshot expected = new DatabaseSnapshot(3, false, true, true, true, true, true,
                    0, 0, 0, null, null, "global");
            assertEquals(expected, snapshot(connection), "fresh databases should run first startup schema creation");
        }
    }

    @Test
    void migratesLegacyAggregateTableToScopedVersionTwoSchema() throws Exception {
        try (Connection connection = openSqlite()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE `" + TABLE_PREFIX + "` ("
                        + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "`uuid` BLOB NOT NULL UNIQUE,"
                        + "`name` TEXT UNIQUE,"
                        + "`time` INTEGER NOT NULL DEFAULT 0"
                        + ")");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO `" + TABLE_PREFIX + "` (`uuid`, `name`, `time`) VALUES (?, ?, ?)")) {
                insert.setBytes(1, UuidUtil.toBytes(PLAYER_UUID));
                insert.setString(2, "Lorias_");
                insert.setLong(3, 123L);
                insert.executeUpdate();
            }
        }

        migrate();

        try (Connection connection = openSqlite()) {
            final DatabaseSnapshot expected = new DatabaseSnapshot(3, false, true, true, true, true, true,
                    1, 0, 1, "LEGACY_IMPORT", "WORLD", "global");
            assertEquals(expected, snapshot(connection), "legacy v1 aggregate data should be imported into scoped storage");
        }
    }

    private void migrate() throws StorageException {
        final LoggerFactory loggerFactory = new LoggerFactory(Logger.getLogger("test"));
        final DatabaseStorage storage = new DatabaseStorage(loggerFactory, config(), dataFolder);
        try {
            new DatabaseMigrationPreflight(storage, loggerFactory.create(DatabaseMigrationPreflight.class)).migrateIfNecessary();
        } finally {
            storage.shutdown();
        }
    }

    private Configuration config() {
        final Configuration config = mock(Configuration.class);
        when(config.getString("storageMethod")).thenReturn("sqlite");
        when(config.getString("storageMethod", "sqlite")).thenReturn("sqlite");
        when(config.getString("data.tablePrefix", TABLE_PREFIX)).thenReturn(TABLE_PREFIX);
        return config;
    }

    private Connection openSqlite() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "loritime.db"));
    }

    private boolean tableExists(final Connection connection, final String table) {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1 FROM `" + table + "` WHERE 1 = 0");
            return true;
        } catch (final SQLException ignored) {
            return false;
        }
    }

    private int maxVersion(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(`version_no`) FROM `" + TABLE_PREFIX + "_version`")) {
            requireRow(resultSet, "version query should return a row");
            return resultSet.getInt(1);
        }
    }

    private int countRows(final Connection connection, final String table) throws SQLException {
        if (!tableExists(connection, table)) {
            return 0;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
            requireRow(resultSet, "count query should return a row");
            return resultSet.getInt(1);
        }
    }

    private String singleString(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return null;
        }
    }

    private void requireRow(final ResultSet resultSet, final String message) throws SQLException {
        if (!resultSet.next()) {
            throw new AssertionError(message);
        }
    }

    private DatabaseSnapshot snapshot(final Connection connection) throws SQLException {
        final boolean playerTableExists = tableExists(connection, TABLE_PREFIX + "_player");
        final boolean serverTableExists = tableExists(connection, TABLE_PREFIX + "_server");
        final boolean worldTableExists = tableExists(connection, TABLE_PREFIX + "_world");
        final boolean timeTableExists = tableExists(connection, TABLE_PREFIX + "_time");
        final boolean adjustmentTableExists = tableExists(connection, TABLE_PREFIX + "_time_adjustment");
        return new DatabaseSnapshot(
                maxVersion(connection),
                tableExists(connection, TABLE_PREFIX),
                playerTableExists,
                serverTableExists,
                worldTableExists,
                timeTableExists,
                adjustmentTableExists,
                countRows(connection, TABLE_PREFIX + "_player"),
                countRows(connection, TABLE_PREFIX + "_time"),
                countRows(connection, TABLE_PREFIX + "_time_adjustment"),
                adjustmentTableExists ? singleString(connection, "SELECT `reason` FROM `" + TABLE_PREFIX + "_time_adjustment`") : null,
                adjustmentTableExists ? singleString(connection, "SELECT `scope_type` FROM `"
                        + TABLE_PREFIX + "_time_adjustment`") : null,
                worldTableExists ? singleString(connection, "SELECT `world` FROM `" + TABLE_PREFIX + "_world`") : null);
    }

    private record DatabaseSnapshot(
            int version,
            boolean legacyTableExists,
            boolean playerTableExists,
            boolean serverTableExists,
            boolean worldTableExists,
            boolean timeTableExists,
            boolean adjustmentTableExists,
            int players,
            int timeEntries,
            int adjustments,
            String reason,
            String scopeType,
            String world) {
    }
}

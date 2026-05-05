package com.jannik_kuehn.common.storage.database;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.PlayerSessionChunk;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.migration.DatabaseMigrationPreflight;
import com.jannik_kuehn.common.storage.database.table.ManualAdjustmentTable;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.CloseResource", "PMD.CheckResultSet"})
class DatabaseTimeAndNameStorageTest {

    private static final String TABLE_PREFIX = "loritime";

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @TempDir
    private File dataFolder;

    @Test
    void totalsIncludeSessionAndManualAdjustmentWithConsoleActor() throws Exception {
        final DatabaseTimeAndNameStorage storage = storage();

        storage.setPlayerName(PLAYER, "Lorias_");
        storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
        storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));

        assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER));
        assertEquals(15L, storage.getAllTimeEntries().get(PLAYER.toString()));
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT `actor_uuid`, `actor_name` FROM `" + TABLE_PREFIX + "_time_adjustment`")) {
            assertTrue(result.next());
            assertNull(result.getBytes("actor_uuid"));
            assertEquals("CONSOLE", result.getString("actor_name"));
        }
    }

    @Test
    void deletePlayerDeletesIdentityAndOwnedHistory() throws Exception {
        final DatabaseTimeAndNameStorage storage = storage();

        storage.setPlayerName(PLAYER, "Lorias_");
        storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
        storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
        storage.deletePlayer(PLAYER);

        assertTrue(storage.getUuid("Lorias_").isEmpty());
        assertEquals(0, countRows(TABLE_PREFIX + "_time"));
        assertEquals(0, countRows(TABLE_PREFIX + "_time_adjustment"));
    }

    @Test
    void inactiveCleanupDeletesOnlyHistory() throws Exception {
        final DatabaseTimeAndNameStorage storage = storage();

        storage.setPlayerName(PLAYER, "Lorias_");
        storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
        storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE `" + TABLE_PREFIX + "_player` SET `last_seen` = DATETIME('now', '-400 days')");
        }

        assertEquals(2, storage.deleteInactiveHistory(365L));
        assertEquals(Optional.of(PLAYER), storage.getUuid("Lorias_"));
        assertEquals(0, countRows(TABLE_PREFIX + "_time"));
        assertEquals(0, countRows(TABLE_PREFIX + "_time_adjustment"));
    }

    @Test
    void inactiveCleanupSkipsRecentPlayers() throws Exception {
        final DatabaseTimeAndNameStorage storage = storage();

        storage.setPlayerName(PLAYER, "Lorias_");
        storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));

        assertEquals(0, storage.deleteInactiveHistory(365L));
        assertEquals(1, countRows(TABLE_PREFIX + "_time"));
        assertFalse(storage.getUuid("Lorias_").isEmpty());
    }

    private DatabaseTimeAndNameStorage storage() throws StorageException {
        final LoggerFactory loggerFactory = new LoggerFactory(Logger.getLogger("test"));
        final DatabaseStorage databaseStorage = new DatabaseStorage(loggerFactory, config(), dataFolder);
        new DatabaseMigrationPreflight(databaseStorage, loggerFactory.create(DatabaseMigrationPreflight.class)).migrateIfNecessary();
        final PlayerTable playerTable = new PlayerTable(TABLE_PREFIX + "_player");
        final ServerTable serverTable = new ServerTable(TABLE_PREFIX + "_server");
        final WorldTable worldTable = new WorldTable(TABLE_PREFIX + "_world", serverTable);
        final TimeTable timeTable = new TimeTable(TABLE_PREFIX + "_time", playerTable, databaseStorage.getDialect());
        final ManualAdjustmentTable adjustmentTable = new ManualAdjustmentTable(TABLE_PREFIX + "_time_adjustment", playerTable);
        return new DatabaseTimeAndNameStorage(databaseStorage.getProvider(), playerTable, serverTable, worldTable,
                timeTable, adjustmentTable, databaseStorage.getDialect());
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

    private int countRows(final String table) throws SQLException {
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }
}

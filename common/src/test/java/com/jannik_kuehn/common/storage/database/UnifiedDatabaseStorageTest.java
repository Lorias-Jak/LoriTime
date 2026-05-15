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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts"})
class UnifiedDatabaseStorageTest {

    private static final String TABLE_PREFIX = "loritime";

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @TempDir
    private File dataFolder;

    @Test
    void totalsIncludeSessionAndManualAdjustmentWithConsoleActor() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));

            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER), "Expected the correct total");
            assertEquals(15L, storage.getAllTimeEntries().get(PLAYER.toString()), "Expected the correct total");
        }
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT `actor_uuid`, `actor_name` FROM `" + TABLE_PREFIX + "_time_adjustment`")) {
            if (!result.next()) {
                fail("Expected a result row");
            }
            assertNull(result.getBytes("actor_uuid"), "Expected a null actor UUID");
            assertEquals("CONSOLE", result.getString("actor_name"), "Expected the correct actor name");
        }
    }

    @Test
    void deletePlayerDeletesIdentityAndOwnedHistory() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
            storage.deletePlayer(PLAYER);

            assertTrue(storage.getUuid("Lorias_").isEmpty(), "Expected the player to be deleted");
        }
        assertEquals(0, countRows(TABLE_PREFIX + "_time"), "Expected no time entries");
        assertEquals(0, countRows(TABLE_PREFIX + "_time_adjustment"), "Expected no time adjustments");
    }

    @Test
    void inactiveCleanupDeletesOnlyHistory() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
            try (Connection connection = openSqlite();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE `" + TABLE_PREFIX + "_player` SET `last_seen` = DATETIME('now', '-400 days')");
            }

            assertEquals(2, storage.deleteInactiveHistory(365L), "Expected two rows to be deleted");
            assertEquals(Optional.of(PLAYER), storage.getUuid("Lorias_"), "Expected the player to still be present");
        }
        assertEquals(0, countRows(TABLE_PREFIX + "_time"), "Expected no time entries");
        assertEquals(0, countRows(TABLE_PREFIX + "_time_adjustment"), "Expected no time adjustments");
    }

    @Test
    void inactiveCleanupSkipsRecentPlayers() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));

            assertEquals(0, storage.deleteInactiveHistory(365L), "Expected no rows to be deleted");
            assertEquals(1, countRows(TABLE_PREFIX + "_time"), "Expected one row to be present");
            assertFalse(storage.getUuid("Lorias_").isEmpty(), "Expected the player to still be present");
        }
    }

    @Test
    void updateSessionWorldChangesContextWithoutCreatingTimeRows() throws Exception {
        final long sessionId;
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            sessionId = storage.startSession(new com.jannik_kuehn.common.api.storage.PlayerSessionContext(PLAYER,
                    "Lorias_", "survival", "global", 1_000L), TimeEntryReason.PLAYER_JOIN);
            storage.updateSessionWorld(sessionId, "survival", "world_nether");
            storage.updateSession(sessionId, 9_000L, TimeEntryReason.PLAYER_LEAVE);

            assertEquals(OptionalLong.of(8L), storage.getTime(PLAYER), "Expected the duration to be unchanged");
        }
        assertEquals(1, countRows(TABLE_PREFIX + "_time"), "Expected the same active time row to be updated");
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT w.`world` FROM `" + TABLE_PREFIX + "_time` t "
                     + "JOIN `" + TABLE_PREFIX + "_world` w ON w.`id` = t.`world_id` "
                     + "WHERE t.`id` = " + sessionId)) {
            if (!result.next()) {
                fail("Expected a result row");
            }
            assertEquals("world_nether", result.getString("world"), "Expected the updated world context");
        }
    }

    private UnifiedDatabaseStorage storage() throws StorageException {
        final LoggerFactory loggerFactory = new LoggerFactory(Logger.getLogger("test"));
        final DatabaseStorage databaseStorage = new DatabaseStorage(loggerFactory, config(), dataFolder);
        new DatabaseMigrationPreflight(databaseStorage, loggerFactory.create(DatabaseMigrationPreflight.class)).migrateIfNecessary();
        final PlayerTable playerTable = new PlayerTable(TABLE_PREFIX + "_player");
        final ServerTable serverTable = new ServerTable(TABLE_PREFIX + "_server");
        final WorldTable worldTable = new WorldTable(TABLE_PREFIX + "_world", serverTable);
        final TimeTable timeTable = new TimeTable(TABLE_PREFIX + "_time", playerTable, databaseStorage.getDialect());
        final ManualAdjustmentTable adjustmentTable = new ManualAdjustmentTable(TABLE_PREFIX + "_time_adjustment", playerTable);
        return new UnifiedDatabaseStorage(databaseStorage.getProvider(), playerTable, serverTable, worldTable,
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
            if (!result.next()) {
                fail("Expected a result row");
            }
            return result.getInt(1);
        }
    }
}

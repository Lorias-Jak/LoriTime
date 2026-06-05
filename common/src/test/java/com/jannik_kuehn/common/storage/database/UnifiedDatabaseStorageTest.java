package com.jannik_kuehn.common.storage.database;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.migration.DatabaseMigrationPreflight;
import com.jannik_kuehn.common.storage.database.table.ManualAdjustmentTable;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.PlayerSessionChunk;
import com.jannik_kuehn.common.storage.model.RecentPlayerIdentity;
import com.jannik_kuehn.common.storage.model.StorageDeleteRequest;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceConfirmation;
import com.jannik_kuehn.common.storage.model.StorageMaintenancePreview;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceScope;
import com.jannik_kuehn.common.storage.model.StorageTransferMapping;
import com.jannik_kuehn.common.storage.model.StorageTransferRequest;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.UnitTestContainsTooManyAsserts"})
class UnifiedDatabaseStorageTest {

    private static final String TABLE_PREFIX = "loritime";

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @TempDir
    private File dataFolder;

    @TempDir
    private File targetDataFolder;

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
    void recentPlayerIdentitiesIncludePlayersWithoutTimeHistory() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");

            final List<RecentPlayerIdentity> identities = storage.getRecentPlayerIdentities(30L);

            assertEquals(1, identities.size(), "Expected recent identity without time history");
            assertEquals(PLAYER, identities.getFirst().uuid(), "Expected the player UUID");
            assertEquals("Lorias_", identities.getFirst().name(), "Expected the player name");
            assertTrue(identities.getFirst().lastSeen().isPresent(), "Expected last_seen metadata");
        }
    }

    @Test
    void recentPlayerIdentitiesSkipInactivePlayers() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            try (Connection connection = openSqlite();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE `" + TABLE_PREFIX + "_player` SET `last_seen` = DATETIME('now', '-40 days')");
            }

            assertTrue(storage.getRecentPlayerIdentities(30L).isEmpty(), "Expected inactive player to be skipped");
        }
    }

    @Test
    void updateSessionWorldChangesContextWithoutCreatingTimeRows() throws Exception {
        final long sessionId;
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            sessionId = storage.startSession(new com.jannik_kuehn.common.storage.model.PlayerSessionContext(PLAYER,
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

    @Test
    void updateSessionPersistsAfkSessionReason() throws Exception {
        final long sessionId;
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            sessionId = storage.startSession(new com.jannik_kuehn.common.storage.model.PlayerSessionContext(PLAYER,
                    "Lorias_", "survival", "global", 1_000L), TimeEntryReason.PLAYER_JOIN);
            storage.updateSession(sessionId, 9_000L, TimeEntryReason.PLAYER_AFK);
        }
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT `reason` FROM `" + TABLE_PREFIX + "_time` "
                     + "WHERE `id` = " + sessionId)) {
            if (!result.next()) {
                fail("Expected a result row");
            }
            assertEquals(TimeEntryReason.PLAYER_AFK.name(), result.getString("reason"), "Expected the AFK session reason");
        }
    }

    @Test
    void updateSessionPersistsAfkKickSessionReason() throws Exception {
        final long sessionId;
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            sessionId = storage.startSession(new com.jannik_kuehn.common.storage.model.PlayerSessionContext(PLAYER,
                    "Lorias_", "survival", "global", 1_000L), TimeEntryReason.PLAYER_JOIN);
            storage.updateSession(sessionId, 9_000L, TimeEntryReason.PLAYER_AFK_KICK);
        }
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT `reason` FROM `" + TABLE_PREFIX + "_time` "
                     + "WHERE `id` = " + sessionId)) {
            if (!result.next()) {
                fail("Expected a result row");
            }
            assertEquals(TimeEntryReason.PLAYER_AFK_KICK.name(), result.getString("reason"),
                    "Expected the AFK kick session reason");
        }
    }

    @Test
    void scopedTotalsIncludeMatchingSessionsAndAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "lobby", "spawn",
                    1_000L, 5_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 2L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));

            assertEquals(OptionalLong.of(26L), storage.getTime(PLAYER), "Expected global total to include all data");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.server("survival")),
                    "Expected server total to include server sessions and matching scoped adjustments");
            assertEquals(OptionalLong.of(12L), storage.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected world total to include exact world sessions and adjustments");
            assertEquals(OptionalLong.of(4L), storage.getTime(PLAYER, TimeScope.server("lobby")),
                    "Expected unrelated scoped adjustments to be excluded");
            assertTrue(storage.getTime(PLAYER, TimeScope.world("survival", "nether")).isEmpty(),
                    "Expected empty total for missing world scope");
        }
    }

    @Test
    void rangedTotalsClipSessionsAndIncludeAdjustmentsInsideRange() throws Exception {
        final TimeRange range = TimeRange.between(Instant.ofEpochMilli(5_000L), Instant.ofEpochMilli(25_000L));
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    0L, 10_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    20_000L, 30_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            updateAdjustmentCreatedAt(Instant.ofEpochMilli(15_000L));

            assertEquals(OptionalLong.of(17L), storage.getTime(PLAYER, TimeScope.GLOBAL, range),
                    "Expected clipped session time plus ranged adjustment");
            assertEquals(OptionalLong.of(17L), storage.getTime(PLAYER, TimeScope.server("survival"), range),
                    "Expected server range to include world-scoped adjustment");
            assertEquals(OptionalLong.of(17L), storage.getTime(PLAYER, TimeScope.world("survival", "world"), range),
                    "Expected world range to include exact world adjustment");
        }
    }

    @Test
    void rangedTotalsExcludeNonOverlappingSessionsAndAdjustments() throws Exception {
        final TimeRange range = TimeRange.between(Instant.ofEpochMilli(50_000L), Instant.ofEpochMilli(60_000L));
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    0L, 10_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            updateAdjustmentCreatedAt(Instant.ofEpochMilli(15_000L));

            assertTrue(storage.getTime(PLAYER, TimeScope.world("survival", "world"), range).isEmpty(),
                    "Expected no ranged total when no entries overlap");
        }
    }

    @Test
    void scopedAdjustmentSchemaStoresScopeReferences() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.addTime(new ManualTimeAdjustment(PLAYER, 2L, TimeEntryReason.AFK_ADJUSTMENT, "SYSTEM",
                    TimeScope.world("survival", "world")));
        }
        try (Connection connection = openSqlite();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT a.`scope_type`, s.`server`, w.`world` "
                     + "FROM `" + TABLE_PREFIX + "_time_adjustment` a "
                     + "JOIN `" + TABLE_PREFIX + "_world` w ON w.`id` = a.`world_id` "
                     + "JOIN `" + TABLE_PREFIX + "_server` s ON s.`id` = w.`server_id`")) {
            if (!result.next()) {
                fail("Expected a scoped adjustment row");
            }
            assertEquals(TimeScope.Type.WORLD.name(), result.getString("scope_type"), "Expected world scope");
            assertEquals("survival", result.getString("server"), "Expected scoped server");
            assertEquals("world", result.getString("world"), "Expected scoped world");
        }
    }

    @Test
    void maintenancePreviewReportsServerImpactAndCollisions() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            storage.persistSession(new PlayerSessionChunk(UUID.randomUUID(), Optional.of("Other"), "target", "world",
                    1_000L, 2_000L, TimeEntryReason.PLAYER_LEAVE));

            final StorageMaintenancePreview preview = storage.previewTransfer(StorageTransferRequest.serverTransfer(List.of(
                    new StorageTransferMapping(StorageMaintenanceScope.server("survival"),
                            StorageMaintenanceScope.server("target")))));

            assertEquals(1L, preview.affectedSessions(), "Expected one source session");
            assertEquals(1L, preview.affectedAdjustments(), "Expected one source adjustment");
            assertEquals(1L, preview.affectedPlayers(), "Expected one affected player");
            assertTrue(preview.targetDataExists(), "Expected target data to be detected");
            assertTrue(preview.confirmationRequired(), "Expected merge confirmation");
            assertEquals(List.of("target/world"), preview.targetCollisions(), "Expected world collision");
        }
    }

    @Test
    void maintenanceServerTransferMovesSessionsAndAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            final StorageTransferRequest request = StorageTransferRequest.serverTransfer(List.of(
                    new StorageTransferMapping(StorageMaintenanceScope.server("survival"),
                            StorageMaintenanceScope.server("target"))));

            final StorageMaintenancePreview preview = storage.previewTransfer(request);
            storage.applyTransfer(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.server("survival")).isEmpty(),
                    "Expected source server to have no remaining time");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.server("target")),
                    "Expected target server to include moved data");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER), "Expected global total to remain unchanged");
        }
    }

    @Test
    void maintenanceWorldTransferMovesExactWorldData() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            final StorageTransferRequest request = StorageTransferRequest.worldTransfer(List.of(
                    new StorageTransferMapping(StorageMaintenanceScope.world("survival", "world"),
                            StorageMaintenanceScope.world("target", "spawn"))));

            final StorageMaintenancePreview preview = storage.previewTransfer(request);
            storage.applyTransfer(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.world("survival", "world")).isEmpty(),
                    "Expected source world to have no remaining time");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.world("target", "spawn")),
                    "Expected target world to include moved data");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER), "Expected global total to remain unchanged");
        }
    }

    @Test
    void maintenanceDeleteServerRemovesServerAndWorldData() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "lobby", "spawn",
                    1_000L, 3_000L, TimeEntryReason.PLAYER_LEAVE));
            final StorageDeleteRequest request = new StorageDeleteRequest(StorageMaintenanceScope.server("survival"));

            final StorageMaintenancePreview preview = storage.previewDelete(request);
            storage.applyDelete(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.server("survival")).isEmpty(),
                    "Expected deleted server data to be gone");
            assertEquals(OptionalLong.of(2L), storage.getTime(PLAYER, TimeScope.server("lobby")),
                    "Expected unrelated server data to remain");
        }
    }

    @Test
    void maintenanceDeleteWorldKeepsServerAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            final StorageDeleteRequest request = new StorageDeleteRequest(StorageMaintenanceScope.world("survival", "world"));

            final StorageMaintenancePreview preview = storage.previewDelete(request);
            storage.applyDelete(request, preview.confirmation());

            assertEquals(OptionalLong.of(5L), storage.getTime(PLAYER, TimeScope.server("survival")),
                    "Expected server adjustment to remain");
            assertTrue(storage.getTime(PLAYER, TimeScope.world("survival", "world")).isEmpty(),
                    "Expected exact world data to be deleted");
        }
    }

    @Test
    void maintenanceConfirmationMismatchRejectsWithoutChangingData() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            final StorageTransferRequest request = StorageTransferRequest.worldTransfer(List.of(
                    new StorageTransferMapping(StorageMaintenanceScope.world("survival", "world"),
                            StorageMaintenanceScope.world("target", "spawn"))));

            assertThrows(StorageException.class, () -> storage.applyTransfer(request,
                    new StorageMaintenanceConfirmation("different")), "Expected mismatched confirmation to fail");
            assertEquals(OptionalLong.of(10L), storage.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected source data to remain after rejection");
        }
    }

    @Test
    void maintenanceStorageTypeTransferCopiesFullHistoryToEmptyTarget() throws Exception {
        try (UnifiedDatabaseStorage source = storage();
             UnifiedDatabaseStorage target = storage(targetDataFolder)) {

            source.setPlayerName(PLAYER, "Lorias_");
            source.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            source.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            source.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));

            final StorageMaintenancePreview preview = source.previewStorageTransferTo(target);
            source.applyStorageTransferTo(target, preview.confirmation());

            assertEquals(Optional.of(PLAYER), target.getUuid("Lorias_"), "Expected copied player identity");
            assertEquals(OptionalLong.of(18L), target.getTime(PLAYER), "Expected copied global total");
            assertEquals(OptionalLong.of(18L), target.getTime(PLAYER, TimeScope.server("survival")),
                    "Expected copied server total");
            assertEquals(OptionalLong.of(13L), target.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected copied world total");
            assertEquals(OptionalLong.of(18L), source.getTime(PLAYER), "Expected source data to remain");
        }
    }

    @Test
    void maintenanceStorageTypeTransferRejectsNonEmptyTarget() throws Exception {
        try (UnifiedDatabaseStorage source = storage();
             UnifiedDatabaseStorage target = storage(targetDataFolder)) {

            source.setPlayerName(PLAYER, "Lorias_");
            source.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            target.setPlayerName(UUID.randomUUID(), "Existing");

            final StorageMaintenancePreview preview = source.previewStorageTransferTo(target);

            assertTrue(preview.targetDataExists(), "Expected non-empty target to be detected");
            assertThrows(StorageException.class, () -> source.applyStorageTransferTo(target, preview.confirmation()),
                    "Expected non-empty target to be rejected");
            assertTrue(target.getUuid("Lorias_").isEmpty(), "Expected source data not to be copied");
        }
    }

    private UnifiedDatabaseStorage storage() throws StorageException {
        return storage(dataFolder);
    }

    private UnifiedDatabaseStorage storage(final File folder) throws StorageException {
        final LoggerFactory loggerFactory = new LoggerFactory(Logger.getLogger("test"));
        final DatabaseStorage databaseStorage = new DatabaseStorage(loggerFactory, config(), folder);
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

    private void updateAdjustmentCreatedAt(final Instant instant) throws SQLException {
        try (Connection connection = openSqlite();
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE `" + TABLE_PREFIX + "_time_adjustment` SET `created_at` = ?")) {
            update.setTimestamp(1, Timestamp.from(instant));
            update.executeUpdate();
        }
    }
}

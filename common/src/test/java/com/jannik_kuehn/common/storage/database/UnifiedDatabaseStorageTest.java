package com.jannik_kuehn.common.storage.database;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.contract.AccumulatingTimeStorage;
import com.jannik_kuehn.common.storage.database.migration.DatabaseMigrationPreflight;
import com.jannik_kuehn.common.storage.database.table.ManualAdjustmentTable;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.PlayerSessionChunk;
import com.jannik_kuehn.common.storage.model.PlayerStorageTransferRequest;
import com.jannik_kuehn.common.storage.model.RecentPlayerIdentity;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.UnitTestContainsTooManyAsserts", "PMD.UseExplicitTypes",
        "PMD.UnitTestAssertionsShouldIncludeMessage", "PMD.AvoidLiteralsInIfCondition"})
class UnifiedDatabaseStorageTest {

    private static final String TABLE_PREFIX = "loritime";

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    private static final UUID OTHER_PLAYER = UUID.fromString("a44f7fed-d003-4de6-b620-191c0fc22bb7");

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
            final Instant afkStart = Instant.parse("2026-07-15T10:00:00Z");
            storage.openAfkPeriod(PLAYER, "Lorias_", "survival", "world", afkStart);
            storage.closeAfkPeriod(PLAYER, afkStart.plusSeconds(30), AfkPeriodEndReason.RESUMED);
            final StorageTransferRequest request = StorageTransferRequest.serverTransfer(List.of(
                    new StorageTransferMapping(StorageMaintenanceScope.server("survival"),
                            StorageMaintenanceScope.server("target"))));

            final StorageMaintenancePreview preview = storage.previewTransfer(request);
            storage.applyTransfer(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.server("survival")).isEmpty(),
                    "Expected source server to have no remaining time");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.server("target")),
                    "Expected target server to include moved data");
            assertEquals(1, storage.getAfkPeriods(TimeRange.between(afkStart, afkStart.plusSeconds(31)),
                    TimeScope.server("target")).size(), "Expected AFK period to move to target server");
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
            final Instant afkStart = Instant.parse("2026-07-15T10:00:00Z");
            storage.openAfkPeriod(PLAYER, "Lorias_", "survival", "world", afkStart);
            storage.closeAfkPeriod(PLAYER, afkStart.plusSeconds(30), AfkPeriodEndReason.RESUMED);
            final StorageTransferRequest request = StorageTransferRequest.worldTransfer(List.of(
                    new StorageTransferMapping(StorageMaintenanceScope.world("survival", "world"),
                            StorageMaintenanceScope.world("target", "spawn"))));

            final StorageMaintenancePreview preview = storage.previewTransfer(request);
            storage.applyTransfer(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.world("survival", "world")).isEmpty(),
                    "Expected source world to have no remaining time");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.world("target", "spawn")),
                    "Expected target world to include moved data");
            assertEquals(1, storage.getAfkPeriods(TimeRange.between(afkStart, afkStart.plusSeconds(31)),
                    TimeScope.world("target", "spawn")).size(), "Expected AFK period to move to target world");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER), "Expected global total to remain unchanged");
        }
    }

    @Test
    void playerServerTransferMovesOnlySelectedPlayerAndScopedAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.setPlayerName(OTHER_PLAYER, "Other");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(OTHER_PLAYER, Optional.of("Other"), "survival", "world",
                    1_000L, 6_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
            final PlayerStorageTransferRequest request = PlayerStorageTransferRequest.serverTransfer(PLAYER,
                    Optional.of("Lorias_"), StorageMaintenanceScope.server("survival"),
                    StorageMaintenanceScope.server("target"), Optional.empty(), Optional.empty());

            final StorageMaintenancePreview preview = storage.previewPlayerTransfer(request);
            storage.applyPlayerTransfer(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.server("survival")).isEmpty(),
                    "Expected selected player source server data to move");
            assertEquals(OptionalLong.of(18L), storage.getTime(PLAYER, TimeScope.server("target")),
                    "Expected selected player scoped data at target");
            assertEquals(OptionalLong.of(25L), storage.getTime(PLAYER), "Expected selected player global total unchanged");
            assertEquals(OptionalLong.of(5L), storage.getTime(OTHER_PLAYER, TimeScope.server("survival")),
                    "Expected other player data to remain at source");
        }
    }

    @Test
    void playerWorldTransferSupportsSameServerTargetAndKeepsServerAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "old",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "old")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            final PlayerStorageTransferRequest request = PlayerStorageTransferRequest.worldTransfer(PLAYER,
                    Optional.of("Lorias_"), StorageMaintenanceScope.world("survival", "old"),
                    StorageMaintenanceScope.world("survival", "new"), Optional.empty(), Optional.empty());

            final StorageMaintenancePreview preview = storage.previewPlayerTransfer(request);
            storage.applyPlayerTransfer(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.world("survival", "old")).isEmpty(),
                    "Expected source world data to move");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.world("survival", "new")),
                    "Expected target world to receive sessions and world adjustments");
            assertEquals(OptionalLong.of(22L), storage.getTime(PLAYER, TimeScope.server("survival")),
                    "Expected server adjustment to remain on the containing server");
        }
    }

    @Test
    void playerWorldTransferSupportsCrossServerTargetAndKeepsGlobalAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.setPlayerName(OTHER_PLAYER, "Other");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "old",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(OTHER_PLAYER, Optional.of("Other"), "survival", "old",
                    1_000L, 6_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "old")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 11L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            final PlayerStorageTransferRequest request = PlayerStorageTransferRequest.worldTransfer(PLAYER,
                    Optional.of("Lorias_"), StorageMaintenanceScope.world("survival", "old"),
                    StorageMaintenanceScope.world("target", "new"), Optional.empty(), Optional.empty());

            final StorageMaintenancePreview preview = storage.previewPlayerTransfer(request);
            storage.applyPlayerTransfer(request, preview.confirmation());

            assertEquals(OptionalLong.of(11L), storage.getTime(PLAYER, TimeScope.server("survival")),
                    "Expected server adjustment to remain at source server");
            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.world("target", "new")),
                    "Expected selected world data to move across servers");
            assertEquals(OptionalLong.of(33L), storage.getTime(PLAYER), "Expected selected player global total unchanged");
            assertEquals(OptionalLong.of(5L), storage.getTime(OTHER_PLAYER, TimeScope.world("survival", "old")),
                    "Expected other player world data to remain at source");
        }
    }

    @Test
    void playerServerTransferDoesNotMoveGlobalAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
            final PlayerStorageTransferRequest request = PlayerStorageTransferRequest.serverTransfer(PLAYER,
                    Optional.of("Lorias_"), StorageMaintenanceScope.server("survival"),
                    StorageMaintenanceScope.server("target"), Optional.empty(), Optional.empty());

            final StorageMaintenancePreview preview = storage.previewPlayerTransfer(request);
            storage.applyPlayerTransfer(request, preview.confirmation());

            assertTrue(storage.getTime(PLAYER, TimeScope.server("survival")).isEmpty(),
                    "Expected source server-scoped data to move");
            assertEquals(OptionalLong.of(18L), storage.getTime(PLAYER, TimeScope.server("target")),
                    "Expected target server to receive sessions and non-global adjustments");
            assertEquals(OptionalLong.of(25L), storage.getTime(PLAYER), "Expected global total unchanged");
        }
    }

    @Test
    void playerTransferTimeRangeSelectsWholeRowsOnly() throws Exception {
        final TimeRange range = TimeRange.between(Instant.ofEpochMilli(10_000L), Instant.ofEpochMilli(25_000L));
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    10_000L, 20_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    30_000L, 40_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    5_000L, 15_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            updateAdjustmentCreatedAt(3L, Instant.ofEpochMilli(12_000L));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            updateAdjustmentCreatedAt(5L, Instant.ofEpochMilli(40_000L));
            final PlayerStorageTransferRequest request = PlayerStorageTransferRequest.worldTransfer(PLAYER,
                    Optional.of("Lorias_"), StorageMaintenanceScope.world("survival", "world"),
                    StorageMaintenanceScope.world("target", "world"), Optional.of(range), Optional.of("15s"));

            final StorageMaintenancePreview preview = storage.previewPlayerTransfer(request);
            assertEquals(1L, preview.affectedSessions(), "Expected only the fully contained session to count");
            assertEquals(1L, preview.affectedAdjustments(), "Expected only the in-range adjustment to count");
            assertThrows(StorageException.class, () -> storage.applyPlayerTransfer(request,
                    new StorageMaintenanceConfirmation("different")), "Expected mismatched confirmation to fail");
            storage.applyPlayerTransfer(request, preview.confirmation());

            assertEquals(OptionalLong.of(25L), storage.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected outside and partially overlapping rows to remain at source");
            assertEquals(OptionalLong.of(13L), storage.getTime(PLAYER, TimeScope.world("target", "world")),
                    "Expected only fully selected rows to move");
        }
    }

    @Test
    void playerTransferPreviewReportsPlayerRangeAndTargetCollision() throws Exception {
        final TimeRange range = TimeRange.between(Instant.ofEpochMilli(10_000L), Instant.ofEpochMilli(25_000L));
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    10_000L, 20_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            updateAdjustmentCreatedAt(Instant.ofEpochMilli(12_000L));
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "target", "world",
                    10_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            final PlayerStorageTransferRequest request = PlayerStorageTransferRequest.worldTransfer(PLAYER,
                    Optional.of("Lorias_"), StorageMaintenanceScope.world("survival", "world"),
                    StorageMaintenanceScope.world("target", "world"), Optional.of(range), Optional.of("15s"));

            final StorageMaintenancePreview preview = storage.previewPlayerTransfer(request);

            assertEquals(Optional.of(PLAYER), preview.playerUuid(), "Expected selected player UUID in preview");
            assertEquals(Optional.of("Lorias_"), preview.playerName(), "Expected selected player name in preview");
            assertEquals(Optional.of(range), preview.timeRange(), "Expected selected time range in preview");
            assertEquals(Optional.of("15s"), preview.timeRangeInput(), "Expected raw time input in preview");
            assertEquals(1L, preview.affectedSessions(), "Expected selected session count");
            assertEquals(1L, preview.affectedAdjustments(), "Expected selected adjustment count");
            assertEquals(1L, preview.affectedPlayers(), "Expected one selected player");
            assertTrue(preview.targetDataExists(), "Expected target data collision to be detected");
            assertTrue(preview.confirmationRequired(), "Expected merge confirmation");
            assertEquals(List.of("target/world"), preview.targetCollisions(), "Expected target collision label");
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
    void maintenanceDeletePlayerServerKeepsOtherPlayersAndGlobalAdjustments() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.setPlayerName(OTHER_PLAYER, "Other");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(OTHER_PLAYER, Optional.of("Other"), "survival", "world",
                    1_000L, 6_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 7L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE"));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            final StorageDeleteRequest request = StorageDeleteRequest.player(StorageMaintenanceScope.server("survival"),
                    PLAYER, Optional.of("Lorias_"), Optional.empty(), Optional.empty());

            final StorageMaintenancePreview preview = storage.previewDelete(request);
            storage.applyDelete(request, preview.confirmation());

            assertEquals(Optional.of(PLAYER), preview.playerUuid(), "Expected selected player UUID in preview");
            assertEquals(Optional.of("Lorias_"), preview.playerName(), "Expected selected player name in preview");
            assertEquals(1L, preview.affectedPlayers(), "Expected one affected player");
            assertEquals(OptionalLong.of(7L), storage.getTime(PLAYER), "Expected global adjustment to remain");
            assertTrue(storage.getTime(PLAYER, TimeScope.server("survival")).isEmpty(),
                    "Expected selected player's server history to be deleted");
            assertEquals(OptionalLong.of(5L), storage.getTime(OTHER_PLAYER, TimeScope.server("survival")),
                    "Expected other player's server history to remain");
            assertEquals(Optional.of(PLAYER), storage.getUuid("Lorias_"), "Expected selected player identity to remain");
        }
    }

    @Test
    void maintenanceDeleteAllPlayerWorldTimeRangeUsesWholeRowsOnly() throws Exception {
        final TimeRange range = TimeRange.between(Instant.ofEpochMilli(10_000L), Instant.ofEpochMilli(25_000L));
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.setPlayerName(OTHER_PLAYER, "Other");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    12_000L, 20_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    5_000L, 15_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(OTHER_PLAYER, Optional.of("Other"), "survival", "world",
                    14_000L, 18_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 3L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            updateAdjustmentCreatedAt(3L, Instant.ofEpochMilli(15_000L));
            storage.addTime(new ManualTimeAdjustment(OTHER_PLAYER, 4L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            updateAdjustmentCreatedAt(4L, Instant.ofEpochMilli(30_000L));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 9L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.server("survival")));
            final StorageDeleteRequest request = StorageDeleteRequest.allPlayers(
                    StorageMaintenanceScope.world("survival", "world"), Optional.of(range), Optional.of("15s"));

            final StorageMaintenancePreview preview = storage.previewDelete(request);
            storage.applyDelete(request, preview.confirmation());

            assertEquals(Optional.of(range), preview.timeRange(), "Expected selected range in preview");
            assertEquals(Optional.of("15s"), preview.timeRangeInput(), "Expected raw range input in preview");
            assertEquals(2L, preview.affectedSessions(), "Expected only fully contained sessions");
            assertEquals(1L, preview.affectedAdjustments(), "Expected only in-range world adjustment");
            assertEquals(2L, preview.affectedPlayers(), "Expected both players to be affected by sessions");
            assertEquals(OptionalLong.of(10L), storage.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected partially overlapping session to remain");
            assertEquals(OptionalLong.of(4L), storage.getTime(OTHER_PLAYER, TimeScope.world("survival", "world")),
                    "Expected out-of-range adjustment to remain");
            assertEquals(OptionalLong.of(19L), storage.getTime(PLAYER, TimeScope.server("survival")),
                    "Expected server adjustment and partial session to remain after world delete");
        }
    }

    @Test
    void maintenanceDeleteConfirmationMismatchRejectsWithoutChangingData() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            final StorageDeleteRequest request = StorageDeleteRequest.allPlayers(
                    StorageMaintenanceScope.world("survival", "world"), Optional.empty(), Optional.empty());

            assertThrows(StorageException.class, () -> storage.applyDelete(request,
                    new StorageMaintenanceConfirmation("different")), "Expected mismatched confirmation to fail");
            assertEquals(OptionalLong.of(10L), storage.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected source data to remain after rejection");
        }
    }

    @Test
    void maintenanceDeleteRollsBackWhenApplyFails() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {

            storage.setPlayerName(PLAYER, "Lorias_");
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    1_000L, 11_000L, TimeEntryReason.PLAYER_LEAVE));
            storage.addTime(new ManualTimeAdjustment(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                    TimeScope.world("survival", "world")));
            final StorageDeleteRequest request = StorageDeleteRequest.allPlayers(
                    StorageMaintenanceScope.world("survival", "world"), Optional.empty(), Optional.empty());
            final StorageMaintenancePreview preview = storage.previewDelete(request);
            try (Connection connection = openSqlite();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TRIGGER fail_adjustment_delete BEFORE DELETE ON `"
                        + TABLE_PREFIX + "_time_adjustment` BEGIN SELECT RAISE(ABORT, 'fail delete'); END");
            }

            assertThrows(StorageException.class, () -> storage.applyDelete(request, preview.confirmation()),
                    "Expected trigger failure to abort delete");

            assertEquals(OptionalLong.of(15L), storage.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected session and adjustment rows to be restored by rollback");
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
            final Instant afkStart = Instant.parse("2026-07-15T10:00:00Z");
            source.openAfkPeriod(PLAYER, "Lorias_", "survival", "world", afkStart);
            source.closeAfkPeriod(PLAYER, afkStart.plusSeconds(30), AfkPeriodEndReason.RESUMED);

            final StorageMaintenancePreview preview = source.previewStorageTransferTo(target);
            source.applyStorageTransferTo(target, preview.confirmation());

            assertEquals(Optional.of(PLAYER), target.getUuid("Lorias_"), "Expected copied player identity");
            assertEquals(OptionalLong.of(18L), target.getTime(PLAYER), "Expected copied global total");
            assertEquals(OptionalLong.of(18L), target.getTime(PLAYER, TimeScope.server("survival")),
                    "Expected copied server total");
            assertEquals(OptionalLong.of(13L), target.getTime(PLAYER, TimeScope.world("survival", "world")),
                    "Expected copied world total");
            final var period = target.getAfkPeriods(TimeRange.between(afkStart, afkStart.plusSeconds(31)),
                    TimeScope.world("survival", "world")).getFirst();
            assertEquals(afkStart, period.startedAt(), "Expected copied AFK-period start");
            assertEquals(afkStart.plusSeconds(30), period.endedAt().orElseThrow(),
                    "Expected copied AFK-period end");
            assertEquals(AfkPeriodEndReason.RESUMED, period.endReason().orElseThrow(),
                    "Expected copied AFK-period reason");
            assertEquals(OptionalLong.of(18L), source.getTime(PLAYER), "Expected source data to remain");
        }
    }

    @Test
    void maintenanceStorageTypeTransferCopiesHistoryLargerThanBatch() throws Exception {
        try (UnifiedDatabaseStorage source = storage();
             UnifiedDatabaseStorage target = storage(targetDataFolder)) {

            source.setPlayerName(PLAYER, "Lorias_");
            for (int index = 0; index < 1_005; index++) {
                source.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                        index * 2_000L, index * 2_000L + 1_000L, TimeEntryReason.PLAYER_LEAVE));
                source.addTime(new ManualTimeAdjustment(PLAYER, 1L, TimeEntryReason.MANUAL_ADJUSTMENT, "CONSOLE",
                        TimeScope.server("survival")));
            }

            final StorageMaintenancePreview preview = source.previewStorageTransferTo(target);
            source.applyStorageTransferTo(target, preview.confirmation());

            assertEquals(1_005L, preview.affectedSessions(), "Expected all source sessions in preview");
            assertEquals(1_005L, preview.affectedAdjustments(), "Expected all source adjustments in preview");
            assertEquals(OptionalLong.of(2_010L), target.getTime(PLAYER), "Expected all batched history to transfer");
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

    @Test
    void afkPeriodsAreUniqueScopedAndRecoveredAsShutdown() throws Exception {
        final Instant start = Instant.parse("2026-07-10T10:00:00Z");
        final TimeRange range = TimeRange.between(start.minusSeconds(1), start.plusSeconds(301));
        try (UnifiedDatabaseStorage storage = storage()) {
            storage.openAfkPeriod(PLAYER, "Lorias_", "survival", "world", start);
            storage.openAfkPeriod(PLAYER, "Lorias_", "survival", "world", start.plusSeconds(10));
            assertEquals(1, storage.getAfkPeriods(range, TimeScope.GLOBAL).size(), "Expected one open period");
            assertTrue(storage.getAfkPeriods(range, TimeScope.server("creative")).isEmpty(),
                    "Expected scope filtering");

            assertEquals(1, storage.recoverOpenAfkPeriods(start.plusSeconds(300)), "Expected stale recovery");
            final var recovered = storage.getAfkPeriods(range, TimeScope.world("survival", "world")).getFirst();
            assertEquals(AfkPeriodEndReason.SHUTDOWN, recovered.endReason().orElseThrow());
            assertEquals(start.plusSeconds(300), recovered.endedAt().orElseThrow());
        }
    }

    @Test
    void afkPeriodClosesWithExplicitReason() throws Exception {
        final Instant start = Instant.parse("2026-07-10T10:00:00Z");
        try (UnifiedDatabaseStorage storage = storage()) {
            storage.openAfkPeriod(PLAYER, "Lorias_", "survival", "world", start);
            storage.closeAfkPeriod(PLAYER, start.plusSeconds(60), AfkPeriodEndReason.RESUMED);
            final var period = storage.getAfkPeriods(TimeRange.between(start, start.plusSeconds(61)), TimeScope.GLOBAL)
                    .getFirst();
            assertEquals(AfkPeriodEndReason.RESUMED, period.endReason().orElseThrow());
        }
    }

    @Test
    void statisticsMigrationsCreateBoundedHistoryIndexes() throws Exception {
        try (UnifiedDatabaseStorage ignored = storage(); Connection connection = openSqlite();
             Statement statement = connection.createStatement()) {
            assertTrue(hasIndex(statement, TABLE_PREFIX + "_time", "idx_" + TABLE_PREFIX + "_time_range"));
            assertTrue(hasIndex(statement, TABLE_PREFIX + "_afk_period", "idx_" + TABLE_PREFIX + "_afk_range"));
        }
    }

    @Test
    void sqliteStatisticsIncludeEpochMillisecondTextSessions() throws Exception {
        final Instant start = Instant.parse("2026-07-10T10:00:00Z");
        try (UnifiedDatabaseStorage storage = storage()) {
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    start.toEpochMilli(), start.plusSeconds(60).toEpochMilli(), TimeEntryReason.PLAYER_LEAVE));

            final var result = storage.getStatistics(new StatisticsRequest(
                    TimeRange.between(start.minusSeconds(1), start.plusSeconds(120)), TimeScope.GLOBAL,
                    Duration.ofMinutes(3), start.plusSeconds(120)));

            assertEquals(1, result.uniqueUsers());
            assertEquals(1, result.sessions());
            assertEquals(Duration.ofSeconds(60), result.totalPlayTime());
        }
    }

    @Test
    void sqliteStatisticsIncludeNewlyJoinedActiveSessionAtObservationTime() throws Exception {
        final Instant observedAt = Instant.parse("2026-07-10T10:02:00Z");
        final LoggerFactory loggerFactory = new LoggerFactory(Logger.getLogger("test"));
        try (AccumulatingTimeStorage accumulator = new AccumulatingTimeStorage(
                loggerFactory.create(AccumulatingTimeStorage.class), storage())) {
            accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "world",
                    observedAt.minusSeconds(90).toEpochMilli());

            final var result = accumulator.getStatistics(new StatisticsRequest(
                    TimeRange.between(observedAt.minusSeconds(300), observedAt), TimeScope.GLOBAL,
                    Duration.ofMinutes(3), observedAt));

            assertEquals(1, result.uniqueUsers());
            assertEquals(1, result.sessions());
            assertEquals(Duration.ofSeconds(90), result.totalPlayTime());
            assertEquals(Duration.ofSeconds(90), result.medianSession());
            assertEquals(0, result.bounces());
            assertEquals(1, result.peakConcurrent());
        }
    }

    @Test
    void sqliteStatisticsIncludeFormattedHistoricalTimestampsAndExcludeOutsideRows() throws Exception {
        final Instant start = Instant.parse("2026-07-10T10:00:00Z");
        try (UnifiedDatabaseStorage storage = storage()) {
            storage.persistSession(new PlayerSessionChunk(PLAYER, Optional.of("Lorias_"), "survival", "world",
                    start.toEpochMilli(), start.plusSeconds(60).toEpochMilli(), TimeEntryReason.PLAYER_LEAVE));
            storage.persistSession(new PlayerSessionChunk(OTHER_PLAYER, Optional.of("Other"), "survival", "world",
                    start.minusSeconds(600).toEpochMilli(), start.minusSeconds(540).toEpochMilli(),
                    TimeEntryReason.PLAYER_LEAVE));
            try (Connection connection = openSqlite(); PreparedStatement update = connection.prepareStatement(
                    "UPDATE `" + TABLE_PREFIX + "_time` SET `join_time` = ?, `leave_time` = ? "
                            + "WHERE `player_id` = (SELECT `id` FROM `" + TABLE_PREFIX + "_player` WHERE `uuid` = ?)")) {
                update.setString(1, Timestamp.from(start).toString());
                update.setString(2, Timestamp.from(start.plusSeconds(60)).toString());
                update.setBytes(3, com.jannik_kuehn.common.utils.UuidUtil.toBytes(PLAYER));
                update.executeUpdate();
            }

            final var result = storage.getStatistics(new StatisticsRequest(
                    TimeRange.between(start.minusSeconds(1), start.plusSeconds(120)), TimeScope.GLOBAL,
                    Duration.ofMinutes(3), start.plusSeconds(120)));

            assertEquals(1, result.uniqueUsers(), "Expected formatted in-range row and excluded older row");
            assertEquals(Duration.ofSeconds(60), result.totalPlayTime());
        }
    }

    @Test
    void sqliteStatisticsCompatibilityDoesNotChangeSessionSchema() throws Exception {
        try (UnifiedDatabaseStorage storage = storage()) {
            storage.getStatistics(new StatisticsRequest(
                    TimeRange.between(Instant.EPOCH, Instant.EPOCH.plusSeconds(1)), TimeScope.GLOBAL,
                    Duration.ofMinutes(3), Instant.EPOCH.plusSeconds(1)));
        }
        try (Connection connection = openSqlite(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info('" + TABLE_PREFIX + "_time')")) {
            String joinType = null;
            while (result.next()) {
                if ("join_time".equals(result.getString("name"))) {
                    joinType = result.getString("type");
                }
            }
            assertEquals("TEXT", joinType, "Expected query-only compatibility without a schema migration");
        }
    }

    private boolean hasIndex(final Statement statement, final String table, final String index) throws SQLException {
        try (ResultSet result = statement.executeQuery("PRAGMA index_list('" + table + "')")) {
            while (result.next()) {
                if (index.equals(result.getString("name"))) {
                    return true;
                }
            }
            return false;
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
        return new UnifiedDatabaseStorage(databaseStorage.getProvider(), databaseStorage.getTablePrefix(), playerTable, serverTable, worldTable,
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

    private void updateAdjustmentCreatedAt(final long amountSeconds, final Instant instant) throws SQLException {
        try (Connection connection = openSqlite();
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE `" + TABLE_PREFIX + "_time_adjustment` SET `created_at` = ? "
                             + "WHERE `amount_seconds` = ?")) {
            update.setTimestamp(1, Timestamp.from(instant));
            update.setLong(2, amountSeconds);
            update.executeUpdate();
        }
    }
}

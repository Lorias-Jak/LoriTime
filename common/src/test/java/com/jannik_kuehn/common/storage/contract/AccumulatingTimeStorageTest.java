package com.jannik_kuehn.common.storage.contract;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.model.AfkPeriod;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.PlayerSessionChunk;
import com.jannik_kuehn.common.storage.model.PlayerSessionContext;
import com.jannik_kuehn.common.storage.model.RecentPlayerIdentity;
import com.jannik_kuehn.common.storage.model.SessionContextDefaults;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
import com.jannik_kuehn.common.storage.model.StatisticsSnapshot;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.CloseResource", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.UnitTestAssertionsShouldIncludeMessage", "PMD.CouplingBetweenObjects", "PMD.DoNotUseThreads"})
class AccumulatingTimeStorageTest {

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    private static final UUID OTHER_PLAYER = UUID.fromString("a44f7fed-d003-4de6-b620-191c0fc22bb7");

    @Test
    void stopPersistsSingleSessionChunkWithContext() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 1_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 6_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected one session chunk after stopAccumulatingAndSaveOnlineTime");
        final PlayerSessionChunk chunk = storage.sessions.getFirst();
        assertEquals(PLAYER, chunk.uuid(), "Expected the same UUID as the one passed to startAccumulating");
        assertEquals(Optional.of("Lorias_"), chunk.name(), "Expected the same name as the one passed to startAccumulating");
        assertEquals("lobby", chunk.server(), "Expected the same server as the one passed to startAccumulating");
        assertEquals("spawn", chunk.world(), "Expected the same world as the one passed to startAccumulating");
        assertEquals(1_000L, chunk.startedAtMs(), "Expected the same start time as the one passed to startAccumulating");
        assertEquals(6_000L, chunk.stoppedAtMs(), "Expected the same stop time as the one passed to stopAccumulatingAndSaveOnlineTime");
        assertEquals(5L, chunk.durationSeconds(), "Expected the correct duration in seconds");
        assertEquals(TimeEntryReason.PLAYER_LEAVE, chunk.reason(), "Expected the same reason as the one passed to stopAccumulatingAndSaveOnlineTime");
    }

    @Test
    void flushPersistsElapsedChunkAndKeepsSessionOpen() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);
        final long start = System.currentTimeMillis() - 5_000L;

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", start);
        accumulator.flushOnlineTimeCache();
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, System.currentTimeMillis() + 2_000L,
                TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected one session chunk after flushOnlineTimeCache");
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.getFirst().reason(), "Expected the same reason as the one passed to stopAccumulatingAndSaveOnlineTime");
        assertTrue(storage.sessions.getFirst().durationSeconds() >= 6L, "Expected duration to be at least 6 seconds");
    }

    @Test
    void closePersistsShutdownFlush() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", System.currentTimeMillis() - 3_000L);
        accumulator.close();

        assertEquals(1, storage.sessions.size(), "Expected one session chunk after close");
        assertEquals(TimeEntryReason.SHUTDOWN_FLUSH, storage.sessions.getFirst().reason(), "Expected the reason to be SHUTDOWN_FLUSH");
        assertTrue(storage.closed, "Expected close to have been called");
    }

    @Test
    void manualAdjustmentForOnlinePlayerAdjustsActiveSession() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 10_000L);
        accumulator.addTime(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 20_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected one session chunk after stopAccumulatingAndSaveOnlineTime");
        assertEquals(10L, storage.sessions.getFirst().durationSeconds(), "Expected the correct duration in seconds");
        assertEquals(5L, storage.adjustments.get(PLAYER), "Expected the correct adjustment");
    }

    @Test
    void manualAdjustmentForOfflinePlayerWritesThrough() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.addTime(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT);

        assertEquals(5L, storage.adjustments.get(PLAYER), "Expected the correct adjustment");
        assertEquals(TimeEntryReason.MANUAL_ADJUSTMENT, storage.directWriteReasons.getFirst(), "Expected the correct reason");
    }

    @Test
    void contextSwitchPersistsPreviousContextAndTracksNext() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 1_000L);
        accumulator.switchContext(PLAYER, "Lorias_", "survival", "nether", 4_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(2, storage.sessions.size(), "Expected two session chunks after stopAccumulatingAndSaveOnlineTime");
        assertEquals("lobby", storage.sessions.getFirst().server(), "Expected the same server as the one passed to startAccumulating");
        assertEquals("spawn", storage.sessions.getFirst().world(), "Expected the same world as the one passed to startAccumulating");
        assertEquals(TimeEntryReason.SERVER_SWITCH, storage.sessions.get(0).reason(), "Expected the reason to be SERVER_SWITCH");
        assertEquals(3L, storage.sessions.get(0).durationSeconds(), "Expected the correct duration in seconds");
        assertEquals("survival", storage.sessions.get(1).server(), "Expected the same server as the one passed to switchContext");
        assertEquals("nether", storage.sessions.get(1).world(), "Expected the same world as the one passed to switchContext");
        assertEquals(5L, storage.sessions.get(1).durationSeconds(), "Expected the correct duration in seconds");
    }

    @Test
    void initialProxyBackendConnectionStartsSessionWithoutContextSwitchRow() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.switchContext(PLAYER, "Lorias_", "survival", SessionContextDefaults.WORLD, 1_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected initial backend connection to create only one row");
        assertEquals("survival", storage.sessions.getFirst().server(), "Expected first row to use the backend server");
        assertEquals(SessionContextDefaults.WORLD, storage.sessions.getFirst().world(), "Expected first row to use the fallback world");
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.getFirst().reason(), "Expected no context-switch row on initial join");
        assertEquals(8L, storage.sessions.getFirst().durationSeconds(), "Expected the correct duration");
    }

    @Test
    void defaultStartAccumulatingUsesSharedContextDefaults() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, 1_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected one session chunk after stopAccumulatingAndSaveOnlineTime");
        assertEquals(SessionContextDefaults.SERVER, storage.sessions.getFirst().server(), "Expected fallback server context");
        assertEquals(SessionContextDefaults.WORLD, storage.sessions.getFirst().world(), "Expected fallback world context");
    }

    @Test
    void autoflushAroundBackendSwitchKeepsOneRowPerBackendSession() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "global", System.currentTimeMillis() - 10_000L);
        accumulator.updateWorldContext(PLAYER, "spawn", System.currentTimeMillis() - 9_000L);
        accumulator.flushOnlineTimeCache();
        accumulator.switchContext(PLAYER, "Lorias_", "survival", "global", System.currentTimeMillis() - 5_000L);
        accumulator.updateWorldContext(PLAYER, "world", System.currentTimeMillis() - 4_000L);
        accumulator.flushOnlineTimeCache();
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, System.currentTimeMillis(), TimeEntryReason.PLAYER_LEAVE);

        assertEquals(2, storage.sessions.size(), "Expected one row per backend server session");
        assertEquals("lobby", storage.sessions.get(0).server(), "Expected first backend context");
        assertEquals("spawn", storage.sessions.get(0).world(), "Expected first row to keep reported Paper world");
        assertEquals(TimeEntryReason.SERVER_SWITCH, storage.sessions.get(0).reason(), "Expected switch to close the first row");
        assertEquals("survival", storage.sessions.get(1).server(), "Expected second backend context");
        assertEquals("world", storage.sessions.get(1).world(), "Expected second row to keep reported Paper world");
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.get(1).reason(), "Expected leave to close the second row");
    }

    @Test
    void paperConfiguredServerNameCannotCreateDuplicateCanonicalRowsThroughWorldContext() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "velocity-backend-name", "global", 1_000L);
        accumulator.updateWorldContext(PLAYER, "paper-world", 2_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected the world update not to create a Paper server row");
        assertEquals("velocity-backend-name", storage.sessions.getFirst().server(), "Expected proxy backend server name to remain canonical");
        assertEquals("paper-world", storage.sessions.getFirst().world(), "Expected only the world context to change");
    }

    @Test
    void duplicateContextSwitchDoesNotCreateNewSession() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 1_000L);
        accumulator.switchContext(PLAYER, "Lorias_", "lobby", "spawn", 4_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected one session chunk after stopAccumulatingAndSaveOnlineTime");
        assertEquals("lobby", storage.sessions.getFirst().server(), "Expected the same server as the one passed to startAccumulating");
        assertEquals("spawn", storage.sessions.getFirst().world(), "Expected the same world as the one passed to startAccumulating");
        assertEquals(8L, storage.sessions.getFirst().durationSeconds(), "Expected the correct duration in seconds");
    }

    @Test
    void worldOnlyContextSwitchUsesWorldSwitchReason() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 1_000L);
        accumulator.switchContext(PLAYER, "Lorias_", "lobby", "nether", 4_000L);

        assertEquals(TimeEntryReason.WORLD_SWITCH, storage.sessions.getFirst().reason(), "Expected the old row to use WORLD_SWITCH");
    }

    @Test
    void remoteWorldContextUpdatesActiveSessionWithoutNewRow() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "global", 1_000L);
        accumulator.updateWorldContext(PLAYER, "world_nether", 2_000L);
        accumulator.flushOnlineTimeCache();
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size(), "Expected world context updates to keep one active row");
        assertEquals("survival", storage.sessions.getFirst().server(), "Expected the canonical proxy server to be preserved");
        assertEquals("world_nether", storage.sessions.getFirst().world(), "Expected the reported world to update the active row");
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.getFirst().reason(), "Expected leave to close the same active row");
    }

    @Test
    void remoteWorldSwitchCreatesNewSessionRow() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "world", 1_000L);
        accumulator.switchWorldContext(PLAYER, "world_nether", 4_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(2, storage.sessions.size(), "Expected world switch to split the active row");
        assertEquals("survival", storage.sessions.get(0).server(), "Expected canonical server to remain on the old row");
        assertEquals("world", storage.sessions.get(0).world(), "Expected old world to remain on the old row");
        assertEquals(TimeEntryReason.WORLD_SWITCH, storage.sessions.get(0).reason(), "Expected world switch to close the old row");
        assertEquals("survival", storage.sessions.get(1).server(), "Expected canonical server to remain on the new row");
        assertEquals("world_nether", storage.sessions.get(1).world(), "Expected new world to be tracked on the new row");
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.get(1).reason(), "Expected leave to close the new row");
    }

    @Test
    void remoteWorldContextWithoutActiveSessionIsIgnored() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.updateWorldContext(PLAYER, "world_nether", 2_000L);

        assertTrue(storage.sessions.isEmpty(), "Expected no standalone session row to be created");
    }

    @Test
    void scopedTotalIncludesActiveSessionOnlyWhenScopeMatches() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "world", System.currentTimeMillis() - 5_000L);

        assertTrue(accumulator.getTime(PLAYER, TimeScope.server("survival")).orElseThrow() >= 5L,
                "Expected active time for matching server");
        assertEquals(OptionalLong.of(0L), accumulator.getTime(PLAYER, TimeScope.server("lobby")),
                "Expected no active time for non-matching server");
        assertTrue(accumulator.getTime(PLAYER, TimeScope.world("survival", "world")).orElseThrow() >= 5L,
                "Expected active time for matching world");
        assertEquals(OptionalLong.of(0L), accumulator.getTime(PLAYER, TimeScope.world("survival", "nether")),
                "Expected no active time for non-matching world");
    }

    @Test
    void rangedTotalIncludesOnlyOverlappingActiveSession() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);
        final long start = System.currentTimeMillis() - 10_000L;

        accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "world", start);

        final TimeRange range = TimeRange.between(Instant.ofEpochMilli(start + 5_000L),
                Instant.ofEpochMilli(System.currentTimeMillis() + 1_000L));
        assertTrue(accumulator.getTime(PLAYER, TimeScope.world("survival", "world"), range).orElseThrow() >= 5L,
                "Expected overlapping active time to contribute to ranged total");
    }

    @Test
    void rangedTotalExcludesNonOverlappingActiveSession() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);
        final long start = System.currentTimeMillis() - 10_000L;

        accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "world", start);

        final TimeRange range = TimeRange.between(Instant.ofEpochMilli(start - 20_000L),
                Instant.ofEpochMilli(start - 10_000L));
        assertEquals(OptionalLong.empty(), accumulator.getTime(PLAYER, TimeScope.world("survival", "world"), range),
                "Expected non-overlapping active time to be excluded");
    }

    @Test
    void statisticsCheckpointAdvancesActiveSessionAndLaterLeave() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);
        final Instant observedAt = Instant.ofEpochMilli(9_000L);
        final StatisticsRequest request = new StatisticsRequest(
                TimeRange.between(Instant.EPOCH, Instant.ofEpochMilli(5_000L)), TimeScope.GLOBAL,
                Duration.ofMinutes(3), observedAt);

        accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "world", 1_000L);
        accumulator.getStatistics(request);

        assertEquals(9_000L, storage.sessions.getFirst().stoppedAtMs(),
                "Expected checkpoint at observation time, not historical range end");
        assertEquals(TimeEntryReason.AUTO_FLUSH, storage.sessions.getFirst().reason());
        assertEquals(request, storage.statisticsRequest);

        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 12_000L, TimeEntryReason.PLAYER_LEAVE);
        assertEquals(12_000L, storage.sessions.getFirst().stoppedAtMs());
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.getFirst().reason());
    }

    @Test
    void checkpointCannotOverwriteSamePlayerLeaveAndDoesNotBlockAnotherPlayer() throws Exception {
        final BlockingUnifiedStorage storage = new BlockingUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);
        final StatisticsRequest request = new StatisticsRequest(TimeRange.between(Instant.EPOCH, Instant.MAX),
                TimeScope.GLOBAL, Duration.ofMinutes(3), Instant.ofEpochMilli(9_000L));
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        accumulator.startAccumulating(PLAYER, "Lorias_", "survival", "world", 1_000L);
        final Thread checkpoint = startThread(() -> accumulator.getStatistics(request), failure);
        assertTrue(storage.autoFlushStarted.await(1, TimeUnit.SECONDS));

        final CountDownLatch otherPlayerStarted = new CountDownLatch(1);
        final Thread otherPlayer = startThread(() -> {
            accumulator.startAccumulating(OTHER_PLAYER, "Other", "creative", "plots", 1_000L);
            otherPlayerStarted.countDown();
        }, failure);
        assertTrue(otherPlayerStarted.await(1, TimeUnit.SECONDS), "Expected unrelated player not to wait for checkpoint");

        final CountDownLatch samePlayerStopped = new CountDownLatch(1);
        final Thread samePlayer = startThread(() -> {
            accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 12_000L, TimeEntryReason.PLAYER_LEAVE);
            samePlayerStopped.countDown();
        }, failure);
        assertFalse(samePlayerStopped.await(100, TimeUnit.MILLISECONDS), "Expected same player to wait for checkpoint");

        storage.releaseAutoFlush.countDown();
        checkpoint.join(1_000L);
        otherPlayer.join(1_000L);
        samePlayer.join(1_000L);

        assertNull(failure.get());
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.getFirst().reason());
        assertEquals(12_000L, storage.sessions.getFirst().stoppedAtMs());
    }

    private Thread startThread(final ThrowingAction action, final AtomicReference<Throwable> failure) {
        final Thread thread = new Thread(() -> {
            try {
                action.run();
            } catch (final StorageException ex) {
                failure.compareAndSet(null, ex);
            }
        });
        thread.start();
        return thread;
    }

    private AccumulatingTimeStorage accumulator(final FakeUnifiedStorage storage) {
        return new AccumulatingTimeStorage(mock(WrappedLogger.class), storage);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws StorageException;
    }

    private static class FakeUnifiedStorage implements UnifiedStorage, StatisticsStorage {

        protected final List<PlayerSessionChunk> sessions = new ArrayList<>();

        private final Map<UUID, Long> adjustments = new java.util.HashMap<>();

        private final List<TimeEntryReason> directWriteReasons = new ArrayList<>();

        private boolean closed;

        private StatisticsRequest statisticsRequest;

        @Override
        public Optional<UUID> getUuid(final String playerName) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getName(final UUID uniqueId) {
            return Optional.empty();
        }

        @Override
        public void setPlayerName(final UUID uniqueId, final String name) {
            // Empty
        }

        @Override
        public void setPlayerNames(final Map<UUID, String> entries) {
            // Empty
        }

        @Override
        public Set<String> getNameEntries() {
            return new HashSet<>();
        }

        @Override
        public List<RecentPlayerIdentity> getRecentPlayerIdentities(final long recentDays) {
            return List.of();
        }

        @Override
        public Set<String> getKnownServerNames() {
            return Set.of();
        }

        @Override
        public Set<String> getKnownWorldNames() {
            return Set.of();
        }

        @Override
        public Map<String, Set<String>> getKnownWorldNamesByServer() {
            return Map.of();
        }

        @Override
        public OptionalLong getTime(final UUID uniqueId) {
            return getTime(uniqueId, TimeScope.GLOBAL);
        }

        @Override
        public OptionalLong getTime(final UUID uniqueId, final TimeScope scope) {
            return OptionalLong.of(adjustments.getOrDefault(uniqueId, 0L)
                    + sessions.stream().filter(session -> session.uuid().equals(uniqueId))
                    .filter(session -> scope.matches(new PlayerSessionContext(session.uuid(), session.name(),
                            session.server(), session.world(), session.startedAtMs())))
                    .mapToLong(PlayerSessionChunk::durationSeconds).sum());
        }

        @Override
        public OptionalLong getTime(final UUID uniqueId, final TimeScope scope, final TimeRange range) {
            final long total = adjustments.getOrDefault(uniqueId, 0L)
                    + sessions.stream().filter(session -> session.uuid().equals(uniqueId))
                    .filter(session -> scope.matches(new PlayerSessionContext(session.uuid(), session.name(),
                            session.server(), session.world(), session.startedAtMs())))
                    .mapToLong(session -> range.overlapSeconds(session.startedAtMs(), session.stoppedAtMs()))
                    .sum();
            return total == 0L ? OptionalLong.empty() : OptionalLong.of(total);
        }

        @Override
        public void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason) {
            adjustments.merge(uuid, additionalTime, Long::sum);
            directWriteReasons.add(reason);
        }

        @Override
        public void addTime(final ManualTimeAdjustment adjustment) {
            adjustments.merge(adjustment.playerUuid(), adjustment.amountSeconds(), Long::sum);
            directWriteReasons.add(adjustment.reason());
        }

        @Override
        public void addTimes(final Map<UUID, Long> additionalTimes, final TimeEntryReason reason) {
            additionalTimes.forEach((uuid, time) -> addTime(uuid, time, reason));
        }

        @Override
        public void addAdjustments(final List<ManualTimeAdjustment> adjustments) {
            adjustments.forEach(this::addTime);
        }

        @Override
        public void openAfkPeriod(final UUID playerId, final String playerName, final String server,
                                  final String world, final Instant startedAt) {
            // Empty
        }

        @Override
        public void closeAfkPeriod(final UUID playerId, final Instant endedAt, final AfkPeriodEndReason reason) {
            // Empty
        }

        @Override
        public int recoverOpenAfkPeriods(final Instant endedAt) {
            return 0;
        }

        @Override
        public List<AfkPeriod> getAfkPeriods(final TimeRange range, final TimeScope scope) {
            return List.of();
        }

        @Override
        public StatisticsSnapshot getStatistics(final StatisticsRequest request) {
            statisticsRequest = request;
            return new StatisticsSnapshot(0, 0, 0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0D,
                    0, 0.0D, 0, 0, 0, Duration.ZERO, 0, 0.0D, Map.of(), Map.of(), List.of());
        }

        @Override
        public long startSession(final PlayerSessionContext context, final TimeEntryReason reason) {
            final PlayerSessionChunk session = new PlayerSessionChunk(context.uuid(), context.name(), context.server(), context.world(),
                    context.startedAtMs(), context.startedAtMs(), reason);
            sessions.add(session);
            return sessions.size() - 1L;
        }

        @Override
        public void updateSession(final long sessionId, final long stoppedAtMs, final TimeEntryReason reason)
                throws StorageException {
            final int index = Math.toIntExact(sessionId);
            final PlayerSessionChunk previous = sessions.get(index);
            sessions.set(index, new PlayerSessionChunk(previous.uuid(), previous.name(), previous.server(), previous.world(),
                    previous.startedAtMs(), stoppedAtMs, reason));
        }

        @Override
        public void updateSessionWorld(final long sessionId, final String server, final String world) {
            final int index = Math.toIntExact(sessionId);
            final PlayerSessionChunk previous = sessions.get(index);
            sessions.set(index, new PlayerSessionChunk(previous.uuid(), previous.name(), server, world,
                    previous.startedAtMs(), previous.stoppedAtMs(), previous.reason()));
        }

        @Override
        public void persistSession(final PlayerSessionChunk session) {
            sessions.add(session);
        }

        @Override
        public Map<String, ?> getAllTimeEntries() {
            return Map.of();
        }

        @Override
        public void deletePlayer(final UUID uniqueId) throws SQLException {
            adjustments.remove(uniqueId);
        }

        @Override
        public int deleteInactiveHistory(final long inactiveDays) {
            return 0;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class BlockingUnifiedStorage extends FakeUnifiedStorage {
        private final CountDownLatch autoFlushStarted = new CountDownLatch(1);

        private final CountDownLatch releaseAutoFlush = new CountDownLatch(1);

        @Override
        public void updateSession(final long sessionId, final long stoppedAtMs, final TimeEntryReason reason)
                throws StorageException {
            if (reason == TimeEntryReason.AUTO_FLUSH) {
                autoFlushStarted.countDown();
                try {
                    if (!releaseAutoFlush.await(1, TimeUnit.SECONDS)) {
                        throw new StorageException("Timed out waiting to release checkpoint");
                    }
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new StorageException(ex);
                }
            }
            super.updateSession(sessionId, stoppedAtMs, reason);
        }
    }
}

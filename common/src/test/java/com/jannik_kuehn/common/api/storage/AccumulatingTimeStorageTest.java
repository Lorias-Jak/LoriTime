package com.jannik_kuehn.common.api.storage;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.exception.StorageException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"PMD.CloseResource", "PMD.UncommentedEmptyMethodBody",
        "PMD.UnitTestAssertionsShouldIncludeMessage", "PMD.UnitTestContainsTooManyAsserts"})
class AccumulatingTimeStorageTest {

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void stopPersistsSingleSessionChunkWithContext() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 1_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 6_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size());
        final PlayerSessionChunk chunk = storage.sessions.getFirst();
        assertEquals(PLAYER, chunk.uuid());
        assertEquals(Optional.of("Lorias_"), chunk.name());
        assertEquals("lobby", chunk.server());
        assertEquals("spawn", chunk.world());
        assertEquals(1_000L, chunk.startedAtMs());
        assertEquals(6_000L, chunk.stoppedAtMs());
        assertEquals(5L, chunk.durationSeconds());
        assertEquals(TimeEntryReason.PLAYER_LEAVE, chunk.reason());
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

        assertEquals(1, storage.sessions.size());
        assertEquals(TimeEntryReason.PLAYER_LEAVE, storage.sessions.get(0).reason());
        assertTrue(storage.sessions.get(0).durationSeconds() >= 6L);
    }

    @Test
    void closePersistsShutdownFlush() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", System.currentTimeMillis() - 3_000L);
        accumulator.close();

        assertEquals(1, storage.sessions.size());
        assertEquals(TimeEntryReason.SHUTDOWN_FLUSH, storage.sessions.getFirst().reason());
        assertTrue(storage.closed);
    }

    @Test
    void manualAdjustmentForOnlinePlayerAdjustsActiveSession() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 10_000L);
        accumulator.addTime(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 20_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size());
        assertEquals(10L, storage.sessions.getFirst().durationSeconds());
        assertEquals(5L, storage.adjustments.get(PLAYER));
    }

    @Test
    void manualAdjustmentForOfflinePlayerWritesThrough() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.addTime(PLAYER, 5L, TimeEntryReason.MANUAL_ADJUSTMENT);

        assertEquals(5L, storage.adjustments.get(PLAYER));
        assertEquals(TimeEntryReason.MANUAL_ADJUSTMENT, storage.directWriteReasons.getFirst());
    }

    @Test
    void contextSwitchPersistsPreviousContextAndTracksNext() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 1_000L);
        accumulator.switchContext(PLAYER, "Lorias_", "survival", "nether", 4_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(2, storage.sessions.size());
        assertEquals("lobby", storage.sessions.get(0).server());
        assertEquals("spawn", storage.sessions.get(0).world());
        assertEquals(TimeEntryReason.CONTEXT_SWITCH, storage.sessions.get(0).reason());
        assertEquals(3L, storage.sessions.get(0).durationSeconds());
        assertEquals("survival", storage.sessions.get(1).server());
        assertEquals("nether", storage.sessions.get(1).world());
        assertEquals(5L, storage.sessions.get(1).durationSeconds());
    }

    @Test
    void duplicateContextSwitchDoesNotCreateNewSession() throws StorageException {
        final FakeUnifiedStorage storage = new FakeUnifiedStorage();
        final AccumulatingTimeStorage accumulator = accumulator(storage);

        accumulator.startAccumulating(PLAYER, "Lorias_", "lobby", "spawn", 1_000L);
        accumulator.switchContext(PLAYER, "Lorias_", "lobby", "spawn", 4_000L);
        accumulator.stopAccumulatingAndSaveOnlineTime(PLAYER, 9_000L, TimeEntryReason.PLAYER_LEAVE);

        assertEquals(1, storage.sessions.size());
        assertEquals("lobby", storage.sessions.getFirst().server());
        assertEquals("spawn", storage.sessions.getFirst().world());
        assertEquals(8L, storage.sessions.getFirst().durationSeconds());
    }

    private AccumulatingTimeStorage accumulator(final FakeUnifiedStorage storage) {
        return new AccumulatingTimeStorage(mock(WrappedLogger.class), storage);
    }

    private static final class FakeUnifiedStorage implements UnifiedStorage {

        private final List<PlayerSessionChunk> sessions = new ArrayList<>();

        private final Map<UUID, Long> adjustments = new java.util.HashMap<>();

        private final List<TimeEntryReason> directWriteReasons = new ArrayList<>();

        private boolean closed;

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
        }

        @Override
        public void setPlayerNames(final Map<UUID, String> entries) {
        }

        @Override
        public Set<String> getNameEntries() {
            return new HashSet<>();
        }

        @Override
        public OptionalLong getTime(final UUID uniqueId) {
            return OptionalLong.of(adjustments.getOrDefault(uniqueId, 0L)
                    + sessions.stream().filter(session -> session.uuid().equals(uniqueId))
                    .mapToLong(PlayerSessionChunk::durationSeconds).sum());
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
        public long startSession(final PlayerSessionContext context, final TimeEntryReason reason) {
            final PlayerSessionChunk session = new PlayerSessionChunk(context.uuid(), context.name(), context.server(), context.world(),
                    context.startedAtMs(), context.startedAtMs(), reason);
            sessions.add(session);
            return sessions.size() - 1L;
        }

        @Override
        public void updateSession(final long sessionId, final long stoppedAtMs, final TimeEntryReason reason) {
            final int index = Math.toIntExact(sessionId);
            final PlayerSessionChunk previous = sessions.get(index);
            sessions.set(index, new PlayerSessionChunk(previous.uuid(), previous.name(), previous.server(), previous.world(),
                    previous.startedAtMs(), stoppedAtMs, reason));
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
}

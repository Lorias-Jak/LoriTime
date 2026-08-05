package com.jannik_kuehn.common.storage.contract;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.model.AfkPeriod;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.PersistedPlayerSession;
import com.jannik_kuehn.common.storage.model.PlayerSessionChunk;
import com.jannik_kuehn.common.storage.model.PlayerSessionContext;
import com.jannik_kuehn.common.storage.model.RecentPlayerIdentity;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
import com.jannik_kuehn.common.storage.model.StatisticsSnapshot;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Unified storage decorator that keeps active sessions in memory while persisting session rows.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
public class AccumulatingTimeStorage implements UnifiedStorage, TimeAccumulator, StatisticsStorage {
    /**
     * Number of bounded per-player lock stripes.
     */
    private static final int SESSION_LOCK_STRIPES = 64;

    /**
     * Logger for accumulator operations.
     */
    private final WrappedLogger log;

    /**
     * Backing storage that owns persistence.
     */
    private final UnifiedStorage storage;

    /**
     * Active persisted sessions keyed by player UUID.
     */
    private final ConcurrentMap<UUID, PersistedPlayerSession> onlineSessions = new ConcurrentHashMap<>();

    /**
     * Bounded locks that serialize persistence for a single player.
     */
    private final ReentrantLock[] sessionLocks = createSessionLocks();

    /**
     * Creates a new accumulating storage wrapper.
     *
     * @param log         the logger.
     * @param timeStorage the backing storage.
     */
    public AccumulatingTimeStorage(final WrappedLogger log, final UnifiedStorage timeStorage) {
        this.log = log;
        this.storage = Objects.requireNonNull(timeStorage);
    }

    @Override
    public Optional<UUID> getUuid(final String playerName) throws StorageException {
        return storage.getUuid(playerName);
    }

    @Override
    public Optional<String> getName(final UUID uniqueId) throws StorageException {
        return storage.getName(uniqueId);
    }

    @Override
    public void setPlayerName(final UUID uniqueId, final String name) throws StorageException {
        storage.setPlayerName(uniqueId, name);
    }

    @Override
    public void setPlayerNames(final Map<UUID, String> entries) throws StorageException {
        storage.setPlayerNames(entries);
    }

    @Override
    public Set<String> getNameEntries() throws StorageException {
        return storage.getNameEntries();
    }

    @Override
    public List<RecentPlayerIdentity> getRecentPlayerIdentities(final long recentDays) throws StorageException {
        return storage.getRecentPlayerIdentities(recentDays);
    }

    @Override
    public Set<String> getKnownServerNames() throws StorageException {
        return storage.getKnownServerNames();
    }

    @Override
    public Set<String> getKnownWorldNames() throws StorageException {
        return storage.getKnownWorldNames();
    }

    @Override
    public Map<String, Set<String>> getKnownWorldNamesByServer() throws StorageException {
        return storage.getKnownWorldNamesByServer();
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId) throws StorageException {
        return getTime(uniqueId, TimeScope.GLOBAL);
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId, final TimeScope scope) throws StorageException {
        Objects.requireNonNull(scope, "scope");
        final PersistedPlayerSession activeSession = onlineSessions.get(uniqueId);
        final PlayerSessionContext context = activeSession == null ? null : activeSession.context();
        if (context != null && scope.matches(context)) {
            final long accumulatedTime = (System.currentTimeMillis() - activeSession.lastPersistedAtMs()) / 1000L;
            final long storedTime = storage.getTime(uniqueId, scope).orElse(0);
            return OptionalLong.of(accumulatedTime + storedTime);
        } else {
            return storage.getTime(uniqueId, scope);
        }
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId, final TimeScope scope, final TimeRange range) throws StorageException {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(range, "range");
        final OptionalLong storedTime = storage.getTime(uniqueId, scope, range);
        final PersistedPlayerSession activeSession = onlineSessions.get(uniqueId);
        final PlayerSessionContext context = activeSession == null ? null : activeSession.context();
        if (context != null && scope.matches(context)) {
            final long accumulatedTime = range.overlapSeconds(activeSession.lastPersistedAtMs(), System.currentTimeMillis());
            if (accumulatedTime > 0L || storedTime.isPresent()) {
                return OptionalLong.of(accumulatedTime + storedTime.orElse(0L));
            }
        }
        return storedTime;
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason) throws StorageException {
        storage.addTime(uuid, additionalTime, reason);
    }

    @Override
    public void addTime(final ManualTimeAdjustment adjustment) throws StorageException {
        storage.addTime(adjustment);
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes, final TimeEntryReason reason) throws StorageException {
        storage.addTimes(additionalTimes, reason);
    }

    @Override
    public void addAdjustments(final List<ManualTimeAdjustment> adjustments) throws StorageException {
        storage.addAdjustments(adjustments);
    }

    @Override
    public void openAfkPeriod(final UUID playerId, final String playerName, final String server, final String world,
                              final Instant startedAt) throws StorageException {
        statisticsStorage().openAfkPeriod(playerId, playerName, server, world, startedAt);
    }

    @Override
    public void closeAfkPeriod(final UUID playerId, final Instant endedAt, final AfkPeriodEndReason reason)
            throws StorageException {
        statisticsStorage().closeAfkPeriod(playerId, endedAt, reason);
    }

    @Override
    public int recoverOpenAfkPeriods(final Instant endedAt) throws StorageException {
        return statisticsStorage().recoverOpenAfkPeriods(endedAt);
    }

    @Override
    public List<AfkPeriod> getAfkPeriods(final TimeRange range, final TimeScope scope) throws StorageException {
        return statisticsStorage().getAfkPeriods(range, scope);
    }

    @Override
    public StatisticsSnapshot getStatistics(final StatisticsRequest request) throws StorageException {
        checkpointActiveSessions(request.observedAt().toEpochMilli());
        return statisticsStorage().getStatistics(request);
    }

    private StatisticsStorage statisticsStorage() throws StorageException {
        if (storage instanceof final StatisticsStorage statistics) {
            return statistics;
        }
        throw new StorageException("Statistics are not supported by the active storage");
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        return storage.getAllTimeEntries();
    }

    @Override
    public long startSession(final PlayerSessionContext context, final TimeEntryReason reason) throws StorageException {
        return storage.startSession(context, reason);
    }

    @Override
    public void updateSession(final long sessionId, final long stoppedAtMs, final TimeEntryReason reason) throws StorageException {
        storage.updateSession(sessionId, stoppedAtMs, reason);
    }

    @Override
    public void updateSessionWorld(final long sessionId, final String server, final String world) throws StorageException {
        storage.updateSessionWorld(sessionId, server, world);
    }

    @Override
    public void deletePlayer(final UUID uniqueId) throws StorageException {
        onlineSessions.remove(uniqueId);
        try {
            storage.deletePlayer(uniqueId);
        } catch (final java.sql.SQLException ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public int deleteInactiveHistory(final long inactiveDays) throws StorageException {
        return storage.deleteInactiveHistory(inactiveDays);
    }

    @Override
    public void persistSession(final PlayerSessionChunk session) throws StorageException {
        storage.persistSession(session);
    }

    @Override
    public void startAccumulating(final UUID uuid, final String name, final String server,
                                  final String world, final long when)
            throws StorageException {
        withSessionLock(uuid, () -> {
            final PlayerSessionContext context = new PlayerSessionContext(uuid, name, server, world, when);
            final long sessionId = storage.startSession(context, TimeEntryReason.PLAYER_JOIN);
            final PersistedPlayerSession previous = onlineSessions.put(uuid, new PersistedPlayerSession(sessionId, context, when));
            if (previous != null) {
                storage.updateSession(previous.sessionId(), when, switchReason(previous.context(), context));
            }
        });
    }

    @Override
    public void stopAccumulatingAndSaveOnlineTime(final UUID uuid, final long when,
                                                  final TimeEntryReason reason)
            throws StorageException {
        withSessionLock(uuid, () -> {
            final PersistedPlayerSession session = onlineSessions.remove(uuid);
            if (session != null) {
                storage.updateSession(session.sessionId(), when, reason);
            }
        });
    }

    @Override
    public void switchContext(final UUID uuid, final String name, final String server,
                              final String world, final long when)
            throws StorageException {
        withSessionLock(uuid, () -> {
            final PlayerSessionContext next = new PlayerSessionContext(uuid, name, server, world, when);
            final PersistedPlayerSession current = onlineSessions.get(uuid);
            if (current != null
                    && current.context().server().equals(server)
                    && current.context().world().equals(world)) {
                return;
            }
            final long sessionId = storage.startSession(next, TimeEntryReason.PLAYER_JOIN);
            final PersistedPlayerSession previous = onlineSessions.put(uuid, new PersistedPlayerSession(sessionId, next, when));
            if (previous != null) {
                storage.updateSession(previous.sessionId(), when, switchReason(previous.context(), next));
            }
        });
    }

    @Override
    public void updateWorldContext(final UUID uuid, final String world, final long observedAtMs)
            throws StorageException {
        withSessionLock(uuid, () -> {
            final PersistedPlayerSession current = onlineSessions.get(uuid);
            if (current == null || current.context().world().equals(world) || observedAtMs < current.context().startedAtMs()) {
                return;
            }
            final PlayerSessionContext previous = current.context();
            final PlayerSessionContext updated = new PlayerSessionContext(previous.uuid(), previous.name(),
                    previous.server(), world, previous.startedAtMs());
            if (onlineSessions.replace(uuid, current,
                    new PersistedPlayerSession(current.sessionId(), updated, current.lastPersistedAtMs()))) {
                storage.updateSessionWorld(current.sessionId(), updated.server(), updated.world());
            }
        });
    }

    @Override
    public void switchWorldContext(final UUID uuid, final String world, final long observedAtMs)
            throws StorageException {
        withSessionLock(uuid, () -> {
            final PersistedPlayerSession current = onlineSessions.get(uuid);
            if (current == null || current.context().world().equals(world) || observedAtMs < current.context().startedAtMs()) {
                return;
            }
            final PlayerSessionContext previous = current.context();
            final PlayerSessionContext next = new PlayerSessionContext(previous.uuid(), previous.name(),
                    previous.server(), world, observedAtMs);
            final long sessionId = storage.startSession(next, TimeEntryReason.PLAYER_JOIN);
            if (onlineSessions.replace(uuid, current, new PersistedPlayerSession(sessionId, next, observedAtMs))) {
                storage.updateSession(current.sessionId(), observedAtMs, TimeEntryReason.WORLD_SWITCH);
            }
        });
    }

    @Override
    public void flushOnlineTimeCache() throws StorageException {
        if (onlineSessions.isEmpty()) {
            return;
        }
        log.debug("Flushing online time cache");
        checkpointActiveSessions(System.currentTimeMillis());
    }

    private void checkpointActiveSessions(final long observedAtMs) throws StorageException {
        for (final Map.Entry<UUID, PersistedPlayerSession> entry : onlineSessions.entrySet()) {
            final UUID uuid = entry.getKey();
            withSessionLock(uuid, () -> checkpointActiveSession(uuid, observedAtMs));
        }
    }

    @Override
    public Optional<PlayerSessionContext> getActiveSessionContext(final UUID uuid) {
        final PersistedPlayerSession session = onlineSessions.get(uuid);
        return session == null ? Optional.empty() : Optional.of(session.context());
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
    public void close() throws StorageException {
        try {
            if (!onlineSessions.isEmpty()) {
                final long now = System.currentTimeMillis();
                for (final UUID uuid : List.copyOf(onlineSessions.keySet())) {
                    withSessionLock(uuid, () -> {
                        final PersistedPlayerSession session = onlineSessions.remove(uuid);
                        if (session != null) {
                            storage.updateSession(session.sessionId(), now, TimeEntryReason.SHUTDOWN_FLUSH);
                        }
                    });
                }
            }
        } finally {
            this.storage.close();
        }
    }

    private TimeEntryReason switchReason(final PlayerSessionContext previous, final PlayerSessionContext next) {
        if (!previous.server().equals(next.server())) {
            return TimeEntryReason.SERVER_SWITCH;
        }
        return TimeEntryReason.WORLD_SWITCH;
    }

    private void checkpointActiveSession(final UUID uuid, final long observedAtMs) throws StorageException {
        final PersistedPlayerSession current = onlineSessions.get(uuid);
        if (current != null && observedAtMs > current.lastPersistedAtMs()
                && onlineSessions.replace(uuid, current,
                new PersistedPlayerSession(current.sessionId(), current.context(), observedAtMs))) {
            storage.updateSession(current.sessionId(), observedAtMs, TimeEntryReason.AUTO_FLUSH);
        }
    }

    private void withSessionLock(final UUID uuid, final StorageAction action) throws StorageException {
        final ReentrantLock lock = sessionLocks[(uuid.hashCode() & Integer.MAX_VALUE) % SESSION_LOCK_STRIPES];
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock[] createSessionLocks() {
        final ReentrantLock[] locks = new ReentrantLock[SESSION_LOCK_STRIPES];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }

    /**
     * Action that may perform a storage operation while holding a session lock.
     */
    @FunctionalInterface
    private interface StorageAction {
        /**
         * Performs the storage operation.
         */
        void run() throws StorageException;
    }
}
